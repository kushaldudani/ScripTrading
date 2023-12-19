package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OrderPollerUtil {
	
	private static OrderPollerUtil singleInstance = null;
	
	public static synchronized OrderPollerUtil getInstance() {
        if (singleInstance == null) {
        	singleInstance = new OrderPollerUtil();
        }
  
        return singleInstance;
    }
	
	
	public synchronized Map<String, String> pollOrder(String baseUrl, String paramString, CloseableHttpClient client) {
		accountPing(client);
		
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		CloseableHttpResponse response = null;
		Map<String, String> pollresultMap = new LinkedHashMap<>();
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			response = HttpUtil.get(baseUrl, paramString, client);
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
						String fCOID = (String) resultEntry.get("order_ref");
						String forderId = (Long) resultEntry.get("orderId") + "";
						String foStatus = (String) resultEntry.get("status");
						if ("Submitted".equalsIgnoreCase(foStatus) || "Filled".equalsIgnoreCase(foStatus)) {
							pollresultMap.put(fCOID, forderId);
						}
						/*if (forderId.equals(orderId)) {
							double executedPrice =  Double.parseDouble((String) resultEntry.get("avgPrice"));
							long conid = (Long) resultEntry.get("conid");
							pollresult = new PollResult(executedPrice, conid);
							break;
						}*/
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
			if(response != null){
				try {
					response.close();
				} catch (Exception e) {}
			}
		}
		
		return pollresultMap;
	}
	
	private void accountPing(CloseableHttpClient client){
		String baseUrl = "https://localhost:5000/v1/api/iserver/accounts";
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		CloseableHttpResponse response = null;
		try{
			response = HttpUtil.get(baseUrl, "", client);
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
			if(response != null){
				try {
					response.close();
				} catch (Exception e) {}
			}
		}
		
		return;
	}
	
	public synchronized Map<String, String> pollOrderInactive(String baseUrl, String paramString, CloseableHttpClient client) {
		accountPing(client);
		
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		CloseableHttpResponse response = null;
		Map<String, String> pollresultMap = new LinkedHashMap<>();
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			response = HttpUtil.get(baseUrl, paramString, client);
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
						String fCOID = (String) resultEntry.get("order_ref");
						String forderId = (Long) resultEntry.get("orderId") + "";
						String foStatus = (String) resultEntry.get("status");
						if ("Inactive".equalsIgnoreCase(foStatus)) {
							pollresultMap.put(fCOID, forderId);
						}
						/*if (forderId.equals(orderId)) {
							double executedPrice =  Double.parseDouble((String) resultEntry.get("avgPrice"));
							long conid = (Long) resultEntry.get("conid");
							pollresult = new PollResult(executedPrice, conid);
							break;
						}*/
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
			if(response != null){
				try {
					response.close();
				} catch (Exception e) {}
			}
		}
		
		return pollresultMap;
	}

}
