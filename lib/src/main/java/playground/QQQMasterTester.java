package playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.LoggerUtil;
import ScripTrading.Util;

public class QQQMasterTester {
	
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
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-07-01");
		Date endDate = sdf.parse("2023-10-07");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>();
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		
		int longSignalCases = 0;
		int shortSignalCases = 0;
		double sumbookedPositionProfit = 0.0;
		double sumSecondProfitPcnt = 0.0;
		List<String> stopLossHit = new ArrayList<>();
		Map<String, Double> longBokkedProfitMap = new LinkedHashMap<>();
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
					//&& currentDateString.equals("2023-04-27")
					) {
				String time = VolumeGraphPatternEntry.startTime;
				double longEnterPrice = 0.0; String longEnterTime = null; String longEnterString = "";
				double shortEnterPrice = 0.0; String shortEnterTime = null; String shortEnterString = "";
				int noOfEntriesForBull = 0;
				int noOfEntriesForBear = 0;
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					String key = currentDateString + "  " + time;
					double callVolume = 0; double callAvgVolume = 0;
					if (allCallCasesVolume.containsKey(key)) {
						callVolume = allCallCasesVolume.get(key);
					}
					if (allCallCasesAvgVolume.containsKey(key)) {
						callAvgVolume = allCallCasesAvgVolume.get(key);
					}
					//String timeBegin = Util.timeNMinsAgo(time, 15);
					//String timeEnd = Util.timeNMinsAgo(time, -15);
					//double avgVolume = findAvgVolume(dayData, timeBegin, timeEnd);
					//double avgVolume = VolumeGraphPatternEntry.findAvgVolumeAcrossDays(volumeMap, timeBegin, timeEnd);
					//double volFactor = dayData.getMinuteDataMap().get(time).getVolume() / avgVolume;
					
					// Long Enter
					if (VolumeGraphPatternEntry.bullEntry(dayData, 0.3, time) 
							&& (callAvgVolume > 0 && callVolume > (2.8 * callAvgVolume))
							&& longEnterPrice == 0) {
						longSignalCases++;
						longEnterPrice = closeAtTime;
						longEnterTime = time;
						longEnterString = "bullEntry";
						noOfEntriesForBull++;
						//System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + closeAtTime + "  " + dayData.getMinuteDataMap().get(time).getVolume());
						//continue;
					}
					
					// Long Exit
					if (longEnterPrice > 0) {
						time = VolumeGraphPatternEntry.longExit(dayData, 0.3, longEnterTime, longEnterPrice, currentDateString, longBokkedProfitMap, longEnterString);
						longEnterPrice = 0;
						longEnterTime = null;
						longEnterString = "";
					}
					
					time = Util.timeNMinsAgo(time, -5);
				}
				/*time = VolumeGraphPatternEntry.startTime;
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					String timeBegin = Util.timeNMinsAgo(time, 30);
					String timeEnd = Util.timeNMinsAgo(time, -30);
					//double avgVolume = findAvgVolume(dayData, timeBegin, timeEnd);
					double avgVolume = VolumeGraphPatternEntry.findAvgVolumeAcrossDays(volumeMap, timeBegin, timeEnd);
					double volFactor = dayData.getMinuteDataMap().get(time).getVolume() / avgVolume;
					
					// Short Enter
					if (VolumeGraphPatternEntry.bearEntry(dayData, 0.3, time, avgVolume) && shortEnterPrice == 0) {
						shortSignalCases++;
						shortEnterPrice = closeAtTime;
						shortEnterTime = time;
						shortEnterString = "bearEntry";
						noOfEntriesForBear++;
						//System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + closeAtTime + "  " + dayData.getMinuteDataMap().get(time).getVolume());
						//continue;
					}
					
					// Short Exit
					if (shortEnterPrice > 0) {
						time = VolumeGraphPatternEntry.shortExit(dayData, 0.3, shortEnterTime, shortEnterPrice, currentDateString, shortBokkedProfitMap, 0.3, shortEnterString);
						shortEnterPrice = 0;
						shortEnterTime = null;
						shortEnterString = "";
					}
					
					time = Util.timeNMinsAgo(time, -5);
				}*/
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
		
		sumbookedPositionProfit = 0; sumSecondProfitPcnt = 0; secondproblemCases = 0;
		for (String key : shortBokkedProfitMap.keySet()) {
			double profit = shortBokkedProfitMap.get(key);
			if (profit >= 0) {
				sumbookedPositionProfit = sumbookedPositionProfit + profit;
			} else {
				sumSecondProfitPcnt = sumSecondProfitPcnt + profit;
				secondproblemCases++;
			}
			System.out.println(key + "  " + shortBokkedProfitMap.get(key));
		}
		System.out.println(shortSignalCases);
		System.out.println("Loss due to position " + sumSecondProfitPcnt + "  " + secondproblemCases + " Stop loss hit exit " + stopLossHit);
		System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (shortSignalCases - secondproblemCases));
		
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

}



/*
 
2022-12-02  07:50  11:55  bullEntry  0.40367082946295474
2022-12-12  08:05  12:35  bullEntry  0.7885431400282747
2023-01-20  08:00  10:35  bullEntry  0.6490741239650198
2023-01-24  07:55  10:45  bullEntry  0.04848977614782196
2023-01-25  08:00  10:10  bullEntry  0.9878221216727334
2023-01-31  08:55  12:35  bullEntry  0.19448169721773406
2023-02-15  08:45  12:35  bullEntry  0.1853562393459074
2023-03-02  08:05  12:35  bullEntry  1.420914762347914
2023-03-03  08:55  12:35  bullEntry  0.6755394232708176
2023-03-08  08:05  08:45  bullEntry  0.14188049693281257
2023-03-08  08:50  12:35  bullEntry  -0.350695422417152
2023-03-16  07:40  12:35  bullEntry  2.10102220890354
2023-03-20  07:55  09:55  bullEntry  0.04719952751255191
2023-03-24  09:00  12:35  bullEntry  0.5339805825242645
2023-03-30  08:10  12:35  bullEntry  -0.011081911154725064
2023-03-31  07:35  12:35  bullEntry  0.70147845234351
2023-04-06  08:00  12:35  bullEntry  1.015711791779079
2023-04-10  07:35  09:35  bullEntry  0.48887599917365576
2023-04-13  09:00  12:35  bullEntry  0.5308853863231895
2023-04-19  07:40  12:35  bullEntry  0.3806827119710491
2023-04-27  08:50  12:35  bullEntry  0.7577186694334481
2023-04-28  07:45  12:35  bullEntry  0.144787644787637
2023-05-01  09:30  12:35  bullEntry  -0.3313411575263968
2023-05-05  08:10  12:35  bullEntry  0.5334867950352987
2023-05-08  08:50  11:50  bullEntry  0.3374235503599802
2023-05-11  08:30  11:40  bullEntry  0.19943260236159538
2023-05-15  08:25  12:35  bullEntry  0.2562915100329982
2023-05-16  07:55  12:35  bullEntry  0.088401158359995
2023-05-25  10:05  12:30  bullEntry  0.3621542811699161
2023-06-01  07:40  12:35  bullEntry  0.5525652771415503
2023-06-05  07:55  12:35  bullEntry  -0.4919204518380434
2023-06-06  08:45  12:35  bullEntry  -0.1745410519202696
2023-06-12  08:35  12:35  bullEntry  0.882225834016964
2023-06-15  08:30  12:35  bullEntry  0.6705032846517258
2023-06-27  08:25  12:35  bullEntry  0.9202794101341594
2023-07-06  08:50  12:35  bullEntry  0.43342129370102944
2023-07-07  09:30  12:35  bullEntry  -0.4572881997997667
2023-07-11  08:55  12:35  bullEntry  -0.12112722030301519
2023-07-13  08:10  12:35  bullEntry  0.8651582863924151
2023-07-17  10:45  12:35  bullEntry  0.24348024626094095
2023-07-18  08:40  12:35  bullEntry  1.0697285138881707
2023-07-28  07:50  11:45  bullEntry  0.45811408675619125
2023-08-01  09:55  12:35  bullEntry  0.03362573099414046
2023-08-07  08:50  12:35  bullEntry  0.42905794963931404
2023-08-18  09:10  11:30  bullEntry  0.1658231387626634
2023-08-21  10:20  12:35  bullEntry  0.4578805097368478
2023-09-05  08:00  12:35  bullEntry  0.05414614175723619
2023-09-08  08:40  12:35  bullEntry  -0.5788460510133051
2023-09-18  08:30  12:35  bullEntry  -0.30981438077533807
2023-09-19  10:45  12:35  bullEntry  -0.054093527709421864
2023-09-25  08:10  12:35  bullEntry  0.20039520837752942
2023-10-02  08:15  11:10  bullEntry  -0.3960464473320391
2023-10-04  07:35  08:20  bullEntry  0.21444164567589696
53
Loss due to position -3.2767958217894733  11 Stop loss hit exit []
Booked Profit from position exit 21.626152040350476  42

2023-01-20  4.29  4.8293333333333335  277.6999  280.0  07:55  12:50  282.68  0.9686715767632612
2023-01-25  4.7  4.811666666666667  282.2  284.0  07:25  12:50  287.22  1.0914245216158753
2023-03-02  4.640000000000001  5.183333333333334  290.09  292.0  07:25  12:50  293.395  0.4998448757282223
2023-04-10  3.94  4.9543333333333335  314.285  316.0  07:30  12:50  317.3228  0.4550010340932592
2023-04-27  4.43  4.498  317.425  319.0  08:10  12:50  320.56  0.5702134362447822
2023-06-01  4.81  3.955  347.87  350.0  07:25  12:50  351.79  0.5461810446431138
2023-06-12  4.5600000000000005  4.0169999999999995  356.27  358.0  07:20  12:50  359.985  0.5838268728773123
2023-06-27  4.72  4.341  359.39  362.0  07:20  12:50  363.9  0.562063496480147
2023-07-18  3.9000000000000004  4.441666666666667  381.195  384.0  07:40  12:50  386.3  0.6374690119230315
2023-08-21  4.4  4.769666666666666  360.47  363.0  07:30  12:50  364.21  0.3412211834549338
2023-10-04  5.49  4.748333333333333  355.42  359.0  07:25  12:50  360.1831  0.34888301164819085 
 
 */
