package ScripTrading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Trigger {
	
	public static void main(String[] args) {
		//String json = getOrderJson("MSFT", "07:30", 1, "BUY");
		
		//System.out.println(json);
	}
	
	//private static double OPTION_PREMIUM_RISE_PERCENT = 10;
	
	//private static double TRANSACTION_SIZE = 400; // Dollar Amount
	
	private static String ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/orders";
	
	private static String MODIFY_ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/order/";
	
	private static String SYMBOL = "QQQ";
	
	
	public void stockEnter(Map<String, MinuteData> minuteDataMap, String time, TradeData tradedata, String currentDate) {
		double strikePrice = tradedata.getStrike();
		double cutOffPrice = strikePrice + (0.002 * strikePrice);
		double closeAttime = minuteDataMap.get(time).getClosePrice();
		
		if (closeAttime >= cutOffPrice && time.compareTo("11:30") < 0) {
			int qty = tradedata.getQty();
			
			MetadataUtil.getInstance().write("/Users/kushd/qqq/positionenter.txt", currentDate, "inprogress", "na");
			new Thread(new OrderPlacer(ORDER_URL, getPositionEnterJson(time, qty, "BUY"), currentDate, "/Users/kushd/qqq/positionenter.txt", true, "error")).start();
		} else {
			LoggerUtil.getLogger().info("Stock Enter tried at " + time + " Price " + closeAttime);
		}
		
	}
	
	public void optionExit(Map<String, MinuteData> minuteDataMap, String time, TradeData tradedata, String currentDate, long optionContract) {
		double strikePrice = tradedata.getStrike();
		double cutOffPrice = strikePrice + (0.002 * strikePrice);
		double closeAttime = minuteDataMap.get(time).getClosePrice();
		double eodCutOffPrice = strikePrice - (0.002 * strikePrice);
		
		if (closeAttime >= cutOffPrice && time.compareTo("11:30") < 0) {
			int qty = tradedata.getQty();
			
			MetadataUtil.getInstance().write("/Users/kushd/qqq/optionexit.txt", currentDate, "inprogress", "na");
			new Thread(new OrderPlacer(ORDER_URL, getOptionExitJson(time, qty,  optionContract), currentDate, "/Users/kushd/qqq/optionexit.txt", true, "error")).start();
		} else if (closeAttime > eodCutOffPrice && time.compareTo("12:50") >= 0) {
			int qty = tradedata.getQty();
			
			MetadataUtil.getInstance().write("/Users/kushd/qqq/optionexit.txt", currentDate, "inprogress", "na");
			new Thread(new OrderPlacer(ORDER_URL, getOptionExitJson(time, qty,  optionContract), currentDate, "/Users/kushd/qqq/optionexit.txt", true, "error")).start();
		} else {
			LoggerUtil.getLogger().info("Option Exit tried at " + time + " Price " + closeAttime);
		}
		
	}
	
	private static String getOptionExitJson(String time, int qty, long contractid) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", contractid+SYMBOL+time);
        orderObject.put("orderType", "MKT");
        orderObject.put("side", "BUY");
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        
        orderArray.add(orderObject);
        
        JSONObject topObject = new JSONObject();
        topObject.put("orders", orderArray);
        
        return topObject.toJSONString();
	}
	
	private static String getPositionEnterJson(String time, int qty, String side) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
		orderObject.put("secType", "STK");
		orderObject.put("cOID", SYMBOL+time);
        orderObject.put("orderType", "MKT");
        orderObject.put("side", side);
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        
        orderArray.add(orderObject);
        
        JSONObject topObject = new JSONObject();
        topObject.put("orders", orderArray);
        
        return topObject.toJSONString();
	}
	
	private static String getPositionExitJson(String time, int qty, String orderType, double limitPrice) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
		orderObject.put("secType", "STK");
		orderObject.put("cOID", SYMBOL+time);
        orderObject.put("orderType", orderType);
        orderObject.put("side", "SELL");
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        if (orderType.equals("LMT")) {
        	orderObject.put("price", limitPrice);
        }
        
        orderArray.add(orderObject);
        
        JSONObject topObject = new JSONObject();
        topObject.put("orders", orderArray);
        
        return topObject.toJSONString();
	}
	
	private static String getPositionExitModifyJson(int qty, String orderType, double limitPrice) {
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
        orderObject.put("orderType", orderType);
        orderObject.put("side", "SELL");
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        if (orderType.equals("LMT")) {
        	orderObject.put("price", limitPrice);
        }
        
        return orderObject.toJSONString();
	}
	
	public void stockExit(Map<String, MinuteData> minuteDataMap, String triggerTime, TradeData tradedata, String currentDate,
			double enterPrice, double optionexitPrice, String orderId, String executionInfo) {
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		List<GraphSegment> graphSegments = new ArrayList<>();
		double dayHigh = 0;
		for (String time : minuteDataMap.keySet()) {
			if (minuteDataMap.get(time).getClosePrice() > dayHigh) {
				dayHigh = minuteDataMap.get(time).getClosePrice();
			}
			Util.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
					minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
					minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
					time, 0.2);
		}
		
		double limitPrice = 0;
		GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
		if (!lastGS.identifier.equals("u") && !lastGS.identifier.equals("ur")) {
			limitPrice = enterPrice + optionexitPrice;
			if (lastGS.identifier.equals("d") || lastGS.identifier.equals("dr")) {
				limitPrice = enterPrice;
			}
			if (triggerTime.compareTo("12:30") >= 0) {
				limitPrice = enterPrice;
			}
		}
		
		if (triggerTime.compareTo("12:50") >= 0 || (((closeAttime - dayHigh) / dayHigh) * 100) < -0.8) {
			int qty = tradedata.getQty();
			
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/Users/kushd/qqq/positionexit.txt", currentDate, "inprogress", "na");
				new Thread(new OrderPlacer(ORDER_URL, getPositionExitJson(triggerTime, qty, "MKT", 0), currentDate, "/Users/kushd/qqq/positionexit.txt", false, "MKT")).start();
			} else {
				if (!executionInfo.equals("MKT")) {
					new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getPositionExitModifyJson(qty, "MKT", 0), currentDate, "/Users/kushd/qqq/positionexit.txt", "MKT")).start();
				}
			}
		} else if (limitPrice > 0) {
			int qty = tradedata.getQty();
			
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/Users/kushd/qqq/positionexit.txt", currentDate, "inprogress", "na");
				new Thread(new OrderPlacer(ORDER_URL, getPositionExitJson(triggerTime, qty, "LMT", limitPrice), currentDate, "/Users/kushd/qqq/positionexit.txt", false, ""+limitPrice)).start();
			} else {
				if (!executionInfo.equals("MKT")) {
					double prevLimitPrice = Double.parseDouble(executionInfo);
					if (limitPrice != prevLimitPrice) {
						new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getPositionExitModifyJson(qty, "LMT", limitPrice), currentDate, "/Users/kushd/qqq/positionexit.txt", ""+limitPrice)).start();
					}
				}
			}
		} else {
			LoggerUtil.getLogger().info("Stock Exit tried at " + triggerTime + " Limit Price " + limitPrice);
		}
	}

}
