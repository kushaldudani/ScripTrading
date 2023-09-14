package ScripTrading;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Trigger {
	
	public static void main(String[] args) {
		String json = getOrderJson("MSFT", "07:30", 1, "BUY");
		
		System.out.println(json);
	}
	
	private static double OPTION_PREMIUM_RISE_PERCENT = 10;
	
	private static double TRANSACTION_SIZE = 1000;
	
	private static String ORDER_URL = "";
	
	private static String SYMBOL = "MSFT";
	
	
	public Trade tradeEnter(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap, String time) {
		
		double premiumAtTime = premiumMap.get(time);
		Double premiumBefore = premiumMap.get(Util.timeNMinsAgo(time, 5));
		double closeAtPremium = minuteDataMap.get(time).getClosePrice();
		
		if (premiumBefore == null) {
			return null;
		}
		
		if ((((premiumAtTime - premiumBefore) / premiumBefore) * 100) >= OPTION_PREMIUM_RISE_PERCENT) {
			int qty = (int) (TRANSACTION_SIZE / closeAtPremium);
			return new OrderPlacer().enter(ORDER_URL, getOrderJson(SYMBOL, time, qty, "BUY"), time, closeAtPremium, qty);
		} else {
			LoggerUtil.getLogger().info("Trade Enter tried at " + time + " Price " + closeAtPremium + " Premium " + premiumAtTime);
		}
		
		return null;
	}
	
	private static String getOrderJson(String symbol, String time, int qty, String side) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 0);
		orderObject.put("secType", "");
		orderObject.put("cOID", symbol+time);
        orderObject.put("orderType", "MKT");
        orderObject.put("side", side);
        orderObject.put("ticker", symbol);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        
        orderArray.add(orderObject);
        
        JSONObject topObject = new JSONObject();
        topObject.put("orders", orderArray);
        
        return topObject.toJSONString();
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
				LoggerUtil.getLogger().info("Trade exit attempt " + time + " Price " + closeAtPremium);
				return new OrderPlacer().exit(ORDER_URL, getOrderJson(SYMBOL, time, trade.getQty(), "SELL"));
			}
		}
		
		int timeH = (time.charAt(0) == '0') ? Integer.parseInt(time.substring(1, 2)) : Integer.parseInt(time.substring(0, 2));
		int timeM = (time.charAt(3) == '0') ? Integer.parseInt(time.substring(4, 5)) : Integer.parseInt(time.substring(3, 5));
		if (timeH >= 12 && timeM >= 50) {
			LoggerUtil.getLogger().info("Trade exit attempt " + time + " Price " + closeAtPremium);
			return new OrderPlacer().exit(ORDER_URL, getOrderJson(SYMBOL, time, trade.getQty(), "SELL"));
		}
		
		return false;
	}

}
