package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QQQTester {
	
	// {"QQQ":[{"name":"INVESCO QQQ TRUST SERIES 1","chineseName":"&#x666F;&#x987A; QQQ&#x4FE1;&#x6258;&#x7CFB;&#x5217;1","assetClass":"STK","contracts":[{"conid":320227571,"exchange":"NASDAQ","isUS":true},{"conid":320227574,"exchange":"MEXI","isUS":false}]}]}
	
	// {"MSFT":[{"name":"MICROSOFT CORP","chineseName":"&#x5FAE;&#x8F6F;&#x516C;&#x53F8;","assetClass":"STK","contracts":[{"conid":272093,"exchange":"NASDAQ","isUS":true},{"conid":38708990,"exchange":"MEXI","isUS":false},{"conid":415569505,"exchange":"EBS","isUS":false}]},{"name":"LS 1X MSFT","chineseName":null,"assetClass":"STK","contracts":[{"conid":493546075,"exchange":"LSEETF","isUS":false}]},{"name":"MICROSOFT CORP-CDR","chineseName":"&#x5FAE;&#x8F6F;&#x516C;&#x53F8;","assetClass":"STK","contracts":[{"conid":518938052,"exchange":"AEQLIT","isUS":false}]}]}
	
	public static void main(String[] args) throws Exception {
		Downloader downloader = new Downloader();
		int increment = 1;
		
		Map<String, DayData> dayDataMap = Util.deserializeHashMap("config/QQQ.txt");
		if (dayDataMap == null) {
			System.out.println("Error reading cache");
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = sdf.parse("2022-12-01");
		Date endDate = sdf.parse("2023-09-06");
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
		List<String> stopLossHit = new ArrayList<>();
		
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
				double profitThreshold = buybackLossPcnt;
				double dayHigh = 0;
				for (String time : dayData.getMinuteDataMap().keySet()) {
					if (dayData.getMinuteDataMap().get(time).getClosePrice() > dayHigh) {
						dayHigh = dayData.getMinuteDataMap().get(time).getClosePrice() ;
					}
					Util.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
							dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
							dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
							time, 0.2);
					if (time.compareTo(touchcutOffPriceTime) > 0 && time.compareTo(closeTime) < 0) {
						if (detectedNonUpTrendViaGS) {
							if ( (((dayData.getMinuteDataMap().get(time).getHighPrice() - breachedPrice) / breachedPrice) * 100) >= profitThreshold) {
								bookedPositionProfit = true;
								sumbookedPositionProfit = sumbookedPositionProfit + profitThreshold;
								break;
							}
						}
						
						GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
						if (!lastGS.identifier.equals("u") && !lastGS.identifier.equals("ur")) {
							detectedNonUpTrendViaGS = true;
							if (lastGS.identifier.equals("d") || lastGS.identifier.equals("dr")) {
								profitThreshold = 0;
							}
							if (time.compareTo("12:30") >= 0) {
								profitThreshold = 0;
							}
						}
						
						if (detectedNonUpTrendViaGS) {
							if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - breachedPrice) / breachedPrice) * 100) >= profitThreshold) {
								bookedPositionProfit = true;
								sumbookedPositionProfit = sumbookedPositionProfit + (((dayData.getMinuteDataMap().get(time).getClosePrice() - breachedPrice) / breachedPrice) * 100);
								break;
							}
						}
						
						if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - dayHigh) / dayHigh) * 100) < -0.8) {
							bookedPositionProfit = true;
							sumbookedPositionProfit = sumbookedPositionProfit + (((dayData.getMinuteDataMap().get(time).getClosePrice() - dayHigh) / dayHigh) * 100);
							stopLossHit.add(currentDateString);
							break;
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
		System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases) + " Stop loss hit exit " + stopLossHit);
		
	}

}

/*
 10.95078955929488
31
190
14.162667012062036

10.508080549562719
30
190
13.940940330882295

9.973905834704155
33
190
13.940940330882295

21.21608240369002
62
190
26.306954893468447

11.58226091079236
7.797514136166318
62
30
190
26.306954893468447

10.068045142272915
5.936843312774853
59
40
15
190
26.306954893468447

9.535362152830304
4.8782572069047685
58
39
14
190
26.306954893468447

Loss due to rollback option selling 19.169183645727973  58
Loss due to position 4.8782572069047685  14
Position taken for case 39
Total Days 190
Profit from Premium selling 26.306954893468447
Booked Profit from position exit 10.712166949319883  25 Stop loss hit exit []

Loss due to rollback option selling 19.169183645727973  58
Loss due to position 0.6895957133388048  2
Position taken for case 39
Total Days 190
Profit from Premium selling 26.306954893468447
Booked Profit from position exit 7.528233818528309  37 Stop loss hit exit [2023-03-06, 2023-07-07]

 */
