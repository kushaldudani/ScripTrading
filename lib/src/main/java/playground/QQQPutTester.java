package playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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
import ScripTrading.IGraphSegment;
import ScripTrading.LoggerUtil;
import ScripTrading.MinuteData;
import ScripTrading.Util;
import playground.QQQCallTester.StrikeWithPrice;

public class QQQPutTester {
	
	private static boolean downloadOptionData(double strikePrice, String currentDateString, DayData dayData, Downloader downloader, boolean downloadedMoreData) {
		if (!dayData.getCallDataMap().containsKey(strikePrice)) {
			String expiryString = currentDateString.substring(2, 4) + currentDateString.substring(5, 7) + currentDateString.substring(8, 10);
			int intPriceCntr = (int) (strikePrice * 1000);
			Map<String, MinuteData> mMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/O:QQQ"+expiryString+"C00"+intPriceCntr+"/range/5/minute/"+currentDateString+"/"+currentDateString+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			dayData.getCallDataMap().put(strikePrice, mMap);
			downloadedMoreData = true;
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
			downloadedMoreData = true;
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
	
	private static double getTargetedStrike(DayData dayData, String time, IGraphSegment lastIGSD) {
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
		double percentHigherFactor = (lastIGSD != null && (((lastIGSD.startPrice - lastIGSD.endPrice) / lastIGSD.startPrice) * 100) >= (5 * 0.3) ) 
				                     ? 0.009 : 0.009;
		double priceLevelToPlaceOrder = (Math.abs(((closeAtTime - openAtTime) / openAtTime) * 100) > 0.2) ? (closeAtTime + openAtTime) / 2 : closeAtTime;
		double targetedStrikePrice = ((int) (priceLevelToPlaceOrder - percentHigherFactor * priceLevelToPlaceOrder)) + 1;
		
		return targetedStrikePrice;
	}
	
	private static StrikeWithPrice getStrikeWithPrice(DayData dayData, String time, Downloader downloader, String currentDateString, boolean downloadedMoreData,
			double prevTargetedStrikePrice, List<IGraphSegment> interpretedGSs) {
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment lastIGSD = null;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("d") ) {
				lastIGSD = interpretedGS;
				break;
			}
			cntr--;
		}
		
		double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
		double targetedStrikePrice = getTargetedStrike(dayData, time, lastIGSD);
		
		downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
		double maxPutPriceTotarget = 0;
		String timeCntr = Util.timeNMinsAgo(time, 0);
		while (timeCntr.compareTo(time) <= 0) {
			double putPriceTotarget = 0;
			if (dayData.getPutDataMap().get(targetedStrikePrice).containsKey(timeCntr)) {
				putPriceTotarget = dayData.getPutDataMap().get(targetedStrikePrice).get(timeCntr).getClosePrice();
			}
			putPriceTotarget = putPriceTotarget * 1.1;
			
			if (putPriceTotarget > maxPutPriceTotarget) {
				maxPutPriceTotarget = putPriceTotarget;
			}
			
			timeCntr = Util.timeNMinsAgo(timeCntr, -5);
		}
		
		/*double premiumPercent = ((maxPutPriceTotarget / closeAtTime) * 100);
		if (premiumPercent < 0.02) {
			targetedStrikePrice = targetedStrikePrice + 1;
			downloadedMoreData = downloadOptionData(targetedStrikePrice, currentDateString, dayData, downloader, downloadedMoreData);
			if (dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time)) {
				maxPutPriceTotarget = dayData.getPutDataMap().get(targetedStrikePrice).get(time).getClosePrice();
			}
			maxPutPriceTotarget = maxPutPriceTotarget * 1.1;
		}*/
		
		maxPutPriceTotarget = Math.max(maxPutPriceTotarget, ((0.05 * closeAtTime) / 100));
		
		return new StrikeWithPrice(targetedStrikePrice, maxPutPriceTotarget, downloadedMoreData);
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
		double sumExperimentProfitPcnt = 0.0;
		int experimentCasesSubset = 0;
		double sumExperimentPremiumPcnt = 0.0;
		int totaldays = 0;
		int problemCases = 0;
		//int problemCasesSubset = 0;
		//int secondproblemCases = 0;
		boolean downloadedMoreData = false;
		String startTime = "07:15";
		String closeTime = "12:50";
		String midMidTime = "08:30";
		String midTime = "09:30";
		double sumPremiumPercent = 0.0;
		double ninetyPercentileBarChange = 0.3;
		//double sumbookedPositionProfit = 0.0;
		//double sumSecondProfitPcnt = 0.0;
		//List<String> stopLossHit = new ArrayList<>();
		Map<String, Double> allCases = new LinkedHashMap<>();
		Map<String, String> allCasesReadable = new LinkedHashMap<>();
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
			double strikePrice = 0;
			double soldPutPrice = 0;
			boolean firstSignal = true;
			//boolean secondSignal = true;
			GSInterpretation gsInterpretation = new GSInterpretation();
			
			double targetedStrikePrice = 0;
			double prevTargetedStrikePrice = 0;
			double putPriceTotarget = 0;
			GraphSegment lastGS = null;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (putPriceTotarget > 0 
						&& dayData.getPutDataMap().get(targetedStrikePrice).containsKey(time) 
						&& dayData.getPutDataMap().get(targetedStrikePrice).get(time).getHighPrice() >= putPriceTotarget
						&& firstSignal == false
						//&& secondSignal == false
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
				lastGS = graphSegments.get(graphSegments.size() - 1);
				//if ( ( lastGS.identifier.equals("d") ) && firstSignal == false ) {
				//	secondSignal = false;
				//}
				List<IGraphSegment> interpretedGSs = gsInterpretation.interpretedGraphSegments(graphSegments);
				if (time.compareTo(startTime) >= 0 && time.compareTo(midTime) < 0) {
					//System.out.println(time);
					if ( (!lastGS.identifier.equals("d")) ) {
					//if ( (lastGS.identifier.equals("u") || lastGS.identifier.equals("dr") || lastGS.identifier.equals("ur") ) ) {
						firstSignal = false;
					}

					prevTargetedStrikePrice = targetedStrikePrice;
					StrikeWithPrice swp = getStrikeWithPrice(dayData, time, downloader, currentDateString, downloadedMoreData, prevTargetedStrikePrice, interpretedGSs);
					targetedStrikePrice = swp.strike;
					downloadedMoreData = swp.downloadedMoreData;
					putPriceTotarget = swp.price;
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
		        //System.out.println(currentDateString + " Did not process as avgVolatility high " + totalOptionPriceAtStartTime + "  " + avgVolatility);
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
				continue;
			}
			if (!dayData.getMinuteDataMap().containsKey(closeTime)) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        //System.out.println(currentDateString + " Did not process as short day");
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
		        if (downloadedMoreData) {
					Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
				}
				continue;
			}
			
			if (avgVolatility == 0 || totalOptionPriceAtStartTime < (1.66 * avgVolatility)) {
				volatilityQueue.add(totalOptionPriceAtStartTime);
				if (volatilityQueue.size() > 30) {
					volatilityQueue.poll();
				}
			}
			
			if (strikePrice == 0) {
				calendar.add(Calendar.DATE, 1);
		        currentDate = calendar.getTime();
		        //System.out.println(currentDateString + " Did not process as no entry");
		        prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
		        if (downloadedMoreData) {
					Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
				}
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
			String shouldAddStopLossAtTime = null;
			for (String time : dayData.getMinuteDataMap().keySet()) {
				if (time.compareTo(optionSellingTime) > 0 && time.compareTo(closeTime) < 0) {
					if (!dayData.getPutDataMap().get(strikePrice).containsKey(time)) {
						continue;
					}
					
					//if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - strikePrice) / strikePrice) * 100) < -0.6 && shouldAddStopLossAtTime == null) {
					//	shouldAddStopLossAtTime = time;
					//}
					
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
					rollBackTime = closeTime; //(shouldAddStopLossAtTime != null) ? shouldAddStopLossAtTime : closeTime;
					double putbuybackprice = 0;
					if (dayData.getPutDataMap().get(strikePrice).containsKey(rollBackTime)) {
						putbuybackprice = dayData.getPutDataMap().get(strikePrice).get(rollBackTime).getClosePrice();
					}
					profitPcnt = ((putbuybackprice / optionSellingTimePrice) * 100);
					sumprofitPcnt = sumprofitPcnt + profitPcnt;
				}
				
				
				String rowToWrite = currentDateString + "  " + strikePrice + "  " + optionSellingTime + "  " + premiumPercent + "  " + profitPcnt;
				allCasesReadable.put(currentDateString, rowToWrite);
				
				
				allCases.put(currentDateString + "  " + optionSellingTime, strikePrice);
				//if (shouldAddStopLossAtTime != null) {	
					
					System.out.println(rowToWrite);
					experimentCasesSubset++;
					sumExperimentProfitPcnt = sumExperimentProfitPcnt + profitPcnt;
					sumExperimentPremiumPcnt = sumExperimentPremiumPcnt + premiumPercent;
				//}
				
				//System.out.println(currentDateString + "  " + optionSellingTimePrice + "  " + strikePrice + "  " + breachedPrice + "  " + rollBackTime + "  " + dayData.getMinuteDataMap().get(closeTime).getClosePrice() + "  " + profitPcnt);
			//}
			prevClosePrice = (dayData.getMinuteDataMap().containsKey("12:55")) ? dayData.getMinuteDataMap().get("12:55").getClosePrice() : 0;
			if (downloadedMoreData) {
				Util.serializeHashMap(dayDataMap, "config/QQQ.txt");
			}
			//Util.serializeDoubleHashMap(allCases, "config/QQQPutCases.txt");
			calendar.add(Calendar.DATE, 1);
	        currentDate = calendar.getTime();
		}
		
		Map<String, String> masterResults = readResults("config/QQQPutMaster.txt");
		
		System.out.println("Loss due to rollback option selling at the end of the day " + sumprofitPcnt + "  " + problemCases);
		System.out.println("Loss due to rollback option selling with Limit order " + sumprofitPcntWithLimit);
		//System.out.println("Loss due to rollback option selling with Limit loss " + sumProfitPcntWithLoss + "  " + problemCasesSubset);
		
		//System.out.println(problemCases);
		//System.out.println("Position taken for case " + problemCasesSubset);
		//System.out.println(secondproblemCases);
		
		System.out.println("Total Days " + totaldays);
		
		System.out.println("Profit from Premium selling " + sumPremiumPercent);
		//System.out.println("Booked Profit from position exit " + sumbookedPositionProfit + "  " + (problemCasesSubset - secondproblemCases) + " Stop loss hit exit " + stopLossHit);
		
		System.out.println("Diff View - Entries missing");
		for (String key : masterResults.keySet()) {
			if (!allCasesReadable.containsKey(key)) {
				System.out.println(masterResults.get(key));
			}
		}
		System.out.println("Diff View - Entries changed");
		for (String key : masterResults.keySet()) {
			if (allCasesReadable.containsKey(key) && !masterResults.get(key).equals(allCasesReadable.get(key)) ) {
				System.out.println(allCasesReadable.get(key));
			}
		}
		System.out.println("Diff View - New Entries");
		for (String key : allCasesReadable.keySet()) {
			if (!masterResults.containsKey(key)) {
				System.out.println(allCasesReadable.get(key));
			}
		}
		
		System.out.println("Experiment premium Pcnt " + sumExperimentPremiumPcnt);
		System.out.println("Experiment Loss Pcnt " + sumExperimentProfitPcnt);
		System.out.println("Experiment count " + experimentCasesSubset);
	}
	
	private static Map<String, String> readResults(String path){
		InputStreamReader is = null;
		BufferedReader br = null;
		Map<String, String> results = new LinkedHashMap<>();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File(path)));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				results.put(linsVals[0], line);
			}
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
			//System.exit(1);
		}finally{
			 try {
				br.close();
				is.close();
				
			} catch (Exception e) {}
		}
		return results;
	}

}

/*

Loss due to rollback option selling at the end of the day 14.431995636948926  72
Loss due to rollback option selling with Limit order 0.6550000000000005
Total Days 203
Profit from Premium selling 24.94826969462124

 */

