package playground;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
import ScripTrading.MinuteData;
import ScripTrading.Util;
import ScripTrading.IGraphSegment.PriceTime;
import ScripTrading.IGraphSegment.PullBackLevel;
// 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
public class GSInterpretation {
	
	public enum GSInterpretationType {
		STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES, 
		
		PULLBACK_LONG_IN_EARLY_STAGES,
		
		UNKNOWN_LONG_IN_EARLY_STAGES,
		
		NON_DIRECTIONAL,
	
		STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES, 
		
		PULLBACK_SHORT_IN_EARLY_STAGES,
		
		UNKNOWN_SHORT_IN_EARLY_STAGES,

	}
	
	private GSInterpretationType currrentLongState;
	
	private GSInterpretationType currrentShortState;
	
	public GSInterpretationType getCurrentLongState() {
		return currrentLongState;
	}
	
	public GSInterpretationType getCurrentShortState() {
		return currrentShortState;
	}
	
	public void interpret(List<GraphSegment> graphArray, double closeAtTime, String time, double strike, double avgVix, Map<String, MinuteData> rawVix,
			LinkedList<String> optionVolumeSignalToUse, LinkedList<String> hugePositiveBars, LinkedList<String> hugeNeativeBars, double prevClosePrice) {
		double ninetyPercentileBarChange = 0.3;
		List<IGraphSegment> interpretedGSs = interpretedGraphSegments(graphArray);
		
		evaluateLong(ninetyPercentileBarChange, graphArray, interpretedGSs, closeAtTime, time, strike, avgVix, rawVix, optionVolumeSignalToUse, hugePositiveBars, hugeNeativeBars, prevClosePrice);
		evaluateShort(ninetyPercentileBarChange, graphArray, interpretedGSs, closeAtTime, time, strike, avgVix, rawVix, optionVolumeSignalToUse,hugePositiveBars, hugeNeativeBars, prevClosePrice);
	}
	
	private void evaluateShort(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike,
			double avgVix, Map<String, MinuteData> rawVixMap, LinkedList<String> optionVolumeSignalToUse,
			LinkedList<String> hugePositiveBars, LinkedList<String> hugeNeativeBars, double prevClosePrice) {
		//double rawVix = (rawVixMap != null) ? rawVixMap.get("07:45").getClosePrice() : 0;
		double strikeCutOff = strike; //+ (strike * 0.001); // Changing this to 0 will cut rows 3/24, 3/30 etc.
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
					//&& (lowestStartU == null || (((lowestStartU.endPrice - lowestStartU.startPrice) / lowestStartU.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange) ) 
					&& closeAtTime <= lastIGS.endPrice
					&& time.compareTo("07:40") >= 0 
					&& ( ( time.compareTo("10:20") <= 0 && (optionVolumeSignalToUse.size() > 0 && Util.diffTime(optionVolumeSignalToUse.peekLast(), time) <= 30) )
					   )
					&& (((lastIGS.startPrice - lastIGS.endPrice) / lastIGS.startPrice) * 100) <= (6 * ninetyPercentileBarChange)
				) {
					currrentShortState = GSInterpretationType.STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES;
			}
		}
	}

	private void evaluateLong(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike,
			double avgVix, Map<String, MinuteData> rawVixMap, LinkedList<String> optionVolumeSignalToUse,
			LinkedList<String> hugePositiveBars, LinkedList<String> hugeNeativeBars, double prevClosePrice) {
		//double rawVix = (rawVixMap != null) ? rawVixMap.get("07:45").getClosePrice() : 0;
		double strikeCutOff = strike; 
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment highestStartD = null;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("d") && (highestStartD == null || interpretedGS.startPrice > highestStartD.startPrice) ) {
				highestStartD = interpretedGS;
			}
			cntr--;
		}
		
		if (segmentsSize > 0) {
			IGraphSegment lastIGS = interpretedGSs.get(segmentsSize - 1);
			//
			if (lastIGS.identifier.equals("u")
					&& (highestStartD == null || lastIGS.endPrice > highestStartD.startPrice)
					//&& (highestStartD == null || (((highestStartD.startPrice - highestStartD.endPrice) / highestStartD.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange) ) 
					&& closeAtTime >= lastIGS.endPrice
					&& time.compareTo("07:40") >= 0 
					&& ( ( time.compareTo("10:20") <= 0 && (optionVolumeSignalToUse.size() > 0 && Util.diffTime(optionVolumeSignalToUse.peekLast(), time) <= 30) )
					   )
					&& (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (6 * ninetyPercentileBarChange)
					
				) {
				
				currrentLongState = GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES;
			}
		}
	}
	
	private boolean mergeIGSForU(ArrayList<IGraphSegment> interpretedGSs) {
		IGraphSegment latestIGS = interpretedGSs.get(interpretedGSs.size() - 1);
		IGraphSegment prevIGS = interpretedGSs.get(interpretedGSs.size() - 2);
		IGraphSegment prevPrevIGS = interpretedGSs.get(interpretedGSs.size() - 3);
		if (latestIGS.endPrice > prevPrevIGS.endPrice && latestIGS.startPrice >= prevPrevIGS.startPrice) {
			prevPrevIGS.endTime = latestIGS.endTime;
			prevPrevIGS.endPrice = latestIGS.endPrice;
		
			PullBackLevel pbLevel = new PullBackLevel();
			pbLevel.identifier = "d";
			pbLevel.up = new PriceTime(prevIGS.startPrice, prevIGS.startTime);
			pbLevel.down = new PriceTime(prevIGS.endPrice, prevIGS.endTime);
			prevPrevIGS.pullBackLevels.add(pbLevel);
			prevPrevIGS.pullBackLevels.addAll(latestIGS.pullBackLevels);
			
			interpretedGSs.remove(latestIGS);
			interpretedGSs.remove(prevIGS);
			
			return true;
		}
		
		return false;
	}
	
	private boolean mergeIGSForD(ArrayList<IGraphSegment> interpretedGSs) {
		IGraphSegment latestIGS = interpretedGSs.get(interpretedGSs.size() - 1);
		IGraphSegment prevIGS = interpretedGSs.get(interpretedGSs.size() - 2);
		IGraphSegment prevPrevIGS = interpretedGSs.get(interpretedGSs.size() - 3);
		if (latestIGS.endPrice < prevPrevIGS.endPrice && latestIGS.startPrice <= prevPrevIGS.startPrice) {
			prevPrevIGS.endTime = latestIGS.endTime;
			prevPrevIGS.endPrice = latestIGS.endPrice;
		
			PullBackLevel pbLevel = new PullBackLevel();
			pbLevel.identifier = "u";
			pbLevel.down = new PriceTime(prevIGS.startPrice, prevIGS.startTime);
			pbLevel.up = new PriceTime(prevIGS.endPrice, prevIGS.endTime);
			prevPrevIGS.pullBackLevels.add(pbLevel);
			prevPrevIGS.pullBackLevels.addAll(latestIGS.pullBackLevels);
			
			interpretedGSs.remove(latestIGS);
			interpretedGSs.remove(prevIGS);
			
			return true;
		}
		
		return false;
	}
	
	public List<IGraphSegment> interpretedGraphSegments(List<GraphSegment> graphSegments) {
		ArrayList<IGraphSegment> interpretedGSs = new ArrayList<>();
		
		
		int cntr = 0; int segmentsSize = graphSegments.size();
		while (cntr < segmentsSize) {
			GraphSegment lastGS = graphSegments.get(cntr);
			IGraphSegment latestIGS = (interpretedGSs.size() > 0) ? interpretedGSs.get(interpretedGSs.size() - 1) : null;
			
			if (lastGS.identifier.equals("u") || lastGS.identifier.equals("ur")) {
				if (latestIGS == null) {
					latestIGS = new IGraphSegment("u", lastGS.startTime, lastGS.endTime, lastGS.startPrice, lastGS.endPrice);
					interpretedGSs.add(latestIGS);
				} else if (latestIGS.identifier.equals("u")) {
					if (lastGS.endPrice > latestIGS.endPrice) {
						latestIGS.endTime = lastGS.endTime;
						latestIGS.endPrice = lastGS.endPrice;
					}
				} else if (latestIGS.identifier.equals("d")) {
					latestIGS = new IGraphSegment("u", lastGS.startTime, lastGS.endTime, lastGS.startPrice, lastGS.endPrice);
					interpretedGSs.add(latestIGS);
				}
				while (true) {
					if (interpretedGSs.size() - 3 >= 0) {
						boolean merged = mergeIGSForU(interpretedGSs);
						if (!merged) {
							break;
						}
					} else {
						break;
					}
				}
				latestIGS = interpretedGSs.get(interpretedGSs.size() - 1);
			}
			
			else if (lastGS.identifier.equals("d") || lastGS.identifier.equals("dr")) {
				if (latestIGS == null) {
					latestIGS = new IGraphSegment("d", lastGS.startTime, lastGS.endTime, lastGS.startPrice, lastGS.endPrice);
					interpretedGSs.add(latestIGS);
				} else if (latestIGS.identifier.equals("d")) {
					if (lastGS.endPrice < latestIGS.endPrice) {
						latestIGS.endTime = lastGS.endTime;
						latestIGS.endPrice = lastGS.endPrice;
					}
				} else if (latestIGS.identifier.equals("u")) {
					latestIGS = new IGraphSegment("d", lastGS.startTime, lastGS.endTime, lastGS.startPrice, lastGS.endPrice);
					interpretedGSs.add(latestIGS);
				}
				while (true) {
					if (interpretedGSs.size() - 3 >= 0) {
						boolean merged = mergeIGSForD(interpretedGSs);
						if (!merged) {
							break;
						}
					} else {
						break;
					}
				}
				latestIGS = interpretedGSs.get(interpretedGSs.size() - 1);
			}
			
			else if (lastGS.identifier.equals("c")) {
				if (latestIGS != null) {
					PullBackLevel pbLevel = new PullBackLevel();
					if (latestIGS.identifier.equals("d")) {
						if (lastGS.endPrice < latestIGS.endPrice) {
							latestIGS.endTime = lastGS.endTime;
							latestIGS.endPrice = lastGS.endPrice;
						}
						
						pbLevel.identifier = "c";
						pbLevel.down = new PriceTime(lastGS.startPrice, lastGS.startTime);
						pbLevel.up = new PriceTime(lastGS.endPrice, lastGS.endTime);
					} else if (latestIGS.identifier.equals("u")) {
						if (lastGS.endPrice > latestIGS.endPrice) {
							latestIGS.endTime = lastGS.endTime;
							latestIGS.endPrice = lastGS.endPrice;
						}
						
						pbLevel.identifier = "c";
						pbLevel.down = new PriceTime(lastGS.endPrice, lastGS.endTime);
						pbLevel.up = new PriceTime(lastGS.startPrice, lastGS.startTime);
					}
					latestIGS.pullBackLevels.add(pbLevel);
				}
			}
			
			cntr++;
		}
		
		return interpretedGSs;
	}
	
	
	private void evaluateShortExp(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike,
			double avgVix, Map<String, MinuteData> rawVixMap, LinkedList<String> optionVolumeSignalToUse,
			LinkedList<String> hugePositiveBars, LinkedList<String> hugeNeativeBars, double prevClosePrice) {
		//double rawVix = (rawVixMap != null) ? rawVixMap.get("07:45").getClosePrice() : 0;
		double strikeCutOff = strike; //+ (strike * 0.001); // Changing this to 0 will cut rows 3/24, 3/30 etc.
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
			boolean hasBigUPullBack = false;
			for (PullBackLevel pbLevel : lastIGS.pullBackLevels) {
				if (pbLevel.identifier.equals("u") && (((pbLevel.up.price - pbLevel.down.price) / pbLevel.down.price) * 100) >= 0.4 ) {
					hasBigUPullBack = true;
				}
			}
			//
			if (lastIGS.identifier.equals("d") 
					&& (lowestStartU == null || lastIGS.endPrice < lowestStartU.startPrice) 
					//&& (lowestStartU == null || (((lowestStartU.endPrice - lowestStartU.startPrice) / lowestStartU.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange) ) 
					&& closeAtTime <= lastIGS.endPrice
					&& time.compareTo("07:40") >= 0 
					&& ( ( time.compareTo("10:20") <= 0 && (optionVolumeSignalToUse.size() > 0 && Util.diffTime(optionVolumeSignalToUse.peekLast(), time) <= 30) 
								 //&& hasBigUPullBack == false
							      // && (prevClosePrice == 0 || (((prevClosePrice - closeAtTime) / prevClosePrice) * 100) <= (5 * ninetyPercentileBarChange) )
					           //&& (hugePositiveBars.size() == 0 || hugeNeativeBars.size() == 0 || hugeNeativeBars.get(0).compareTo(hugePositiveBars.get(0)) < 0)
					           && (((lastIGS.startPrice - lastIGS.endPrice) / lastIGS.startPrice) * 100) <= (6 * ninetyPercentileBarChange) )
						//   || 
						// ( time.compareTo("10:30") >= 0 && (strike > 0 && lastIGS.endPrice <= strikeCutOff) ) 
					   )
				
				) {
					currrentShortState = GSInterpretationType.STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES;
			}
		}
	}

	private void evaluateLongExp(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike,
			double avgVix, Map<String, MinuteData> rawVixMap, LinkedList<String> optionVolumeSignalToUse,
			LinkedList<String> hugePositiveBars, LinkedList<String> hugeNeativeBars, double prevClosePrice) {
		//double rawVix = (rawVixMap != null) ? rawVixMap.get("07:45").getClosePrice() : 0;
		double strikeCutOff = strike; 
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment highestStartD = null;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("d") && (highestStartD == null || interpretedGS.startPrice > highestStartD.startPrice) ) {
				highestStartD = interpretedGS;
			}
			cntr--;
		}
		
		if (segmentsSize > 0) {
			IGraphSegment lastIGS = interpretedGSs.get(segmentsSize - 1);
			boolean hasBigDPullBack = false;
			for (PullBackLevel pbLevel : lastIGS.pullBackLevels) {
				if (pbLevel.identifier.equals("d") && (((pbLevel.up.price - pbLevel.down.price) / pbLevel.down.price) * 100) >= 0.4 ) {
					hasBigDPullBack = true;
				}
			}
			//
			if (lastIGS.identifier.equals("u")
					&& (highestStartD == null || lastIGS.endPrice > highestStartD.startPrice)
					//&& (highestStartD == null || (((highestStartD.startPrice - highestStartD.endPrice) / highestStartD.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange) ) 
					&& closeAtTime >= lastIGS.endPrice
					&& time.compareTo("07:40") >= 0 
					&& ( ( time.compareTo("10:20") <= 0 && (optionVolumeSignalToUse.size() > 0 && Util.diffTime(optionVolumeSignalToUse.peekLast(), time) <= 30) 
					              //&& hasBigDPullBack == false
					             // && (prevClosePrice == 0 || (((closeAtTime - prevClosePrice) / prevClosePrice) * 100) <= (5 * ninetyPercentileBarChange) )
					            //&& (hugePositiveBars.size() == 0 || hugeNeativeBars.size() == 0 || hugePositiveBars.get(0).compareTo(hugeNeativeBars.get(0)) < 0)
					            && (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (6 * ninetyPercentileBarChange)  )
					    //   || 
					     //( time.compareTo("10:30") >= 0 && (strike > 0 && lastIGS.endPrice >= strikeCutOff) ) 
					   )
				) {
				
				currrentLongState = GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES;
			}
		}
	}
	
}
