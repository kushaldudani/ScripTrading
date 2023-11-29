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
	
	private static double getTargetedStrike(DayData dayData, String time) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = (time.compareTo("08:45") <= 0) ? 0.009 : 0.009;
		double priceLevelToPlaceOrder = (((closeAtTime - openAtTime) / openAtTime) * 100 > 0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
		
		return targetedStrikePrice;
	}
	
	private static void calculateOptionVolumeSig(LinkedList<Double> alternateOptionQueue, LinkedList<String> altVolumeSignal, double altStrike, String time, DayData dayData) {
		String timeCnr = "07:20";
		while (timeCnr.compareTo(time) <= 0) {
			double alternateoptionVolumeAtTime = 0;
			if (dayData.getPutDataMap().get(altStrike).containsKey(timeCnr)) {
				alternateoptionVolumeAtTime = dayData.getPutDataMap().get(altStrike).get(timeCnr).getVolume();
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
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date runDate = sdf.parse("2023-11-29");
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
			
			
				String time = "06:45";//VolumeGraphPatternEntry.startTime;
				double shortEnterPrice = 0.0; String shortEnterTime = null; String shortEnterString = "";
				int noOfEntriesForBear = 0;
				putVolumeSignal = new LinkedList<>();
				LinkedList<String> hugePositiveBars = new LinkedList<>();
				LinkedList<String> hugeNegativeBars = new LinkedList<>();
				String strikeTime = null; double strike = 0; // Need to be updated with every run
				double avgVix = 0; // Need to be fixed
				Map<String, MinuteData> rawVix = null; // Need to be fixed
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					VolumeGraphPatternEntry.calculateBarSizes(dayData, 0.3, time, hugePositiveBars, hugeNegativeBars);
					double alternateStrike = getTargetedStrike(dayData, time);
					//String key = currentDateString + "  " + time;
					LinkedList<String> altPutVolumeSignal = new LinkedList<>();
					if (time.compareTo("07:20") >= 0 && time.compareTo("08:55") <= 0) {
						LinkedList<Double> alternateCallOptionQueue = new LinkedList<>();
						downloadOptionData(alternateStrike - 1, currentDateString, dayData, downloader);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike - 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike - 1));
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadOptionData(alternateStrike, currentDateString, dayData, downloader);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + alternateStrike);
						
						alternateCallOptionQueue = new LinkedList<>();
						downloadOptionData(alternateStrike + 1, currentDateString, dayData, downloader);
						calculateOptionVolumeSig(alternateCallOptionQueue, altPutVolumeSignal, alternateStrike + 1, time, dayData);
						//System.out.println("alternateCallOptionQueue " + alternateCallOptionQueue);
						//System.out.println("alternateStrike " + (alternateStrike + 1));
						
						altPutVolumeSignal.sort(String::compareToIgnoreCase);
						//System.out.println("altCallVolumeSignal " + altCallVolumeSignal);
					}
					
					// Short Enter
					String returnedTime = VolumeGraphPatternEntry.bearEntry(dayData, 0.3, time, putVolumeSignal, altPutVolumeSignal, strike, avgVix, rawVix, alternateStrike,
							hugePositiveBars, hugeNegativeBars);
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
