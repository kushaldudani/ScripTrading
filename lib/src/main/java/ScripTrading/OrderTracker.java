package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrderTracker implements Runnable {
	
	private Map<String, MinuteData> minuteDataMap;
	private Map<String, String> notifictionMap;
	private Trade optionEnterTrade = null;
	private Trade optionExitTrade = null;
	private Trade stockEnterTrade = null;
	private Trade stockExitTrade = null;
	private Trigger trigger = new Trigger();
	private TradeData tradedata = null;
	private TradeConfirmation tradeconfirmation = null;
	private Set<String> timeTracker = new HashSet<>();
	private double volatility = 0;
	private String startTime = "07:15";
	
	public OrderTracker(Map<String, MinuteData> minuteDataMap, Map<String, String> notifictionMap) {
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
		
		
		while (timeToTrigger.compareTo("12:55") < 0) {
			try {
				if (minuteDataMap.containsKey(timeToTrigger) && !timeTracker.contains(timeToTrigger)) {
					StringBuilder notifyBuilder = new StringBuilder();
					double avgVolatility = MetadataUtil.getInstance().readVolatilityData();
					tradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString);
					tradedata = MetadataUtil.getInstance().readTradeData(currentDateString);
					notifyBuilder.append("TradeData : " + tradedata + "<br>");
					optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/optionenter.txt");
					notifyBuilder.append("optionEnterTrade : " + optionEnterTrade + "<br>");
					optionExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/optionexit.txt");
					notifyBuilder.append("optionExitTrade : " + optionExitTrade + "<br>");
					stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/positionenter.txt");
					notifyBuilder.append("stockEnterTrade : " + stockEnterTrade + "<br>");
					stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/positionexit.txt");
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
					if ((tradeconfirmation == null || tradeconfirmation.getHasOrderFilled() == false)
							&& volatility < (1.66 * avgVolatility)) {
						double closeAttime = minuteDataMap.get(timeToTrigger).getClosePrice();
						double percentHigherFactor = 0.009;
						double targetedStrikePrice = ((int) (closeAttime + percentHigherFactor * closeAttime));
						double callPriceTotarget = 0.0;
						long conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, targetedStrikePrice, monthString, timeToTrigger, "C");
						if (conId > 0) {
							MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, timeToTrigger, "&exchange=CBOE");
							if (mData != null) {
								callPriceTotarget = mData.getClosePrice();
								LoggerUtil.getLogger().info(timeToTrigger + " Call Price at " + targetedStrikePrice + "  " + callPriceTotarget);
								notifyBuilder.append(timeToTrigger + " Call Price at " + targetedStrikePrice + "  " + callPriceTotarget + "<br>");
							}
						}
						callPriceTotarget = callPriceTotarget * 1.1;
						
						String orderId = "";
						if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("") && !optionEnterTrade.getOrderId().equals("na")) {
							orderId = optionEnterTrade.getOrderId();
						}
						
						if (targetedStrikePrice > 0 && callPriceTotarget > 0) {
							trigger.optionEnter(minuteDataMap, timeToTrigger, targetedStrikePrice, currentDateString, conId, notifyBuilder, callPriceTotarget, orderId);
						}
					}
					// stock enter
					/*if (tradedata != null && tradedata.getStrike() > 0 && (stockEnterTrade == null || stockEnterTrade.getExecutionInfo().equals("") || stockEnterTrade.getExecutionInfo().equals("failed"))) {
						trigger.stockEnter(minuteDataMap, timeToTrigger, tradedata, currentDateString, notifyBuilder);
					}*/
					// option exit
					if (tradedata != null && tradeconfirmation != null && tradeconfirmation.getHasOrderFilled() && tradedata.getStrike() > 0
							&& (optionExitTrade == null || !optionExitTrade.getExecutionInfo().equals("inprogress"))) {
						long conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, tradedata.getStrike(), monthString, timeToTrigger, "C");
						if (conId > 0) {
							MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, timeToTrigger, "&exchange=CBOE");
							if (mData != null) {
								LoggerUtil.getLogger().info(timeToTrigger + " In optionExit, Call Price at " + tradedata.getStrike() + "  " + mData.getClosePrice());
								notifyBuilder.append(timeToTrigger + " In optionExit, Call Price at " + tradedata.getStrike() + "  " + mData.getClosePrice() + "<br>");
							}
							
							String orderId = "";
							if (optionExitTrade != null && !optionExitTrade.getOrderId().equals("") && !optionExitTrade.getOrderId().equals("na")) {
								orderId = optionExitTrade.getOrderId();
							}
							
							trigger.optionExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, conId, notifyBuilder, orderId, optionExitTrade.getExecutionInfo());
						}
					}
					// stock exit
					/*if (stockEnterTrade != null
							&& !stockEnterTrade.getExecutionInfo().equals("failed")
							&& !stockEnterTrade.getExecutionInfo().equals("")
							&& !stockEnterTrade.getExecutionInfo().equals("inprogress")
							&& optionExitTrade != null
							&& !optionExitTrade.getExecutionInfo().equals("failed")
							&& !optionExitTrade.getExecutionInfo().equals("")
							&& !optionExitTrade.getExecutionInfo().equals("inprogress")
							&& (stockExitTrade == null || !stockExitTrade.getExecutionInfo().equals("inprogress"))) {
						double enterPrice = Double.parseDouble(stockEnterTrade.getExecutionInfo());
						double optionexitPrice = Double.parseDouble(optionExitTrade.getExecutionInfo());
						
						String orderId = "";
						if (stockExitTrade != null && !stockExitTrade.getOrderId().equals("") && !stockExitTrade.getOrderId().equals("na")) {
							orderId = stockExitTrade.getOrderId();
						}
						
						trigger.stockExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, enterPrice, optionexitPrice, orderId, stockExitTrade.getExecutionInfo(), notifyBuilder);
					}*/
					
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
	
	private double calculateVolatility(double closeAtStartTime, String currentDateString, String triggerTime, String monthString) {
		double volatility = 0;
		
		double closeAtStartTimeFloor = (int) closeAtStartTime;
		
		long conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "C");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, startTime, "&exchange=CBOE");
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "P");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, startTime, "&exchange=CBOE");
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		
		closeAtStartTimeFloor = closeAtStartTimeFloor + 1;
		
		conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "C");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, startTime, "&exchange=CBOE");
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		conId = new ContractSearcher().search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, closeAtStartTimeFloor, monthString, triggerTime, "P");
		if (conId > 0) {
			MinuteData mData = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", conId, startTime, "&exchange=CBOE");
			if (mData != null) {
				volatility = volatility + mData.getClosePrice();
			}
		}
		
		return volatility;
	}
	

}
