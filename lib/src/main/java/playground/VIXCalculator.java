package playground;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import ScripTrading.LoggerUtil;
import ScripTrading.MinuteData;
import ScripTrading.Util;

public class VIXCalculator {
	
	public static void main(String[] args) throws Exception {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-12-01");
		//Date endDate = sdf.parse("2023-09-06");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date nowDate = calendar.getTime();
		LinkedList<Double> volatilityQueue = new LinkedList<>();
		LinkedList<String> internalResultsToWrite = new LinkedList<>();
		Map<String, Map<String, MinuteData>> vixRawData = Util.readVixRawData("config/VIX_full_5min.txt");
		Map<String, Double> vixAvgMap = new LinkedHashMap<>();
		
		
		Date currentDate = startDate;
		while (currentDate.before(nowDate)) {
			calendar.setTime(currentDate);
			String currentDateString = sdf.format(currentDate);
	        // which day of the week, get the price of underlying stock/etf
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			if (dayOfWeek == 1 || dayOfWeek == 7) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			if (!vixRawData.containsKey(currentDateString)) {
				System.out.println("No data on " + currentDateString);
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			
			Map<String, MinuteData> vixMinuteData = vixRawData.get(currentDateString);
			if (currentDateString.compareTo("2022-12-01") >= 0 && !currentDateString.equals("2023-07-03") )
			{
				double totalOptionPriceAtStartTime = vixMinuteData.get("12:55").getClosePrice();
				double avgVolatility = (volatilityQueue.size() > 0) ? findAvgVolatilityAcrossDays(volatilityQueue) : 0;
			
				volatilityQueue.add(totalOptionPriceAtStartTime);
				if (volatilityQueue.size() > 19) {
					volatilityQueue.poll();
				}

				internalResultsToWrite.add(currentDateString + "  " + avgVolatility + "  " + vixMinuteData.get("07:45").getClosePrice());
				
				vixAvgMap.put(currentDateString, avgVolatility);
				
			}
			
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		writeVolatilityData(internalResultsToWrite);
		Util.serializeDoubleHashMap(vixAvgMap, "config/VIXAvgMap.txt");
		
	}
	
	private static void writeVolatilityData(LinkedList<String> internalResultsToWrite) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File("config/VIXProcessed2.txt"),false));
			bw =  new BufferedWriter(out);
			for (String row : internalResultsToWrite) {
				bw.write(row);
				bw.write("\n");
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		} finally{
			 try {
				 bw.close();
				out.close();
			} catch (Exception e) {}
		}		
	}
	
	private static double findAvgVolatilityAcrossDays(LinkedList<Double> volatilityQueue) {
		double sumVolatility = 0;
		Iterator<Double> queueIterator = volatilityQueue.iterator();
		int queueCount = 0;
		while (queueIterator.hasNext()) {
			double vol = queueIterator.next();
			sumVolatility = sumVolatility + vol;
			queueCount++;
		}
		
		double avgVol = (sumVolatility / queueCount);
		return avgVol;
	}

}
