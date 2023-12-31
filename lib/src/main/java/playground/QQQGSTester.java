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
import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
import ScripTrading.Util;

public class QQQGSTester {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2023-09-11");
		Date endDate = sdf.parse("2023-09-12");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		//Map<String, LinkedList<Double>> volumeMap = new LinkedHashMap<>(); 
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		String optionSellingTime = "07:15";
		String closeTime = "12:50";
		String midTime = "11:30";
		int signalCases = 0;
		
		GSInterpretation gsInterpreter = new GSInterpretation();
		
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
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			
			if (currentDateString.compareTo("2022-12-01") >= 0) {
				for (String time : dayData.getMinuteDataMap().keySet()) {
					System.out.println(time);
					if (time.equals("09:20")) {
						break;
					}
					
					GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
							dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
							dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
							time, 0.3, priceWithTime);
					
					for (GraphSegment graphSegment : graphSegments) {
						System.out.println(graphSegment.toString());
		    		}
					
					List<IGraphSegment> interpretedGSs = gsInterpreter.interpretedGraphSegments(graphSegments);
					for (IGraphSegment interpretedGS : interpretedGSs) {
						System.out.println(interpretedGS.toString());
		    		}
				}
			}
			
			System.out.println("Graph segments for " + currentDateString);
			for (GraphSegment graphSegment : graphSegments) {
				System.out.println(graphSegment.toString());
    		}
			
			List<IGraphSegment> interpretedGSs = gsInterpreter.interpretedGraphSegments(graphSegments);
			System.out.println("Interpreted Graph segments for " + currentDateString);
			for (IGraphSegment interpretedGS : interpretedGSs) {
				System.out.println(interpretedGS.toString());
    		}
			
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		//System.out.println(signalCases);
		
	}

}
