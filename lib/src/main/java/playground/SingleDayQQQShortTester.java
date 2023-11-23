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

public class SingleDayQQQShortTester {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date runDate = sdf.parse("2023-11-17");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Map<String, DayData> dayDataMap = new LinkedHashMap<>();
		LinkedList<String> putVolumeSignal = new LinkedList<>();
		
		Map<String, Double> shortBokkedProfitMap = new LinkedHashMap<>();
		
			calendar.setTime(runDate);
			String currentDateString = sdf.format(runDate);
	        
			Map<String, MinuteData> minuteDataMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/QQQ/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (minuteDataMap.isEmpty()) {
				System.out.println("Unexpected Data missing " + currentDateString);
				return;
			} else {
				Map<Double, Map<String, MinuteData>> callDataMap = new LinkedHashMap<>();
				Map<Double, Map<String, MinuteData>> putDataMap = new LinkedHashMap<>();
				dayDataMap.put(currentDateString, new DayData(minuteDataMap, null, callDataMap, putDataMap));
			}
			
			DayData dayData = dayDataMap.get(currentDateString);
			
			
				String time = VolumeGraphPatternEntry.startTime;
				double shortEnterPrice = 0.0; String shortEnterTime = null; String shortEnterString = "";
				int noOfEntriesForBear = 0;
				putVolumeSignal = new LinkedList<>();
				LinkedList<String> alternatePutVolumeSignal = new LinkedList<>();
				LinkedList<Double> putOptionQueue = new LinkedList<>();
				LinkedList<Double> alternatePutOptionQueue = new LinkedList<>();
				String strikeTime = null; double strike = 0; // Need to be updated with every run
				double avgVix = 0; // Need to be fixed
				Map<String, MinuteData> rawVix = null; // Need to be fixed
				double alternateStrike = ((int) (dayData.getMinuteDataMap().get("07:20").getClosePrice() - 0.009 * dayData.getMinuteDataMap().get("07:20").getClosePrice())) + 1;
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					//double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					//String key = currentDateString + "  " + time;
					double putVolume = 0; double putAvgVolume = 0;
					double alternateoptionVolumeAtTime = 0; double alternatePutAvgVolume = 0;
					if (time.compareTo("07:20") >= 0) {
						downloadOptionData(alternateStrike, currentDateString, dayData, downloader);
						if (dayData.getPutDataMap().get(alternateStrike).containsKey(time)) {
							alternateoptionVolumeAtTime = dayData.getPutDataMap().get(alternateStrike).get(time).getVolume();
						}
						
						alternatePutAvgVolume = (alternatePutOptionQueue.size() > 0) ? findAvg(alternatePutOptionQueue) : 0;
						if (alternatePutAvgVolume > 0 && alternateoptionVolumeAtTime > (2.8 * alternatePutAvgVolume) ) {
							alternatePutVolumeSignal.add(time);
						}
						if (alternateoptionVolumeAtTime > 0) {
							alternatePutOptionQueue.add(alternateoptionVolumeAtTime);
							if (alternatePutOptionQueue.size() > 9) {
								alternatePutOptionQueue.poll();
							}
						}
					}
					if (strikeTime != null && time.compareTo(strikeTime) >= 0) {
						downloadOptionData(strike, currentDateString, dayData, downloader);
						if (dayData.getPutDataMap().get(strike).containsKey(time)) {
							putVolume = dayData.getPutDataMap().get(strike).get(time).getVolume();
						}
						
						putAvgVolume = (putOptionQueue.size() > 0) ? findAvg(putOptionQueue) : 0;
						if (putAvgVolume > 0 && putVolume > (2.8 * putAvgVolume) ) {
							putVolumeSignal.add(time);
						}
						if (putVolume > 0) {
							putOptionQueue.add(putVolume);
							if (putOptionQueue.size() > 9) {
								putOptionQueue.poll();
							}
						}
					}
					
					// Short Enter
					String returnedTime = VolumeGraphPatternEntry.bearEntry(dayData, 0.3, time, putVolumeSignal, alternatePutVolumeSignal, strike, avgVix, rawVix, alternateStrike);
					if (returnedTime != null
							&& shortEnterPrice == 0
							&& noOfEntriesForBear == 0) {
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
			
			
		
		for (String key : shortBokkedProfitMap.keySet()) {
			String row = key + "  " + shortBokkedProfitMap.get(key);
			System.out.println(row);
		}
		
	}
	
	private static void downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader) {
		if (!dayData.getPutDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"P00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (dayData.getPutDataMap() == null) {
				Map<Double, Map<String, MinuteData>> putMap = new LinkedHashMap<>();
				dayData.setPutDataMap(putMap);
			}
			dayData.getPutDataMap().put(strikePrice, mMap);
		}
		
		return;
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
