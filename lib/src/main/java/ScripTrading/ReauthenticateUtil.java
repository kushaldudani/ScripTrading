package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

public class ReauthenticateUtil {
	
	public static void main(String[] args) {
		new ReauthenticateUtil().reauth();
	}
	
	private CloseableHttpClient client;

	
	public ReauthenticateUtil() {
		client = HttpUtil.createHttpClient();
	}
	
	
	void reauth(){
		String baseUrl = "https://localhost:5000/v1/api/iserver/reauthenticate";
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		CloseableHttpResponse response = null;
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			response = HttpUtil.post(baseUrl, client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("reauth responseStatusCode 404 ");
				LoggerUtil.getLogger().info("reauth responseStatusCode 404 ");
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
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//JSONParser parser = new JSONParser(); 
					//JSONObject jsonObject = (JSONObject) parser.parse(line);
					
					//sessionId = (String) jsonObject.get("session");
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
		
		return;
	}
	
}
