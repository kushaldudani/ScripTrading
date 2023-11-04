package playground;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.DayData;
import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
import ScripTrading.Util;
import playground.GSInterpretation.GSInterpretationType;

public class VolumeGraphPatternEntry {
	
	public static String startTime = "07:15";
	public static String closeTime = "12:35";
	public static String midTime = "11:30";
	public static String midMidTime = "09:00";
	
	/*public static boolean entry(DayData dayData, Map<String, LinkedList<Double>> volumeMap, double ninetyPercentileBarChange, String time, double avgVolume) {
		if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			for (String tme : dayData.getMinuteDataMap().keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(tme).getVolume(),
					dayData.getMinuteDataMap().get(tme).getOpenPrice(), dayData.getMinuteDataMap().get(tme).getClosePrice(),
					dayData.getMinuteDataMap().get(tme).getHighPrice(), dayData.getMinuteDataMap().get(tme).getLowPrice(),
					tme, ninetyPercentileBarChange, priceWithTime);
				if (tme.equals(time)) {
					break;
				}
			}
			//System.out.println(time);
			//for (GraphSegment gs : graphSegments) {
			//	System.out.println(gs);
			//}
			
			double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
			double closeAt15MinsAgo = dayData.getMinuteDataMap().get(Util.timeNMinsAgo(time, 15)).getClosePrice();
			//double openAtTime = dayData.getMinuteDataMap().get(time).getOpenPrice();
				
			boolean supportedByGS = Util.goodTimeToLongBasedOffGraphSegments(graphSegments, closeAtTime, ninetyPercentileBarChange, closeAt15MinsAgo);
			double curVolume = dayData.getMinuteDataMap().get(time).getVolume();
			//System.out.println("curVolume " + curVolume);
			//System.out.println("avgVolume " + avgVolume);
			if (curVolume > (1.5 * avgVolume) 
				&& supportedByGS
				) {
				return true;
			}
		}
			
		return false;
	}*/
	
	public static String longExit(DayData dayData, double ninetyPercentileBarChange, String enterTime, double enterPrice, String currentDateString,
			Map<String, Double> bookedProfitMap, String enterString) {
		List<GraphSegment> graphSegments = new ArrayList<>();
		Map<String, Double> priceWithTime = new LinkedHashMap<>();
		LinkedList<Double> closePricesQueue = new LinkedList<>();
		int trendViaGS = 0;
		double pThreshold = 0;
		/*List<Double> profitLevels = new ArrayList<>();
		profitLevels.add(profitThreshold);
		profitLevels.add(0.0);
		profitLevels.add(-1 * profitThreshold);
		profitLevels.add(-2 * profitThreshold);*/
		//profitLevels.add(-2 * ninetyPercentileBarChange);
		//double profitThreshold = (enterTime.compareTo(midMidTime) > 0) ? ninetyPercentileBarChange * volumeFactor * 0.75 : ninetyPercentileBarChange * volumeFactor;
		//double profitThreshold = ninetyPercentileBarChange / 2;
		//double dayHigh = 0;
		for (String time : dayData.getMinuteDataMap().keySet()) {
			//if (dayData.getMinuteDataMap().get(time).getClosePrice() > dayHigh) {
			//	dayHigh = dayData.getMinuteDataMap().get(time).getClosePrice() ;
			//}
			//double closePriceAvg = (closePricesQueue.size() > 0) ? findAvg(closePricesQueue) : 0;
			GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
					dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
					dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
					time, ninetyPercentileBarChange, priceWithTime);
			if (time.compareTo(enterTime) > 0 && time.compareTo(closeTime) < 0) {
				GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
				if (lastGS.identifier.equals("d")) {
					if (closePricesQueue.size() > 0) {
						if (closePricesQueue.peekLast() != lastGS.endPrice) {
							closePricesQueue.add(lastGS.endPrice);
						}
					} else {
						closePricesQueue.add(lastGS.endPrice);
					}
				}
				
				System.out.println("In exit " + time);
				for (GraphSegment gs : graphSegments) {
					System.out.println(gs);
				}
				System.out.println(" enterPrice " + enterPrice + " closePriceQueue " + closePricesQueue);
				System.out.println(trendViaGS);
				System.out.println("pThreshold " + pThreshold);
				System.out.println("high price " + dayData.getMinuteDataMap().get(time).getHighPrice() + " highest Pcnt " + ((dayData.getMinuteDataMap().get(time).getHighPrice() - enterPrice) / enterPrice) * 100);
				
				if (trendViaGS == -1 && pThreshold != 0) {
					if ( (((dayData.getMinuteDataMap().get(time).getHighPrice() - enterPrice) / enterPrice) * 100) >= pThreshold) {
						bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + time + "  " + enterString, pThreshold);
						return time;
					}
				}
				
				if ((lastGS.identifier.equals("d") && trendViaGS == 1) || trendViaGS == -1) {
				//if (!lastGS.identifier.equals("u")) {
					trendViaGS = -1;
					double currentPL = 0;
					//if (closePriceAvg > 0) {
						currentPL = (((closePricesQueue.peekLast() - enterPrice) / enterPrice) * 100);
					//} else {
					//	currentPL = (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100);
					//}
					/*int returnIndex = 0;
					for (int index = 0; index < profitLevels.size(); index++) {
						if (currentPL <= profitLevels.get(index) && (index - 1) >= 0) {
							returnIndex = index - 1;
						}
					}*/
					double constantLimitFactor = (time.compareTo(midTime) > 0) ? 0.5 : 0.5;
					pThreshold = currentPL + constantLimitFactor;//profitLevels.get(returnIndex);
					
					//if (dayData.getMinuteDataMap().get(time).getClosePrice() > enterPrice && lastGS.identifier.equals("d")) {
					//	pThreshold = Math.min(pThreshold, 0.01);
					//}
				}// else {
				//	trendViaGS = 0;
				//}
				// Util.diffTime(enterTime, time) >= 75
				// 
				if ((Util.diffTime(enterTime, time) >= 75 || lastGS.identifier.equals("u")) && trendViaGS == 0) {
					trendViaGS = 1;
				}
				/*if (lastGS.identifier.equals("c") && (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100) < (ninetyPercentileBarChange / 3)) {
					detectedNonUpTrendViaGS = true;
				}
				if (time.compareTo("12:10") >= 0) {
					detectedNonUpTrendViaGS = true;
					
					double currentPL = (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100);
					profitThreshold = currentPL + ninetyPercentileBarChange / 3;
				}
				if (detectedNonUpTrendViaGS) {
					if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100) >= profitThreshold) {
						double prof = (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100);
						bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + time + "  " + enterString, prof);
						return time;
					}
				}*/
				
				/*if ( (((dayData.getMinuteDataMap().get(time).getClosePrice() - enterPrice) / enterPrice) * 100) < -0.8) {
					secondproblemCases++;
					bookedPositionProfit = true;
					sumSecondProfitPcnt = sumSecondProfitPcnt + (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100);
					stopLossHit.add(currentDateString);
					break;
				}*/
			}
			//closePricesQueue.add(dayData.getMinuteDataMap().get(time).getClosePrice());
			//if (closePricesQueue.size() > 6) {
			//	closePricesQueue.poll();
			//}
		}
		
		double prof = (((dayData.getMinuteDataMap().get(closeTime).getClosePrice() - enterPrice) / enterPrice) * 100);
		bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + closeTime + "  " + enterString, prof);
		return closeTime;
	}
	
	
	public static boolean bullEntry(DayData dayData, double ninetyPercentileBarChange, String time) {
		if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			for (String tme : dayData.getMinuteDataMap().keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(tme).getVolume(),
					dayData.getMinuteDataMap().get(tme).getOpenPrice(), dayData.getMinuteDataMap().get(tme).getClosePrice(),
					dayData.getMinuteDataMap().get(tme).getHighPrice(), dayData.getMinuteDataMap().get(tme).getLowPrice(),
					tme, ninetyPercentileBarChange, priceWithTime);
				if (tme.equals(time)) {
					break;
				}
			}
			
			
			double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
			//double curVolume = dayData.getMinuteDataMap().get(time).getVolume();
			
			GSInterpretation gsInterpretation = new GSInterpretation();
			gsInterpretation.interpret(graphSegments, closeAtTime, time);
			System.out.println("In entry " + time);
			for (GraphSegment gs : graphSegments) {
				System.out.println(gs);
			}
			List<IGraphSegment> interpretedGSs = gsInterpretation.interpretedGraphSegments(graphSegments);
			for (IGraphSegment igs : interpretedGSs) {
				System.out.println(igs);
			}
			
			/*int segmentsSize = graphSegments.size();
			int cntr = segmentsSize - 1;
			if (!graphSegments.get(cntr).identifier.equals("u")) {
				return false;
			}
			cntr--;
			while (cntr > 0) {
				GraphSegment graphSegment = graphSegments.get(cntr);
				if ((graphSegment.identifier.equals("d") || graphSegment.identifier.equals("dr"))) {
					break;
				}
				cntr--;
			}
			
			if ((graphSegments.get(cntr).identifier.equals("d") || graphSegments.get(cntr).identifier.equals("dr"))) {
				return false;
			}
			double floorPrice = graphSegments.get(cntr).endPrice; */
			
			if (
					gsInterpretation.getCurrentState() == GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES ||
					gsInterpretation.getCurrentState() == GSInterpretationType.PULLBACK_LONG_IN_EARLY_STAGES// ||
					//gsInterpretation.getCurrentState() == GSInterpretationType.UNKNOWN_LONG_IN_EARLY_STAGES
				) {
				return true;
			}
		}
			
		return false;
	}
	
	/*public static boolean optionBasedLongEntry(DayData dayData, String time, double prevClosePrice) {
		int increment = 1;
		double optionSellingTimePrice = dayData.getMinuteDataMap().get(startTime).getOpenPrice();
		double strikePrice = 0;
		double priceCntr = ((int) optionSellingTimePrice) + increment;
		while(strikePrice == 0) {
			double callPrice = 0.0;
			if (dayData.getCallDataMap().containsKey(priceCntr)) {
				callPrice = dayData.getCallDataMap().get(priceCntr).get(startTime).getOpenPrice();
			} else {
				throw new IllegalStateException("Data for call price not found");
			}
			if (callPrice < ((optionSellingTimePrice / 1000))) {
				strikePrice = priceCntr - increment;
				break;
			}
			
			priceCntr = priceCntr + increment;
		}
		
		//String touchcutOffPriceTime = null;
		//double breachedPrice = 0.0;
		double cutOffPrice = strikePrice + (0.002 * strikePrice);
		if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
			if (prevClosePrice > 0 && (((dayData.getMinuteDataMap().get(time).getClosePrice() - prevClosePrice) / prevClosePrice) * 100) > 2.5) {
				return false;
			}
			if (dayData.getMinuteDataMap().get(time).getClosePrice() >= cutOffPrice) {
				//touchcutOffPriceTime = time;
				//breachedPrice = dayData.getMinuteDataMap().get(time).getClosePrice();
				return true;
			}
		}
		
		return false;
	}*/
	
	public static double findAvg(LinkedList<Double> queue) {
		double sum = 0;
		Iterator<Double> queueIterator = queue.iterator();
		int queueCount = 0;
		while (queueIterator.hasNext()) {
			double entry = queueIterator.next();
			sum = sum + entry;
			queueCount++;
		}
		
		double avg = (sum / queueCount);
		return avg;
	}
	
	public static double findAvgVolume(DayData dayData, String timeBegin, String timeEnd) {
		double sumVolume = 0;
		int cntr = 0;
		String time = timeBegin;
		while (time.compareTo(timeEnd) < 0) {
			double volm = dayData.getMinuteDataMap().get(time).getVolume();
			sumVolume = sumVolume + volm;
			cntr++;
			time = Util.timeNMinsAgo(time, -5);
		}
		
		double avgVolume = (sumVolume / cntr);
		return avgVolume;
	}
	
	public static double findAvgVolumeAcrossDays(Map<String, LinkedList<Double>> volumeMap, String timeBegin, String timeEnd) {
		int days = 30;
		double sumVolume = 0;
		int cntr = 0;
		for (String time : volumeMap.keySet()) {
			if (time.compareTo(timeBegin) >= 0 && time.compareTo(timeEnd) < 0) {
				Iterator<Double> volumeQueueIterator = volumeMap.get(time).iterator();
				int queueCount = 0;
				while (volumeQueueIterator.hasNext()) {
					double volm = volumeQueueIterator.next();
					sumVolume = sumVolume + volm;
					queueCount++;
				}
				if (queueCount != days) {
					throw new IllegalStateException("findAvgVolumeAcrossDays has issues at time " +  time);
				}
				cntr++;
			}
		}
		
		double avgVolume = (sumVolume / (days * cntr));
		return avgVolume;
	}
	
	public static String shortExit(DayData dayData, double ninetyPercentileBarChange, String enterTime, double enterPrice, String currentDateString,
			Map<String, Double> bookedProfitMap, double profitThreshold, String enterString) {
		List<GraphSegment> graphSegments = new ArrayList<>();
		Map<String, Double> priceWithTime = new LinkedHashMap<>();
		boolean detectedNonUpTrendViaGS = false;
		List<Double> profitLevels = new ArrayList<>();
		profitLevels.add(profitThreshold);
		profitLevels.add(0.0);
		profitLevels.add(-1 * profitThreshold);
		profitLevels.add(-2 * profitThreshold);
		for (String time : dayData.getMinuteDataMap().keySet()) {
			//if (dayData.getMinuteDataMap().get(time).getClosePrice() > dayHigh) {
			//	dayHigh = dayData.getMinuteDataMap().get(time).getClosePrice() ;
			//}
			GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(time).getVolume(),
					dayData.getMinuteDataMap().get(time).getOpenPrice(), dayData.getMinuteDataMap().get(time).getClosePrice(),
					dayData.getMinuteDataMap().get(time).getHighPrice(), dayData.getMinuteDataMap().get(time).getLowPrice(),
					time, ninetyPercentileBarChange, priceWithTime);
			if (time.compareTo(enterTime) > 0 && time.compareTo(closeTime) < 0) {
				//System.out.println(time);
				//for (GraphSegment gs : graphSegments) {
				//	System.out.println(gs);
				//}
				if (detectedNonUpTrendViaGS) {
					if ( (((enterPrice - dayData.getMinuteDataMap().get(time).getLowPrice()) / enterPrice) * 100) >= profitThreshold) {
						bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + time + "  " + enterString, profitThreshold);
						return time;
					}
				}
				
				GraphSegment lastGS = graphSegments.get(graphSegments.size() - 1);
				if (lastGS.identifier.equals("u")) {
					detectedNonUpTrendViaGS = true;
					
					double currentPL = (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100);
					int returnIndex = 0;
					for (int index = 0; index < profitLevels.size(); index++) {
						if (currentPL <= profitLevels.get(index) && (index - 1) >= 0) {
							returnIndex = index - 1;
						}
					}
					profitThreshold = profitLevels.get(returnIndex);
				}
				if (lastGS.identifier.equals("c") && (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100) < (ninetyPercentileBarChange / 3)) {
					detectedNonUpTrendViaGS = true;
				}
				if (time.compareTo("12:10") >= 0) {
					detectedNonUpTrendViaGS = true;
					
					double currentPL = (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100);
					profitThreshold = currentPL + ninetyPercentileBarChange / 3;
				}
				//System.out.println("profitThreshold " + profitThreshold);
				if (detectedNonUpTrendViaGS) {
					if ( (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100) >= profitThreshold) {
						double prof = (((enterPrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / enterPrice) * 100);
						bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + time + "  " + enterString, prof);
						return time;
					}
				}
				
			}
		}
		
		double prof = (((enterPrice - dayData.getMinuteDataMap().get(closeTime).getClosePrice()) / enterPrice) * 100);
		bookedProfitMap.put(currentDateString + "  " + enterTime + "  " + closeTime + "  " + enterString, prof);
		return closeTime;
	}
	
	/*public static boolean optionBasedShortEntry(DayData dayData, String time, double prevClosePrice) {
		int increment = 1;
		double optionSellingTimePrice = dayData.getMinuteDataMap().get(startTime).getOpenPrice();
		double strikePrice = 0;
		double priceCntr = ((int) optionSellingTimePrice) - increment;
		while(strikePrice == 0) {
			double putPrice = 0.0;
			if (dayData.getPutDataMap().containsKey(priceCntr)) {
				putPrice = dayData.getPutDataMap().get(priceCntr).get(startTime).getOpenPrice();
			} else {
				throw new IllegalStateException("Data for put price not found");
			}
			if (putPrice < ((optionSellingTimePrice / 1000))) {
				strikePrice = priceCntr + increment;
				break;
			}
			
			priceCntr = priceCntr - increment;
		}
		
		//String touchcutOffPriceTime = null;
		//double breachedPrice = 0.0;
		double cutOffPrice = strikePrice - (0.002 * strikePrice);
		if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
			if (prevClosePrice > 0 && (((prevClosePrice - dayData.getMinuteDataMap().get(time).getClosePrice()) / prevClosePrice) * 100) > 2.5) {
				return false;
			}
			if (dayData.getMinuteDataMap().get(time).getClosePrice() <= cutOffPrice) {
				//touchcutOffPriceTime = time;
				//breachedPrice = dayData.getMinuteDataMap().get(time).getClosePrice();
				return true;
			}
		}
		
		return false;
	}*/
	
	public static boolean bearEntry(DayData dayData, double ninetyPercentileBarChange, String time, double avgVolume) {
		if (time.compareTo(startTime) > 0 && time.compareTo(midTime) < 0) {
			List<GraphSegment> graphSegments = new ArrayList<>();
			Map<String, Double> priceWithTime = new LinkedHashMap<>();
			for (String tme : dayData.getMinuteDataMap().keySet()) {
				GSUtil.calculateGraphSegments(graphSegments, dayData.getMinuteDataMap().get(tme).getVolume(),
					dayData.getMinuteDataMap().get(tme).getOpenPrice(), dayData.getMinuteDataMap().get(tme).getClosePrice(),
					dayData.getMinuteDataMap().get(tme).getHighPrice(), dayData.getMinuteDataMap().get(tme).getLowPrice(),
					tme, ninetyPercentileBarChange, priceWithTime);
				if (tme.equals(time)) {
					break;
				}
			}
			//System.out.println(time);
			//for (GraphSegment gs : graphSegments) {
			//	System.out.println(gs);
			//}
			
			double closeAtTime = dayData.getMinuteDataMap().get(time).getClosePrice();
				
			int segmentsSize = graphSegments.size();
			int cntr = segmentsSize - 1;
			if (!graphSegments.get(cntr).identifier.equals("d")) {
				return false;
			}
			cntr--;
			while (cntr > 0) {
				GraphSegment graphSegment = graphSegments.get(cntr);
				if ((graphSegment.identifier.equals("u") || graphSegment.identifier.equals("ur"))) {
					break;
				}
				cntr--;
			}
			
			if ((graphSegments.get(cntr).identifier.equals("u") || graphSegments.get(cntr).identifier.equals("ur"))) {
				return false;
			}
			
			double ceilPrice = graphSegments.get(cntr).endPrice;
			double curVolume = dayData.getMinuteDataMap().get(time).getVolume();
			//System.out.println("curVolume " + curVolume);
			//System.out.println("avgVolume " + avgVolume);
			if (curVolume > (1.5 * avgVolume) 
				//&& (closeAtTime > (ceilPrice - (3.5 * ninetyPercentileBarChange * ceilPrice / 100)))
				) {
				return true;
			}
		}
			
		return false;
	}

}
