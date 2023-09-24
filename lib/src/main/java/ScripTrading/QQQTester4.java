package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QQQTester4 {
	
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
		Date endDate = sdf.parse("2023-09-06");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>(); 
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		String optionSellingTime = "07:15";
		String closeTime = "12:50";
		String midTime = "11:30";
		int signalCases = 0;
		
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
			
			if (currentDateString.compareTo("2022-12-01") >= 0 && currentDateString.equals("2023-06-23")) {
				List<GraphSegment> graphSegments = new ArrayList<>();
				for (String time : dayData.getMinuteDataMap().keySet()) {
					String timeBegin = (Util.timeNMinsAgo(time, 30).compareTo("06:30") >= 0) ? Util.timeNMinsAgo(time, 30) : "06:30";
					String timeEnd = (Util.timeNMinsAgo(time, -30).compareTo("12:55") <= 0) ? Util.timeNMinsAgo(time, -30) : "12:55";
					double avgVolume = findAvgVolumeAcrossDays(volumeMap, timeBegin, timeEnd);
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
					
					Util.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
							dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
							dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
							time, 0.2);
					boolean supportedByGS = Util.goodTimeToLongBasedOffGraphSegmentsDorDr(graphSegments, closeAtTime, 0.2);
					//Double lastMajorDownSegmentStartPrice = Util.doesGraphSegmentsHaveDorDrInLastNMins(graphSegments, 150, time);
					//if (lastMajorDownSegmentStartPrice != null) {
					//	supportedByGS = (closeAtTime > lastMajorDownSegmentStartPrice) ? true : false;
					//} else {
					//	supportedByGS = true;
					//}
					
					if (dayData.getMinuteDataMap().get(time).getVolume() > (1.75 * avgVolume) && time.compareTo(midTime) < 0) {
						System.out.println(time + "  " + avgVolume + "  " + dayData.getMinuteDataMap().get(time).getVolume());
					}
					
					if (dayData.getMinuteDataMap().get(time).getVolume() > (1.75 * avgVolume) 
							&& time.compareTo(midTime) < 0
							&& closeAtTime > openAtTime
							&& supportedByGS) {
						signalCases++;
						System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + closeAtTime + "  " + dayData.getMinuteDataMap().get(time).getVolume());
					}
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
				if (volumeQueue.size() > 90) {
					volumeQueue.poll();
				}
			}
			
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		System.out.println(signalCases);
		
	}
	
	private static double findAvgVolumeAcrossDays(Map<String, LinkedList<Double>> volumeMap, String timeBegin, String timeEnd) {
		double sumVolume = 0;
		int cntr = 0;
		for (String time : volumeMap.keySet()) {
			if (time.compareTo(timeBegin) >= 0 && time.compareTo(timeEnd) < 0) {
				Iterator<Double> volumeQueueIterator = volumeMap.get(time).iterator();
				int queueCount = 0;
				while (volumeQueueIterator.hasNext()) {
					double volm = volumeQueueIterator.next();
					sumVolume = sumVolume + volm;
					queueCount++;
				}
				if (queueCount != 90) {
					throw new IllegalStateException("findAvgVolumeAcrossDays has issues at time " +  time);
				}
				cntr++;
			}
		}
		
		double avgVolume = (sumVolume / (90 * cntr));
		return avgVolume;
	}

}


/*
 
 2022-12-02  06:30  1223739.524074074  289.91  2569122.0
2022-12-16  06:30  1203966.9888888889  276.5143  2468722.0
2022-12-21  08:40  527756.4203703704  273.7284  1642016.0
2022-12-29  07:30  820981.4768518518  265.96  2933653.0
2023-01-11  09:50  428292.26203703706  275.77  983346.0
2023-01-12  07:10  932236.2518518518  277.82  2007478.0
2023-01-20  07:25  824894.0796296296  278.24  2169497.0
2023-01-24  10:45  474222.13703703706  289.04  1577444.0
2023-01-31  09:50  410584.2064814815  293.11  5309478.0
2023-02-01  11:05  482506.7074074074  295.3  1796593.0
2023-02-02  06:30  1165101.2333333334  308.28  3473917.0
2023-02-03  06:30  1165245.151851852  306.1801  3083328.0
2023-02-07  09:40  406239.72962962964  305.97  2028484.0
2023-02-23  11:15  487438.2564814815  296.15  1442551.0
2023-02-24  06:30  1138803.0166666666  292.29  2677331.0
2023-03-02  10:35  428633.7611111111  290.99  1815834.0
2023-03-06  07:40  674007.9157407407  303.61  1686461.0
2023-03-10  07:55  617676.5657407407  292.74  1512565.0
2023-03-13  06:30  1104394.2907407407  286.92  2786180.0
2023-03-14  06:30  1115575.677777778  294.53  3263024.0
2023-03-15  06:30  1127260.2037037036  295.67  3016856.0
2023-03-16  07:45  670864.8212962963  301.32  2287803.0
2023-03-22  07:25  796118.4981481482  311.6905  1666401.0
2023-03-23  09:25  413032.43333333335  313.07  862408.0
2023-03-24  08:45  501148.3759259259  308.89  1343116.0
2023-03-29  06:30  1106580.7333333334  311.29  2279701.0
2023-03-30  07:25  785828.0657407407  315.87  1689431.0
2023-03-31  09:25  413004.0351851852  319.312  1115834.0
2023-04-05  10:35  445263.5722222222  315.13  1314482.0
2023-04-10  09:05  459884.38333333336  314.8  1200558.0
2023-04-20  09:40  414903.92685185187  318.0899  970576.0
2023-04-21  07:20  831029.4740740741  315.965  1787828.0
2023-04-27  10:45  453227.59074074076  319.7193  931103.0
2023-05-04  10:20  425972.75  317.9  878814.0
2023-05-17  08:15  604690.2240740741  328.58  1234643.0
2023-05-18  08:45  514255.35185185185  336.18  1081725.0
2023-05-25  09:20  437675.15555555554  340.07  1146297.0
2023-05-26  08:45  533413.7888888889  347.44  1229643.0
2023-05-30  06:30  1137360.212962963  352.81  2778959.0
2023-06-14  07:40  743700.5222222222  365.0632  1592912.0
2023-06-30  09:50  430012.01481481484  369.92  2744423.0
2023-07-07  09:35  446476.7990740741  369.68  1036024.0
2023-07-13  09:20  461607.6972222222  377.69  1374267.0
2023-07-18  08:45  529398.8453703703  383.44  1777861.0
2023-07-19  08:50  519158.4574074074  387.3899  1829858.0
2023-07-25  11:20  482165.6703703704  379.85  1511291.0
2023-07-27  06:30  1092885.3907407408  383.5  2314008.0
2023-07-28  09:45  411548.3703703704  383.87  1229959.0
2023-08-04  09:25  436542.0759259259  377.63  1050320.0
2023-08-09  11:15  452407.1222222222  370.51  1028507.0 // Case that needs to be solved, major down in the day and trying to recover in the next half
2023-08-18  06:30  1099826.6203703703  355.63  2512300.0
2023-08-23  10:15  404317.89074074075  369.59  842655.0
2023-08-25  07:10  836546.200925926  364.575  1703607.0  // Avoid major news time
2023-08-29  07:00  958851.9388888889  370.23  2666398.0
54

 
 
 */
