package playground;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
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
	
	public void interpret(List<GraphSegment> graphArray, double closeAtTime, String time, double strike) {
		double ninetyPercentileBarChange = 0.3;
		List<IGraphSegment> interpretedGSs = interpretedGraphSegments(graphArray);
		
		evaluateLong(ninetyPercentileBarChange, graphArray, interpretedGSs, closeAtTime, time, strike);
		evaluateShort(ninetyPercentileBarChange, graphArray, interpretedGSs, closeAtTime, time, strike);
	}
	
	private void evaluateShort(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike) {
		double strikeCutOff = strike + (strike * 0.003);
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment lowestStartU = null;
		double highestUPrice = 0;
		double highestDPrice = 0;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("u") && (lowestStartU == null || interpretedGS.startPrice < lowestStartU.startPrice) ) {
				lowestStartU = interpretedGS;
			}
			if (interpretedGS.identifier.equals("d") && (highestDPrice == 0 || interpretedGS.startPrice > highestDPrice) ) {
				highestDPrice = interpretedGS.startPrice;
			}
			if (interpretedGS.identifier.equals("u") && (highestUPrice == 0 || interpretedGS.endPrice > highestUPrice) ) {
				highestUPrice = interpretedGS.endPrice;
			}
			cntr--;
		}
		
		if (segmentsSize > 0) {
			IGraphSegment lastIGS = interpretedGSs.get(segmentsSize - 1);
			//
			if (lastIGS.identifier.equals("d") 
					&& (lowestStartU == null || lastIGS.endPrice < lowestStartU.startPrice) 
					&& closeAtTime <= lastIGS.endPrice
					&& time.compareTo("07:45") >= 0
					&& ( (((lastIGS.startPrice - lastIGS.endPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
					    || (strike > 0 && lastIGS.endPrice <= strikeCutOff) )
				) {
				boolean hasUPullBack = false;
				Iterator<PullBackLevel> itr = lastIGS.pullBackLevels.iterator();
				while (itr.hasNext()) {
					PullBackLevel pbLevel = itr.next();
					if (pbLevel.identifier.equals("u")) {
						hasUPullBack = true;
					}
				}
				//if (!hasUPullBack || time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
					currrentShortState = GSInterpretationType.STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES;
				//}
			}
			//
			if (currrentShortState != GSInterpretationType.STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES
					&& lastIGS.identifier.equals("d") && lastIGS.pullBackLevels.isEmpty()
					&& highestUPrice != 0 && highestUPrice >= highestDPrice
					&& (((lastIGS.startPrice - lastIGS.endPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				IGraphSegment secondLastIGS = interpretedGSs.get(segmentsSize - 2);
				if (secondLastIGS.identifier.equals("u")
						&& (((secondLastIGS.endPrice - secondLastIGS.startPrice) / secondLastIGS.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange)) {
					currrentShortState = GSInterpretationType.PULLBACK_SHORT_IN_EARLY_STAGES;
				}
			}
			//
			if (currrentShortState != GSInterpretationType.STRONG_DIRECTIONAL_SHORT_IN_EARLY_STAGES
					&& currrentShortState != GSInterpretationType.PULLBACK_SHORT_IN_EARLY_STAGES
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				currrentShortState = GSInterpretationType.UNKNOWN_SHORT_IN_EARLY_STAGES;
			}
		} else {
			currrentShortState = GSInterpretationType.NON_DIRECTIONAL;
		}
	}

	private void evaluateLong(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time, double strike) {
		double strikeCutOff = strike - (0.003 * strike);
		int segmentsSize = interpretedGSs.size();
		int cntr = segmentsSize - 1;
		IGraphSegment highestStartD = null;
		double lowestUPrice = 0;
		double lowestDPrice = 0;
		while (cntr >= 0) {
			IGraphSegment interpretedGS = interpretedGSs.get(cntr);
			if (interpretedGS.identifier.equals("d") && (highestStartD == null || interpretedGS.startPrice > highestStartD.startPrice) ) {
				highestStartD = interpretedGS;
			}
			if (interpretedGS.identifier.equals("d") && (lowestDPrice == 0 || interpretedGS.endPrice < lowestDPrice) ) {
				lowestDPrice = interpretedGS.endPrice;
			}
			if (interpretedGS.identifier.equals("u") && (lowestUPrice == 0 || interpretedGS.startPrice < lowestUPrice) ) {
				lowestUPrice = interpretedGS.startPrice;
			}
			cntr--;
		}
		
		if (segmentsSize > 0) {
			IGraphSegment lastIGS = interpretedGSs.get(segmentsSize - 1);
			//
			if (lastIGS.identifier.equals("u") 
					&& (highestStartD == null || lastIGS.endPrice > highestStartD.startPrice)
					&& closeAtTime >= lastIGS.endPrice
					&& time.compareTo("07:45") >= 0
					//&& (((strike - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (4 * ninetyPercentileBarChange)
					&& (  (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
					   || (strike > 0 && lastIGS.endPrice >= strikeCutOff) )
				) {
				boolean hasDPullBack = false;
				Iterator<PullBackLevel> itr = lastIGS.pullBackLevels.iterator();
				while (itr.hasNext()) {
					PullBackLevel pbLevel = itr.next();
					if (pbLevel.identifier.equals("d")) {
						hasDPullBack = true;
					}
				}
				//if (!hasDPullBack || time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
					currrentLongState = GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES;
				//}
			}
			//
			if (currrentLongState != GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES
					&& lastIGS.identifier.equals("u") && lastIGS.pullBackLevels.isEmpty()
					&& lowestDPrice != 0 && lowestDPrice <= lowestUPrice
					&& (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
					//&& !graphSegments.get(graphSegments.size() - 1).identifier.equals("c")
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				IGraphSegment secondLastIGS = interpretedGSs.get(segmentsSize - 2);
				if (secondLastIGS.identifier.equals("d")
						&& (((secondLastIGS.startPrice - secondLastIGS.endPrice) / secondLastIGS.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange)) {
					currrentLongState = GSInterpretationType.PULLBACK_LONG_IN_EARLY_STAGES;
				}
			}
			//
			if (currrentLongState != GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES
					&& currrentLongState != GSInterpretationType.PULLBACK_LONG_IN_EARLY_STAGES
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				currrentLongState = GSInterpretationType.UNKNOWN_LONG_IN_EARLY_STAGES;
			}
		} else {
			currrentLongState = GSInterpretationType.NON_DIRECTIONAL;
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
						pbLevel.identifier = "c";
						pbLevel.down = new PriceTime(lastGS.startPrice, lastGS.startTime);
						pbLevel.up = new PriceTime(lastGS.endPrice, lastGS.endTime);
					} else if (latestIGS.identifier.equals("u")) {
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
	
}
