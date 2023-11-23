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

public class PolygonMarketHistory {
	
	public static void main(String[] args) {
		//PolygonMarketHistory pmh = new PolygonMarketHistory();
		
		Map<String, MinuteData> mDataMap = new PolygonMarketHistory().data("2023-11-20", 390, "C");
		System.out.println(mDataMap);
	}
	
	
	private HttpClient client;

	
	public PolygonMarketHistory() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	
	Map<String, MinuteData> data(String currentDateString, double strike, String optionSide){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //Util.reauthIfNeeded(TickleMapProvider.getInstance().getTickleMap(), simpleDateFormat.format(calendar.getTime()));
		
        String fullUrl = getFullUrl(currentDateString, strike, optionSide);
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		while (responseStatusCode != 200 && (currentTime - startTime) < 60000) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = HttpUtil.get(fullUrl, "", client);
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
			if(responseStatusCode == 500){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					System.out.println(line);
					LoggerUtil.getLogger().info(line);
				}
				break;
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
					
					JSONArray data = (JSONArray) jsonObject.get("results");
					if (data == null) {
						break;
					}
					Iterator resultsIterator = data.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						
						calendar.setTimeInMillis((Long) resultEntry.get("t"));
						String time = simpleDateFormat.format(calendar.getTime());
						int currentTimeH = (time.charAt(0) == '0') ? Integer.parseInt(time.substring(1, 2)) : Integer.parseInt(time.substring(0, 2));
						int currentTimeM = (time.charAt(3) == '0') ? Integer.parseInt(time.substring(4, 5)) : Integer.parseInt(time.substring(3, 5));
						if (((currentTimeH == 6 && currentTimeM >= 30) || currentTimeH >= 7) && currentTimeH < 13) {
							MinuteData minuteData = new MinuteData();
							minuteData.setClosePrice(getFromJson(resultEntry, "c"));
							minuteData.setOpenPrice(getFromJson(resultEntry, "o"));
							minuteData.setHighPrice(getFromJson(resultEntry, "h"));
							minuteData.setLowPrice(getFromJson(resultEntry, "l"));
							minuteData.setVolume(getFromJson(resultEntry, "v"));
                		
							minuteDataMap.put(time, minuteData);
						}
					}
				}
			}
		}catch(Exception e){
			//e.printStackTrace();
			LoggerUtil.getLogger().info(e.getMessage());
			try {
				Thread.sleep(5000);
			} catch (Exception e1) {
				//e1.printStackTrace();
			}
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
		currentTime = System.currentTimeMillis();
		}
		
		return minuteDataMap;
	}
	
	private String getFullUrl(String currentDateString, double strike, String optionSide) {
		String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
		int intPriceCntr = (int) (strike * 1000);
		String paramString = "https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+optionSide+"00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS";
		
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
