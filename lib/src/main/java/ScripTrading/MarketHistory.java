package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MarketHistory {
	
	// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=NOV23&right=C&exchange=SMART&strike=370
		// https://localhost:5000/v1/api/iserver/secdef/info?conid=320227571&sectype=OPT&month=SEP23&right=C&exchange=SMART&strike=370
		
		public static void main(String[] args) {
			MarketHistory cs = new MarketHistory();
			
			//MinuteData minuteData = cs.data("https://localhost:5000/v1/api/iserver/marketdata/history", 320227571, "09:35", null);
			Map<String, MinuteData> mDataMap = cs.data("https://localhost:5000/v1/api/iserver/marketdata/history", 664123971, "&exchange=CBOE", "1d", "5min");
			System.out.println(mDataMap);
		}
		
		
		private HttpClient client;

		
		public MarketHistory() {
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
			client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		}
		
		
		Map<String, MinuteData> data(String baseUrl, long cId, String exchangeInfo, String period, String bar){
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
	        Calendar calendar = Calendar.getInstance();
	        calendar.setTimeInMillis(System.currentTimeMillis());
	        //Util.reauthIfNeeded(TickleMapProvider.getInstance().getTickleMap(), simpleDateFormat.format(calendar.getTime()));
			
	        String paramString = getParamString(cId, exchangeInfo, period, bar);
			int responseStatusCode = 0;
			InputStreamReader inputStreamReader = null;
			BufferedReader bufferedReader = null;
			Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
			//long startTime = System.currentTimeMillis();
			//long currentTime = System.currentTimeMillis();
			//while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
			try{
				// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
				HttpResponse response = HttpUtil.get(baseUrl, paramString, client);
				System.out.println(response.getStatusLine());
				responseStatusCode = response.getStatusLine().getStatusCode();
				if (responseStatusCode == 404) {
					System.out.println("MarketHistory responseStatusCode 404 ");
					LoggerUtil.getLogger().info("MarketHistory responseStatusCode 404 ");
					//cache.put(symbol + "-" + date, new Record(null, null, null, null));
					//Thread.sleep(5000);
					//break;
				}
				//if (responseStatusCode == 429) { // Too many requests
					//Thread.sleep(5000);
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
						JSONParser parser = new JSONParser(); 
						JSONObject jsonObject = (JSONObject) parser.parse(line);
						
						JSONArray data = (JSONArray) jsonObject.get("data");
						//System.out.println(jsonObject);
						Iterator resultsIterator = data.iterator();
						while(resultsIterator.hasNext()) {
							JSONObject resultEntry = (JSONObject) resultsIterator.next();
							
							calendar.setTimeInMillis((Long) resultEntry.get("t"));
							String time = simpleDateFormat.format(calendar.getTime());
							//if (time.equals(timeToFill)) {
							MinuteData minuteData = new MinuteData();
							minuteData.setClosePrice((Double) resultEntry.get("c"));
							minuteData.setOpenPrice((Double) resultEntry.get("o"));
							minuteData.setHighPrice((Double) resultEntry.get("h"));
							minuteData.setLowPrice((Double) resultEntry.get("l"));
							minuteData.setVolume(getFromJson(resultEntry, "v"));
							//}
                    		
                    		minuteDataMap.put(time, minuteData);
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
			
			return minuteDataMap;
		}
		
		private String getParamString(long cId, String exchangeInfo, String period, String bar) {
			String paramString = "?conid=" + cId + "&period="+period+"&bar="+bar;  // 1d 5min
			if (exchangeInfo != null) {
				paramString = paramString + exchangeInfo;
			}
			
			return paramString;
			
		}
		
		private Double getFromJson(JSONObject resultEntry, String key) {
			try {
				return (Double) resultEntry.get(key);
			} catch (Exception e) {
				return ((Long) resultEntry.get(key)).doubleValue();
			}
		}
		

}
