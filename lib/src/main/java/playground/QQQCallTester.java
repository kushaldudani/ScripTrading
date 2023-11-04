package playground;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.GraphSegment;
import ScripTrading.MinuteData;
import ScripTrading.Util;

public class QQQCallTester {
	
	// {"QQQ":[{"name":"INVESCO QQQ TRUST SERIES 1","chineseName":"&#x666F;&#x987A; QQQ&#x4FE1;&#x6258;&#x7CFB;&#x5217;1","assetClass":"STK","contracts":[{"conid":320227571,"exchange":"NASDAQ","isUS":true},{"conid":320227574,"exchange":"MEXI","isUS":false}]}]}
	
	// {"MSFT":[{"name":"MICROSOFT CORP","chineseName":"&#x5FAE;&#x8F6F;&#x516C;&#x53F8;","assetClass":"STK","contracts":[{"conid":272093,"exchange":"NASDAQ","isUS":true},{"conid":38708990,"exchange":"MEXI","isUS":false},{"conid":415569505,"exchange":"EBS","isUS":false}]},{"name":"LS 1X MSFT","chineseName":null,"assetClass":"STK","contracts":[{"conid":493546075,"exchange":"LSEETF","isUS":false}]},{"name":"MICROSOFT CORP-CDR","chineseName":"&#x5FAE;&#x8F6F;&#x516C;&#x53F8;","assetClass":"STK","contracts":[{"conid":518938052,"exchange":"AEQLIT","isUS":false}]}]}
	
	private static boolean downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader, boolean downloadedMoreData) {
		if (!dayData.getCallDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			dayData.getCallDataMap().put(strikePrice, mMap);
			return true;
		}
		
		if (!dayData.getPutDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"P00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			if (dayData.getPutDataMap() == null) {
				Map<Double, Map<String, MinuteData>> putMap = new LinkedHashMap<>();
				dayData.setPutDataMap(putMap);
			}
			dayData.getPutDataMap().put(strikePrice, mMap);
			return true;
		}
		
		return downloadedMoreData;
	}
	
	private static double findAvgVolatilityAcrossDays(LinkedList<Double> volatilityQueue) {
		double sumVolatility = 0;
		Iterator<Double> queueIterator = volatilityQueue.iterator();
		int queueCount = 0;
		while (queueIterator.hasNext()) {
			double vol = queueIterator.next();
			sumVolatility = sumVolatility + vol;
			queueCount++;
		}
		
		double avgVol = (sumVolatility / queueCount);
		return avgVol;
	}
	
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
		Date endDate = sdf.parse("2023-10-07");
		//Date startDate = sdf.parse("2023-04-27");
		//Date endDate = sdf.parse("2023-04-28");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		double prevClosePrice = 0;
		double sumprofitPcnt = 0.0;
		double sumprofitPcntWithLimit = 0.0;
		double sumProfitPcntWithLoss = 0.0;
		int totaldays = 0;
		int problemCases = 0;
		int problemCasesSubset = 0;
		
		boolean downloadedMoreData = false;
		String startTime = "07:15";
		String closeTime = "12:50";
		String midMidTime = "08:30";
		String midTime = "11:00";
		double ninetyPercentileBarChange = 0.3;
		double sumPremiumPercent = 0.0;
		//double sumbookedPositionProfit = 0.0;
		//double sumSecondProfitPcnt = 0.0;
		List<String> stopLossHit = new ArrayList<>();
		Map<String, Double> allCases = new LinkedHashMap<>();
		LinkedList<Double> volatilityQueue = new LinkedList<>();
		
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
			
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			String optionSellingTime = null;
			double closeAtStartTime = dayData.getMinuteDataMap().get(startTime).getClosePrice();
			double strikePrice = 0;//((int) (closeAtStartTime + 0.01 * closeAtStartTime));
			//downloadedMoreData = downloadCallData(strikePrice, currentDateString, dayData, downloader, downloadedMoreData);
			double soldCallPrice = 0;//dayData.getCallDataMap().get(strikePrice).get(optionSellingTime).getClosePrice();//0;
			//double dayLowPrice = dayData.getMinuteDataMap().get(startTime).getClosePrice();
			
			double percentHigherFactor = 0.009;
			double priceLevelToPlaceOrder = closeAtStartTime;
			double targetedStrikePrice = ((int) (priceLevelToPlaceOrder + percentHigherFactor * priceLevelToPlaceOrder));
			downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
			double callPriceTotarget = dayData.getCallDataMap().get(targetedStrikePrice).get(startTime).getClosePrice();
			callPriceTotarget = callPriceTotarget * 1.1;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
						dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
						dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
						time, ninetyPercentileBarChange, priceWithTime);
				if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
					//System.out.println(time);
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
					if (callPriceTotarget >= (closeAtTime / 4000) 
							&& dayData.getCallDataMap().get(targetedStrikePrice).containsKey(time) 
							&& dayData.getCallDataMap().get(targetedStrikePrice).get(time).getHighPrice() >= callPriceTotarget
							&& !lastGS.identifier.equals("u") //|| doesGSHasAnyDorDr(graphSegments))
							//&& !lastGS.identifier.equals("ur")
						) {
						strikePrice = targetedStrikePrice;
						optionSellingTime = time;
						soldCallPrice = callPriceTotarget;
						break;
					}
					priceLevelToPlaceOrder = closeAtTime;
					targetedStrikePrice = (time.compareTo(midMidTime) > 0) 
							              ? ((int) (priceLevelToPlaceOrder + percentHigherFactor * priceLevelToPlaceOrder))
							              : ((int) (priceLevelToPlaceOrder + percentHigherFactor * priceLevelToPlaceOrder));
					//System.out.println(targetedStrikePrice);
					downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
					callPriceTotarget = 0;
					if (dayData.getCallDataMap().get(targetedStrikePrice).containsKey(time)) {
						callPriceTotarget = dayData.getCallDataMap().get(targetedStrikePrice).get(time).getClosePrice();
					}
					callPriceTotarget = callPriceTotarget * 1.1;
				}
			}
			/*double strikePrice = 0;
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
			}*/
			
			/*if (prevClosePrice > 0 && (((optionSellingTimePrice - prevClosePrice) / prevClosePrice) * 100) > (1.5 * ninetyPercentileBarChange)) {
				prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
				continue;
			}*/
			
			double closeAtStartTimeFloor = (int) closeAtStartTime;
			downloadedMoreData = downloadOptionData(closeAtStartTimeFloor, currentDateString, dayData, downloader, downloadedMoreData);
			downloadedMoreData = downloadOptionData(closeAtStartTimeFloor + 1, currentDateString, dayData, downloader, downloadedMoreData);
			double sumCallPriceAtStartTime = dayData.getCallDataMap().get(closeAtStartTimeFloor).get(startTime).getClosePrice() 
					                         + dayData.getCallDataMap().get(closeAtStartTimeFloor + 1).get(startTime).getClosePrice();
			double sumPutPriceAtStartTime = dayData.getPutDataMap().get(closeAtStartTimeFloor).get(startTime).getClosePrice() 
                                             + dayData.getPutDataMap().get(closeAtStartTimeFloor + 1).get(startTime).getClosePrice();
			double totalOptionPriceAtStartTime = (sumCallPriceAtStartTime + sumPutPriceAtStartTime);
			//System.out.println(currentDateString + "  " + (sumCallPriceAtStartTime + sumPutPriceAtStartTime));
			double avgVolatility = (volatilityQueue.size() > 0) ? findAvgVolatilityAcrossDays(volatilityQueue) : 0;
			
			if (avgVolatility > 0 && totalOptionPriceAtStartTime >= (1.66 * avgVolatility)) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        System.out.println(currentDateString + " Did not process as avgVolatility high " + totalOptionPriceAtStartTime + "  " + avgVolatility);
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				continue;
			}
			if (!dayData.getMinuteDataMap().containsKey(closeTime)) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        System.out.println(currentDateString + " Did not process as short day");
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				continue;
			}
			
			volatilityQueue.add(totalOptionPriceAtStartTime);
			if (volatilityQueue.size() > 30) {
				volatilityQueue.poll();
			}
			
			if (strikePrice == 0) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        System.out.println(currentDateString + " Did not process as no entry");
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				continue;
			}
			double optionSellingTimePrice = dayData.getMinuteDataMap().get(optionSellingTime).getClosePrice();
			//double callPriceAtStrike = dayData.getCallDataMap().get(strikePrice).get(optionSellingTime).getOpenPrice();
			double premiumPercent = ((soldCallPrice / optionSellingTimePrice) * 100);
			//System.out.println(premiumPercent);
			sumPremiumPercent = sumPremiumPercent + premiumPercent;
			
			
			/*String touchcutOffPriceTime = null;
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
			}*/
			
			String closedPriceTime = null; //String lossTime = null;
			double closedThreshold = 0.005; //double lossThreshold = 0;
			//double cutOffPrice = strikePrice + (0.002 * strikePrice);
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(closeTime) < 0) {
					/*if (lossThreshold > 0 && currentProfitPercent <= lossThreshold) {
						lossTime = time;
						break;
					}*/
					if (!dayData.getCallDataMap().get(strikePrice).containsKey(time)) {
						closedPriceTime = time;
						break;
					}
					double currrentCallPriceAtStrike = dayData.getCallDataMap().get(strikePrice).get(time).getLowPrice();
					double currentProfitPercent = ((currrentCallPriceAtStrike / optionSellingTimePrice) * 100);
					if (currentProfitPercent <= closedThreshold) {
						closedPriceTime = time;
						break;
					}
					/*if (dayData.getMinuteDataMap().get(time).getClosePrice() >= cutOffPrice && lossThreshold == 0) {
						lossThreshold = (dayData.getCallDataMap().get(strikePrice).get(time).getClosePrice() / optionSellingTimePrice) * 100;
					}*/
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
			//if (breachedPrice > 0) {
			//	double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(touchcutOffPriceTime).getClosePrice();
			//	double buybackLossPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
				
			//	String exitTime = VolumeGraphPatternEntry.longExit(dayData, 0.3, touchcutOffPriceTime, breachedPrice, currentDateString, bokkedProfitMap, 0.3, "optionBasedLongEntry");
			//}
			/*boolean bookedPositionProfit = false;
			if (breachedPrice > 0) {
				double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(touchcutOffPriceTime).getClosePrice();
				double buybackLossPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
				List<GraphSegment> graphSegments = new ArrayList<>();
				Map<String, Double> priceWithTime = new LinkedHashMap<>();
				boolean detectedNonUpTrendViaGS = false;
				double profitThreshold = buybackLossPcnt;
				double dayHigh = 0;
				for (String time : dayData.getMinuteDataMap().keySet()) {
					if (dayData.getMinuteDataMap().get(time).getClosePrice() > dayHigh) {
						dayHigh = dayData.getMinuteDataMap().get(time).getClosePrice() ;
					}
					GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
							dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
							dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
							time, 0.2, priceWithTime);
					if (time.compareTo(touchcutOffPriceTime) > 0 && time.compareTo(closeTime) < 0) {
						if (detectedNonUpTrendViaGS) {
							if ( (((dayData.getMinuteDataMap().get(time).getHighPrice() - breachedPrice) / breachedPrice) * 100) >= profitThreshold) {
								bookedPositionProfit = true;
								sumbookedPositionProfit = sumbookedPositionProfit + profitThreshold;
								bokkedProfitMap.put(currentDateString, profitThreshold);
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
								double prof = (((dayData.getMinuteDataMap().get(time).getClosePrice() - breachedPrice) / breachedPrice) * 100);
								sumbookedPositionProfit = sumbookedPositionProfit + prof;
								bokkedProfitMap.put(currentDateString, prof);
								break;
							}
						}
						
						if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - dayHigh) / dayHigh) * 100) < -0.8) {
							secondproblemCases++;
							bookedPositionProfit = true;
							sumSecondProfitPcnt = sumSecondProfitPcnt + (((dayHigh - dayData.getMinuteDataMap().get(time).getClosePrice()) / dayHigh) * 100);
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
			}*/
			//
			
			totaldays++;
			
			//if (dayData.getMinuteDataMap().containsKey(closeTime) && (dayData.getMinuteDataMap().get(closeTime).getClosePrice() > strikePrice || touchcutOffPriceTime != null)) {
				
				String rollBackTime = null;
				double profitPcnt = 0;
				if (closedPriceTime != null) {
					rollBackTime = closedPriceTime;
					profitPcnt = closedThreshold;
					sumprofitPcntWithLimit = sumprofitPcntWithLimit + profitPcnt;
				} /*else if (lossTime != null) {
					rollBackTime = lossTime;
					problemCasesSubset++;
					profitPcnt = lossThreshold;
					sumProfitPcntWithLoss = sumProfitPcntWithLoss + profitPcnt;
		        }*/ else {
					problemCases++;
					rollBackTime = closeTime;
					double callbuybackprice = 0;
					if (dayData.getCallDataMap().get(strikePrice).containsKey(rollBackTime)) {
						callbuybackprice = dayData.getCallDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
					}
					profitPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
					sumprofitPcnt = sumprofitPcnt + profitPcnt;
				}
				//double referencePrice = Math.min(strikePrice, dayData.getMinuteDataMap().get("12:55").getClosePrice());
				
				//if (bookedPositionProfit == false) {
					
					
				//	if (touchcutOffPriceTime != null) {
				//		System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
				//	}
				//} else {
					//double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(touchcutOffPriceTime).getClosePrice();
					//profitPcnt = ((callbuybackprice / optionSellingTimePrice) * 100) - 0.1;
					//sumprofitPcnt = sumprofitPcnt + profitPcnt;
				allCases.put(currentDateString + "  " + optionSellingTime, strikePrice);
				if (profitPcnt > 0.3) {	
					System.out.println(currentDateString + "  " + totalOptionPriceAtStartTime + "  " + avgVolatility + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + optionSellingTime + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
				}
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
			//}
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			Util.serializeDoubleHashMap(allCases, "config/QQQCallCases.txt");
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		/*for (String key : bokkedProfitMap.keySet()) {
			double profit = bokkedProfitMap.get(key);
			if (profit >= 0) {
				sumbookedPositionProfit = sumbookedPositionProfit + profit;
			} else {
				sumSecondProfitPcnt = sumSecondProfitPcnt + profit;
				secondproblemCases++;
			}
			System.out.println(key + "  " + bokkedProfitMap.get(key));
		}*/
		
		System.out.println("Loss due to rollback option selling at the end of the day " + sumprofitPcnt + "  " + problemCases);
		System.out.println("Loss due to rollback option selling with Limit order " + sumprofitPcntWithLimit);
		System.out.println("Loss due to rollback option selling with Limit loss " + sumProfitPcntWithLoss + "  " + problemCasesSubset);
		
		//System.out.println(problemCases);
		//System.out.println("Position taken for case " + problemCasesSubset);
		//System.out.println(secondproblemCases);
		
		System.out.println("Total Days " + totaldays);
		
		System.out.println("Profit from Premium selling " + sumPremiumPercent);
		//System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases) + " Stop loss hit exit " + stopLossHit);
		//System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases));
		
		//for (String key : bokkedProfitMap.keySet()) {
		//	System.out.println(key + "  " + bokkedProfitMap.get(key));
		//}
	}

}

/*

Loss due to rollback option selling at the end of the day 15.745210423946967  57
Loss due to rollback option selling with Limit order 0.7750000000000006
Total Days 212
Profit from Premium selling 19.98269952791261

Loss due to rollback option selling 7.349199172592204  39
Total Days 212
Profit from Premium selling 15.168938189796814

 */
