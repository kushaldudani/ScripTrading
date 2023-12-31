package playground;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.LoggerUtil;
import ScripTrading.MetadataUtil;
import ScripTrading.MinuteData;
import ScripTrading.Util;

public class QQQVolatilityWriter {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-12-01");
		//Date endDate = sdf.parse("2023-09-06");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date nowDate = calendar.getTime();
		LinkedList<Double> volatilityQueue = new LinkedList<>();
		boolean downloadedMoreData = false;
		String startTime = "07:10";
		LinkedList<String> internalResultsToWrite = new LinkedList<>();
		
		
		Date currentDate = startDate;
		while (currentDate.before(nowDate)) {
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
				Map<String, MinuteData> minuteDataMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/QQQ/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
				if (minuteDataMap.isEmpty()) {
					calendar.add(Calendar.DATE, 1);
			        currentDate = calendar.getTime();
					continue;
				} else {
					downloadedMoreData = true;
					Map<Double, Map<String, MinuteData>> callDataMap = new LinkedHashMap<>();
					Map<Double, Map<String, MinuteData>> putDataMap = new LinkedHashMap<>();
					dayDataMap.put(currentDateString, new DayData(minuteDataMap, null, callDataMap, putDataMap));
				}
			}
			DayData dayData = dayDataMap.get(currentDateString);
			
			double closeAtStartTimeFloor = (int) (dayData.getMinuteDataMap().get(startTime).getClosePrice());
			downloadedMoreData = downloadOptionData(closeAtStartTimeFloor, currentDateString, dayData, downloader, downloadedMoreData);
			downloadedMoreData = downloadOptionData(closeAtStartTimeFloor + increment, currentDateString, dayData, downloader, downloadedMoreData);
			double sumCallPriceAtStartTime = dayData.getCallDataMap().get(closeAtStartTimeFloor).get(startTime).getClosePrice() 
					                         + dayData.getCallDataMap().get(closeAtStartTimeFloor + increment).get(startTime).getClosePrice();
			double sumPutPriceAtStartTime = dayData.getPutDataMap().get(closeAtStartTimeFloor).get(startTime).getClosePrice() 
                                             + dayData.getPutDataMap().get(closeAtStartTimeFloor + increment).get(startTime).getClosePrice();
			double totalOptionPriceAtStartTime = (sumCallPriceAtStartTime + sumPutPriceAtStartTime);
			double avgVolatility = (volatilityQueue.size() > 0) ? findAvgVolatilityAcrossDays(volatilityQueue) : 0;
			
			if (avgVolatility == 0 || totalOptionPriceAtStartTime < (1.66 * avgVolatility)) {
				volatilityQueue.add(totalOptionPriceAtStartTime);
				if (volatilityQueue.size() > 9) {
					volatilityQueue.poll();
				}
			}
			internalResultsToWrite.add(currentDateString + "  " + avgVolatility + "  " + totalOptionPriceAtStartTime);
			MetadataUtil.getInstance().writeVolatilityData(currentDateString, avgVolatility);
			
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		writeVolatilityData(internalResultsToWrite);
		
	}
	
	private static void writeVolatilityData(LinkedList<String> internalResultsToWrite) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File("config/QQQVolatility.txt"),false));
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
	
	private static boolean downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader, boolean downloadedMoreData) {
		if (!dayData.getCallDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			dayData.getCallDataMap().put(strikePrice, mMap);
			downloadedMoreData = true;
		}
		
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


