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

public class QQQPutTester {
	
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
		//Date startDate = sdf.parse("2023-02-08");
		//Date endDate = sdf.parse("2023-02-09");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		//String nowDate = sdf.format(calendar.getTime());
		double prevClosePrice = 0;
		double sumprofitPcnt = 0.0;
		double sumprofitPcntWithLimit = 0.0;
		//double sumProfitPcntWithLoss = 0.0;
		int totaldays = 0;
		int problemCases = 0;
		//int problemCasesSubset = 0;
		//int secondproblemCases = 0;
		boolean downloadedMoreData = false;
		String startTime = "07:15";
		String closeTime = "12:50";
		String midMidTime = "08:30";
		String midTime = "11:00";
		double sumPremiumPercent = 0.0;
		double ninetyPercentileBarChange = 0.3;
		//double sumbookedPositionProfit = 0.0;
		//double sumSecondProfitPcnt = 0.0;
		//List<String> stopLossHit = new ArrayList<>();
		//Map<String, Double> bokkedProfitMap = new LinkedHashMap<>();
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
			
			if (!dayData.getMinuteDataMap().containsKey(closeTime)) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        System.out.println(currentDateString + " Did not process as short day");
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				continue;
			}
			
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			String optionSellingTime = null;
			double closeAtStartTime = dayData.getMinuteDataMap().get(startTime).getClosePrice();
			double strikePrice = 0;
			double soldPutPrice = 0;
			
			double percentHigherFactor = 0.009;
			double priceLevelToPlaceOrder = closeAtStartTime;
			//System.out.println(priceLevelToPlaceOrder);
			double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
			//System.out.println(targetedStrikePrice);
			downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
			double putPriceTotarget = dayData.getPutDataMap().get(targetedStrikePrice).get(startTime).getClosePrice();
			putPriceTotarget = putPriceTotarget * 1.1;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
						dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
						dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
						time, ninetyPercentileBarChange, priceWithTime);
				if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
					//System.out.println(time);
					double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
					GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
					if (putPriceTotarget >= (closeAtTime / 4000) 
							&& dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time) 
							&& dayData.getPutDataMap().get(targetedStrikePrice).get(time).getHighPrice() >= putPriceTotarget
							&& !lastGS.identifier.equals("d") //|| doesGSHasAnyDorDr(graphSegments))
							//&& !lastGS.identifier.equals("ur")
						) {
						strikePrice = targetedStrikePrice;
						optionSellingTime = time;
						soldPutPrice = putPriceTotarget;
						break;
					}
					priceLevelToPlaceOrder = closeAtTime;
					targetedStrikePrice = (time.compareTo(midMidTime) > 0) 
							              ? ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1
							              : ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
					//System.out.println(targetedStrikePrice);
					downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
					putPriceTotarget = 0;
					if (dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time)) {
						putPriceTotarget = dayData.getPutDataMap().get(targetedStrikePrice).get(time).getClosePrice();
					}
					putPriceTotarget = putPriceTotarget * 1.1;
				}
			}
			
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
			double premiumPercent = ((soldPutPrice / optionSellingTimePrice) * 100);
			//System.out.println(premiumPercent);
			sumPremiumPercent = sumPremiumPercent + premiumPercent;
			
			/*String touchcutOffPriceTime = null;
			double breachedPrice = 0.0;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				double cutOffPrice = strikePrice - (0.002 * strikePrice);
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(midTime) < 0) {
					if (prevClosePrice > 0 && (((prevClosePrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / prevClosePrice) * 100) > 2.5) {
						break;
					}
					if (dayData.getMinuteDataMap().get(time).getClosePrice() <= cutOffPrice) {
						touchcutOffPriceTime = time;
						breachedPrice = dayData.getMinuteDataMap().get(time).getClosePrice();
						break;
					}
				}
			}*/
			
			String closedPriceTime = null;
			double closedThreshold = 0.005;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(closeTime) < 0) {
					if (!dayData.getPutDataMap().get(strikePrice).containsKey(time)) {
						closedPriceTime = time;
						break;
					}
					double currrentPutPriceAtStrike = dayData.getPutDataMap().get(strikePrice).get(time).getLowPrice();
					double currentProfitPercent = ((currrentPutPriceAtStrike / optionSellingTimePrice) * 100);
					if (currentProfitPercent <= closedThreshold) {
						closedPriceTime = time;
						break;
					}
				}
			}
			
			/*
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
			
			//if (dayData.getMinuteDataMap().containsKey(closeTime) && (dayData.getMinuteDataMap().get(closeTime).getClosePrice() < strikePrice || touchcutOffPriceTime != null)) {
				String rollBackTime = null;
				double profitPcnt = 0;
				//if (touchcutOffPriceTime != null) {
				//	rollBackTime = touchcutOffPriceTime;
				//	problemCasesSubset++;
				//	double callbuybackprice =  dayData.getCallDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
				//	profitPcnt = ((callbuybackprice / optionSellingTimePrice) * 100);
				//	sumprofitPcntWithPosition = sumprofitPcntWithPosition + profitPcnt;
				//} else {
				//if (dayData.getMinuteDataMap().get(closeTime).getClosePrice() < strikePrice) {
				if (closedPriceTime != null) {	
					rollBackTime = closedPriceTime;
					profitPcnt = closedThreshold;
					sumprofitPcntWithLimit = sumprofitPcntWithLimit + profitPcnt;
				} else {
					problemCases++;
					rollBackTime = closeTime;
					double putbuybackprice = 0;
					if (dayData.getPutDataMap().get(strikePrice).containsKey(rollBackTime)) {
						putbuybackprice = dayData.getPutDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
					}
					profitPcnt = ((putbuybackprice / optionSellingTimePrice) * 100);
					sumprofitPcnt = sumprofitPcnt + profitPcnt;
				}
				
				if (profitPcnt > 0.3) {	
					System.out.println(currentDateString + "  " + totalOptionPriceAtStartTime + "  " + avgVolatility + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + optionSellingTime + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
				}
				
				//System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
			//}
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		System.out.println("Loss due to rollback option selling at the end of the day " + sumprofitPcnt + "  " + problemCases);
		System.out.println("Loss due to rollback option selling with Limit order " + sumprofitPcntWithLimit);
		//System.out.println("Loss due to rollback option selling with Limit loss " + sumProfitPcntWithLoss + "  " + problemCasesSubset);
		
		//System.out.println(problemCases);
		//System.out.println("Position taken for case " + problemCasesSubset);
		//System.out.println(secondproblemCases);
		
		System.out.println("Total Days " + totaldays);
		
		System.out.println("Profit from Premium selling " + sumPremiumPercent);
		//System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases) + " Stop loss hit exit " + stopLossHit);
		
		//for (String key : bokkedProfitMap.keySet()) {
		//	System.out.println(key + "  " + bokkedProfitMap.get(key));
		//}
	}

}
