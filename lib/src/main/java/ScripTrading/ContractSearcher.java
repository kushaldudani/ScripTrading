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

public class ContractSearcher {
	
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=NOV23&right=C&exchange=SMART&strike=370
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=SEP23&right=C&exchange=SMART&strike=370
	// VIX contract id -> 13455763 https://localhost:5000/v1/api/iserver/marketdata/history?conid=13455763&period=30d&bar=1d
	// java -Djavax.net.ssl.trustStore="/usr/lib/jvm/java-11-openjdk-amd64/conf/security/jssecacerts" -jar combined_2023-11-15.jar
	
	public static void main(String[] args) {
		ContractSearcher cs = new ContractSearcher();
		
		//cs.searchPrequisite("https://localhost:5000/v1/api/iserver/secdef/search");
		long conId = cs.search("https://localhost:5000/v1/api/iserver/secdef/info", "2023-11-22", 391, "NOV23", "10:00", "C");
		System.out.println(conId);
	}
	
	
	private HttpClient client;

	
	public ContractSearcher() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	private void searchPrequisite(String baseUrl){
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		//long cId = 0;
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = post(baseUrl, getJsonString());
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("ContractSearcher responseStatusCode 404 ");
				LoggerUtil.getLogger().info("ContractSearcher responseStatusCode 404 ");
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
				//String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				//while ((line = bufferedReader.readLine()) != null) {
					//System.out.println(line);
					
				//}
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
		
		return ;
	}
	
	long searchWithTries(String baseUrl, String expiryDate, double strike, String monthString, String time, String callOrPut){
		int attempts = 0;
		long cId = 0;
		while (attempts < 3) {
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
			client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
			cId = search(baseUrl, expiryDate, strike, monthString, time, callOrPut);
			if (cId > 0) {
				break;
			} else {
				try {
					Thread.sleep(3000);
				} catch (Exception e1) {}
			}
			
			attempts++;
		}
		
		return cId;
	}
	
	private long search(String baseUrl, String expiryDate, double strike, String monthString, String time, String callOrPut){
		Util.reauthIfNeeded(TickleMapProvider.getInstance().getTickleMap(), time);
		searchPrequisite("https://localhost:5000/v1/api/iserver/secdef/search");
		
		expiryDate = expiryDate.substring(0, 4) + expiryDate.substring(5, 7) + expiryDate.substring(8, 10);
		String paramString = getParamString(strike, monthString, callOrPut);
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		long cId = 0;
		//long startTime = System.currentTimeMillis();
		//long currentTime = System.currentTimeMillis();
		//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = HttpUtil.get(baseUrl, paramString, client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("ContractSearcher responseStatusCode 404 ");
				LoggerUtil.getLogger().info("ContractSearcher responseStatusCode 404 ");
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
					JSONArray jsonArray = (JSONArray) parser.parse(line);
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						String maturityDate = (String) resultEntry.get("maturityDate");
						
						if (maturityDate.equals(expiryDate)) {
							return (long) resultEntry.get("conid");
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
		//currentTime = System.currentTimeMillis();
		//}
		
		return cId;
	}
	
	private String getParamString(double strike, String monthString, String callOrPut) {
		
		return "?conid=320227571&sectype=OPT&month="+monthString+"&right="+callOrPut+"&exchange=SMART&strike="+strike;
		
	}
	
	private String getJsonString() {
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("symbol", "QQQ");
		jsonObject.put("secType", "IND");
        
        return jsonObject.toJSONString();
		
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
