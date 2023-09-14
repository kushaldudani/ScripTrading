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

public class QQQTester2 {
	
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
			
			if (currentDateString.compareTo("2022-12-01") >= 0 && !currentDateString.equals("2023-07-03")) {
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
					boolean supportedByGS = false;
					Double lastMajorDownSegmentStartPrice = Util.doesGraphSegmentsHaveDorDrInLastNMins(graphSegments, 210, time);
					if (lastMajorDownSegmentStartPrice != null) {
						supportedByGS = (closeAtTime > lastMajorDownSegmentStartPrice) ? true : false;
					} else {
						supportedByGS = true;
					}
					
					if (dayData.getMinuteDataMap().get(time).getVolume() > (2 * avgVolume) 
							&& time.compareTo(midTime) < 0
							&& closeAtTime > openAtTime
							&& supportedByGS) {
						signalCases++;
						System.out.println(currentDateString + "  " + time + "  " + avgVolume + "  " + closeAtTime + "  " + dayData.getMinuteDataMap().get(time).getVolume());
						break;
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
 
 2023-01-06  263.39  266.0  266.76  08:50  268.7499  0.0
2023-01-20  277.89  280.0  280.69  10:45  282.68  0.0
2023-01-23  286.21  288.0  288.95  08:05  289.0301  0.5380664547010937
2023-01-25  281.59  284.0  285.25  08:50  287.22  0.0
2023-02-13  301.88  304.0  304.7  09:15  304.02  0.4173843911488008
2023-02-15  305.02  308.0  308.784  11:10  308.33  0.36718903678447323
2023-03-02  289.895  292.0  292.6502  11:00  293.395  0.0
2023-03-06  302.095  303.0  303.61  07:40  299.97  0.40384647213624847
2023-03-16  299.26  302.0  302.63  08:10  306.495  0.0
2023-03-31  317.04  318.0  318.7696  08:55  320.48  0.0
2023-04-06  315.09  316.0  316.7384  08:40  317.895  0.0
2023-04-10  314.04  315.0  315.65  08:30  317.3228  0.0
2023-04-13  317.05  318.0  318.67  10:45  319.07  0.0
2023-04-19  317.1  318.0  318.82  09:10  318.69  0.39419741406496367
2023-04-27  316.61  318.0  318.83  09:40  320.56  0.0
2023-05-05  320.705  322.0  322.675  11:00  323.04  0.0
2023-05-08  321.765  323.0  323.695  10:35  323.78  0.26727580687769026
2023-05-15  324.12  325.0  325.74  08:15  326.645  0.0
2023-05-16  326.6  327.0  327.73  07:25  327.325  0.36436007348438454
2023-05-17  327.932  329.0  329.715  09:40  331.3601  0.0
2023-05-18  333.39  334.0  334.805  07:30  337.0806  0.0
2023-05-25  337.22  339.0  339.68  08:10  339.95  0.0
2023-05-26  343.96  346.0  347.2393  08:40  348.5627  0.0
2023-06-01  348.49  351.0  351.71  09:20  351.79  0.0
2023-06-08  350.8  352.0  352.7507  08:05  353.03  0.38198403648802737
2023-06-12  355.76  357.0  357.8201  10:40  359.985  0.0
2023-06-15  367.0  369.0  369.75  11:25  370.628  0.0
2023-06-23  362.19  364.0  364.7289  10:20  362.75  0.3037079985642895
2023-06-27  359.01  361.0  361.875  09:25  363.9  0.0
2023-06-28  362.81  364.0  365.1999  07:30  363.8671  0.4878586588021278
2023-07-07  368.25  369.0  369.81  09:50  366.45  0.2905634758995248
2023-07-11  364.8  366.0  366.89  07:40  367.96  0.0
2023-07-13  376.38  377.0  377.78  09:35  379.565  0.0
2023-07-17  380.39  381.0  381.9201  10:45  382.92  0.0
2023-07-18  380.93  382.0  383.44  08:45  386.3  0.0
2023-08-01  381.32  382.0  383.0  07:45  383.2  0.39861533620056644
2023-08-04  373.83  376.0  376.8182  08:40  371.64  0.35042666452665655
2023-08-29  370.59  372.0  372.85  08:10  374.55  0.0
2023-09-05  376.55  378.0  378.86  08:05  378.33  0.3532067454521312
9.535362152830304
4.8782572069047685
58
39
14
190
26.306954893468447

 
 */
