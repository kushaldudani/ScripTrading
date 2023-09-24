package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class OrderTracker implements Runnable {
	
	private Map<String, MinuteData> minuteDataMap;
	private Trade optionExitTrade = null;
	private Trade stockEnterTrade = null;
	private Trade stockExitTrade = null;
	private Trigger trigger = new Trigger();
	private TradeData tradedata = null;
	
	public OrderTracker(Map<String, MinuteData> minuteDataMap) {
		this.minuteDataMap = minuteDataMap;
	}

	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		String currentDateString = sdf.format(currentDate);
		
		tradedata = MetadataUtil.getInstance().read(currentDateString);
		optionExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/optionexit.txt");
		stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/positionenter.txt");
		stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/positionexit.txt");
		
		SimpleDateFormat monthformat = new SimpleDateFormat("MMM");
		String monthString = monthformat.format(currentDate) + currentDateString.substring(2, 4);
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String timeToTrigger = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
		if (timeToTrigger.compareTo("06:30") < 0) {
			timeToTrigger = "06:30";
		}
		
		String timeToTriggerWithSec = timeToTrigger + ":00";
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
		
		while (timeToTrigger.compareTo("12:55") <= 0) {
			try {
				if (minuteDataMap.containsKey(timeToTrigger)) {
					calendar.setTimeInMillis(System.currentTimeMillis());
					String currentTime = timeFormatter.format(calendar.getTime());
					int diffBCTAndTTT = Util.diffTimeWithSec(timeToTriggerWithSec, currentTime);
					// stock enter
					if (diffBCTAndTTT < 150 && tradedata != null && tradedata.getStrike() > 0 && (stockEnterTrade == null || stockEnterTrade.getExecutionInfo().equals("") || stockEnterTrade.getExecutionInfo().equals("failed"))) {
						trigger.stockEnter(minuteDataMap, timeToTrigger, tradedata, currentDateString);
					}
					// option exit
					if (diffBCTAndTTT < 150 && tradedata != null && tradedata.getStrike() > 0 && (optionExitTrade == null || optionExitTrade.getExecutionInfo().equals("") || optionExitTrade.getExecutionInfo().equals("failed"))) {
						ContractSearcher cs = new ContractSearcher();
						long conId = cs.search("https://localhost:5000/v1/api/iserver/secdef/info", currentDateString, tradedata.getStrike(), monthString);
						if (conId > 0) {
							trigger.optionExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, conId);
						}
					}
					// stock exit
					if (diffBCTAndTTT < 150 && stockEnterTrade != null
							&& !stockEnterTrade.getExecutionInfo().equals("failed")
							&& !stockEnterTrade.getExecutionInfo().equals("")
							&& !stockEnterTrade.getExecutionInfo().equals("inprogress")
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
						
						trigger.stockExit(minuteDataMap, timeToTrigger, tradedata, currentDateString, enterPrice, optionexitPrice, orderId, stockExitTrade.getExecutionInfo());
					}
				
					timeToTrigger = Util.timeNMinsAgo(timeToTrigger, -5);
					calendar.setTimeInMillis(System.currentTimeMillis());
					currentTime = timeFormatter.format(calendar.getTime());
					timeToTriggerWithSec = timeToTrigger + ":00";
					int timeToSleep = Util.diffTimeWithSec(currentTime, timeToTriggerWithSec) > 5 ? ((Util.diffTimeWithSec(currentTime, timeToTriggerWithSec) - 5) * 1000) : 0;
					
					Thread.sleep(timeToSleep);
					tradedata = MetadataUtil.getInstance().read(currentDateString);
					optionExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/optionexit.txt");
					stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/positionenter.txt");
					stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/Users/kushd/qqq/positionexit.txt");
				} else {
					int timeToSleep = 1000;
					Thread.sleep(timeToSleep);
				}
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}
	

}
