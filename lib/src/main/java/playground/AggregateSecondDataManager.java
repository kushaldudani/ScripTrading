package playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ScripTrading.LoggerUtil;
import ScripTrading.MinuteData;

public class AggregateSecondDataManager {
	
	public static void main(String[] args) throws Exception {
		AggregateSecondDataManager downloader = new AggregateSecondDataManager();
		downloader.getData();
		
	}
	
	private Map<String, Map<String, MinuteData>> dayDataMap = new LinkedHashMap<>();
	
	private HttpClient client;

	public AggregateSecondDataManager() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60*1000).setConnectTimeout(60*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	private void getData() throws Exception {
		//if (dayDataMap == null) {
		//	dayDataMap = Util.deserializeHashMap("config/QQQSeconds.txt");
		//}
		//if (dayDataMap == null) {
		//	throw new IllegalStateException("Error reading QQQSeconds cache");
		//}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-12-01");
		//Date endDate = sdf.parse("2023-09-06");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date nowDate = calendar.getTime();
		boolean downloadedMoreData = false;
		String startTime = "07:15";
		
		
		Date currentDate = startDate;
		while (currentDate.before(nowDate)) {
			downloadedMoreData = false;
			calendar.setTime(currentDate);
			String currentDateString = sdf.format(currentDate);
	        // which day of the week, get the price of underlying stock/etf
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			if (dayOfWeek == 1 || dayOfWeek == 7) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			if (!dayDataMap.containsKey(currentDateString)) {
				Map<String, MinuteData> minuteDataMap = downloadPrice("https://api.polygon.io/v2/aggs/ticker/VIX/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
				if (minuteDataMap.isEmpty()) {
					calendar.add(Calendar.DATE, 1);
			        currentDate = calendar.getTime();
					continue;
				} else {
					downloadedMoreData = true;
					dayDataMap.put(currentDateString, minuteDataMap);
				}
			}
			Map<String, MinuteData> dayData = dayDataMap.get(currentDateString);
			System.out.println(dayData.get(currentDateString));
			
			
			if (downloadedMoreData) {
				
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		
		
	}
	
	private Map<String, MinuteData> downloadPrice(String baseUrl){
		Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		while (minuteDataMap.isEmpty() && responseStatusCode != 200) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = retry(baseUrl);
			//System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("404 No data ");
				LoggerUtil.getLogger().info("404 No data ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			if (responseStatusCode == 429) { // Too many requests
				Thread.sleep(30000);
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						//System.out.println(line);
					
						JSONParser parser = new JSONParser(); 
						JSONObject json = (JSONObject) parser.parse(line);
						JSONArray results = (JSONArray) json.get("results");
						if (results == null) {
							break;
						}
						Iterator resultsIterator = results.iterator();
						while(resultsIterator.hasNext()) {
							
							JSONObject resultEntry = (JSONObject) resultsIterator.next();
							
							calendar.setTimeInMillis((Long) resultEntry.get("t"));
							String currentTime = sdf.format(calendar.getTime());
							int currentTimeH = (currentTime.charAt(0) == '0') ? Integer.parseInt(currentTime.substring(1, 2)) : Integer.parseInt(currentTime.substring(0, 2));
							int currentTimeM = (currentTime.charAt(3) == '0') ? Integer.parseInt(currentTime.substring(4, 5)) : Integer.parseInt(currentTime.substring(3, 5));
							if (((currentTimeH == 6 && currentTimeM >= 30) || currentTimeH >= 7) && currentTimeH < 13) {
								MinuteData mData = new MinuteData();
								mData.setClosePrice(getFromJson(resultEntry, "c"));
								mData.setOpenPrice(getFromJson(resultEntry, "o"));
								mData.setHighPrice(getFromJson(resultEntry, "h"));
								mData.setLowPrice(getFromJson(resultEntry, "l"));
								mData.setVolume(getFromJson(resultEntry, "v"));
								minuteDataMap.put(currentTime, mData);
							}	
					   }	
				}
			}else{
				System.out.println("Failed for " + baseUrl);
				
			}
			Thread.sleep(1000);
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
		
		return minuteDataMap;
	}
	
	private Double getFromJson(JSONObject resultEntry, String key) {
		try {
			return (Double) resultEntry.get(key);
		} catch (Exception e) {
			return ((Long) resultEntry.get(key)).doubleValue();
		}
	}
	
	private HttpResponse retry(String baseUrl) throws Exception {
		String finalurl = //getUrl(baseUrl, suffixUrl, date);
				baseUrl;
		System.out.println(finalurl);
		HttpGet request = new HttpGet(finalurl);
		//request.setHeader("User-Agent", "runscope/0.1");
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		//request.setHeader("Accept", "*/*");
		int responsecode=0;
		//int nooftries = 1;
		HttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(request);
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
