package playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.LoggerUtil;
import ScripTrading.MinuteData;
import ScripTrading.Util;

public class QQQMasterShortTester {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		Map<String, Double> allPutCasesVolume = Util.deserializeDoubleHashMap("config/QQQPutCasesVolume.txt");
		Map<String, Double> allPutCasesAvgVolume = Util.deserializeDoubleHashMap("config/QQQPutCasesAvgVolume.txt");
		Map<String, Double> allPutCasesStrike = Util.deserializeDoubleHashMap("config/QQQPutCases.txt");
		Map<String, Double> vixAvgMap = Util.deserializeDoubleHashMap("config/VIXAvgMap.txt");
		Map<String, Map<String, MinuteData>> vixRawData = Util.readVixRawData("config/VIX_full_5min.txt");
		Map<String, Map<String, Double>> volumeDataMap = UtilRun.readVolumeData("config/QQQAvgVolume.txt");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-07-01");
		Date endDate = sdf.parse("2023-10-07");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		LinkedList<String> putVolumeSignal = new LinkedList<>();
		Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>();
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		LinkedList<Double> volatilityQueue = new LinkedList<>();
		
		int shortSignalCases = 0;
		double sumbookedPositionProfit = 0.0;
		double sumSecondProfitPcnt = 0.0;
		List<String> stopLossHit = new ArrayList<>();
		Map<String, Double> shortBokkedProfitMap = new LinkedHashMap<>();
		int secondproblemCases = 0;
		
		Date currentDate = startDate;
		while (currentDate.before(endDate)) {
			downloadedMoreData = false;
			calendar.setTime(currentDate);
			String currentDateString = sdf.format(currentDate);
	        // which day of the week, get the price of underlying stock/etf
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			if (dayOfWeek == 1 || dayOfWeek == 7) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			if (!dayDataMap.containsKey(currentDateString)) {
				//System.out.println("Unexpected Data missing " + currentDateString);
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			DayData dayData = dayDataMap.get(currentDateString);
			
			if (currentDateString.compareTo("2022-12-01") >= 0 && !currentDateString.equals("2023-07-03")
					// && currentDateString.equals("2023-02-08")
					) {
				double closeAtStartTimeFloor = (int) dayData.getMinuteDataMap().get(VolumeGraphPatternEntry.startTime).getClosePrice();
				downloadedMoreData = downloadOptionData(closeAtStartTimeFloor, currentDateString, dayData, downloader, downloadedMoreData);
				downloadedMoreData = downloadOptionData(closeAtStartTimeFloor + 1, currentDateString, dayData, downloader, downloadedMoreData);
				double sumCallPriceAtStartTime = dayData.getCallDataMap().get(closeAtStartTimeFloor).get(VolumeGraphPatternEntry.startTime).getClosePrice() 
						                         + dayData.getCallDataMap().get(closeAtStartTimeFloor + 1).get(VolumeGraphPatternEntry.startTime).getClosePrice();
				double sumPutPriceAtStartTime = dayData.getPutDataMap().get(closeAtStartTimeFloor).get(VolumeGraphPatternEntry.startTime).getClosePrice() 
	                                             + dayData.getPutDataMap().get(closeAtStartTimeFloor + 1).get(VolumeGraphPatternEntry.startTime).getClosePrice();
				double totalOptionPriceAtStartTime = (sumCallPriceAtStartTime + sumPutPriceAtStartTime);
				double avgVolatility = (volatilityQueue.size() > 0) ? findAvg(volatilityQueue) : 0;
				
				if (avgVolatility > 0 && totalOptionPriceAtStartTime >= (1.66 * avgVolatility)) {
					calendar.add(Calendar.DATE, 1);
			        currentDate = calendar.getTime();
			        //System.out.println(currentDateString + " Did not process as avgVolatility high " + totalOptionPriceAtStartTime + "  " + avgVolatility);
			        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
					continue;
				}
				volatilityQueue.add(totalOptionPriceAtStartTime);
				if (volatilityQueue.size() > 30) {
					volatilityQueue.poll();
				}
				
				String time = VolumeGraphPatternEntry.startTime;
				double shortEnterPrice = 0.0; String shortEnterTime = null; String shortEnterString = "";
				int noOfEntriesForBear = 0;
				putVolumeSignal = new LinkedList<>();
				LinkedList<String> alternatePutVolumeSignal = new LinkedList<>();
				LinkedList<Double> putOptionQueue = new LinkedList<>();
				LinkedList<Double> alternatePutOptionQueue = new LinkedList<>();
				String strikeTime = null; double strike = 0;
				double avgVix = vixAvgMap.get(currentDateString);
				Map<String, MinuteData> rawVix = vixRawData.get(currentDateString);
				double alternateStrike = ((int) (dayData.getMinuteDataMap().get("07:20").getClosePrice() - 0.009 * dayData.getMinuteDataMap().get("07:20").getClosePrice())) + 1;
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					//double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					String key = currentDateString + "  " + time;
					double putVolume = 0; double putAvgVolume = 0;
					double alternateoptionVolumeAtTime = 0; double alternatePutAvgVolume = 0;
					if (time.compareTo("07:20") >= 0) {
						downloadedMoreData = downloadOptionData(alternateStrike, currentDateString, dayData, downloader, downloadedMoreData);
						if (dayData.getPutDataMap().get(alternateStrike).containsKey(time)) {
							alternateoptionVolumeAtTime = dayData.getPutDataMap().get(alternateStrike).get(time).getVolume();
						}
						
						alternatePutAvgVolume = (alternatePutOptionQueue.size() > 0) ? findAvg(alternatePutOptionQueue) : 0;
						if (alternatePutAvgVolume > 0 && alternateoptionVolumeAtTime > (2.8 * alternatePutAvgVolume) ) {
							alternatePutVolumeSignal.add(time);
						}
						if (alternateoptionVolumeAtTime > 0) {
							alternatePutOptionQueue.add(alternateoptionVolumeAtTime);
							if (alternatePutOptionQueue.size() > 9) {
								alternatePutOptionQueue.poll();
							}
						}
					}
					if (allPutCasesStrike.containsKey(key)) {
						if (strikeTime == null) {
							strikeTime = time;
						}
						if (strike == 0) {
							strike = allPutCasesStrike.get(key);
						}
					}
					
					if (strikeTime != null && strike > 0) {
						downloadedMoreData = downloadOptionData(strike, currentDateString, dayData, downloader, downloadedMoreData);
						if (dayData.getPutDataMap().get(strike).containsKey(time)) {
							putVolume = dayData.getPutDataMap().get(strike).get(time).getVolume();
						}
						
						putAvgVolume = (putOptionQueue.size() > 0) ? findAvg(putOptionQueue) : 0;
						if (putAvgVolume > 0 && putVolume > (2.8 * putAvgVolume) ) {
							putVolumeSignal.add(time);
						}
						if (putVolume > 0) {
							putOptionQueue.add(putVolume);
							if (putOptionQueue.size() > 9) {
								putOptionQueue.poll();
							}
						}
					}
					
					// Short Enter
					String returnedTime = VolumeGraphPatternEntry.bearEntry(dayData, 0.3, time, putVolumeSignal, alternatePutVolumeSignal, strike, avgVix, rawVix, alternateStrike);
					if (returnedTime != null
							&& shortEnterPrice == 0
							&& noOfEntriesForBear == 0) {
						shortSignalCases++;
						shortEnterPrice = dayData.getMinuteDataMap().get(returnedTime).getClosePrice();
						shortEnterTime = returnedTime; time = returnedTime;
						shortEnterString = "bearEntry";
						noOfEntriesForBear++;
						//System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + closeAtTime + "  " + dayData.getMinuteDataMap().get(time).getVolume());
						//continue;
					}
					
					// Short Exit
					if (shortEnterPrice > 0) {
						time = VolumeGraphPatternEntry.shortExit(dayData, 0.3, shortEnterTime, shortEnterPrice, currentDateString, shortBokkedProfitMap, shortEnterString, strike, strikeTime
								);
						shortEnterPrice = 0;
						shortEnterTime = null;
						shortEnterString = "";
					}
					
					time = Util.timeNMinsAgo(time, -5);
				}
			}
			
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (!volumeMap.containsKey(time)) {
					volumeMap.put(time, new LinkedList<>());
				}
				LinkedList<Double> volumeQueue = volumeMap.get(time);
				if (dayData.getMinuteDataMap().containsKey(time)) {
					volumeQueue.add(dayData.getMinuteDataMap().get(time).getVolume());
				}
				if (volumeQueue.size() > 30) {
					volumeQueue.poll();
				}
			}
			
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		Map<String, String> masterShortResults = readResults("config/QQQMasterShortResult.txt");
		
		Map<String, String> currentShortResults = new LinkedHashMap<>();
		
		sumbookedPositionProfit = 0; sumSecondProfitPcnt = 0; secondproblemCases = 0;
		for (String key : shortBokkedProfitMap.keySet()) {
			double profit = shortBokkedProfitMap.get(key);
			if (profit >= 0) {
				sumbookedPositionProfit = sumbookedPositionProfit + profit;
			} else {
				sumSecondProfitPcnt = sumSecondProfitPcnt + profit;
				secondproblemCases++;
			}
			String row = key + "  " + shortBokkedProfitMap.get(key);
			System.out.println(row);
			String[] rowVals = row.split("  ");
			currentShortResults.put(rowVals[0], row);
		}
		System.out.println(shortSignalCases);
		System.out.println("Loss due to position " + sumSecondProfitPcnt + "  " + secondproblemCases + " Stop loss hit exit " + stopLossHit);
		System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (shortSignalCases - secondproblemCases));
		
		System.out.println("Diff View for Short - Entries missing");
		for (String key : masterShortResults.keySet()) {
			if (!currentShortResults.containsKey(key)) {
				System.out.println(masterShortResults.get(key));
			}
		}
		System.out.println("Diff View for Short - Entries changed");
		for (String key : masterShortResults.keySet()) {
			if (currentShortResults.containsKey(key) && !masterShortResults.get(key).equals(currentShortResults.get(key)) ) {
				System.out.println(currentShortResults.get(key));
			}
		}
		System.out.println("Diff View for Short - New Entries");
		for (String key : currentShortResults.keySet()) {
			if (!masterShortResults.containsKey(key)) {
				System.out.println(currentShortResults.get(key));
			}
		}
	}
	
	private static Map<String, String> readResults(String path){
		InputStreamReader is = null;
		BufferedReader br = null;
		Map<String, String> results = new LinkedHashMap<>();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File(path)));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				results.put(linsVals[0], line);
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
			//System.exit(1);
		}finally{
			 try {
				br.close();
				is.close();
				
			} catch (Exception e) {}
		}
		return results;
	}
	
	private static boolean downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader, boolean downloadedMoreData) {
		if (!dayData.getPutDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"P00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (dayData.getPutDataMap() == null) {
				Map<Double, Map<String, MinuteData>> putMap = new LinkedHashMap<>();
				dayData.setPutDataMap(putMap);
			}
			dayData.getPutDataMap().put(strikePrice, mMap);
			downloadedMoreData = true;
		}
		
		return downloadedMoreData;
	}
	
	private static double findAvg(LinkedList<Double> queue) {
		double sum = 0;
		Iterator<Double> queueIterator = queue.iterator();
		int queueCount = 0;
		while (queueIterator.hasNext()) {
			double vol = queueIterator.next();
			sum = sum + vol;
			queueCount++;
		}
		
		double avg = (sum / queueCount);
		return avg;
	}

}

/*

2022-12-05  10:20  12:35  bearEntry  288.0  07:50  0.41490174293451604
2022-12-15  08:05  12:35  bearEntry  278.0  07:20  0.28432607522045006
2022-12-16  08:45  12:20  bearEntry  273.0  08:00  -0.4576072631424805
2022-12-19  10:55  12:35  bearEntry  270.0  07:25  0.4445965825967403
2022-12-22  09:20  11:15  bearEntry  264.0  08:10  -0.030283529545362484
2022-12-28  07:50  12:35  bearEntry  263.0  07:20  0.7929549006900173
2023-01-03  08:30  11:50  bearEntry  263.0  08:00  -0.6541283514529036
2023-01-18  07:50  12:35  bearEntry  281.0  07:25  0.8514428215176296
2023-01-19  07:55  12:35  bearEntry  274.0  07:20  -0.6356202305716749
2023-01-26  07:50  11:45  bearEntry  289.0  07:20  -1.0026275757156609
2023-02-08  07:55  12:35  bearEntry  306.0  07:20  0.4620964579793471
2023-02-09  08:00  12:35  bearEntry  306.0  07:20  1.5772478887233057
2023-02-27  07:45  12:35  bearEntry  294.0  07:20  0.3060295636106769
2023-03-01  08:05  12:35  bearEntry  291.0  07:50  -0.10495707083740255
2023-03-09  09:00  12:35  bearEntry  298.0  07:45  1.7787157069641062
2023-03-10  10:20  12:35  bearEntry  289.0  07:20  0.38730202641953265
2023-03-23  11:25  12:35  bearEntry  309.0  07:20  -0.03561253561254003
2023-03-27  07:55  12:35  bearEntry  309.0  07:25  0.3127317277622037
2023-03-28  10:50  12:35  bearEntry  305.0  07:20  -0.43922905467419426
2023-04-03  07:50  12:35  bearEntry  318.0  07:30  -0.4448633364750225
2023-04-04  08:05  12:35  bearEntry  318.0  07:20  0.2880310572618314
2023-04-12  07:50  12:35  bearEntry  314.0  07:25  0.5458238131505545
2023-04-17  08:25  12:30  bearEntry  316.0  07:25  -0.41624621594348926
2023-04-24  08:10  12:35  bearEntry  315.0  07:25  -0.34792831723435763
2023-04-25  08:35  12:35  bearEntry  311.0  07:50  0.763505095358268
2023-05-10  07:50  12:35  bearEntry  323.0  07:35  -0.8290873383547479
2023-05-12  09:45  12:35  bearEntry  323.0  07:30  -0.38983429954037935
2023-05-23  11:20  12:35  bearEntry  333.0  09:45  -0.1710735616314995
2023-05-31  08:15  12:35  bearEntry  345.0  08:00  -0.3746073826470367
2023-06-09  07:45  12:35  bearEntry  355.0  07:20  0.16007839195131718
2023-06-16  08:45  12:35  bearEntry  367.0  07:20  0.1326259946949473
2023-06-21  08:05  11:20  bearEntry  361.0  07:35  -0.7001909611712367
2023-06-26  08:00  12:35  bearEntry  361.0  07:30  0.7868363656442301
2023-06-29  07:45  12:35  bearEntry  360.0  07:35  -0.1640564685121789
2023-07-10  09:00  12:35  bearEntry  364.0  07:25  -0.5328352879782465
2023-07-12  08:35  12:35  bearEntry  371.0  07:25  -0.32809861728523937
2023-07-14  10:00  12:35  bearEntry  380.0  07:20  0.37234882374610906
2023-07-20  11:15  12:35  bearEntry  378.0  08:05  0.34124275851121966
2023-07-26  07:50  08:55  bearEntry  375.0  07:20  -0.14625029389534525
2023-07-27  10:35  12:35  bearEntry  380.0  07:50  0.9929376487762477
2023-08-10  08:00  12:35  bearEntry  371.0  07:20  0.7088948787061983
2023-08-16  09:25  12:35  bearEntry  364.0  07:20  0.28829521429943017
2023-08-22  09:10  12:35  bearEntry  363.0  07:20  0.008265145879832922
2023-08-28  07:45  10:10  bearEntry  364.0  07:25  -0.3584817885778404
2023-09-01  07:45  12:30  bearEntry  376.0  07:20  0.007947019867542442
2023-09-06  08:00  12:35  bearEntry  0.0  null  0.010686044026506856
2023-09-08  07:50  12:35  bearEntry  372.0  07:25  0.39509281332868135
2023-09-15  07:50  12:35  bearEntry  370.0  07:40  0.33088531999032045
2023-09-20  07:55  12:35  bearEntry  367.0  07:45  1.1242491476811443
2023-09-27  09:00  11:40  bearEntry  353.0  07:30  -0.5268375584194912
2023-09-29  08:35  12:00  bearEntry  359.0  07:20  0.18201269920665517
51
Loss due to position -9.090457039218329  22 Stop loss hit exit []
Booked Profit from position exit 15.052103726499562  29


2022-12-15  278.0  07:20  0.2914428929466523  0.447547440028643
2022-12-28  263.0  07:20  0.1831875465775154  1.139244866112234
2023-01-18  281.0  07:25  0.07754670426506875  0.8847373986605569
2023-02-03  308.0  07:25  0.24135260712441925  0.44527620030975734
2023-02-08  306.0  07:20  0.14292210745143896  0.535957902942896
2023-02-09  306.0  07:20  0.21462709680104816  1.2324798437514735
2023-03-09  298.0  07:45  0.07684886390099471  1.5868791376958649
2023-06-26  361.0  07:30  0.05011836024614968  0.7320968602677548
2023-07-27  380.0  07:50  0.06045854271356784  0.9291247906197654
2023-08-10  371.0  07:20  0.09713024282560707  0.6073984881931902
2023-09-20  367.0  07:45  0.11901219875037193  0.4733439723026156
 
 */


