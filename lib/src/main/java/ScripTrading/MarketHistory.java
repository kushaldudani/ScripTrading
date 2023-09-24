package ScripTrading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MarketHistory {
	
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=NOV23&right=C&exchange=SMART&strike=370
		// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=SEP23&right=C&exchange=SMART&strike=370
		
		public static void main(String[] args) {
			MarketHistory cs = new MarketHistory();
			
			Map<String, MinuteData> minuteDataMap = cs.data("https://localhost:5000/v1/api/iserver/marketdata/history");
			System.out.println(minuteDataMap);
		}
		
		
		private HttpClient client;

		
		public MarketHistory() {
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60*1000).setConnectTimeout(60*1000).build();
			client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		}
		
		
		Map<String, MinuteData> data(String baseUrl){
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
	        Calendar calendar = Calendar.getInstance();
			String paramString = getParamString();
			int responseStatusCode = 0;
			InputStreamReader inputStreamReader = null;
			BufferedReader bufferedReader = null;
			Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
			long startTime = System.currentTimeMillis();
			long currentTime = System.currentTimeMillis();
			while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
			try{
				// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
				HttpResponse response = retry(baseUrl, paramString);
				System.out.println(response.getStatusLine());
				responseStatusCode = response.getStatusLine().getStatusCode();
				if (responseStatusCode == 404) {
					System.out.println("MarketHistory responseStatusCode 404 ");
					LoggerUtil.getLogger().info("MarketHistory responseStatusCode 404 ");
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
						JSONObject jsonObject = (JSONObject) parser.parse(line);
						
						JSONArray data = (JSONArray) jsonObject.get("data");
						//System.out.println(jsonObject);
						Iterator resultsIterator = data.iterator();
						while(resultsIterator.hasNext()) {
							JSONObject resultEntry = (JSONObject) resultsIterator.next();
							
							calendar.setTimeInMillis((Long) resultEntry.get("t"));
                    		MinuteData mData = new MinuteData();
                    		mData.setClosePrice((Double) resultEntry.get("c"));
                    		mData.setOpenPrice((Double) resultEntry.get("o"));
                    		mData.setHighPrice((Double) resultEntry.get("h"));
                    		mData.setLowPrice((Double) resultEntry.get("l"));
                    		String time = simpleDateFormat.format(calendar.getTime());
                    		minuteDataMap.put(time, mData);
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
			
			return minuteDataMap;
		}
		
		private static String getParamString() {
			
			return "?conid=320227571&period=1d&bar=5min";
			
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
