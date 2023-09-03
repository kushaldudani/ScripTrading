package ScripTrading;

import java.util.Map;

public class OrderTracker implements Runnable {
	
	private Map<String, MinuteData> minuteDataMap;
	private Map<String, Double> premiumMap;
	private Trade trade = null;
	private Trigger trigger = new Trigger();
	
	public OrderTracker(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap) {
		this.minuteDataMap = minuteDataMap;
		this.premiumMap = premiumMap;
	}

	@Override
	public void run() {
		
		String timeToTrigger = "06:30";
		
		while (timeToTrigger.compareTo("12:55") <= 0) {
			if (minuteDataMap.containsKey(timeToTrigger) && premiumMap.containsKey(timeToTrigger)) {
				if (trade == null) {
					trade = trigger.tradeEnter(minuteDataMap, null, timeToTrigger);
				} else {
					if (trigger.tradeExit(minuteDataMap, null, timeToTrigger, trade))
						trade = null;
				}
				
				timeToTrigger = Util.timeNMinsAgo(timeToTrigger, -5);
				
				int timeToSleep = 240000;
				try {
					Thread.sleep(timeToSleep);
				} catch (InterruptedException e) {
					LoggerUtil.getLogger().info(e.getMessage());
				}
			} else {
				int timeToSleep = 1000;
				try {
					Thread.sleep(timeToSleep);
				} catch (InterruptedException e) {
					LoggerUtil.getLogger().info(e.getMessage());
				}
			}
		}
		
	}

}
