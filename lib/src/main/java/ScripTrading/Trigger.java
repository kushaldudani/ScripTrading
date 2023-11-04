package ScripTrading;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import playground.GSUtil;

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
	
	
	public void stockEnter(Map<String, MinuteData> minuteDataMap, String time, TradeData tradedata, String currentDate, StringBuilder notifyBuilder) {
		double strikePrice = tradedata.getStrike();
		double cutOffPrice = strikePrice + (0.002 * strikePrice);
		double closeAttime = minuteDataMap.get(time).getClosePrice();
		
		if (closeAttime >= cutOffPrice && time.compareTo("11:30") < 0) {
			int qty = tradedata.getQty() * 100;
			
			MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionenter.txt", currentDate, "inprogress", "na");
			new Thread(new OrderPlacer(ORDER_URL, getPositionEnterJson(time, qty, "BUY"), currentDate, "/home/kushaldudani/qqq/positionenter.txt", true, null, "failed", qty, 0)).start();
			notifyBuilder.append("stockEnter Order Placed : " + time + " Close Price at that time " + closeAttime + "<br>");
		} else {
			LoggerUtil.getLogger().info("Stock Enter tried at " + time + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Stock Enter tried at " + time + " Close Price at that time " + closeAttime + "<br>");
		}
		
	}
	
	public void optionExit(Map<String, MinuteData> minuteDataMap, String time, TradeData tradedata, String currentDate, long optionContract, StringBuilder notifyBuilder,
			String orderId, String executionInfo) {
		DecimalFormat decfor = new DecimalFormat("0.00"); 
		double strikePrice = tradedata.getStrike();
		double eodCutOffPrice = strikePrice - (0.002 * strikePrice);
		
		double closeAttime = minuteDataMap.get(time).getClosePrice();
		double limitPrice = 0.015;
		limitPrice = Double.parseDouble(decfor.format(limitPrice));
		
		if (orderId.equals("")) {
			int qty = tradedata.getQty();
			
			MetadataUtil.getInstance().write("/home/kushaldudani/qqq/optionexit.txt", currentDate, "inprogress", "na");
			new Thread(new OrderPlacer(ORDER_URL, getOptionExitJson(time, qty,  optionContract, "LMT", limitPrice), currentDate, "/home/kushaldudani/qqq/optionexit.txt",
					false, limitPrice+"", limitPrice+"", qty, 0)).start();
			notifyBuilder.append("optionExit LMT Order Placed : " + time + " Close Price at that time " + closeAttime + " Limit price used "+ limitPrice + "<br>");
		} else if (closeAttime > eodCutOffPrice && time.compareTo("12:50") >= 0) {
			int qty = tradedata.getQty();
			
			if (!executionInfo.equals("MKT")) {
				new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getOptionExitModifyJson(qty, optionContract), currentDate, "/home/kushaldudani/qqq/optionexit.txt", "MKT", qty, 0)).start();
				notifyBuilder.append("optionExit Order Modified to MKT : " + time + " Close Price at that time " + closeAttime + "<br>");
			}
		}
		
	}
	
	public void optionEnter(Map<String, MinuteData> minuteDataMap, String triggerTime, double targetedStrikePrice, String currentDate, long optionContract,
			StringBuilder notifyBuilder, double callPriceTotarget, String orderId) {
		DecimalFormat decfor = new DecimalFormat("0.00");  
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		List<GraphSegment> graphSegments = new ArrayList<>();
		Map<String, Double> priceWithTime = new LinkedHashMap<>();
		callPriceTotarget = Double.parseDouble(decfor.format(callPriceTotarget));
		boolean isLastGSU = false;
		try {
			for (String time : minuteDataMap.keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
					minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
					minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
					time, 0.3, priceWithTime);
			}
		
			GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
			isLastGSU = lastGS.identifier.equals("u");
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
		
		if (callPriceTotarget >= 0.1 
			&& isLastGSU == false
			&& triggerTime.compareTo("07:15") > 0 && triggerTime.compareTo("11:00") < 0
				) {
			int qty = 1;
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq/optionenter.txt", currentDate, "inprogress", "na");
				new Thread(new OrderPlacer(ORDER_URL, getOptionEnterJson(triggerTime, qty,  optionContract, "LMT", callPriceTotarget), currentDate, "/home/kushaldudani/qqq/optionenter.txt",
						false, callPriceTotarget+"", callPriceTotarget+"", qty, targetedStrikePrice)).start();
				notifyBuilder.append("optionEnter LMT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getOptionEnterModifyJson(qty, optionContract, "LMT", callPriceTotarget), currentDate, "/home/kushaldudani/qqq/optionenter.txt",
						""+callPriceTotarget, qty, targetedStrikePrice)).start();
				notifyBuilder.append("optionEnter LMT Order Modified : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			}
		} else {
			LoggerUtil.getLogger().info("Option Enter tried at " + triggerTime + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Option Enter tried at " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
		}
		
	}
	
	private String getOptionEnterJson(String time, int qty, long contractid, String orderType, double limitPrice) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", contractid+SYMBOL+time);
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
	private String getOptionEnterModifyJson(int qty, long contractid, String orderType, double limitPrice) {
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
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
	
	private String getOptionExitJson(String time, int qty, long contractid, String orderType, double limitPrice) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", contractid+SYMBOL+time);
        orderObject.put("orderType", orderType);
        orderObject.put("side", "BUY");
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
	private String getOptionExitModifyJson(int qty, long contractid) {
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
        orderObject.put("orderType", "MKT");
        orderObject.put("side", "BUY");
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        
        return orderObject.toJSONString();
	}
	
	private String getPositionEnterJson(String time, int qty, String side) {
		
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
	
	private String getPositionExitJson(String time, int qty, String orderType, double limitPrice) {
		
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
	
	private String getPositionExitModifyJson(int qty, String orderType, double limitPrice) {
		
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
			double enterPrice, double optionexitPrice, String orderId, String executionInfo, StringBuilder notifyBuilder) {
		DecimalFormat decfor = new DecimalFormat("0.00");  
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		List<GraphSegment> graphSegments = new ArrayList<>();
		double dayHigh = 0;
		double limitPrice = 0;
		try {
			for (String time : minuteDataMap.keySet()) {
				if (minuteDataMap.get(time).getClosePrice() > dayHigh) {
					dayHigh = minuteDataMap.get(time).getClosePrice();
				}
				Util.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
					minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
					minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
					time, 0.2);
			}
		
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
			
			if (!executionInfo.equals("") && !executionInfo.equals("MKT") && !executionInfo.equals("inprogress") && orderId.equals("")) {
				limitPrice = Double.parseDouble(executionInfo);
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
		limitPrice = Double.parseDouble(decfor.format(limitPrice));
		
		notifyBuilder.append("StockExit Info : Limit price " + limitPrice + " dayHigh " + dayHigh + "<br>");
		
		if (triggerTime.compareTo("12:35") >= 0 || (dayHigh > 0 && (((closeAttime - dayHigh) / dayHigh) * 100) < -0.8)) {
			int qty = tradedata.getQty() * 100;
			
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionexit.txt", currentDate, "inprogress", "na");
				new Thread(new OrderPlacer(ORDER_URL, getPositionExitJson(triggerTime, qty, "MKT", 0), currentDate, "/home/kushaldudani/qqq/positionexit.txt", false, "MKT", "MKT", qty, 0)).start();
				notifyBuilder.append("stockExit MKT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				if (!executionInfo.equals("MKT")) {
					new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getPositionExitModifyJson(qty, "MKT", 0), currentDate, "/home/kushaldudani/qqq/positionexit.txt", "MKT", qty, 0)).start();
					notifyBuilder.append("stockExit Order Modified to MKT : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
				}
			}
		} else if (limitPrice > 0) {
			int qty = tradedata.getQty() * 100;
			
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionexit.txt", currentDate, "inprogress", "na");
				new Thread(new OrderPlacer(ORDER_URL, getPositionExitJson(triggerTime, qty, "LMT", limitPrice), currentDate, "/home/kushaldudani/qqq/positionexit.txt", false, ""+limitPrice, ""+limitPrice, qty, 0)).start();
				notifyBuilder.append("stockExit LMT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				if (!executionInfo.equals("MKT")) {
					double prevLimitPrice = Double.parseDouble(executionInfo);
					if (limitPrice < prevLimitPrice) {
						new Thread(new OrderModifier(MODIFY_ORDER_URL+orderId, getPositionExitModifyJson(qty, "LMT", limitPrice), currentDate, "/home/kushaldudani/qqq/positionexit.txt", ""+limitPrice, qty, 0)).start();
						notifyBuilder.append("stockExit Order Modified with change in LMT price : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
					}
				}
			}
		} else {
			LoggerUtil.getLogger().info("Stock Exit tried at " + triggerTime + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Stock Exit tried at " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
		}
	}

}
