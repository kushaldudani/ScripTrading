package ScripTrading;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OrderUtil {
	
	private static String SYMBOL = "QQQ";
	
	static JSONObject getOptionEnterJson(int qty, long contractid, String orderType, double limitPrice, String cOID) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", cOID);
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
        
        return topObject;
	}
	
	static String getOptionEnterModifyJson(int qty, long contractid, String orderType, double limitPrice) {
		
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
	
	static JSONObject getOptionExitJson(int qty, long contractid, String orderType, double limitPrice, String cOID) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", cOID);
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
        
        return topObject;
	}
	
	static String getOptionExitModifyJson(int qty, long contractid) {
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
        orderObject.put("orderType", "MKT");
        orderObject.put("side", "BUY");
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        
        return orderObject.toJSONString();
	}
	
	static JSONObject getPositionJson(int qty, String orderType, double limitPrice, String side, String cOID) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
		orderObject.put("secType", "STK");
		orderObject.put("cOID", cOID);
		orderObject.put("orderType", orderType);
        orderObject.put("side", side);
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        if (orderType.equals("LMT")) {
        	orderObject.put("price", limitPrice);
        }
        
        orderArray.add(orderObject);
        
        JSONObject topObject = new JSONObject();
        topObject.put("orders", orderArray);
        
        return topObject;
	}
	
	static String getPositionModifyJson(int qty, String orderType, double limitPrice, String side) {
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
        orderObject.put("orderType", orderType);
        orderObject.put("side", side);
        orderObject.put("ticker", SYMBOL);
        orderObject.put("tif", "DAY");
        orderObject.put("quantity", qty);
        if (orderType.equals("LMT")) {
        	orderObject.put("price", limitPrice);
        }
        
        return orderObject.toJSONString();
	}

}
