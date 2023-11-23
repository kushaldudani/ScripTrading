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

public class SingleDayQQQLongTester {
	
	private static double getTargetedStrike(DayData dayData, String time) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = (time.compareTo("08:45") <= 0) ? 0.009 : 0.009;
		double priceLevelToPlaceOrder = (((closeAtTime - openAtTime) / openAtTime) * 100 < -0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder + percentHigherFactor * priceLevelToPlaceOrder));
		
		return targetedStrikePrice;
	}
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date runDate = sdf.parse("2023-11-17");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Map<String, DayData> dayDataMap = new LinkedHashMap<>();
		LinkedList<String> callVolumeSignal = new LinkedList<>();
		Map<String, Double> longBokkedProfitMap = new LinkedHashMap<>();
		
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
				double longEnterPrice = 0.0; String longEnterTime = null; String longEnterString = "";
				int noOfEntriesForBull = 0;
				callVolumeSignal = new LinkedList<>();
				LinkedList<String> altCallVolumeSignal = new LinkedList<>();
				String strikeTime = null; double strike = 0;   // Need to be updated with every run
				double avgVix = 0; // Need to be fixed
				Map<String, MinuteData> rawVix = null; // Need to be fixed
				LinkedList<Double> callOptionQueue = new LinkedList<>();
				LinkedList<Double> alternateCallOptionQueue = new LinkedList<>();
				double alternateStrike = getTargetedStrike(dayData, "07:20");
				while (time.compareTo(VolumeGraphPatternEntry.closeTime) < 0) {
					//double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					//String key = currentDateString + "  " + time;
					double callAvgVolume = 0; double optionVolumeAtTime = 0;
					double alternateoptionVolumeAtTime = 0; double alternateCallAvgVolume = 0;
					
					if (time.compareTo("07:20") >= 0) {
						downloadOptionData(alternateStrike, currentDateString, dayData, downloader);
						if (dayData.getCallDataMap().get(alternateStrike).containsKey(time)) {
							alternateoptionVolumeAtTime = dayData.getCallDataMap().get(alternateStrike).get(time).getVolume();
						}
						
						alternateCallAvgVolume = (alternateCallOptionQueue.size() > 0) ? findAvg(alternateCallOptionQueue) : 0;
						if (alternateCallAvgVolume > 0 && alternateoptionVolumeAtTime > (2.8 * alternateCallAvgVolume) ) {
							altCallVolumeSignal.add(time);
						}
						if (alternateoptionVolumeAtTime > 0) {
							alternateCallOptionQueue.add(alternateoptionVolumeAtTime);
							if (alternateCallOptionQueue.size() > 9) {
								alternateCallOptionQueue.poll();
							}
						}
					}
					if (strikeTime != null && time.compareTo(strikeTime) >= 0) {
						downloadOptionData(strike, currentDateString, dayData, downloader);
						if (dayData.getCallDataMap().get(strike).containsKey(time)) {
							optionVolumeAtTime = dayData.getCallDataMap().get(strike).get(time).getVolume();
						}
						
						callAvgVolume = (callOptionQueue.size() > 0) ? findAvg(callOptionQueue) : 0;
						if (callAvgVolume > 0 && optionVolumeAtTime > (2.8 * callAvgVolume) ) {
							callVolumeSignal.add(time);
						}
						if (optionVolumeAtTime > 0) {
							callOptionQueue.add(optionVolumeAtTime);
							if (callOptionQueue.size() > 9) {
								callOptionQueue.poll();
							}
						}
					}
					
					// Long Enter
					String returnedTime = VolumeGraphPatternEntry.bullEntry(dayData, 0.3, time, callVolumeSignal, altCallVolumeSignal, strike, avgVix, rawVix, alternateStrike);
					if (returnedTime != null
							&& longEnterPrice == 0
							&& noOfEntriesForBull == 0
							//&& (prevClosePrice > 0 && (((closeAtTime - prevClosePrice) / prevClosePrice) * 100) < 2) 
							) {
						String[] returnedTimeVals = returnedTime.split("  ");
						time = returnedTimeVals[0];
						if (returnedTimeVals[1].equals("true")) {
							longEnterPrice = dayData.getMinuteDataMap().get(returnedTimeVals[0]).getClosePrice();
							longEnterTime = returnedTimeVals[0]; 
							longEnterString = "bullEntry";
							noOfEntriesForBull++;
						}
					}
					
					// Long Exit
					if (longEnterPrice > 0) {
						time = VolumeGraphPatternEntry.longExit(dayData, 0.3, longEnterTime, longEnterPrice, currentDateString, longBokkedProfitMap, longEnterString, strike, strikeTime
								);
						longEnterPrice = 0;
						longEnterTime = null;
						longEnterString = "";
					}
					
					time = Util.timeNMinsAgo(time, -5);
				}
				
			
				for (String key : longBokkedProfitMap.keySet()) {
					String row = key + "  " + longBokkedProfitMap.get(key);
					System.out.println(row);
				}
		
		
	}
	
	private static void downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader) {
		if (!dayData.getCallDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			dayData.getCallDataMap().put(strikePrice, mMap);
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
