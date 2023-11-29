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

public class QQQMasterLongTester {
	
	private static double getTargetedStrike(DayData dayData, String time) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = (time.compareTo("08:45") <= 0) ? 0.009 : 0.009;
		double priceLevelToPlaceOrder = (((closeAtTime - openAtTime) / openAtTime) * 100 < -0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder + percentHigherFactor * priceLevelToPlaceOrder));
		
		return targetedStrikePrice;
	}
	
	private static void calculateOptionVolumeSig(LinkedList<Double> alternateOptionQueue, LinkedList<String> altVolumeSignal, double altStrike, String time, DayData dayData) {
		String timeCnr = "07:20";
		while (timeCnr.compareTo(time) <= 0) {
			double alternateoptionVolumeAtTime = 0;
			if (dayData.getCallDataMap().get(altStrike).containsKey(timeCnr)) {
				alternateoptionVolumeAtTime = dayData.getCallDataMap().get(altStrike).get(timeCnr).getVolume();
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
		Map<String, Double> allCallCasesVolume = Util.deserializeDoubleHashMap("config/QQQCallCasesVolume.txt");
		Map<String, Double> allCallCasesAvgVolume = Util.deserializeDoubleHashMap("config/QQQCallCasesAvgVolume.txt");
		Map<String, Double> allCallCasesStrike = Util.deserializeDoubleHashMap("config/QQQCallCases.txt");
		Map<String, Double> vixAvgMap = Util.deserializeDoubleHashMap("config/VIXAvgMap.txt");
		Map<String, Map<String, MinuteData>> vixRawData = Util.readVixRawData("config/VIX_full_5min.txt");
		//Map<String, Map<String, Double>> volumeDataMap = UtilRun.readVolumeData("config/QQQAvgVolume.txt");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-07-01");
		Date endDate = sdf.parse("2023-10-07");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>();
		LinkedList<String> callVolumeSignal = new LinkedList<>();
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		LinkedList<Double> volatilityQueue = new LinkedList<>();
		
		int longSignalCases = 0;
		double sumbookedPositionProfit = 0.0;
		double sumSecondProfitPcnt = 0.0;
		List<String> stopLossHit = new ArrayList<>();
		Map<String, Double> longBokkedProfitMap = new LinkedHashMap<>();
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
					// && currentDateString.equals("2023-01-27")
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
				
				String time = "06:45";//VolumeGraphPatternEntry.startTime;
				double longEnterPrice = 0.0; String longEnterTime = null; String longEnterString = "";
				int noOfEntriesForBull = 0;
				callVolumeSignal = new LinkedList<>();
				LinkedList<String> hugePositiveBars = new LinkedList<>();
				LinkedList<String> hugeNegativeBars = new LinkedList<>();
				String strikeTime = null; double strike = 0;
				double avgVix = vixAvgMap.get(currentDateString);
				Map<String, MinuteData> rawVix = vixRawData.get(currentDateString);
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					VolumeGraphPatternEntry.calculateBarSizes(dayData, 0.3, time, hugePositiveBars, hugeNegativeBars);
					double alternateStrike = getTargetedStrike(dayData, time);
					//double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					String key = currentDateString + "  " + time;
					//System.out.println(time);
					//double optionVolumeAtTime = 0; double callAvgVolume = 0;
					if (allCallCasesStrike.containsKey(key)) {
						if (strikeTime == null) {
							strikeTime = time;
						}
						if (strike == 0) {
							strike = allCallCasesStrike.get(key);
						}
					}
					LinkedList<String> altCallVolumeSignal = new LinkedList<>();
					if (time.compareTo("07:20") >= 0 && time.compareTo("08:55") <= 0) {
						LinkedList<Double> alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike - 1, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altCallVolumeSignal, alternateStrike - 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike - 1));
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altCallVolumeSignal, alternateStrike, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + alternateStrike);
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadedMoreData = downloadOptionData(alternateStrike + 1, currentDateString, dayData, downloader, downloadedMoreData);
						calculateOptionVolumeSig(alternateCallOptionQueue, altCallVolumeSignal, alternateStrike + 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike + 1));
						
						altCallVolumeSignal.sort(String::compareToIgnoreCase);
						//System.out.println("altCallVolumeSignal " + altCallVolumeSignal);
					}
					
					//System.out.println("callOptionQueue " + callOptionQueue);
					//System.out.println("callVolumeSignal " + callVolumeSignal);
					//System.out.println("strike " + strike);
					
					// Long Enter
					String returnedTime = VolumeGraphPatternEntry.bullEntry(dayData, 0.3, time, callVolumeSignal, altCallVolumeSignal, strike, avgVix, rawVix, alternateStrike,
							hugePositiveBars, hugeNegativeBars);
					if (returnedTime != null
							&& longEnterPrice == 0
							&& noOfEntriesForBull == 0
							//&& (prevClosePrice > 0 && (((closeAtTime - prevClosePrice) / prevClosePrice) * 100) < 2) 
							) {
						String[] returnedTimeVals = returnedTime.split("  ");
						time = returnedTimeVals[0];
						if (returnedTimeVals[1].equals("true")) {
							longSignalCases++;
							longEnterPrice = dayData.getMinuteDataMap().get(returnedTimeVals[0]).getClosePrice();
							longEnterTime = returnedTimeVals[0]; 
							longEnterString = "bullEntry";
							noOfEntriesForBull++;
						}
					}
					
					// Long Exit
					if (longEnterPrice > 0) {
						time = VolumeGraphPatternEntry.longExit(dayData, 0.3, longEnterTime, longEnterPrice, currentDateString, longBokkedProfitMap, longEnterString, strike, strikeTime
								);
						longEnterPrice = 0;
						longEnterTime = null;
						longEnterString = "";
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
		
		Map<String, String> masterLongResults = readResults("config/QQQMasterLongResult.txt");
		
		Map<String, String> currentLongResults = new LinkedHashMap<>();
		sumbookedPositionProfit = 0; sumSecondProfitPcnt = 0; secondproblemCases = 0;
		for (String key : longBokkedProfitMap.keySet()) {
			double profit = longBokkedProfitMap.get(key);
			if (profit >= 0) {
				sumbookedPositionProfit = sumbookedPositionProfit + profit;
			} else {
				sumSecondProfitPcnt = sumSecondProfitPcnt + profit;
				secondproblemCases++;
			}
			String row = key + "  " + longBokkedProfitMap.get(key);
			System.out.println(row);
			String[] rowVals = row.split("  ");
			currentLongResults.put(rowVals[0], row);
		}
		System.out.println(longSignalCases);
		System.out.println("Loss due to position " + sumSecondProfitPcnt + "  " + secondproblemCases + " Stop loss hit exit " + stopLossHit);
		System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (longSignalCases - secondproblemCases));
		
		System.out.println("Diff View for Long - Entries missing");
		for (String key : masterLongResults.keySet()) {
			if (!currentLongResults.containsKey(key)) {
				System.out.println(masterLongResults.get(key));
			}
		}
		System.out.println("Diff View for Long - Entries changed");
		for (String key : masterLongResults.keySet()) {
			if (currentLongResults.containsKey(key) && !masterLongResults.get(key).equals(currentLongResults.get(key)) ) {
				System.out.println(currentLongResults.get(key));
			}
		}
		System.out.println("Diff View for Long - New Entries");
		for (String key : currentLongResults.keySet()) {
			if (!masterLongResults.containsKey(key)) {
				System.out.println(currentLongResults.get(key));
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
		if (!dayData.getCallDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			dayData.getCallDataMap().put(strikePrice, mMap);
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
 
2022-12-09  07:55  10:15  bullEntry  285.0  07:20  -0.487224251787778
2022-12-12  08:05  12:35  bullEntry  284.0  07:20  0.7885431400282747
2022-12-21  07:55  12:35  bullEntry  0.0  null  -0.06938611547310293
2022-12-29  07:40  12:35  bullEntry  0.0  null  0.27974391228432666
2023-01-04  08:30  10:00  bullEntry  266.0  07:20  -0.29247989200743557
2023-01-06  07:45  12:35  bullEntry  0.0  null  1.5413222003684943
2023-01-09  07:40  10:50  bullEntry  275.0  07:30  -0.6161090637306803
2023-01-10  11:10  12:35  bullEntry  272.0  07:35  -0.09365359189069909
2023-01-13  07:45  08:40  bullEntry  0.0  null  -0.5263706651274218
2023-01-20  08:00  12:35  bullEntry  280.0  07:55  1.449431542647781
2023-01-23  07:45  12:35  bullEntry  0.0  null  0.2519591999861067
2023-01-25  08:10  12:35  bullEntry  284.0  07:25  1.4316547041720744
2023-01-27  08:30  12:35  bullEntry  297.0  07:35  0.8448891823707708
2023-01-31  07:55  12:35  bullEntry  0.0  null  0.12982132486078218
2023-02-02  07:40  11:35  bullEntry  0.0  null  -0.241444805717413
2023-02-03  07:40  08:50  bullEntry  0.0  null  -0.3082455689699395
2023-02-13  07:40  12:35  bullEntry  304.0  07:25  0.6312171585313543
2023-02-15  08:40  12:35  bullEntry  308.0  07:25  0.247250959724117
2023-03-02  10:55  12:35  bullEntry  292.0  07:25  0.7420785045449162
2023-03-03  11:10  12:35  bullEntry  299.0  07:55  0.10359577596578072
2023-03-06  07:40  09:45  bullEntry  0.0  null  -0.4364151378413058
2023-03-08  08:05  09:35  bullEntry  298.0  07:20  -0.33796268300711935
2023-03-10  08:25  08:50  bullEntry  293.0  07:25  -0.3482453151618477
2023-03-14  07:45  10:15  bullEntry  0.0  null  -0.3601480982834231
2023-03-15  11:20  12:35  bullEntry  297.0  07:20  0.11763124285809165
2023-03-16  07:40  12:35  bullEntry  301.0  07:20  2.10102220890354
2023-03-20  07:45  09:00  bullEntry  305.0  07:20  -0.14973107510852787
2023-03-23  07:40  10:10  bullEntry  0.0  null  -0.10241638662185733
2023-03-24  08:55  12:35  bullEntry  310.0  07:25  0.4202359786649279
2023-03-30  08:15  12:35  bullEntry  317.0  07:20  -0.0806834361651623
2023-03-31  07:40  12:35  bullEntry  320.0  07:35  0.6223479490806281
2023-04-06  08:05  12:35  bullEntry  317.0  07:40  0.910013317268059
2023-04-10  07:45  12:35  bullEntry  316.0  07:30  0.7688400736461258
2023-04-13  08:35  12:35  bullEntry  319.0  07:45  0.5847341473020836
2023-04-19  07:40  12:35  bullEntry  319.0  07:25  0.3806827119710491
2023-04-21  08:00  12:35  bullEntry  317.0  07:45  -0.014230598950103066
2023-04-26  08:20  12:35  bullEntry  315.0  07:25  -0.8064131316505303
2023-04-27  07:40  12:35  bullEntry  0.0  null  0.9163622622496614
2023-05-04  08:05  11:30  bullEntry  318.0  07:25  -0.2046149481448894
2023-05-05  07:40  12:35  bullEntry  0.0  null  0.48659909209626195
2023-05-08  08:40  12:35  bullEntry  0.0  null  0.2851917294398512
2023-05-11  07:55  12:35  bullEntry  327.0  07:20  0.1259600614439401
2023-05-15  08:15  12:35  bullEntry  0.0  null  0.287038742555413
2023-05-16  07:55  12:35  bullEntry  0.0  null  0.088401158359995
2023-05-17  07:40  12:35  bullEntry  0.0  null  0.7982782603875335
2023-05-18  08:00  12:35  bullEntry  337.0  08:00  0.24297170795695008
2023-05-25  07:40  12:35  bullEntry  339.0  07:25  0.6711012564671047
2023-05-26  07:40  12:35  bullEntry  0.0  null  0.6645862228386533
2023-06-01  07:40  12:35  bullEntry  350.0  07:25  0.5525652771415503
2023-06-05  07:50  10:55  bullEntry  358.0  07:20  -0.2776375567895028
2023-06-06  07:45  10:00  bullEntry  357.0  07:20  -0.46725026036534695
2023-06-08  07:40  12:35  bullEntry  0.0  null  0.43820960077398635
2023-06-12  08:35  12:35  bullEntry  358.0  07:20  0.882225834016964
2023-06-15  07:45  12:35  bullEntry  0.0  null  0.8361090340561608
2023-06-22  08:40  12:35  bullEntry  366.0  07:40  0.10367734165381128
2023-06-27  08:25  12:35  bullEntry  362.0  07:20  0.9202794101341594
2023-06-28  07:45  09:35  bullEntry  0.0  null  -0.44754308814291377
2023-07-11  08:55  12:35  bullEntry  369.0  08:00  -0.12112722030301519
2023-07-17  08:10  12:35  bullEntry  0.0  null  0.5502225840763817
2023-07-18  08:35  12:35  bullEntry  0.0  null  1.186015418671772
2023-07-19  07:40  09:40  bullEntry  389.0  07:20  -0.48531158036036853
2023-07-25  08:35  12:35  bullEntry  381.0  07:20  0.19660623334125157
2023-07-28  08:10  10:35  bullEntry  385.0  07:30  -0.1645221029599238
2023-08-01  07:45  12:35  bullEntry  384.0  07:20  0.044073107049602114
2023-08-14  07:40  12:35  bullEntry  0.0  null  0.23025943720567132
2023-08-18  07:40  12:35  bullEntry  358.0  07:20  0.3980378416257928
2023-08-21  10:40  12:35  bullEntry  363.0  07:30  0.26428807400065507
2023-08-23  07:40  12:35  bullEntry  0.0  null  0.4427302604774956
2023-08-29  08:10  12:35  bullEntry  375.0  08:10  0.5122703500066965
2023-09-05  07:45  12:35  bullEntry  380.0  07:25  0.29122872044690973
2023-09-07  07:40  12:35  bullEntry  373.0  07:35  -0.010755868670833746
2023-09-13  08:00  12:05  bullEntry  375.0  07:20  -0.32309949319818465
2023-09-14  07:40  12:35  bullEntry  377.0  07:20  0.22304240460953637
2023-09-18  08:30  12:35  bullEntry  0.0  null  -0.30981438077533807
2023-09-22  07:40  10:35  bullEntry  362.0  07:20  -0.39062013057975825
2023-09-25  08:10  12:35  bullEntry  360.0  07:30  0.20039520837752942
2023-09-28  08:20  12:35  bullEntry  357.0  07:35  0.1750234531427211
2023-10-02  08:10  08:45  bullEntry  363.0  07:20  -0.16746709479190108
2023-10-04  07:40  12:35  bullEntry  359.0  07:25  0.7577664066213627
2023-10-06  07:40  12:35  bullEntry  0.0  null  1.8770713231403373
80
Loss due to position -8.640923542576324  29 Stop loss hit exit []
Booked Profit from position exit 29.995603049545007  51

2023-01-20  280.0  07:55  0.07129998966510251  0.9686715767632612
2023-01-25  284.0  07:25  0.1403260099220411  1.0914245216158753
2023-02-14  306.0  07:40  0.13424811162054293  0.42880232212949826
2023-03-02  292.0  07:25  0.094798166086387  0.4998448757282223
2023-03-16  301.0  07:20  0.16908939014202176  1.8245614035087718
2023-04-10  316.0  07:30  0.07700017500039773  0.4550010340932592
2023-04-27  319.0  08:10  0.07623848153107034  0.5702134362447822
2023-06-01  350.0  07:25  0.08853882197372583  0.5461810446431138
2023-06-12  358.0  07:20  0.08953883290762625  0.5838268728773123
2023-06-27  362.0  07:20  0.0826400289379226  0.562063496480147
 
 */
