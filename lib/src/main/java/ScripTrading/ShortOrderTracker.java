package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ShortOrderTracker implements Runnable {
	
	private Map<String, MinuteData> minuteDataMap;
	private Map<String, String> notifictionMap;
	private Trade optionEnterTrade = null;
	private Trade optionExitTrade = null;
	private Trade stockEnterTrade = null;
	private Trade stockExitTrade = null;
	private ShortTrigger trigger = new ShortTrigger();
	private TradeData tradedata = null;
	private Map<Double, String> strikeToEnterOrderMap = null;
	private TradeConfirmation optiontradeconfirmation = null;
	private TradeConfirmation stocktradeconfirmation = null;
	private TradeConfirmation stockexittradeconfirmation = null;
	private Set<String> timeTracker = new HashSet<>();
	private double volatility = 0;
	private String startTime = "07:10";
	
	public ShortOrderTracker(Map<String, MinuteData> minuteDataMap, Map<String, String> notifictionMap) {
		this.minuteDataMap = minuteDataMap;
		this.notifictionMap = notifictionMap;
	}

	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		String currentDateString = sdf.format(currentDate);
		
		SimpleDateFormat monthformat = new SimpleDateFormat("MMM");
		String monthString = monthformat.format(currentDate) + currentDateString.substring(2, 4);
		monthString = monthString.toUpperCase();
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String timeToTrigger = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
		timeToTrigger = Util.timeNMinsAgo(timeToTrigger, 5);
		
		Map<Double, Long> strikeToConId = new LinkedHashMap<>();
		
		while (timeToTrigger.compareTo("12:55") < 0) {
			try {
				if (minuteDataMap.containsKey(timeToTrigger) && !timeTracker.contains(timeToTrigger)) {
					StringBuilder notifyBuilder = new StringBuilder();
					double avgVolatility = MetadataUtil.getInstance().readVolatilityData();
					optiontradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/optiontradeconfirmation.txt");
					stocktradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/stocktradeconfirmation.txt");
					stockexittradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/stockexittradeconfirmation.txt");
					tradedata = MetadataUtil.getInstance().readTradeData("/home/kushaldudani/qqq2/metadata.txt");
					strikeToEnterOrderMap = MetadataUtil.getInstance().readStrikeEnterOrderMap(currentDateString, "/home/kushaldudani/qqq2/strikeenterordermap.txt");
					optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/optionenter.txt");
					notifyBuilder.append("optionEnterTrade : " + optionEnterTrade + "<br>");
					optionExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/optionexit.txt");
					notifyBuilder.append("optionExitTrade : " + optionExitTrade + "<br>");
					stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/positionenter.txt");
					notifyBuilder.append("stockEnterTrade : " + stockEnterTrade + "<br>");
					stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/positionexit.txt");
					notifyBuilder.append("stockExitTrade : " + stockExitTrade + "<br>");
					
					// CalculateVolatility
					if (timeToTrigger.compareTo(startTime) >= 0 && volatility == 0) {
						volatility = calculateVolatility(minuteDataMap.get(startTime).getClosePrice(), currentDateString, timeToTrigger, monthString); 
						LoggerUtil.getLogger().info(timeToTrigger + " Today's volatility " + volatility + " Avg. volatility " + avgVolatility);
						notifyBuilder.append(timeToTrigger + " Today's volatility " + volatility + " Avg. volatility " + avgVolatility + "<br>");
						if (volatility >= (1.66 * avgVolatility)) {
							LoggerUtil.getLogger().info(timeToTrigger + " Today's volatility too high, not going to do options sell trades ");
							notifyBuilder.append(timeToTrigger + " Today's volatility too high, not going to do options sell trades " + "<br>");
						}
					}
					
					// option enter
					if ((optiontradeconfirmation == null || optiontradeconfirmation.getHasOrderFilled() == false)
							&& volatility < (1.66 * avgVolatility) && tradedata != null) {
						double closeAttime = minuteDataMap.get(timeToTrigger).getClosePrice();
						double targetedStrikePrice = getTargetedStrikePrice(timeToTrigger);
						double putPriceTotarget = 0.0;
						MinuteData mData = new PolygonMarketHistory().dataWithRetries(currentDateString, targetedStrikePrice, "P").get(timeToTrigger);
						if (mData != null) {
							putPriceTotarget = mData.getClosePrice();
							LoggerUtil.getLogger().info("ShortOrderTracker " + timeToTrigger + " Put Price at " + targetedStrikePrice + "  " + putPriceTotarget);
							notifyBuilder.append(timeToTrigger + " Put Price at " + targetedStrikePrice + "  " + putPriceTotarget + "<br>");
							
							putPriceTotarget = putPriceTotarget * 1.1;
							putPriceTotarget = Math.max(putPriceTotarget, ((0.05 * closeAttime) / 100));
						}
						
						String orderId = "";
						if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("") && !optionEnterTrade.getOrderId().equals("na")) {
							orderId = optionEnterTrade.getOrderId();
						}
						
						if (targetedStrikePrice > 0 && !strikeToConId.containsKey(targetedStrikePrice)) {
							long fetchedOptionId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, targetedStrikePrice, monthString, timeToTrigger, "P");
							if (fetchedOptionId > 0) {
								strikeToConId.put(targetedStrikePrice, fetchedOptionId);
							}
						}
						
						if (targetedStrikePrice > 0 && putPriceTotarget > 0 && strikeToConId.containsKey(targetedStrikePrice)) {
							trigger.optionEnter(minuteDataMap, timeToTrigger, targetedStrikePrice, currentDateString, strikeToConId.get(targetedStrikePrice), notifyBuilder, putPriceTotarget, orderId, tradedata,
									strikeToEnterOrderMap);
						}
					}
					// stock enter
					if (volatility < (1.66 * avgVolatility) && tradedata != null //&& optiontradeconfirmation != null && optiontradeconfirmation.getHasOrderFilled()
							&& (stocktradeconfirmation == null || stocktradeconfirmation.getHasOrderFilled() == false)
							&& (stockEnterTrade == null || !stockEnterTrade.getExecutionInfo().equals("inprogress")) ) {
						
						String orderId = "";
						if (stockEnterTrade != null && !stockEnterTrade.getOrderId().equals("") && !stockEnterTrade.getOrderId().equals("na")) {
							orderId = stockEnterTrade.getOrderId();
						}
						
						LinkedList<String> putVolumeSignal = new LinkedList<>();
						if (timeToTrigger.compareTo("07:20") >= 0 && timeToTrigger.compareTo("08:55") <= 0) {
							double strikeUsed = getTargetedStrikePrice(timeToTrigger);
							LoggerUtil.getLogger().info("ShortOrderTracker " + timeToTrigger + " StockEnter Strike to do volume calc " + strikeUsed);
						
							LinkedList<Double> putOptionQueue = new LinkedList<>();
							Map<String, MinuteData> mDataMap = new PolygonMarketHistory().dataWithRetries(currentDateString, strikeUsed, "P");
							calculatePutVolumeSignal(putVolumeSignal, putOptionQueue, timeToTrigger, mDataMap);
							notifyBuilder.append("StockEnter Info : Put Volume Queue " + putOptionQueue + " StrikeUsed " + strikeUsed + "<br>");
							
							putOptionQueue = new LinkedList<>();
							mDataMap = new PolygonMarketHistory().dataWithRetries(currentDateString, strikeUsed + 1, "P");
							calculatePutVolumeSignal(putVolumeSignal, putOptionQueue, timeToTrigger, mDataMap);
							notifyBuilder.append("StockEnter Info : Put Volume Queue " + putOptionQueue + " StrikeUsed " + (strikeUsed + 1) + "<br>");
							
							putOptionQueue = new LinkedList<>();
							mDataMap = new PolygonMarketHistory().dataWithRetries(currentDateString, strikeUsed - 1, "P");
							calculatePutVolumeSignal(putVolumeSignal, putOptionQueue, timeToTrigger, mDataMap);
							notifyBuilder.append("StockEnter Info : Put Volume Queue " + putOptionQueue + " StrikeUsed " + (strikeUsed - 1) + "<br>");
							
							putVolumeSignal.sort(String::compareToIgnoreCase);
						}
						notifyBuilder.append("StockEnter Info : Put Volume Signal " + putVolumeSignal + "<br>");
						
						trigger.stockEnter(minuteDataMap, timeToTrigger, tradedata, currentDateString, notifyBuilder, orderId, putVolumeSignal, optiontradeconfirmation.getStrike(), stockEnterTrade.getExecutionInfo());
					}
					// option exit
					if (tradedata != null && optiontradeconfirmation != null && optiontradeconfirmation.getHasOrderFilled()
							&& (optionExitTrade == null || !optionExitTrade.getExecutionInfo().equals("inprogress"))) {
						double enteredStrike = optiontradeconfirmation.getStrike();
						//MinuteData mData = new PolygonMarketHistory().data(currentDateString, enteredStrike, "P").get(timeToTrigger);
						//if (mData != null) {
						//	LoggerUtil.getLogger().info("ShortOrderTracker " + timeToTrigger + " In optionExit, Put Price at " + enteredStrike + "  " + mData.getClosePrice());
						//	notifyBuilder.append(timeToTrigger + " In optionExit, Put Price at " + enteredStrike + "  " + mData.getClosePrice() + "<br>");
						//}
							
						String orderId = "";
						if (optionExitTrade != null && !optionExitTrade.getOrderId().equals("") && !optionExitTrade.getOrderId().equals("na")) {
							orderId = optionExitTrade.getOrderId();
						}
						
						if (enteredStrike > 0 && !strikeToConId.containsKey(enteredStrike)) {
							long fetchedOptionId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, enteredStrike, monthString, timeToTrigger, "P");
							if (fetchedOptionId > 0) {
								strikeToConId.put(enteredStrike, fetchedOptionId);
							}
						}
						
						if (strikeToConId.containsKey(enteredStrike)) {
							trigger.optionExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, strikeToConId.get(enteredStrike), notifyBuilder, orderId, optionExitTrade.getExecutionInfo(), enteredStrike);
						}
					}
					// stock exit
					if (tradedata != null && stocktradeconfirmation != null && stocktradeconfirmation.getHasOrderFilled()
							&& (stockexittradeconfirmation == null || stockexittradeconfirmation.getHasOrderFilled() == false)
							&& (stockExitTrade == null || !stockExitTrade.getExecutionInfo().equals("inprogress")) ) {
						
						String orderId = "";
						if (stockExitTrade != null && !stockExitTrade.getOrderId().equals("") && !stockExitTrade.getOrderId().equals("na")) {
							orderId = stockExitTrade.getOrderId();
						}
						
						double enterPrice = Double.parseDouble(stockEnterTrade.getExecutionInfo());
						
						trigger.stockExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, orderId, stockExitTrade.getExecutionInfo(), notifyBuilder, enterPrice);
					}
					
					timeTracker.add(timeToTrigger);
					notifictionMap.put(timeToTrigger, notifyBuilder.toString());
				}
				
				int timeToSleep = 2000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				timeToTrigger = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
				timeToTrigger = Util.timeNMinsAgo(timeToTrigger, 5);
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}
	
	private double getTargetedStrikePrice(String timeToFetch) {
		double closeAttime = minuteDataMap.get(timeToFetch).getClosePrice();
		double openAtTime = minuteDataMap.get(timeToFetch).getOpenPrice();
		double percentHigherFactor = 0.009;
		double priceLevelToPlaceOrder = (Math.abs(((closeAttime - openAtTime) / openAtTime) * 100) > 0.2) ? (closeAttime + openAtTime) / 2 : closeAttime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
		
		return targetedStrikePrice;
	}
	
	private void calculatePutVolumeSignal(LinkedList<String> putVolumeSignal, LinkedList<Double> putOptionQueue, String timeToTrigger,
			Map<String, MinuteData> mDataMap) {
		String timeCntr = "07:20";
		double avgOptionVolume = 0;
		while (timeCntr.compareTo(timeToTrigger) <= 0) {
			double optionVolumeAtTime = 0;
			MinuteData mData = mDataMap.get(timeCntr);
			if (mData != null) {
				optionVolumeAtTime = mData.getVolume();
			}
			
			//
			avgOptionVolume = 0;
			Iterator<Double> queueIterator = putOptionQueue.iterator();
			int queueCount = 0; int maxCount = 9;
			while (queueCount < maxCount && queueIterator.hasNext()) {
				avgOptionVolume = avgOptionVolume + queueIterator.next();
				queueCount++;
			}
			avgOptionVolume = (queueCount > 0) ? (avgOptionVolume / queueCount) : 0;
			//
			if (avgOptionVolume > 0 && optionVolumeAtTime > (3 * avgOptionVolume) ) {
				putVolumeSignal.add(timeCntr);
			}
			if (optionVolumeAtTime > 0) {
				putOptionQueue.addFirst(optionVolumeAtTime);
			}
			
			timeCntr = Util.timeNMinsAgo(timeCntr, -5);
		}
		
		return;
	}
	
	private double calculateVolatility(double closeAtStartTime, String currentDateString, String triggerTime, String monthString) {
		double volatility = 0;
		
		double closeAtStartTimeFloor = (int) closeAtStartTime;
		
		long conId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "C");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, "&exchange=CBOE", "1d", "5min").get(startTime);
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		conId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "P");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, "&exchange=CBOE", "1d", "5min").get(startTime);
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		
		closeAtStartTimeFloor = closeAtStartTimeFloor + 1;
		
		conId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "C");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, "&exchange=CBOE", "1d", "5min").get(startTime);
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		conId = new ContractSearcher().searchWithTries("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "P");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, "&exchange=CBOE", "1d", "5min").get(startTime);
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		
		return volatility;
	}
	

}
