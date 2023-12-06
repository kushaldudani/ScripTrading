package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

public class OrderPoller implements Runnable {

	private HttpClient client;
	//private Map<Long, Double> conIdToStrike;
	
	public OrderPoller() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10*1000).setConnectTimeout(10*1000).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		
		//this.conIdToStrike = conIdToStrike;
	}
	
	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		String currentDateString = sdf.format(currentDate);
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String actualTime = shorttimeFormatter.format(currentDate);
		String time = Util.findNearestFiveMinute(actualTime);
		//time = Util.timeNMinsAgo(time, 5);
		
		while (time.compareTo("13:02") <= 0) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				actualTime = shorttimeFormatter.format(currentDate);
				time = Util.findNearestFiveMinute(actualTime);
				//time = Util.timeNMinsAgo(time, 5);
				if (actualTime.charAt(4) == '0' || actualTime.charAt(4) == '5') {
					continue;
				}
				
				Map<String, String> pollresultMap = pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "");
				LoggerUtil.getLogger().info("OrderPoller polled map " + pollresultMap);
				
				Trade optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/optionenter.txt");
				TradeConfirmation optiontradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq/optiontradeconfirmation.txt");
				if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("")
						&& (optiontradeconfirmation == null || optiontradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "LT"+currentDateString+"OE"+optionEnterTrade.getContract();
					String orderId = null;
					if (optionEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/optionenter.txt", currentDateString, optionEnterTrade.getExecutionInfo(), orderId, optionEnterTrade.getStrike(), optionEnterTrade.getContract());
							
							String strikeenterordermapPath = "/home/kushaldudani/qqq/strikeenterordermap.txt";
							Map<Double, String> strikeToEnterOrderMap = MetadataUtil.getInstance().readStrikeEnterOrderMap(currentDateString, strikeenterordermapPath);
							strikeToEnterOrderMap.put(optionEnterTrade.getStrike(), orderId);
							MetadataUtil.getInstance().writeStrikeEnterOrderMap(strikeenterordermapPath, currentDateString, strikeToEnterOrderMap);
						}
					} else {
						orderId = optionEnterTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, optionEnterTrade.getStrike(), "/home/kushaldudani/qqq/optiontradeconfirmation.txt");
						}
					}
				}
				
				Trade stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/positionenter.txt");
				TradeConfirmation stockradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq/stocktradeconfirmation.txt");
				if (stockEnterTrade != null && !stockEnterTrade.getOrderId().equals("")
						&& (stockradeconfirmation == null || stockradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "LT"+currentDateString+"SE"+stockEnterTrade.getContract();
					String orderId = null;
					if (stockEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionenter.txt", currentDateString, stockEnterTrade.getExecutionInfo(), orderId, stockEnterTrade.getStrike(), stockEnterTrade.getContract());
						}
					} else {
						orderId = stockEnterTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, 0, "/home/kushaldudani/qqq/stocktradeconfirmation.txt");
						}
					}
				}
				
				Trade stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/positionexit.txt");
				TradeConfirmation stockexittradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq/stockexittradeconfirmation.txt");
				if (stockExitTrade != null && !stockExitTrade.getOrderId().equals("")
						&& (stockexittradeconfirmation == null || stockexittradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "LT"+currentDateString+"SEx"+stockExitTrade.getContract();
					String orderId = null;
					if (stockExitTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionexit.txt", currentDateString, stockExitTrade.getExecutionInfo(), orderId, stockExitTrade.getStrike(), stockExitTrade.getContract());
						}
					} else {
						orderId = stockExitTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, 0, "/home/kushaldudani/qqq/stockexittradeconfirmation.txt");
						}
					}
				}
				
				///////
				
				optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/optionenter.txt");
				optiontradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/optiontradeconfirmation.txt");
				if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("")
						&& (optiontradeconfirmation == null || optiontradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "ST"+currentDateString+"OE"+optionEnterTrade.getContract();
					String orderId = null;
					if (optionEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/optionenter.txt", currentDateString, optionEnterTrade.getExecutionInfo(), orderId, optionEnterTrade.getStrike(), optionEnterTrade.getContract());
							
							String strikeenterordermapPath = "/home/kushaldudani/qqq2/strikeenterordermap.txt";
							Map<Double, String> strikeToEnterOrderMap = MetadataUtil.getInstance().readStrikeEnterOrderMap(currentDateString, strikeenterordermapPath);
							strikeToEnterOrderMap.put(optionEnterTrade.getStrike(), orderId);
							MetadataUtil.getInstance().writeStrikeEnterOrderMap(strikeenterordermapPath, currentDateString, strikeToEnterOrderMap);
						}
					} else {
						orderId = optionEnterTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, optionEnterTrade.getStrike(), "/home/kushaldudani/qqq2/optiontradeconfirmation.txt");
						}
					}
				}
				
				stockEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/positionenter.txt");
				stockradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/stocktradeconfirmation.txt");
				if (stockEnterTrade != null && !stockEnterTrade.getOrderId().equals("")
						&& (stockradeconfirmation == null || stockradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "ST"+currentDateString+"SE"+stockEnterTrade.getContract();
					String orderId = null;
					if (stockEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionenter.txt", currentDateString, stockEnterTrade.getExecutionInfo(), orderId, stockEnterTrade.getStrike(), stockEnterTrade.getContract());
						}
					} else {
						orderId = stockEnterTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, 0, "/home/kushaldudani/qqq2/stocktradeconfirmation.txt");
						}
					}
				}
				
				stockExitTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq2/positionexit.txt");
				stockexittradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq2/stockexittradeconfirmation.txt");
				if (stockExitTrade != null && !stockExitTrade.getOrderId().equals("")
						&& (stockexittradeconfirmation == null || stockexittradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = "ST"+currentDateString+"SEx"+stockExitTrade.getContract();
					String orderId = null;
					if (stockExitTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionexit.txt", currentDateString, stockExitTrade.getExecutionInfo(), orderId, stockExitTrade.getStrike(), stockExitTrade.getContract());
						}
					} else {
						orderId = stockExitTrade.getOrderId();
					}
					if (orderId != null) {
						String ordStatus = orderStatus("https://localhost:5000/v1/api/iserver/account/order/status/"+orderId);
						if (ordStatus.equalsIgnoreCase("filled")) {
							MetadataUtil.getInstance().writeTradeConfirmation(currentDateString, true, time, 0, "/home/kushaldudani/qqq2/stockexittradeconfirmation.txt");
						}
					}
				}
				
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}
	
	private String orderStatus(String baseUrl) {
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		String orderStatus = "";
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			HttpResponse response = HttpUtil.get(baseUrl, "", client);
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
					orderStatus = (String) jsonObject.get("order_status");
					
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
		
		return orderStatus;
	}
	
	private Map<String, String> pollOrder(String baseUrl, String paramString) {
		accountPing();
		
		int responseStatusCode = 0;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		Map<String, String> pollresultMap = new LinkedHashMap<>();
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
						String fCOID = (String) resultEntry.get("order_ref");
						String forderId = (Long) resultEntry.get("orderId") + "";
						pollresultMap.put(fCOID, forderId);
						/*if (forderId.equals(orderId)) {
							double executedPrice =  Double.parseDouble((String) resultEntry.get("avgPrice"));
							long conid = (Long) resultEntry.get("conid");
							pollresult = new PollResult(executedPrice, conid);
							break;
						}*/
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
		
		return pollresultMap;
	}
	
	void accountPing(){
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
	}

	
	static class PollResult {
		double executedPrice;
		long conid;
		
		PollResult(double executedPrice, long conid) {
			this.executedPrice = executedPrice;
			this.conid = conid;
		}
	}
}
