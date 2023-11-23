package playground;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.MinuteData;
import ScripTrading.Util;

public class QQQOptionSignalIdentifierShort {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		Map<String, Double> allPutCases = Util.deserializeDoubleHashMap("config/QQQPutCases.txt");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-12-01");
		Date endDate = sdf.parse("2023-10-07");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		LinkedList<Double> sameDayVolatilityQueue = new LinkedList<>();
		Map<String, Double> allPutCasesVolume = new LinkedHashMap<>();
		Map<String, Double> allPutCasesAvgVolume = new LinkedHashMap<>();
		Map<String, Double> allPutCasesStrike = new LinkedHashMap<>();
		double prevClosePrice = 0;
		boolean downloadedMoreData = false;
		int totaldays = 0;
		Map<String, Integer> volatilitySignal = new LinkedHashMap<>();
		
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
					) {
				volatilitySignal.put(currentDateString, 0);
				sameDayVolatilityQueue = new LinkedList<>();
				String time = VolumeGraphPatternEntry.startTime;
				String prevTime = null;
				double strike = 0;
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					double totalOptionPriceAtTime = 0;
					
					String key = currentDateString + "  " + time;
					if (allPutCases.containsKey(key)) {
						strike = allPutCases.get(key);
					}
					
					if (strike > 0) {
						downloadedMoreData = downloadOptionData(strike, currentDateString, dayData, downloader, downloadedMoreData);
						if (dayData.getPutDataMap().get(strike).containsKey(time)) {
							double sumPutVolumeAtTime =  dayData.getPutDataMap().get(strike).get(time).getVolume();
							totalOptionPriceAtTime = sumPutVolumeAtTime;
						}
					}
					
					double sameDayAvgVolatility = (sameDayVolatilityQueue.size() > 0) ? findAvgVolatility(sameDayVolatilityQueue) : 0;
					
					if (currentDateString.equals("2023-01-20")) {
						//}
						if (sameDayAvgVolatility > 0 //) { 
								&& totalOptionPriceAtTime > (2.8 * sameDayAvgVolatility) ) {
							volatilitySignal.put(currentDateString, volatilitySignal.get(currentDateString) + 1);
							System.out.println(currentDateString + "  " + time + "  " + sameDayAvgVolatility + "  " + strike + "  " + totalOptionPriceAtTime);
						}
					}
					
					if (totalOptionPriceAtTime > 0) { //avgVolatility == 0 || totalOptionPriceAtStartTime < (1.66 * avgVolatility)) {
						sameDayVolatilityQueue.add(totalOptionPriceAtTime);
						if (sameDayVolatilityQueue.size() > 9) {
							sameDayVolatilityQueue.poll();
						}
					}
					if (strike > 0) {
						allPutCasesVolume.put(key, totalOptionPriceAtTime);
						allPutCasesAvgVolume.put(key, sameDayAvgVolatility);
						allPutCasesStrike.put(key, strike);
					}
					prevTime = time;
					time = Util.timeNMinsAgo(time, -5);
				}
			}
			
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		Util.serializeDoubleHashMap(allPutCasesVolume, "config/QQQPutCasesVolume.txt");
		Util.serializeDoubleHashMap(allPutCasesAvgVolume, "config/QQQPutCasesAvgVolume.txt");
		Util.serializeDoubleHashMap(allPutCasesStrike, "config/QQQPutCasesStrike.txt");
		
		
		int totalSignal = 0;
		for (String key : volatilitySignal.keySet()) {
			totaldays++;
			if (volatilitySignal.get(key) > 0) {
				totalSignal++;
			}
		}
		System.out.println("Total Days " + totaldays);
		System.out.println("Volatility Signal Cases " + totalSignal);
		
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
	
	private static double findAvgVolatility(LinkedList<Double> volatilityQueue) {
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
