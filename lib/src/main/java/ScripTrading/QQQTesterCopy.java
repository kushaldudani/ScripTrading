package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class QQQTesterCopy {
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2023-07-17");
		Date endDate = sdf.parse("2023-07-18");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		double prevClosePrice = 0;
		double sumprofitPcnt = 0.0;
		//double sumSecondPremiumPcnt = 0.0;
		int totaldays = 0;
		int problemCases = 0;
		int problemCasesSubset = 0;
		int secondproblemCases = 0;
		boolean downloadedMoreData = false;
		String optionSellingTime = "07:15";
		String closeTime = "12:50";
		String midTime = "11:30";
		double sumPremiumPercent = 0.0;
		double sumbookedPositionProfit = 0.0;
		double sumSecondProfitPcnt = 0.0;
		
		Date currentDate = startDate;
		while (currentDate.before(endDate)) {
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
				//System.out.println("Unexpected Data missing " + currentDateString);
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}
			DayData dayData = dayDataMap.get(currentDateString);
			
			double optionSellingTimePrice = dayData.getMinuteDataMap().get(optionSellingTime).getOpenPrice();
			double strikePrice = 0;
			double priceCntr = ((int) optionSellingTimePrice) + increment;
			while(strikePrice == 0) {
				double callPrice = 0.0;
				if (dayData.getCallDataMap().containsKey(priceCntr)) {
					callPrice = dayData.getCallDataMap().get(priceCntr).get(optionSellingTime).getOpenPrice();
				} else {
					downloadedMoreData = true;
					String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
					int intPriceCntr = (int) (priceCntr * 1000);
					Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
					callPrice = mMap.get(optionSellingTime).getOpenPrice();
					dayData.getCallDataMap().put(priceCntr, mMap);
				}
				if (callPrice < ((optionSellingTimePrice / 1000))) {
					strikePrice = priceCntr - increment;
					break;
				}
				
				priceCntr = priceCntr + increment;
			}
			
			double callPriceAtStrike = dayData.getCallDataMap().get(strikePrice).get(optionSellingTime).getOpenPrice();
			double premiumPercent = ((callPriceAtStrike / optionSellingTimePrice) * 100);
			//System.out.println(premiumPercent);
			sumPremiumPercent = sumPremiumPercent + premiumPercent;
			
			String touchcutOffPriceTime = null;
			double breachedPrice = 0.0;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				double cutOffPrice = strikePrice + (0.002 * strikePrice);
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(midTime) < 0) {
					if (prevClosePrice > 0 && (((dayData.getMinuteDataMap().get(time).getClosePrice() - prevClosePrice) / prevClosePrice) * 100) > 2.5) {
						break;
					}
					if (dayData.getMinuteDataMap().get(time).getClosePrice() >= cutOffPrice) {
						touchcutOffPriceTime = time;
						breachedPrice = dayData.getMinuteDataMap().get(time).getClosePrice();
						break;
					}
				}
			}
			
			//
			/*double newStrikePrice = 0;
			if (breachedPrice > 0) {
				priceCntr = ((int) breachedPrice) + increment;
				while(newStrikePrice == 0) {
					double callPrice = 0.0;
					if (dayData.getCallDataMap().containsKey(priceCntr)) {
						callPrice = dayData.getCallDataMap().get(priceCntr).get(touchcutOffPriceTime).getOpenPrice();
					} else {
						downloadedMoreData = true;
						String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
						int intPriceCntr = (int) (priceCntr * 1000);
						Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
						callPrice = mMap.get(touchcutOffPriceTime).getOpenPrice();
						dayData.getCallDataMap().put(priceCntr, mMap);
					}
					if (callPrice < ((breachedPrice / 1000))) {
						newStrikePrice = priceCntr;
						break;
					}
					priceCntr = priceCntr + increment;
				}
			
				double callPriceAtSecondStrike = dayData.getCallDataMap().get(newStrikePrice).get(touchcutOffPriceTime).getOpenPrice();
				double secondpremiumPercent = ((callPriceAtSecondStrike / breachedPrice) * 100);
				sumSecondPremiumPcnt = sumSecondPremiumPcnt + secondpremiumPercent;
			}*/
			boolean bookedPositionProfit = false;
			if (breachedPrice > 0) {
				double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(touchcutOffPriceTime).getClosePrice();
				double buybackLossPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
				List<GraphSegment> graphSegments = new ArrayList<>();
				boolean detectedNonUpTrendViaGS = false;
				for (String time : dayData.getMinuteDataMap().keySet()) {
					Util.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
							dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
							dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
							time, 0.2);
					if (time.compareTo(touchcutOffPriceTime) > 0 && time.compareTo(closeTime) < 0) {
						if (detectedNonUpTrendViaGS) {
							if ( (((dayData.getMinuteDataMap().get(time).getHighPrice() - breachedPrice) / breachedPrice) * 100) >= buybackLossPcnt) {
								bookedPositionProfit = true;
								sumbookedPositionProfit = sumbookedPositionProfit + buybackLossPcnt;
								System.out.println("Booked profit at " + time + "  " + breachedPrice);
								break;
							}
						}
						
						GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
						if (!lastGS.identifier.equals("u") && !lastGS.identifier.equals("ur")) {
							detectedNonUpTrendViaGS = true;
						}
						
						if (detectedNonUpTrendViaGS) {
							if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - breachedPrice) / breachedPrice) * 100) >= buybackLossPcnt) {
								bookedPositionProfit = true;
								sumbookedPositionProfit = sumbookedPositionProfit + (((dayData.getMinuteDataMap().get(time).getClosePrice() - breachedPrice) / breachedPrice) * 100);
								System.out.println("Booked profit at " + time + "  " + breachedPrice + "  " + dayData.getMinuteDataMap().get(time).getClosePrice());
								break;
							}
						}
					}
				}
				
				if (bookedPositionProfit == false) {
					secondproblemCases++;
					double secondprofitPcnt = (((breachedPrice - dayData.getMinuteDataMap().get(closeTime).getClosePrice()) / breachedPrice) * 100);
					sumSecondProfitPcnt = sumSecondProfitPcnt + secondprofitPcnt;
				}
			}
			//
			
			totaldays++;
			
			if (dayData.getMinuteDataMap().containsKey(closeTime) && (dayData.getMinuteDataMap().get(closeTime).getClosePrice() > strikePrice || touchcutOffPriceTime != null)) {
				problemCases++;
				String rollBackTime = null;
				if (touchcutOffPriceTime != null) {
					rollBackTime = touchcutOffPriceTime;
					problemCasesSubset++;
				} else {
					rollBackTime = closeTime;
				}
				//double referencePrice = Math.min(strikePrice, dayData.getMinuteDataMap().get("12:55").getClosePrice());
				double profitPcnt = 0;
				//if (bookedPositionProfit == false) {
					double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
					profitPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
					sumprofitPcnt = sumprofitPcnt + profitPcnt;
					
				//	if (touchcutOffPriceTime != null) {
				//		System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
				//	}
				//} else {
					//double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(touchcutOffPriceTime).getClosePrice();
					//profitPcnt = ((callbuybackprice / optionSellingTimePrice) * 100) - 0.1;
					//sumprofitPcnt = sumprofitPcnt + profitPcnt;
					
					System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
				//}
				//System.out.println(profitPcnt);
				//if (dayData.getMinuteDataMap().get("12:55").getClosePrice() > strikePrice) {
				//	problemNegativeCases++;
				//	profitPcnt = ((strikePrice - dayData.getMinuteDataMap().get("12:55").getClosePrice()) / strikePrice) * 100;
				//}
				
				
				/*double secondprofitPcnt = 0.0;
				if (newStrikePrice > 0 && dayData.getMinuteDataMap().get(closeTime).getClosePrice() > newStrikePrice) {
					secondproblemCases++;
					double secondcallbuybackprice =  dayData.getCallDataMap().get(newStrikePrice).get(closeTime).getClosePrice();
					secondprofitPcnt = ((secondcallbuybackprice / breachedPrice) * 100);
					sumSecondProfitPcnt = sumSecondProfitPcnt + secondprofitPcnt;
				}*/
				
				//System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
			}
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		System.out.println("Loss due to rollback option selling " + sumprofitPcnt + "  " + problemCases);
		System.out.println("Loss due to position " + sumSecondProfitPcnt + "  " + secondproblemCases);
		
		//System.out.println(problemCases);
		System.out.println("Position taken for case " + problemCasesSubset);
		//System.out.println(secondproblemCases);
		
		System.out.println("Total Days " + totaldays);
		
		System.out.println("Profit from Premium selling " + sumPremiumPercent);
		System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases));
		
	}

}
