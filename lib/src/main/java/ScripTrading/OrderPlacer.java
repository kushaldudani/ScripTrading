package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

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
	
	private static String MODIFY_ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/order/";
	
	public static void main(String[] args) {
		// 106064239
        //double limitPrice = 365.128;
        //String triggerTime = "10:05";
        String currentDate = "2023-12-05";
        int qty = 1;
        long optionContract = 667566485;
        String cOID = "LT"+currentDate+"OE"+optionContract;
        OrderPlacer op = new OrderPlacer(ORDER_URL, OrderUtil.getOptionEnterJson(qty,  optionContract, "LMT", 10.0, cOID), currentDate, "/home/kushaldudani/qqq/optionenter.txt",
				"10.0", 390, cOID, optionContract);
        //String orderId = op.place();
		//System.out.println(orderId);
        //String orderId = op.pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "?filters=filled", cOID);
        //System.out.println(orderId);
	}
	
	// https://localhost:5000/v1/api/iserver/account/U12784344/orders?conid=320227571&secType=STK&cOID=QQQ12:15&orderType=LMT&side=SELL&ticker=QQQ&tif=DAY&quantity=100&price=365.128
	
	
	private HttpClient client;
	private String baseUrl;
	private String orderJson;
	private String currentDate;
	private String writePath;
	private String eInfoToWrite;
	private double strike;
	private String cOID;
	private long contract;
	private String directory;

	
	public OrderPlacer(String baseUrl, String orderJson, String currentDate, String writePath,
			String eInfoToWrite, double strike, String cOID, long contract) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		this.client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		this.baseUrl = baseUrl;
		this.orderJson = orderJson;
		this.currentDate = currentDate;
		this.writePath = writePath;
		this.eInfoToWrite = eInfoToWrite;
		this.strike = strike;
		this.cOID = cOID;
		this.contract = contract;
		this.directory = (writePath.contains("/qqq/")) ? "qqq" : "qqq2";
	}
	
	@Override
	public void run() {
		TradeData tradedata = MetadataUtil.getInstance().readTradeData("/home/kushaldudani/" + directory +"/metadata.txt");
		Trade existingTrade = MetadataUtil.getInstance().readTrade(currentDate, writePath);
		String existingOrderId = "";
		if (existingTrade != null && !existingTrade.getOrderId().equals("") && !existingTrade.getOrderId().equals("na")) {
			if (!writePath.contains("optionenter.txt")) {
				throw new IllegalStateException("Multiple order modification happening for trade other than optionEnter");
			}
			double infiniteLimit = 10.0;
			double existingTradeLimit = Double.parseDouble(existingTrade.getExecutionInfo());
			if (infiniteLimit != existingTradeLimit) {
				// Requires existing Order modification
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
			} else {
				existingOrderId = existingTrade.getOrderId();
			}
		}
		
		// Poll the modified order to check for partial fills(assumption: For fully filled the pre step should already fail) to decide to place a new order or un-modify the existing order.
		
		PlaceReply newOrderPlaceReply = new PlaceReply();
		int attempts = 0;
		while (attempts < 3) {
			//String newOId = pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "?filters=submitted,filled", cOID);
			//if (!newOId.isEmpty()) {
			//	newOrderPlaceReply.orderId = newOId;
			//	break;
			//}
			
			//RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
			//client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
			if (newOrderPlaceReply == null || newOrderPlaceReply.replyUrl == null) {
				newOrderPlaceReply = place();
			} else {
				newOrderPlaceReply = confirmReply(newOrderPlaceReply.replyUrl, newOrderPlaceReply.replyJson);
			}
			
			if (!(newOrderPlaceReply.orderId.isEmpty())) {
				break;
			} else {
				try {
					Thread.sleep(5000);
				} catch (Exception e1) {}
			}
			
			attempts++;
		}
		
		String newOrderId = newOrderPlaceReply.orderId;
		if (!newOrderId.isEmpty()) {
			MetadataUtil.getInstance().write(writePath, currentDate, eInfoToWrite, newOrderId, strike, contract);
			if (writePath.contains("optionenter.txt")) {
				String strikeenterordermapPath = "/home/kushaldudani/" + directory + "/strikeenterordermap.txt";
				Map<Double, String> strikeToEnterOrderMap = MetadataUtil.getInstance().readStrikeEnterOrderMap(currentDate, strikeenterordermapPath);
				strikeToEnterOrderMap.put(strike, newOrderId);
				MetadataUtil.getInstance().writeStrikeEnterOrderMap(strikeenterordermapPath, currentDate, strikeToEnterOrderMap);
			}
		} else {
			if (existingOrderId.isEmpty()) {
				MetadataUtil.getInstance().write(writePath, currentDate, eInfoToWrite, "na", strike, contract);
			}
		}
	}
	
	
	private PlaceReply place(){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		PlaceReply placeReply = new PlaceReply();
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
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
						String id = (String) resultEntry.get("id");
						String url = "https://localhost:5000/v1/api/iserver/reply/" + id;
						
						JSONObject postJson = new JSONObject();
						postJson.put("confirmed", true);
						
						placeReply = confirmReply(url, postJson.toJSONString());
						
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
		
		return placeReply;
	}
	
	private PlaceReply confirmReply(String baseUrl, String postJson){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		PlaceReply placeReply = new PlaceReply();
		placeReply.replyUrl = baseUrl; placeReply.replyJson = postJson; 
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
							placeReply = confirmReply(url, crRecurringPostJson.toJSONString());
						} else {
							placeReply.orderId = (String) resultEntry.get("order_id");
						}
						return placeReply;
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
		
		return placeReply;
	}
	
	/*private String pollOrder(String baseUrl, String paramString, String cOID) {
		accountPing();
		
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderId = "";
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
						System.out.println(line);
					JSONParser parser = new JSONParser(); 
					JSONObject jsonObject = (JSONObject) parser.parse(line);
					JSONArray jsonArray = (JSONArray) jsonObject.get("orders");
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						String fCOID = (String) resultEntry.get("order_ref");
						String foDesc = (String) resultEntry.get("orderDesc");
						String foId = (Long) resultEntry.get("orderId") + "";
						String foLimitPrice = (String) resultEntry.get("price");
						LoggerUtil.getLogger().info("OrderPoller in OrderPlacer order_ref " + fCOID + " orderDesc " + foDesc + " Limit price " + foLimitPrice + " orderId " + foId);
						if (cOID.equals(fCOID)) {
							orderId =  foId;
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
		
		return orderId;
	}
	
	void accountPing(){
		String baseUrl = "https://localhost:5000/v1/api/iserver/accounts";
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try{
			HttpResponse response = HttpUtil.get(baseUrl, "", client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("tickle responseStatusCode 404 ");
				LoggerUtil.getLogger().info("tickle responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
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
				//inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				//bufferedReader = new BufferedReader(inputStreamReader);
				//String line;  
				//while ((line = bufferedReader.readLine()) != null) {
					
					
				//}
			}
		}catch(Exception e){
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
		
		return;
	}*/
	
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
	
	
	static class PlaceReply {
		String orderId="";
		String replyUrl;
		String replyJson;
	}

}
