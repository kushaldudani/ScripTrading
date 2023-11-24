package playground;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
import ScripTrading.MinuteData;

public class SingleDayQQQPutTester {
	
	private static void downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader) {
		
		if (!dayData.getPutDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"P00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (dayData.getPutDataMap() == null) {
				Map<Double, Map<String, MinuteData>> putMap = new LinkedHashMap<>();
				dayData.setPutDataMap(putMap);
			}
			dayData.getPutDataMap().put(strikePrice, mMap);
		}
		
		return;
	}
	
	private static StrikeWithPrice getStrikeWithPrice(DayData dayData, String time, Downloader downloader, String currentDateString,
			double prevTargetedStrikePrice, List<IGraphSegment> interpretedGSs) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = 0.009;
		double priceLevelToPlaceOrder = (((closeAtTime - openAtTime) / openAtTime) * 100 > 0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
		
		downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader);
		double putPriceTotarget = 0;
		if (dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time)) {
			putPriceTotarget = dayData.getPutDataMap().get(targetedStrikePrice).get(time).getClosePrice();
		}
		putPriceTotarget = putPriceTotarget * 1.1;
		
		putPriceTotarget = Math.max(putPriceTotarget, ((0.05 * closeAtTime) / 100));
		
		return new StrikeWithPrice(targetedStrikePrice, putPriceTotarget);
	}
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date runDate = sdf.parse("2023-11-17");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Map<String, DayData> dayDataMap = new LinkedHashMap<>();
		String startTime = "07:15";
		String closeTime = "12:50";
		String midMidTime = "08:30";
		String midTime = "09:30";
		double ninetyPercentileBarChange = 0.3;
		
			calendar.setTime(runDate);
			String currentDateString = sdf.format(runDate);
	        // which day of the week, get the price of underlying stock/etf
			Map<String, MinuteData> minuteDataMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/QQQ/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (minuteDataMap.isEmpty()) {
				System.out.println("Unexpected Data missing " + currentDateString);
				return;
			} else {
				Map<Double, Map<String, MinuteData>> callDataMap = new LinkedHashMap<>();
				Map<Double, Map<String, MinuteData>> putDataMap = new LinkedHashMap<>();
				dayDataMap.put(currentDateString, new DayData(minuteDataMap, null, callDataMap, putDataMap));
			}
			DayData dayData = dayDataMap.get(currentDateString);
			
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			String optionSellingTime = null;
			//double closeAtStartTime = dayData.getMinuteDataMap().get(startTime).getClosePrice();
			double strikePrice = 0;
			double soldPutPrice = 0;
			boolean uSignal = true;
			
			double targetedStrikePrice = 0;
			double prevTargetedStrikePrice = 0;
			double putPriceTotarget = 0;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (putPriceTotarget > 0
						&& dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time) 
						&& dayData.getPutDataMap().get(targetedStrikePrice).get(time).getHighPrice() >= putPriceTotarget
						&& uSignal == false//&& !lastGS.identifier.equals("d") //|| doesGSHasAnyDorDr(graphSegments))
						//&& !lastGS.identifier.equals("ur")
					) {
					strikePrice = targetedStrikePrice;
					optionSellingTime = time;
					soldPutPrice = putPriceTotarget;
					break;
				}
				
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
						dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
						dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
						time, ninetyPercentileBarChange, priceWithTime);
				if (time.compareTo(startTime) >= 0 && time.compareTo(midTime) < 0) {
					//System.out.println(time);
					GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
					if (!lastGS.identifier.equals("d") && time.compareTo(startTime) >= 0) {
						uSignal = false;
					}

					prevTargetedStrikePrice = targetedStrikePrice;
					StrikeWithPrice swp = getStrikeWithPrice(dayData, time, downloader, currentDateString, prevTargetedStrikePrice, null);
					targetedStrikePrice = swp.strike;
					putPriceTotarget = swp.price;
				}
			}
			
			if (strikePrice == 0) {
		        System.out.println(currentDateString + " Did not process as no entry");
				return;
			}
			double optionSellingTimePrice = dayData.getMinuteDataMap().get(optionSellingTime).getClosePrice();
			double premiumPercent = ((soldPutPrice / optionSellingTimePrice) * 100);
			
			
			String closedPriceTime = null;
			double closedThreshold = 0.005;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(closeTime) < 0) {
					if (!dayData.getPutDataMap().get(strikePrice).containsKey(time)) {
						continue;
					}
					double currrentPutPriceAtStrike = dayData.getPutDataMap().get(strikePrice).get(time).getLowPrice();
					double currentProfitPercent = ((currrentPutPriceAtStrike / optionSellingTimePrice) * 100);
					if (currentProfitPercent <= closedThreshold) {
						closedPriceTime = time;
						break;
					}
				}
			}
			
			
			
				String rollBackTime = null;
				double profitPcnt = 0;
				if (closedPriceTime != null) {	
					rollBackTime = closedPriceTime;
					profitPcnt = closedThreshold;
				} else {
					rollBackTime = closeTime;
					double putbuybackprice = 0;
					if (dayData.getPutDataMap().get(strikePrice).containsKey(rollBackTime)) {
						putbuybackprice = dayData.getPutDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
					}
					profitPcnt = ((putbuybackprice / optionSellingTimePrice) * 100);
				}
				
				String rowToWrite = currentDateString + "  " + strikePrice + "  " + optionSellingTime + "  " + premiumPercent + "  " + profitPcnt;
				
				System.out.println(rowToWrite);
		
	}

	static class StrikeWithPrice {
		double strike;
		double price;
		
		public StrikeWithPrice(double strike, double price) {
			this.strike = strike;
			this.price = price;
		}
	}
}
