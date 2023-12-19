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
	
	private static double getTargetedStrike(DayData dayData, String time) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = (time.compareTo("08:45") <= 0) ? 0.009 : 0.009;
		double priceLevelToPlaceOrder = (((closeAtTime - openAtTime) / openAtTime) * 100 > 0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
		
		return targetedStrikePrice;
	}
	
	private static void calculateOptionVolumeSig(LinkedList<Double> alternateOptionQueue, LinkedList<String> altVolumeSignal, double altStrike, String time, DayData dayData) {
		String timeCnr = "07:20";
		while (timeCnr.compareTo(time) <= 0) {
			double alternateoptionVolumeAtTime = 0;
			if (dayData.getPutDataMap().get(altStrike).containsKey(timeCnr)) {
				alternateoptionVolumeAtTime = dayData.getPutDataMap().get(altStrike).get(timeCnr).getVolume();
			}
			
			double alternateCallAvgVolume = 0;
			Iterator<Double> queueIterator = alternateOptionQueue.iterator();
			int queueCount = 0; int maxCount = 9;
			while (queueCount < maxCount && queueIterator.hasNext()) {
				alternateCallAvgVolume = alternateCallAvgVolume + queueIterator.next();
				queueCount++;
			}
			alternateCallAvgVolume = (queueCount > 0) ? (alternateCallAvgVolume / queueCount) : 0;
			
			if (alternateCallAvgVolume > 0 && alternateoptionVolumeAtTime > (3 * alternateCallAvgVolume) ) {
				altVolumeSignal.add(timeCnr);
			}
			
			if (alternateoptionVolumeAtTime > 0) {
				alternateOptionQueue.addFirst(alternateoptionVolumeAtTime);
			}
			
			timeCnr = Util.timeNMinsAgo(timeCnr, -5);
		}
	}
	
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
					// && currentDateString.equals("2023-08-25")
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
				
				String time = "06:35";//VolumeGraphPatternEntry.startTime;
				double shortEnterPrice = 0.0; String shortEnterTime = null; String shortEnterString = "";
				int noOfEntriesForBear = 0;
				putVolumeSignal = new LinkedList<>();
				LinkedList<String> hugePositiveBars = new LinkedList<>();
				LinkedList<String> hugeNegativeBars = new LinkedList<>();
				String strikeTime = null; double strike = 0;
				double avgVix = vixAvgMap.get(currentDateString);
				Map<String, MinuteData> rawVix = vixRawData.get(currentDateString);
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					VolumeGraphPatternEntry.calculateBarSizes(dayData, 0.3, time, hugePositiveBars, hugeNegativeBars);
					double alternateStrike = getTargetedStrike(dayData, time);
					String key = currentDateString + "  " + time;
					//double putVolume = 0; double putAvgVolume = 0;
					//double alternateoptionVolumeAtTime = 0; double alternatePutAvgVolume = 0;
					LinkedList<String> altPutVolumeSignal = new LinkedList<>();
					if (time.compareTo("07:20") >= 0 && time.compareTo("10:20") <= 0) {
						LinkedList<Double> alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike - 1, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike - 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike - 1));
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + alternateStrike);
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike + 1, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike + 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike + 1));
						
						altPutVolumeSignal.sort(String::compareToIgnoreCase);
						//System.out.println("altCallVolumeSignal " + altCallVolumeSignal);
					}
					if (allPutCasesStrike.containsKey(key)) {
						if (strikeTime == null) {
							strikeTime = time;
						}
						if (strike == 0) {
							strike = allPutCasesStrike.get(key);
						}
					}
					
					// Short Enter
					String returnedTime = VolumeGraphPatternEntry.bearEntry(dayData, 0.3, time, putVolumeSignal, altPutVolumeSignal, strike, avgVix, rawVix, alternateStrike,
							hugePositiveBars, hugeNegativeBars, prevClosePrice);
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

2022-12-05  08:30  12:35  bearEntry  288.0  07:50  0.7611798287345347
2022-12-06  07:55  12:35  bearEntry  281.0  07:35  0.6043897783904073
2022-12-15  07:50  12:35  bearEntry  278.0  07:20  0.5634712701431982
2022-12-16  08:20  12:15  bearEntry  273.0  08:00  -0.3492778750330349
2022-12-19  08:35  12:35  bearEntry  270.0  07:25  0.4887983706720952
2022-12-22  08:35  12:10  bearEntry  264.0  08:10  -0.2263040772451336
2022-12-28  07:50  12:35  bearEntry  263.0  07:20  0.7929549006900173
2023-01-05  10:10  10:25  bearEntry  261.0  07:20  -0.6209727439245161
2023-01-10  07:40  08:00  bearEntry  270.0  07:20  -0.6853385832621475
2023-01-18  07:40  12:35  bearEntry  281.0  07:25  1.1262252642371489
2023-01-19  07:45  12:35  bearEntry  274.0  07:20  -0.4946893641786544
2023-01-26  07:55  08:45  bearEntry  289.0  07:20  -0.3911388023537541
2023-01-30  07:40  08:20  bearEntry  0.0  null  -0.5351289063171841
2023-02-08  07:55  12:35  bearEntry  306.0  07:20  0.4620964579793471
2023-02-09  08:00  12:35  bearEntry  306.0  07:20  1.5772478887233057
2023-02-10  08:55  10:10  bearEntry  299.0  07:25  -0.25098933530082623
2023-02-17  08:20  11:25  bearEntry  298.0  07:20  -0.4684937924572614
2023-02-21  08:30  12:35  bearEntry  294.0  08:05  0.3993502098280787
2023-02-23  07:40  11:25  bearEntry  0.0  null  -0.1385743738804097
2023-02-27  07:45  12:35  bearEntry  294.0  07:20  0.3060295636106769
2023-03-09  09:00  12:35  bearEntry  298.0  07:45  1.7787157069641062
2023-03-10  10:15  12:35  bearEntry  289.0  07:20  0.5145916076670728
2023-03-17  08:35  09:10  bearEntry  303.0  08:20  -0.4606778545574315
2023-03-27  07:40  12:35  bearEntry  309.0  07:25  0.4515097114510384
2023-03-29  07:45  12:20  bearEntry  309.0  07:20  -0.8702140002578382
2023-04-03  07:50  12:15  bearEntry  318.0  07:30  -0.33631793905120366
2023-04-04  08:05  12:35  bearEntry  318.0  07:20  0.2880310572618314
2023-04-05  07:40  12:35  bearEntry  315.0  07:25  -0.10878568141789856
2023-04-11  07:40  12:35  bearEntry  314.0  07:30  -0.1708898148720171
2023-04-12  07:50  09:00  bearEntry  314.0  07:25  -0.20944402132519932
2023-04-14  08:35  11:25  bearEntry  315.0  07:50  -0.27606878056475787
2023-04-17  08:30  12:25  bearEntry  316.0  07:25  -0.46235659844407534
2023-04-24  07:40  12:35  bearEntry  315.0  07:25  -0.3096811078643193
2023-04-25  08:35  12:35  bearEntry  311.0  07:50  0.763505095358268
2023-05-01  08:15  09:35  bearEntry  320.0  07:20  -0.42230044904407227
2023-05-02  07:45  10:40  bearEntry  0.0  null  -0.25872612663467515
2023-05-09  08:55  12:35  bearEntry  0.0  null  0.01710225594303606
2023-05-10  07:40  11:15  bearEntry  323.0  07:35  -0.1546264225630876
2023-05-12  07:50  12:35  bearEntry  323.0  07:30  0.15231003553900968
2023-05-19  08:35  12:35  bearEntry  335.0  07:25  -0.1369577515109952
2023-05-23  09:30  12:35  bearEntry  0.0  null  0.5986240581350303
2023-05-30  09:25  11:45  bearEntry  348.0  07:35  -0.17411534649410246
2023-05-31  07:40  10:40  bearEntry  0.0  null  -0.36253776435045054
2023-06-07  07:40  12:35  bearEntry  0.0  null  1.1254928658553824
2023-06-09  07:45  10:55  bearEntry  355.0  07:20  -0.15472836653867894
2023-06-16  08:45  12:35  bearEntry  367.0  07:20  0.1326259946949473
2023-06-21  07:45  09:20  bearEntry  361.0  07:35  -0.1739466563587154
2023-06-26  07:40  12:35  bearEntry  361.0  07:30  0.8703088148076555
2023-06-27  08:05  08:40  bearEntry  357.0  07:30  -0.5850025307118663
2023-06-29  07:40  08:25  bearEntry  360.0  07:35  -0.4906284454244687
2023-07-06  07:55  09:30  bearEntry  363.0  07:30  -0.5126231497555246
2023-07-10  09:00  10:00  bearEntry  364.0  07:25  -0.3790271636134067
2023-07-12  08:35  12:35  bearEntry  371.0  07:25  -0.32809861728523937
2023-07-14  10:00  12:35  bearEntry  380.0  07:20  0.37234882374610906
2023-07-19  09:20  12:35  bearEntry  384.0  07:25  0.09324733856554865
2023-07-20  09:30  12:35  bearEntry  378.0  08:05  0.630390631181913
2023-07-26  07:50  11:40  bearEntry  375.0  07:20  -0.5978860788582767
2023-07-27  10:15  12:35  bearEntry  380.0  07:50  1.3083377031987289
2023-07-31  09:55  12:35  bearEntry  381.0  07:30  0.09530399362899568
2023-08-02  08:55  12:35  bearEntry  374.0  07:40  -0.040084501016152095
2023-08-07  08:15  12:35  bearEntry  371.0  07:20  -0.767995755211064
2023-08-09  07:50  10:15  bearEntry  368.0  07:20  -0.12194791469065572
2023-08-10  08:00  12:35  bearEntry  371.0  07:20  0.7088948787061983
2023-08-11  08:20  12:35  bearEntry  365.0  07:25  -0.276454809218823
2023-08-16  08:45  12:35  bearEntry  364.0  07:20  0.6048662999151488
2023-08-22  09:10  12:35  bearEntry  363.0  07:20  0.008265145879832922
2023-08-24  07:40  12:35  bearEntry  0.0  null  0.9435226559760671
2023-08-25  07:50  08:30  bearEntry  361.0  07:20  -0.5763929496282665
2023-08-28  08:40  10:00  bearEntry  364.0  07:25  -0.4661365505895226
2023-08-31  09:45  12:35  bearEntry  376.0  07:20  -0.5132217595416817
2023-09-01  07:45  12:35  bearEntry  376.0  07:20  0.06357615894039977
2023-09-06  07:55  12:35  bearEntry  0.0  null  -0.008016032064120967
2023-09-08  07:50  12:35  bearEntry  372.0  07:25  0.39509281332868135
2023-09-12  07:45  12:35  bearEntry  374.0  07:20  0.5257820006405539
2023-09-15  07:45  12:35  bearEntry  370.0  07:40  0.5902870941776197
2023-09-19  07:55  10:20  bearEntry  365.0  07:35  -0.5041833592238208
2023-09-20  07:55  12:35  bearEntry  367.0  07:45  1.1242491476811443
2023-09-27  07:50  11:40  bearEntry  353.0  07:30  -0.25139822608893375
2023-09-29  08:25  12:35  bearEntry  359.0  07:20  0.4925014913777493
2023-10-02  09:05  11:15  bearEntry  358.0  07:20  -0.40521911074596084
2023-10-03  08:30  12:35  bearEntry  353.0  08:30  0.39371742498942935
2023-10-05  07:45  09:15  bearEntry  354.0  07:35  -0.5937693476670342
82
Loss due to position -16.62136883713726  45 Stop loss hit exit []
Booked Profit from position exit 22.124944338620303  37


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


