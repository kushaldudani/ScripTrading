package ScripTrading;

import static ScripTrading.OrderUtil.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import playground.GSInterpretation;
import playground.GSUtil;

public class ShortTrigger {

	public static void main(String[] args) {
		//String json = getOrderJson("MSFT", "07:30", 1, "BUY");
		
		//System.out.println(json);
	}
	
	//private static double OPTION_PREMIUM_RISE_PERCENT = 10;
	
	//private static double TRANSACTION_SIZE = 400; // Dollar Amount
	
	private static String ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/orders";
	
	private static String MODIFY_ORDER_URL = "https://localhost:5000/v1/api/iserver/account/U12784344/order/";
	
	//private static String SYMBOL = "QQQ";
	
	private static String startTime = "07:15";
	
	private static String midTime = "11:30";
	
	private ExecutorService orderThreadPool;
	
	public ShortTrigger(ExecutorService orderThreadPool) {
		this.orderThreadPool = orderThreadPool;
	}
	
	
	public void stockEnter(Map<String, MinuteData> minuteDataMap, String triggerTime, TradeData tradedata, String currentDate, StringBuilder notifyBuilder, String orderId,
			TreeSet<String> putVolumeSignal, String executionInfo, String existingCOID) {
		DecimalFormat decfor = new DecimalFormat("0.00");  
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		double limitPrice = 0;
		try {
			boolean entrybool = bearEntry(minuteDataMap, 0.3, triggerTime, putVolumeSignal);
			if (entrybool) {
				limitPrice = closeAttime;
			}
			if (!executionInfo.equals("") && !executionInfo.equals("MKT") && !executionInfo.equals("inprogress") && orderId.equals("")) {
				limitPrice = Double.parseDouble(executionInfo);
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
		limitPrice = Double.parseDouble(decfor.format(limitPrice));
		if (limitPrice > 0) {
			int qty = tradedata.getQty() * 100;
			//String cOID = SYMBOL+triggerTime;
			String cOID = "ST"+currentDate+"SE";
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionenter.txt", currentDate, "inprogress", "na", 0, 320227571, cOID);
				orderThreadPool.submit(new OrderPlacer(ORDER_URL, getPositionJson(qty,  "LMT", limitPrice, "SELL", cOID), currentDate, "/home/kushaldudani/qqq2/positionenter.txt",
						limitPrice+"", 0, cOID, 320227571));
				notifyBuilder.append("stockEnter LMT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				orderThreadPool.submit(new OrderModifier(MODIFY_ORDER_URL, getPositionModifyJson(qty, "LMT", limitPrice, "SELL"), currentDate, "/home/kushaldudani/qqq2/positionenter.txt",
						""+limitPrice, 0, orderId, existingCOID, 320227571));
				notifyBuilder.append("stockEnter LMT Order Modified : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			}
		} else {
			LoggerUtil.getLogger().info("ShortTrigger Stock Enter tried at " + triggerTime + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Stock Enter tried at " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
		}
		
	}
	
	private boolean bearEntry(Map<String, MinuteData> minuteDataMap, double ninetyPercentileBarChange, String triggerTime, TreeSet<String> putVolumeSignal) {
		if (triggerTime.compareTo(startTime) > 0 && triggerTime.compareTo(midTime) < 0) {
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			for (String time : minuteDataMap.keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
					minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
					minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
					time, ninetyPercentileBarChange, priceWithTime);
			}
			double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
			GSInterpretation gsInterpretation = new GSInterpretation();
			List<IGraphSegment> interpretedGSs = gsInterpretation.interpretedGraphSegments(graphSegments);
			if (
					evaluateShort(ninetyPercentileBarChange, interpretedGSs, closeAttime, triggerTime, putVolumeSignal)
				) {
				return true;
			}
		}
			
		return false;
	}
	
	private boolean evaluateShort(double ninetyPercentileBarChange, List<IGraphSegment> interpretedGSs, double closeAtTime, String triggerTime,
			TreeSet<String> putVolumeSignal) {
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment lowestStartU = null;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("u") && (lowestStartU == null || interpretedGS.startPrice < lowestStartU.startPrice) ) {
				lowestStartU = interpretedGS;
			}
			cntr--;
		}
		
		if (segmentsSize > 0) {
			IGraphSegment lastIGS = interpretedGSs.get(segmentsSize - 1);
			//
			if (lastIGS.identifier.equals("d") 
					&& (lowestStartU == null || lastIGS.endPrice < lowestStartU.startPrice) 
					&& closeAtTime <= lastIGS.endPrice
					&& triggerTime.compareTo("07:40") >= 0 
					&& ( ( triggerTime.compareTo("10:20") <= 0 && (putVolumeSignal.size() > 0 && Util.diffTime(putVolumeSignal.last(), triggerTime) <= 30) )
					   )
					&& (((lastIGS.startPrice - lastIGS.endPrice) / lastIGS.startPrice) * 100) <= (6 * ninetyPercentileBarChange)
				) {
				return true;
			}
		}
		
		return false;
	}
	
	public void optionExit(Map<String, MinuteData> minuteDataMap, String time, TradeData tradedata, String currentDate, long optionContract, StringBuilder notifyBuilder,
			String orderId, String executionInfo, double strikePrice) {
		DecimalFormat decfor = new DecimalFormat("0.00"); 
		double eodCutOffPrice = strikePrice + (0.002 * strikePrice);
		
		double closeAttime = minuteDataMap.get(time).getClosePrice();
		double limitPrice = 0.015;
		limitPrice = Double.parseDouble(decfor.format(limitPrice));
		
		String cOID = "ST"+currentDate+"OEx"+optionContract;
		if (orderId.equals("")) {
			int qty = tradedata.getQty();
			MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/optionexit.txt", currentDate, "inprogress", "na", strikePrice, optionContract, cOID);
			orderThreadPool.submit(new OrderPlacer(ORDER_URL, getOptionExitJson(qty,  optionContract, "LMT", limitPrice, cOID), currentDate, "/home/kushaldudani/qqq2/optionexit.txt",
					limitPrice+"", strikePrice, cOID, optionContract));
			notifyBuilder.append("optionExit LMT Order Placed : " + time + " Close Price at that time " + closeAttime + " Limit price used "+ limitPrice + "<br>");
		} else if (closeAttime < eodCutOffPrice && time.compareTo("12:50") >= 0) {
			int qty = tradedata.getQty();
			
			if (!executionInfo.equals("MKT")) {
				orderThreadPool.submit(new OrderModifier(MODIFY_ORDER_URL, getOptionExitModifyJson(qty, optionContract), currentDate, "/home/kushaldudani/qqq2/optionexit.txt", "MKT", strikePrice, orderId, cOID, optionContract));
				notifyBuilder.append("optionExit Order Modified to MKT : " + time + " Close Price at that time " + closeAttime + "<br>");
			}
		}
		
	}
	
	public void optionEnter(Map<String, MinuteData> minuteDataMap, String triggerTime, double targetedStrikePrice, String currentDate, long optionContract,
			StringBuilder notifyBuilder, double callPriceTotarget, String orderId, TradeData tradedata, Map<Double, String> strikeToEnterOrderMap) {
		DecimalFormat decfor = new DecimalFormat("0.00");  
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		List<GraphSegment> graphSegments = new ArrayList<>();
		Map<String, Double> priceWithTime = new LinkedHashMap<>();
		callPriceTotarget = Double.parseDouble(decfor.format(callPriceTotarget));
		boolean isLastGSU = false;
		try {
			for (String time : minuteDataMap.keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
					minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
					minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
					time, 0.3, priceWithTime);
			}
		
			GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
			isLastGSU = lastGS.identifier.equals("d");
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
		
		if (isLastGSU == false
			&& triggerTime.compareTo("07:15") >= 0 && triggerTime.compareTo("09:30") < 0
				) {
			int qty = tradedata.getQty();
			//String cOID = optionContract+SYMBOL+triggerTime;
			String cOID = "ST"+currentDate+"OE"+optionContract;
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/optionenter.txt", currentDate, "inprogress", "na", targetedStrikePrice, optionContract, cOID);
				orderThreadPool.submit(new OrderPlacer(ORDER_URL, getOptionEnterJson(qty,  optionContract, "LMT", callPriceTotarget, cOID), currentDate, "/home/kushaldudani/qqq2/optionenter.txt",
						callPriceTotarget+"", targetedStrikePrice, cOID, optionContract));
				notifyBuilder.append("optionEnter LMT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else if (!strikeToEnterOrderMap.containsKey(targetedStrikePrice)) {
				orderThreadPool.submit(new OrderPlacer(ORDER_URL, getOptionEnterJson(qty,  optionContract, "LMT", callPriceTotarget, cOID), currentDate, "/home/kushaldudani/qqq2/optionenter.txt",
						callPriceTotarget+"", targetedStrikePrice, cOID, optionContract));
				notifyBuilder.append("optionEnter new LMT Order with different Strike Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				orderThreadPool.submit(new OrderModifier(MODIFY_ORDER_URL, getOptionEnterModifyJson(qty,  optionContract, "LMT", callPriceTotarget), currentDate, "/home/kushaldudani/qqq2/optionenter.txt",
						""+callPriceTotarget, targetedStrikePrice, strikeToEnterOrderMap.get(targetedStrikePrice), cOID, optionContract));
				notifyBuilder.append("optionEnter LMT Order Modified : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			}
		} else {
			LoggerUtil.getLogger().info("ShortTrigger Option Enter tried at " + triggerTime + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Option Enter tried at " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
		}
		
	}
	
	
	public void stockExit(Map<String, MinuteData> minuteDataMap, String triggerTime, TradeData tradedata, String currentDate,
			String orderId, String executionInfo, StringBuilder notifyBuilder, double enterPrice, String existingCOID) {
		DecimalFormat decfor = new DecimalFormat("0.00");  
		double closeAttime = minuteDataMap.get(triggerTime).getClosePrice();
		List<GraphSegment> graphSegments = new ArrayList<>();
		Map<String, Double> priceWithTime = new LinkedHashMap<>();
		double limitPrice = 0;
		try {
			for (String time : minuteDataMap.keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, minuteDataMap.get(time).getVolume(),
						minuteDataMap.get(time).getOpenPrice(), minuteDataMap.get(time).getClosePrice(),
						minuteDataMap.get(time).getHighPrice(), minuteDataMap.get(time).getLowPrice(),
						time, 0.3, priceWithTime);
			}
		
			GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
			if ((lastGS.identifier.equals("u") && closeAttime >= lastGS.endPrice 
					&& (((lastGS.endPrice - lastGS.startPrice) / lastGS.startPrice) * 100) > 0.5
					&& (((enterPrice - closeAttime) / enterPrice) * 100) < -0.1 )) {
				limitPrice = closeAttime;
			}
			
			if (!executionInfo.equals("") && !executionInfo.equals("MKT") && !executionInfo.equals("inprogress") && orderId.equals("")) {
				limitPrice = Double.parseDouble(executionInfo);
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
		limitPrice = Double.parseDouble(decfor.format(limitPrice));
		
		notifyBuilder.append("StockExit Info : Limit price " + limitPrice + "<br>");
		
		if (triggerTime.compareTo("12:35") >= 0) {
			int qty = tradedata.getQty() * 100;
			//String cOID = SYMBOL+triggerTime;
			String cOID = "ST"+currentDate+"SEx";
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionexit.txt", currentDate, "inprogress", "na", 0, 320227571, cOID);
				orderThreadPool.submit(new OrderPlacer(ORDER_URL, getPositionJson(qty, "MKT", 0, "BUY", cOID), currentDate, "/home/kushaldudani/qqq2/positionexit.txt", "MKT", 0, cOID, 320227571));
				notifyBuilder.append("stockExit MKT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				if (!executionInfo.equals("MKT")) {
					orderThreadPool.submit(new OrderModifier(MODIFY_ORDER_URL, getPositionModifyJson(qty, "MKT", 0, "BUY"), currentDate, "/home/kushaldudani/qqq2/positionexit.txt", "MKT", 0, orderId, existingCOID, 320227571));
					notifyBuilder.append("stockExit Order Modified to MKT : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
				}
			}
		} else if (limitPrice > 0) {
			int qty = tradedata.getQty() * 100;
			//String cOID = SYMBOL+triggerTime;
			String cOID = "ST"+currentDate+"SEx";
			if (orderId.equals("")) {
				MetadataUtil.getInstance().write("/home/kushaldudani/qqq2/positionexit.txt", currentDate, "inprogress", "na", 0, 320227571, cOID);
				orderThreadPool.submit(new OrderPlacer(ORDER_URL, getPositionJson(qty, "LMT", limitPrice, "BUY", cOID), currentDate, "/home/kushaldudani/qqq2/positionexit.txt", ""+limitPrice, 0, cOID, 320227571));
				notifyBuilder.append("stockExit LMT Order Placed : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
			} else {
				if (!executionInfo.equals("MKT")) {
					double prevLimitPrice = Double.parseDouble(executionInfo);
					if (limitPrice < prevLimitPrice) {
						orderThreadPool.submit(new OrderModifier(MODIFY_ORDER_URL, getPositionModifyJson(qty, "LMT", limitPrice, "BUY"), currentDate, "/home/kushaldudani/qqq2/positionexit.txt", ""+limitPrice, 0, orderId, existingCOID, 320227571));
						notifyBuilder.append("stockExit Order Modified with change in LMT price : " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
					}
				}
			}
		} else {
			LoggerUtil.getLogger().info("ShortTrigger Stock Exit tried at " + triggerTime + " Close Price at that time " + closeAttime);
			notifyBuilder.append("Stock Exit tried at " + triggerTime + " Close Price at that time " + closeAttime + "<br>");
		}
	}

	
}
