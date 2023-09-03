package ScripTrading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Downloader {

	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		
		String scrip = "MSFT";
		//String wday = "2023-08-25";
		//int startPrice = 450000;
		//int endPrice = 475000;
		int increment = 2500;
		//String sym = "O:"+scrip+"230825";
		
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/MSFT.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			dayDataMap = new LinkedHashMap<>();
		}
		//Map<String, DayData> dayDataMap = new LinkedHashMap<>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2023-01-01");
		Date endDate = sdf.parse("2023-08-29");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		String nowDate = sdf.format(calendar.getTime());
		
		List<String> expiryDates = Util.getExpiryDates();
		Date currentDate = startDate;
		while (currentDate.before(endDate)) {
			calendar.setTime(currentDate);
			String currentDateString = sdf.format(currentDate);
			if (dayDataMap.containsKey(currentDateString)) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
	        // which day of the week, get the price of underlying stock/etf
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			if (dayOfWeek == 1 || dayOfWeek == 7) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			Map<String, MinuteData> minuteDataMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+scrip+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			//Double avgStockPr = getDownloader().downloadStockPrice("https://api.polygon.io/v1/open-close/" + symbol + "/", "?adjusted=true&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS", currentDateString, symbol, cache, null, stockSplitMultiplier);
			if (minuteDataMap.isEmpty()) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			
			double dayHigh = getDayHigh(minuteDataMap);
			double dayLow = getDayLow(minuteDataMap);
			
			int startPrice = (int) (dayLow * 1000) - (int) ((dayLow * 1000) % increment);
			int endPrice = ((int) (dayHigh * 1000) - (int) ((dayHigh * 1000) % increment));
			String expiryDate = getThisWeekExpiry(currentDateString, expiryDates);
			expiryDate = expiryDate.substring(2, 4) + expiryDate.substring(5, 7) + expiryDate.substring(8, 10);
			String sym = "O:"+scrip+expiryDate;
			
			Map<Double, Map<String, MinuteData>> callDataMap = new LinkedHashMap<>();
			int cntr=startPrice;
			while (cntr<=endPrice) {
				Map<String, MinuteData> callDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"C00"+cntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
				double cntrPrice = cntr / 1000;
				callDataMap.put(cntrPrice, callDatamap); //System.out.println(callDatamap);
				
				cntr = cntr + increment;
				//Thread.sleep(20000);
			}
			
	        Map<Double, Map<String, MinuteData>> putDataMap = new LinkedHashMap<>();
	        startPrice = startPrice + increment;
			endPrice = endPrice + increment;
			int pcntr=startPrice;
			while (pcntr<=endPrice) {
				Map<String, MinuteData> putDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"P00"+pcntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
				double pcntrPrice = pcntr / 1000;
				putDataMap.put(pcntrPrice, putDatamap); //System.out.println(putDatamap);
				
				pcntr = pcntr + increment;
				//Thread.sleep(20000);
			}
			
			Map<String, Double> premiumMap = new LinkedHashMap<>();
			for (String fiveMinute : minuteDataMap.keySet()) {
				double closePrice = minuteDataMap.get(fiveMinute).getClosePrice();
				
				double prevCallKey = 0.0;
				for (Double callKey : callDataMap.keySet()) {
					if(callKey > closePrice) {
						break;
					}
					prevCallKey = callKey;
				}
				
				double curPutKey = 0.0;
				for (Double putKey : putDataMap.keySet()) {
					curPutKey = putKey;
					if(putKey > closePrice) {
						break;
					}
				}
				
				double totalPremium = callDataMap.get(prevCallKey).get(fiveMinute).getClosePrice() + putDataMap.get(curPutKey).get(fiveMinute).getClosePrice();
				premiumMap.put(fiveMinute, totalPremium);
				//System.out.println(fiveMinute + "   " + totalPremium);
			}
			
			dayDataMap.put(currentDateString, new DayData(minuteDataMap, premiumMap));
			Util.serializeHashMap(dayDataMap, "config/MSFT.txt");
		}
		
	}
	
	private static String getThisWeekExpiry(String currentDate, List<String> expiryDates) {
		for (String expiryDate : expiryDates) {
			if (expiryDate.compareTo(currentDate) >= 0) {
				return expiryDate;
			}
		}
		
		return null;
	}
	
	private static double getDayHigh(Map<String, MinuteData> minuteDataMap) {
		double dayHigh = 0;
		for (String fiveMinute : minuteDataMap.keySet()) {
			if (minuteDataMap.get(fiveMinute).getClosePrice() >= dayHigh) {
				dayHigh = minuteDataMap.get(fiveMinute).getClosePrice();
			}
		}
		return dayHigh;
	}
	
	private static double getDayLow(Map<String, MinuteData> minuteDataMap) {
		double dayLow = 999999;
		for (String fiveMinute : minuteDataMap.keySet()) {
			if (minuteDataMap.get(fiveMinute).getClosePrice() <= dayLow) {
				dayLow = minuteDataMap.get(fiveMinute).getClosePrice();
			}
		}
		return dayLow;
	}
	// https://api.polygon.io/v2/aggs/ticker/TSLA/range/1/minute/2023-07-02/2023-07-03?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS
	
	private HttpClient client;

	
	public Downloader() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60*1000).setConnectTimeout(60*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	Map<String, MinuteData> downloadStockPrice(String baseUrl){
		//if (cache.containsKey(symbol + "-" + date)) {
		//	return "close".equals(whichPrice) ? cache.get(symbol + "-" + date).getClose() : cache.get(symbol + "-" + date).getAvgPrice();
		//}
		Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		//Double price = null;
		while (minuteDataMap.isEmpty() && responseStatusCode != 200) {
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = retry(baseUrl);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("No data for ");
				LoggerUtil.getLogger().info("No data for ");
				//cache.put(symbol + "-" + date, new Record(null, null, null, null));
				//Thread.sleep(5000);
				break;
			}
			if (responseStatusCode == 429) { // Too many requests
				Thread.sleep(10000);
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						System.out.println(line);
					
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
								minuteDataMap.put(currentTime, mData);
							}	
							//long lastUpdatedTime = (Long) resultEntryDay.getOrDefault("last_updated", 0L);
							//expirationDate = (String) resultEntryDetails.get("expiration_date");
							//Object strikePrice = (Object) resultEntryDetails.get("strike_price");
					   }	
						
					/**/
					
				}
			}else{
				System.out.println("Failed for ");
				
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
			//if(writer != null){
			//	try {
			//		writer.close();
			//	} catch (IOException e) {}
			//}
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
	
	private String getUrl(String baseUrl, String suffixUrl, String date) {
		if (suffixUrl != null && date != null) {
			return baseUrl + date + suffixUrl;
		} else {
			return baseUrl;
		}
	}
}

/*

06:30   9.1
06:35   9.0
06:40   8.96
06:45   8.99
06:50   8.95
06:55   9.0
07:00   8.9
07:05   8.89
07:10   8.85
07:15   8.7
07:20   8.68
07:25   8.64
07:30   8.600000000000001
07:35   8.77
07:40   8.48
07:45   8.53
07:50   8.43
07:55   8.33
08:00   8.4
08:05   8.53
08:10   8.379999999999999
08:15   8.51
08:20   8.3
08:25   8.22
08:30   9.350000000000001
08:35   9.149999999999999
08:40   9.66
08:45   9.2
08:50   9.690000000000001
08:55   9.84
09:00   10.2
09:05   10.75
09:10   11.1
09:15   10.83
09:20   10.83
09:25   11.4
09:30   11.969999999999999
09:35   12.219999999999999
09:40   12.3
09:45   12.0
09:50   11.0
09:55   11.2
10:00   11.370000000000001
10:05   11.42
10:10   11.55
10:15   11.74
10:20   11.440000000000001
10:25   11.44
10:30   11.5
10:35   11.45
10:40   11.32
10:45   11.45
10:50   11.370000000000001
10:55   11.35
11:00   11.32
11:05   11.25
11:10   11.35
11:15   11.25
11:20   11.29
11:25   11.2
11:30   11.149999999999999
11:35   11.25
11:40   11.4
11:45   11.2
11:50   11.36
11:55   10.93
12:00   11.07
12:05   10.95
12:10   10.94
12:15   10.81
12:20   10.6
12:25   10.399999999999999
12:30   10.2
12:35   10.42
12:40   10.399999999999999
12:45   10.100000000000001
12:50   10.649999999999999
12:55   10.15

*/

