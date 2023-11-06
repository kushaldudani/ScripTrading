package playground;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.Util;

public class QQQVolumeSignalIdentifier {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-07-01");
		Date endDate = sdf.parse("2023-10-07");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>(); 
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		int totaldays = 0;
		int volumeSignal = 0;
		
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
					&& currentDateString.equals("2023-06-12")
					) {
				totaldays++;
				String time = VolumeGraphPatternEntry.startTime;
				while (time.compareTo(VolumeGraphPatternEntry.midTime) < 0) {
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					String timeBegin = Util.timeNMinsAgo(time, 15);
					String timeEnd = time;//Util.timeNMinsAgo(time, -15);
					double avgVolume = VolumeGraphPatternEntry.findAvgVolume(dayData, timeBegin, timeEnd);
					//double avgVolume = VolumeGraphPatternEntry.findAvgVolumeAcrossDays(volumeMap, timeBegin, timeEnd);
					//double volFactor = dayData.getMinuteDataMap().get(time).getVolume() / avgVolume;
					
					double curVolume = dayData.getMinuteDataMap().get(time).getVolume();
					//System.out.println(time);
					//System.out.println("curVolume " + curVolume);
					//System.out.println("avgVolume " + avgVolume);
					if (curVolume >= (2 * avgVolume) 
						//&& (closeAtTime < (floorPrice + (3 * ninetyPercentileBarChange * floorPrice / 100)))
						) {
						volumeSignal++;
						System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + curVolume);
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
		
		System.out.println("Total Days " + totaldays);
		System.out.println("Volume Signal Cases " + volumeSignal);
		
	}
	
	

}

/*
 
2022-12-14 Did not process as avgVolatility high 12.37  5.084444444444445
2023-01-20  4.29  4.8293333333333335  277.6999  280.0  07:55  12:50  282.68  0.9686715767632612
2023-01-25  4.7  4.811666666666667  282.2  284.0  07:25  12:50  287.22  1.0914245216158753
2023-02-01 Did not process as avgVolatility high 10.5  4.761666666666668
2023-02-07 Did not process as avgVolatility high 8.4  4.938999999999999
2023-03-02  4.640000000000001  5.183333333333334  290.09  292.0  07:25  12:50  293.395  0.4998448757282223
2023-03-22 Did not process as avgVolatility high 10.16  5.380666666666667
2023-04-10  3.94  4.9543333333333335  314.285  316.0  07:30  12:50  317.3228  0.4550010340932592
2023-04-27  4.43  4.498  317.425  319.0  08:10  12:50  320.56  0.5702134362447822
2023-05-03 Did not process as avgVolatility high 7.36  4.219
2023-05-17  3.35  3.873666666666667  328.01  330.0  07:25  12:50  331.3601  0.42376756806194926
2023-06-01  4.81  3.955  347.87  350.0  07:25  12:50  351.79  0.5461810446431138
2023-06-12  4.5600000000000005  4.0169999999999995  356.27  358.0  07:20  12:50  359.985  0.5838268728773123
2023-06-14 Did not process as avgVolatility high 7.91  4.091666666666667
2023-06-27  4.72  4.341  359.39  362.0  07:20  12:50  363.9  0.562063496480147
2023-07-03 Did not process as short day
2023-07-18  3.9000000000000004  4.441666666666667  381.195  384.0  07:40  12:50  386.3  0.6374690119230315
2023-08-21  4.4  4.769666666666666  360.47  363.0  07:30  12:50  364.21  0.3412211834549338
2023-10-04  5.49  4.748333333333333  355.42  359.0  07:25  12:50  360.1831  0.34888301164819085 
  
  
2022-12-14 Did not process as avgVolatility high 12.37  5.084444444444445
2022-12-15  5.609999999999999  5.084444444444445  279.3  278.0  07:20  12:50  276.8  0.447547440028643
2022-12-19  4.29  5.239090909090909  272.5784  271.0  07:25  12:50  269.67  0.5319570442852405
2022-12-28  4.1  5.0182352941176465  264.2101  263.0  07:20  12:50  259.975  1.139244866112234
2023-02-01 Did not process as avgVolatility high 10.5  4.761666666666668
2023-02-03  7.17  4.815666666666668  309.92  309.0  07:25  12:50  306.58  0.8002065049044915
2023-02-07 Did not process as avgVolatility high 8.4  4.938999999999999
2023-03-09  4.4399999999999995  5.285333333333335  300.59  298.0  07:45  12:50  293.22  1.5868791376958649
2023-03-10  6.57  5.295333333333333  292.27  291.0  08:00  12:50  288.18  0.9785472337222432
2023-03-22 Did not process as avgVolatility high 10.16  5.380666666666667
2023-04-25  5.109999999999999  4.666666666666667  313.45  311.0  07:50  12:50  310.46  0.3286010527994896
2023-05-03 Did not process as avgVolatility high 7.36  4.219
2023-06-07  3.8099999999999996  4.045  352.3271  350.0  08:20  12:50  348.67  0.3632987641313995
2023-06-14 Did not process as avgVolatility high 7.91  4.091666666666667
2023-06-26  4.04  4.323333333333334  364.11  361.0  07:20  12:50  358.345  0.7305484606300294
2023-07-03 Did not process as short day
2023-07-14  4.41  4.504333333333334  382.5  380.0  07:20  12:50  378.765  0.32679738562091504
2023-07-20  4.73  4.430666666666667  380.63  378.0  08:05  12:50  376.84  0.31263957123715946
2023-07-27  4.29  4.635666666666666  382.53  379.0  07:40  12:50  376.36  0.6692285572373409
2023-08-10  4.5  4.637  373.725  371.0  07:20  12:50  368.52  0.6073984881931902
2023-08-16  4.5600000000000005  4.657333333333333  366.7691  364.0  07:20  12:50  362.85  0.31900179159040387
2023-09-20  5.96  4.683666666666667  369.71  367.0  07:45  12:50  365.24  0.4733439723026156

 */