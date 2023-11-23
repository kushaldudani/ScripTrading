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

public class OrderModifier implements Runnable {
	
	private static String MODIFY_ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/order/";
	
	public static void main(String[] args) {
		// 665210712
		String orderId = "106064239";
		String currentDate = "2023-11-22";
		int qty = 1;
        OrderModifier om = new OrderModifier(MODIFY_ORDER_URL, OrderUtil.getOptionEnterModifyJson(qty, 0, "LMT", 9.5),
        		                             currentDate, "/home/kushaldudani/qqq/optionenter.txt", "9.5", 391, orderId, 0);
        		
        orderId = om.modify();
        System.out.println(orderId);
	}
	
	private HttpClient client;
	private String baseUrl;
	private String orderJson;
	private String currentDate;
	private String writePath;
	private String limitPriceOrMkt;
	private double strike;
	private String orderIdToModify;
	private long contract;
	private String directory;

	
	public OrderModifier(String baseUrl, String orderJson, String currentDate, String writePath, String limitPriceOrMkt, double strike, String orderIdToModify, long contract) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		
		this.baseUrl = baseUrl;
		this.orderJson = orderJson;
		this.currentDate = currentDate;
		this.writePath = writePath;
		this.limitPriceOrMkt = limitPriceOrMkt;
		this.strike = strike;
		this.orderIdToModify = orderIdToModify;
		this.contract = contract;
		this.directory = (writePath.contains("/qqq/")) ? "qqq" : "qqq2";
	}
	
	@Override
	public void run() {
		TradeData tradedata = MetadataUtil.getInstance().readTradeData("/home/kushaldudani/" + directory +"/metadata.txt");
		Trade existingTrade = MetadataUtil.getInstance().readTrade(currentDate, writePath);
		if (existingTrade != null && !existingTrade.getOrderId().equals(orderIdToModify)) {
			if (!writePath.contains("optionenter.txt")) {
				throw new IllegalStateException("Multiple order modification happening for trade other than optionEnter");
			}
			double infiniteLimit = 10.0;
			double existingTradeLimit = Double.parseDouble(existingTrade.getExecutionInfo());
			if (infiniteLimit != existingTradeLimit) {
				// Requires existing Order modification
				String existingOrderId = "";
				int attempts = 0;
				while (attempts < 3) {
					OrderModifier om = new OrderModifier(MODIFY_ORDER_URL, 
							            OrderUtil.getOptionEnterModifyJson(tradedata.getQty(), existingTrade.getContract(), "LMT", infiniteLimit),
							            currentDate, writePath, ""+infiniteLimit, existingTrade.getStrike(), existingTrade.getOrderId(), existingTrade.getContract());
					existingOrderId = om.modify();
					if (!existingOrderId.isEmpty()) {
						break;
					} else {
						try {
							Thread.sleep(5000);
						} catch (Exception e1) {}
					}
				
					attempts++;
				}
			
				if (existingOrderId.isEmpty()) {
					return;
				} else {
					MetadataUtil.getInstance().write(writePath, currentDate, infiniteLimit+"", existingOrderId, existingTrade.getStrike(), existingTrade.getContract());
				}
			}
		}
		
		// Poll the modified order to check for partial fills(assumption: For fully filled the pre step should already fail) to decide to modify intended order or un-modify the existing order.
		
		String orderIdModified = "";
		int attempts = 0;
		while (attempts < 3) {
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
			client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
			orderIdModified = modify();
			
			if (!orderIdModified.isEmpty()) {
				break;
			} else {
				try {
					Thread.sleep(5000);
				} catch (Exception e1) {}
			}
			
			attempts++;
		}
		
		if (!orderIdModified.isEmpty()) {
			MetadataUtil.getInstance().write(writePath, currentDate, limitPriceOrMkt, orderIdModified, strike, contract);
		}
		
	}
	
	
	String modify(){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderId = "";
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = post(baseUrl+orderIdToModify, orderJson);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("modify responseStatusCode 404 ");
				LoggerUtil.getLogger().info("modify responseStatusCode 404 ");
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
				//break;
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
					LoggerUtil.getLogger().info(line);
					
					JSONParser parser = new JSONParser(); 
					JSONArray jsonArray = (JSONArray) parser.parse(line);
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						orderId = (String) resultEntry.get("order_id");
						if (orderId == null) {
							String id = (String) resultEntry.get("id");
							String url = "https://localhost:5000/v1/api/iserver/reply/" + id;
						
							JSONObject postJson = new JSONObject();
							postJson.put("confirmed", true);
						
							orderId = confirmReply(url, postJson.toJSONString());
						}
				        break;
					}
						
					//trade = new Trade(time, closeAtPremium, orderId, qty);
					//return trade;
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
		//currentTime = System.currentTimeMillis();
		//}
		
		return orderId;
	}
	
	private String confirmReply(String baseUrl, String postJson){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderId = "";
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = post(baseUrl, postJson);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("confirmReply responseStatusCode 404 ");
				LoggerUtil.getLogger().info("confirmReply responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//break;
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
				//break;
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
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
		//currentTime = System.currentTimeMillis();
		//}
		
		return orderId;
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
