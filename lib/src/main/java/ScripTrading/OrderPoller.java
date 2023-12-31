package ScripTrading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OrderPoller implements Runnable {

	private CloseableHttpClient client;
	//private Map<Long, Double> conIdToStrike;
	
	public OrderPoller() {
		client = HttpUtil.createHttpClient();
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
				if (actualTime.charAt(4) == '0' || actualTime.charAt(4) == '5' || actualTime.charAt(4) == '1' || actualTime.charAt(4) == '6') {
					continue;
				}
				
				Map<String, String> pollresultMap = OrderPollerUtil.getInstance().pollOrder("https://localhost:5000/v1/api/iserver/account/orders", "", client);
				LoggerUtil.getLogger().info("OrderPoller polled map " + pollresultMap);
				
				Trade optionEnterTrade = MetadataUtil.getInstance().readTrade(currentDateString, "/home/kushaldudani/qqq/optionenter.txt");
				TradeConfirmation optiontradeconfirmation = MetadataUtil.getInstance().readTradeConfirmation(currentDateString, "/home/kushaldudani/qqq/optiontradeconfirmation.txt");
				if (optionEnterTrade != null && !optionEnterTrade.getOrderId().equals("")
						&& (optiontradeconfirmation == null || optiontradeconfirmation.getHasOrderFilled() == false)) {
					String localOrderId = optionEnterTrade.getLocalOId();
					String orderId = null;
					if (optionEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/optionenter.txt", currentDateString, optionEnterTrade.getExecutionInfo(), orderId, optionEnterTrade.getStrike(), optionEnterTrade.getContract(), localOrderId);
							
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
					String localOrderId = stockEnterTrade.getLocalOId();
					String orderId = null;
					if (stockEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionenter.txt", currentDateString, stockEnterTrade.getExecutionInfo(), orderId, stockEnterTrade.getStrike(), stockEnterTrade.getContract(), localOrderId);
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
					String localOrderId = stockExitTrade.getLocalOId();
					String orderId = null;
					if (stockExitTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq/positionexit.txt", currentDateString, stockExitTrade.getExecutionInfo(), orderId, stockExitTrade.getStrike(), stockExitTrade.getContract(), localOrderId);
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
					String localOrderId = optionEnterTrade.getLocalOId();
					String orderId = null;
					if (optionEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/optionenter.txt", currentDateString, optionEnterTrade.getExecutionInfo(), orderId, optionEnterTrade.getStrike(), optionEnterTrade.getContract(), localOrderId);
							
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
					String localOrderId = stockEnterTrade.getLocalOId();
					String orderId = null;
					if (stockEnterTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionenter.txt", currentDateString, stockEnterTrade.getExecutionInfo(), orderId, stockEnterTrade.getStrike(), stockEnterTrade.getContract(), localOrderId);
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
					String localOrderId = stockExitTrade.getLocalOId();
					String orderId = null;
					if (stockExitTrade.getOrderId().equals("na")) {
						orderId = pollresultMap.get(localOrderId);
						if (orderId != null) {
							MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionexit.txt", currentDateString, stockExitTrade.getExecutionInfo(), orderId, stockExitTrade.getStrike(), stockExitTrade.getContract(), localOrderId);
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
		CloseableHttpResponse response = null;
		String orderStatus = "";
		try{
			// writer = new BufferedWriter(new FileWriter("data2/" + symbol + ".csv", false));
			response = HttpUtil.get(baseUrl, "", client);
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
			if(response != null){
				try {
					response.close();
				} catch (Exception e) {}
			}
		}
		
		return orderStatus;
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
