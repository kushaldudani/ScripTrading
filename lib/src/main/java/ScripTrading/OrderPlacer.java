package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OrderPlacer implements Runnable {
	
	private static String ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/orders";
	
	public static void main(String[] args) {
		
        //double limitPrice = 365.128;
        String triggerTime = "10:30";
        int qty = 1;
        String currentDate = "2023-10-23";
        OrderPlacer op = new OrderPlacer(ORDER_URL, getOptionEnterJson(triggerTime, qty,  658738797, "LMT", 0.2), currentDate, "/home/kushaldudani/qqq/optionenter.txt",
				false, "0.2", "0.2", qty, 360);
        String orderId = op.place();
		System.out.println(orderId);
	}
	private static String getOptionEnterJson(String time, int qty, long contractid, String orderType, double limitPrice) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", contractid);
		orderObject.put("secType", "OPT");
		orderObject.put("cOID", contractid+"QQQ"+time);
        orderObject.put("orderType", orderType);
        orderObject.put("side", "SELL");
        orderObject.put("ticker", "QQQ");
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
	// https://localhost:5000/v1/api/iserver/account/U12784344/orders?conid=320227571&secType=STK&cOID=QQQ12:15&orderType=LMT&side=SELL&ticker=QQQ&tif=DAY&quantity=100&price=365.128
	private static String getPositionExitJson(String time, int qty, String orderType, double limitPrice) {
		
		JSONArray orderArray = new JSONArray();
		
		JSONObject orderObject = new JSONObject();
		orderObject.put("conid", 320227571);
		orderObject.put("secType", "STK");
		orderObject.put("cOID", "QQQ"+time);
        orderObject.put("orderType", orderType);
        orderObject.put("side", "SELL");
        orderObject.put("ticker", "QQQ");
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
	
	private HttpClient client;
	private String baseUrl;
	private String orderJson;
	private String currentDate;
	private String writePath;
	private boolean needPolling;
	private String eInfoWhenSucceeded;
	private String eInfoWhenFailed;
	private int qty;
	private double strikePrice;

	
	public OrderPlacer(String baseUrl, String orderJson, String currentDate, String writePath, boolean needPolling,
			String eInfoWhenSucceeded, String eInfoWhenFailed, int qty, double strikePrice) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		
		this.baseUrl = baseUrl;
		this.orderJson = orderJson;
		this.currentDate = currentDate;
		this.writePath = writePath;
		this.needPolling = needPolling;
		this.eInfoWhenSucceeded = eInfoWhenSucceeded;
		this.eInfoWhenFailed = eInfoWhenFailed;
		this.qty = qty;
		this.strikePrice = strikePrice;
	}
	
	@Override
	public void run() {
		String orderId = place();
		double executedPrice = 0;
		if (!orderId.isEmpty()) {
			String oIdStringToWrite = orderId;
			String einfoToWrite = eInfoWhenSucceeded;
			if (needPolling) {
				int attempts = 0;
				while (executedPrice == 0 && attempts < 15) {
					executedPrice = pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "?filters=filled", orderId);
					try {
						Thread.sleep(5000);
					} catch (Exception e1) {}
					attempts++;
				}
			
				if (executedPrice > 0) {
					if (einfoToWrite == null) {
						einfoToWrite = executedPrice+"";
					}
					MetadataUtil.getInstance().write(writePath, currentDate, einfoToWrite, oIdStringToWrite);
				}
			} else {
				MetadataUtil.getInstance().write(writePath, currentDate, einfoToWrite, oIdStringToWrite);
				
				if (strikePrice > 0) {
					MetadataUtil.getInstance().writeTradeData(currentDate, strikePrice, qty);
				}
			}
		} else {
			String oIdStringToWrite = "na";
			String einfoToWrite = eInfoWhenFailed;//(needPolling == true) ? "failed" : limitPriceOrMkt;
			MetadataUtil.getInstance().write(writePath, currentDate, einfoToWrite, oIdStringToWrite);
		}
	}
	
	
	private String place(){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderId = "";
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = post(baseUrl, orderJson);
			System.out.println(response.getStatusLine());
			//System.out.println(response);
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("stockenter responseStatusCode 404 ");
				LoggerUtil.getLogger().info("stockenter responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			if (responseStatusCode == 429) { // Too many requests
				Thread.sleep(5000);
			}
			if(responseStatusCode == 500){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					System.out.println(line);
					LoggerUtil.getLogger().info(line);
				}
				break;
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						System.out.println(line);
						LoggerUtil.getLogger().info(line);
					
					JSONParser parser = new JSONParser(); 
					JSONArray jsonArray = (JSONArray) parser.parse(line);
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						String id = (String) resultEntry.get("id");
						String url = "https://localhost:5000/v1/api/iserver/reply/" + id;
						
						JSONObject postJson = new JSONObject();
						postJson.put("confirmed", true);
						
						orderId = confirmReply(url, postJson.toJSONString());
						
				        break;
					}
						
					//trade = new Trade(time, closeAtPremium, orderId, qty);
					//return trade;
				}
			}
		}catch(Exception e){
			//e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (Exception e1) {
				//e1.printStackTrace();
			}
		}finally{
			if(bufferedReader != null){
				try {
					bufferedReader.close();
				} catch (Exception e) {}
			}
			if(inputStreamReader != null){
				try {
					inputStreamReader.close();
				} catch (Exception e) {}
			}
		}
		currentTime = System.currentTimeMillis();
		}
		
		return orderId;
	}
	
	private String confirmReply(String baseUrl, String postJson){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderId = "";
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = post(baseUrl, postJson);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("confirmReply responseStatusCode 404 ");
				LoggerUtil.getLogger().info("confirmReply responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
			}
			//if (responseStatusCode == 429) { // Too many requests
			//	Thread.sleep(5000);
			//}
			if(responseStatusCode == 500){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					System.out.println(line);
					LoggerUtil.getLogger().info(line);
				}
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						System.out.println(line);
						LoggerUtil.getLogger().info(line);
					JSONParser parser = new JSONParser(); 
					JSONArray jsonArray = (JSONArray) parser.parse(line);
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						//System.out.println(resultEntry);
						String id = (String) resultEntry.get("id");
						if (id != null) {
							String url = "https://localhost:5000/v1/api/iserver/reply/" + id;
							JSONObject crRecurringPostJson = new JSONObject();
							crRecurringPostJson.put("confirmed", true);
							orderId = confirmReply(url, crRecurringPostJson.toJSONString());
						} else {
							orderId = (String) resultEntry.get("order_id");
						}
						return orderId;
					}
					
				}
			}
		}catch(Exception e){
			//e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (Exception e1) {
				//e1.printStackTrace();
			}
		}finally{
			if(bufferedReader != null){
				try {
					bufferedReader.close();
				} catch (Exception e) {}
			}
			if(inputStreamReader != null){
				try {
					inputStreamReader.close();
				} catch (Exception e) {}
			}
		}
		
		return orderId;
	}
	
	private double pollOrder(String baseUrl, String paramString, String orderId) {
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		double executedPrice = 0;
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = HttpUtil.get(baseUrl, paramString, client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("pollOrder responseStatusCode 404 ");
				LoggerUtil.getLogger().info("pollOrder responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				//break;
			}
			if(responseStatusCode == 500){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					System.out.println(line);
					LoggerUtil.getLogger().info(line);
				}
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						//System.out.println(line);
					JSONParser parser = new JSONParser(); 
					JSONObject jsonObject = (JSONObject) parser.parse(line);
					JSONArray jsonArray = (JSONArray) jsonObject.get("orders");
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						String forderId = (Long) resultEntry.get("orderId") + "";
						if (forderId.equals(orderId)) {
							executedPrice =  Double.parseDouble((String) resultEntry.get("avgPrice"));
							break;
						}
					}
					
				}
			}
		}catch(Exception e){
			//e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
		}finally{
			if(bufferedReader != null){
				try {
					bufferedReader.close();
				} catch (Exception e) {}
			}
			if(inputStreamReader != null){
				try {
					inputStreamReader.close();
				} catch (Exception e) {}
			}
		}
		
		return executedPrice;
	}
	
	private HttpResponse post(String baseUrl, String jsonString) throws Exception {
		
		System.out.println(baseUrl);
		HttpPost post = new HttpPost(baseUrl);

		StringEntity requestEntity = new StringEntity(
				jsonString,
			    ContentType.APPLICATION_JSON);
        // add request parameter, form parameters
        //List<NameValuePair> urlParameters = new ArrayList<>();
        //urlParameters.add(new BasicNameValuePair("username", "abc"));
        //urlParameters.add(new BasicNameValuePair("password", "123"));
        //urlParameters.add(new BasicNameValuePair("custom", "secret"));

        post.setEntity(requestEntity);
		//request.setHeader("User-Agent", "runscope/0.1");
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		//request.setHeader("Accept", "*/*");
		int responsecode=0;
		//int nooftries = 1;
		HttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(post);
				responsecode = response.getStatusLine().getStatusCode();
			}catch(Exception e){
				e.printStackTrace();
				LoggerUtil.getLogger().info(e.getMessage());	
			}
		//	try {
		//		Thread.sleep(nooftries * 1000);
		//	} catch (InterruptedException e) {}
		//	nooftries++;
		//}
			//System.out.println(responsecode);
		
		return response;
	}
	
	
	

}
