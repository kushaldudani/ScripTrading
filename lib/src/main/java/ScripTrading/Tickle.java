package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Tickle {
	
	public static void main(String[] args) {
		Tickle cs = new Tickle();
		
		boolean isAuthenticated = cs.tickle();
		//cs.accountPing();
		System.out.println(isAuthenticated);
	}
	
	
	private CloseableHttpClient client;

	
	public Tickle() {
		client = HttpUtil.createHttpClient();
	}
	
	
	boolean tickle(){
		String baseUrl = "https://localhost:5000/v1/api/tickle";
		boolean isAuthenticated = false;
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		CloseableHttpResponse response = null;
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			response = HttpUtil.post(baseUrl, client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("tickle responseStatusCode 404 ");
				LoggerUtil.getLogger().info("tickle responseStatusCode 404 ");
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
					JSONParser parser = new JSONParser(); 
					JSONObject jsonObject = (JSONObject) parser.parse(line);
					
					//sessionId = (String) jsonObject.get("session");
					JSONObject iserver = (JSONObject) jsonObject.get("iserver");
					if (iserver != null) {
						JSONObject authStatus = (JSONObject) iserver.get("authStatus");
						if (authStatus != null) {
							isAuthenticated = (Boolean) authStatus.get("authenticated");
						}
					}
					// System.out.println(line);
					
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
		//currentTime = System.currentTimeMillis();
		//}
		
		return isAuthenticated;
	}
	
	
	
	
	/*void accountPing(){
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

}
