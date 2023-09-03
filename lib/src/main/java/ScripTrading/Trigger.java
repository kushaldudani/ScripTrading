package ScripTrading;

import java.util.Map;

public class Trigger {
	
	private static double OPTION_PREMIUM_RISE_PERCENT = 10;
	
	
	public Trade tradeEnter(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap, String time) {
		
		double premiumAtTime = premiumMap.get(time);
		Double premiumBefore = premiumMap.get(Util.timeNMinsAgo(time, 5));
		double closeAtPremium = minuteDataMap.get(time).getClosePrice();
		
		if (premiumBefore == null) {
			return null;
		}
		
		if ((((premiumAtTime - premiumBefore) / premiumBefore) * 100) >= OPTION_PREMIUM_RISE_PERCENT) {
			return new Trade(time, closeAtPremium);
		} else {
			LoggerUtil.getLogger().info("Trade Enter tried at " + time + " Price " + closeAtPremium + " Premium " + premiumAtTime);
		}
		
		return null;
	}
	
	public boolean tradeExit(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap, String time, Trade trade) {
		boolean momentumDisappeared = false;
		double premiumAtTime = premiumMap.get(time);
		Double premiumBefore1 = premiumMap.get(Util.timeNMinsAgo(time, 5));
		Double premiumBefore2 = premiumMap.get(Util.timeNMinsAgo(time, 10));
		Double premiumBefore3 = premiumMap.get(Util.timeNMinsAgo(time, 15));
		double closeAtPremium = minuteDataMap.get(time).getClosePrice();
		
		if (premiumBefore1 == null || premiumBefore2 == null || premiumBefore3 == null) {
			return false;
		}
		
		if (premiumAtTime < premiumBefore1 && premiumBefore1 < premiumBefore2 && premiumBefore2 < premiumBefore3) {
			momentumDisappeared = true;
		}
		
		if (momentumDisappeared) {
			if (closeAtPremium > trade.getEnterPrice()) {
				LoggerUtil.getLogger().info("Trade exited at " + time + " Price " + closeAtPremium);
				return true;
			}
		}
		
		int timeH = (time.charAt(0) == '0') ? Integer.parseInt(time.substring(1, 2)) : Integer.parseInt(time.substring(0, 2));
		int timeM = (time.charAt(3) == '0') ? Integer.parseInt(time.substring(4, 5)) : Integer.parseInt(time.substring(3, 5));
		if (timeH >= 12 && timeM >= 50) {
			LoggerUtil.getLogger().info("Trade exited at " + time + " Price " + closeAtPremium);
			return true;
		}
		
		return false;
	}

}
