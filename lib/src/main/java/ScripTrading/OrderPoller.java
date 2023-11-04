package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OrderPoller implements Runnable {

	private HttpClient client;
	
	public OrderPoller() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}
	
	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		String currentDateString = sdf.format(currentDate);
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String time = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
		time = Util.timeNMinsAgo(time, 5);
		
		while (time.compareTo("13:02") <= 0) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				time = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
				time = Util.timeNMinsAgo(time, 5);
				
				Trade optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/optionenter.txt");
				TradeConfirmation tradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString);
				if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("") && !optionEnterTrade.getOrderId().equals("na")
						&& (tradeconfirmation == null || tradeconfirmation.getHasOrderFilled() == false)) {
					String orderId = optionEnterTrade.getOrderId();
					double executedPrice = pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "?filters=filled", orderId);
					if (executedPrice > 0) {
						MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true);
					}
				}
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}
	
	private double pollOrder(String baseUrl, String paramString, String orderId) {
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		double executedPrice = 0;
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = HttpUtil.get(baseUrl, paramString, client);
			System.out.println(response.getStatusLine());
			responseStatusCode = response.getStatusLine().getStatusCode();
			if (responseStatusCode == 404) {
				System.out.println("pollOrder responseStatusCode 404 ");
				LoggerUtil.getLogger().info("pollOrder responseStatusCode 404 ");
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
			}
			if(responseStatusCode == 200){
				inputStreamReader = new InputStreamReader(response.getEntity().getContent());
				bufferedReader = new BufferedReader(inputStreamReader);
				String line;  
				//SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        //Calendar calendar = Calendar.getInstance();
				while ((line = bufferedReader.readLine()) != null) {
					//if (line.contains("56.67")) {
						//System.out.println(line);
					JSONParser parser = new JSONParser(); 
					JSONObject jsonObject = (JSONObject) parser.parse(line);
					JSONArray jsonArray = (JSONArray) jsonObject.get("orders");
					Iterator resultsIterator = jsonArray.iterator();
					while(resultsIterator.hasNext()) {
						JSONObject resultEntry = (JSONObject) resultsIterator.next();
						String forderId = (Long) resultEntry.get("orderId") + "";
						if (forderId.equals(orderId)) {
							executedPrice =  Double.parseDouble((String) resultEntry.get("avgPrice"));
							break;
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
		
		return executedPrice;
	}

}
