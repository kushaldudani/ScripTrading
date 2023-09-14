package ScripTrading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OrderPlacer {
	
	private HttpClient client;

	
	public OrderPlacer() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60*1000).setConnectTimeout(60*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	
	Trade enter(String baseUrl, String orderJson, String time, double closeAtPremium, int qty){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		Trade trade = null;
		//Double price = null;
		while (responseStatusCode != 200) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = retry(baseUrl, orderJson);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("responseStatusCode 404 ");
				LoggerUtil.getLogger().info("responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			//if (responseStatusCode == 429) { // Too many requests
			//	Thread.sleep(10000);
			//}
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
						JSONObject json = (JSONObject) parser.parse(line);
						
						trade = new Trade(time, closeAtPremium, (String) json.get("id"), (String) json.get("message"), qty);
						trade.setEnterPrice(closeAtPremium);
					/**/
					
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				//e1.printStackTrace();
			}
		}finally{
			if(bufferedReader != null){
				try {
					bufferedReader.close();
				} catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try {
					inputStreamReader.close();
				} catch (IOException e) {}
			}
		}
		}
		
		return trade;
	}
	
	private HttpResponse retry(String baseUrl, String jsonString) throws Exception {
		
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
			}catch(Exception e){e.printStackTrace();}
		//	try {
		//		Thread.sleep(nooftries * 1000);
		//	} catch (InterruptedException e) {}
		//	nooftries++;
		//}
			//System.out.println(responsecode);
		
		return response;
	}
	
	
	boolean exit(String baseUrl, String orderJson){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		boolean succeed = false;
		//Double price = null;
		while (responseStatusCode != 200) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = retry(baseUrl, orderJson);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("responseStatusCode 404 ");
				LoggerUtil.getLogger().info("responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			//if (responseStatusCode == 429) { // Too many requests
			//	Thread.sleep(10000);
			//}
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
						JSONObject json = (JSONObject) parser.parse(line);
						
						String id = (String) json.get("id");
						String message = (String) json.get("message");
						
						if (!message.contains("?")) {
							succeed = true;
						}
					/**/
					
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				//e1.printStackTrace();
			}
		}finally{
			if(bufferedReader != null){
				try {
					bufferedReader.close();
				} catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try {
					inputStreamReader.close();
				} catch (IOException e) {}
			}
		}
		}
		
		return succeed;
	}

}