package ScripTrading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ContractSearcher {
	
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=NOV23&right=C&exchange=SMART&strike=370
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=SEP23&right=C&exchange=SMART&strike=370
	
	public static void main(String[] args) {
		ContractSearcher cs = new ContractSearcher();
		
		long conId = cs.search("https://localhost:5000/v1/api/iserver/secdef/info", "2023-09-19", 370, "SEP23");
		System.out.println(conId);
	}
	
	
	private HttpClient client;

	
	public ContractSearcher() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60*1000).setConnectTimeout(60*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	
	long search(String baseUrl, String expiryDate, double strike, String monthString){
		expiryDate = expiryDate.substring(0, 4) + expiryDate.substring(5, 7) + expiryDate.substring(8, 10);
		String paramString = getParamString(strike, monthString);
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		long cId = 0;
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = retry(baseUrl, paramString);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("ContractSearcher responseStatusCode 404 ");
				LoggerUtil.getLogger().info("ContractSearcher responseStatusCode 404 ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			if (responseStatusCode == 429) { // Too many requests
				Thread.sleep(5000);
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
		currentTime = System.currentTimeMillis();
		}
		
		return cId;
	}
	
	private static String getParamString(double strike, String monthString) {
		
		return "?conid=320227571&sectype=OPT&month="+monthString+"&right=C&exchange=SMART&strike="+strike;
		
	}
	
	private HttpResponse retry(String baseUrl, String paramString) throws Exception {
		
		System.out.println(baseUrl);
		//HttpPost post = new HttpPost(baseUrl);
		HttpGet get = new HttpGet(baseUrl + paramString);

		//StringEntity requestEntity = new StringEntity(
		//		jsonString,
	//		    ContentType.APPLICATION_JSON);
        // add request parameter, form parameters
        //List<NameValuePair> urlParameters = new ArrayList<>();
        //urlParameters.add(new BasicNameValuePair("username", "abc"));
        //urlParameters.add(new BasicNameValuePair("password", "123"));
        //urlParameters.add(new BasicNameValuePair("custom", "secret"));

        // post.setEntity(requestEntity);
		//request.setHeader("User-Agent", "runscope/0.1");
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		//request.setHeader("Accept", "*/*");
		int responsecode=0;
		//int nooftries = 1;
		HttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(get);
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
	

}
