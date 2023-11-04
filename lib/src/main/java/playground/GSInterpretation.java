package playground;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ScripTrading.GraphSegment;
import ScripTrading.IGraphSegment;
import ScripTrading.IGraphSegment.PriceTime;
import ScripTrading.IGraphSegment.PullBackLevel;

public class GSInterpretation {
	
	public enum GSInterpretationType {
		STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES, 
		
		PULLBACK_LONG_IN_EARLY_STAGES,
		
		UNKNOWN_LONG_IN_EARLY_STAGES,
		
		NON_DIRECTIONAL,
	
		//STRONG_DIRECTIONAL_SHORT;

	}
	
	private GSInterpretationType currrentState;
	
	private GraphSegment biggestLongSegment;
	
	public GSInterpretationType getCurrentState() {
		return currrentState;
	}
	
	public void interpret(List<GraphSegment> graphArray, double closeAtTime, String time) {
		double ninetyPercentileBarChange = 0.3;
		List<IGraphSegment> interpretedGSs = interpretedGraphSegments(graphArray);
		
		strongDirectionalLong(ninetyPercentileBarChange, graphArray, interpretedGSs, closeAtTime, time);
		
	}

	private void strongDirectionalLong(double ninetyPercentileBarChange, List<GraphSegment> graphSegments, List<IGraphSegment> interpretedGSs, double closeAtTime, String time) {
		
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
					&& (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
				) {
				boolean hasDPullBack = false;
				Iterator<PullBackLevel> itr = lastIGS.pullBackLevels.iterator();
				while (itr.hasNext()) {
					PullBackLevel pbLevel = itr.next();
					if (pbLevel.identifier.equals("d")) {
						hasDPullBack = true;
					}
				}
				if (!hasDPullBack || time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
					currrentState = GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES;
				}
			}
			//
			if (currrentState != GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES
					&& lastIGS.identifier.equals("u") && lastIGS.pullBackLevels.isEmpty()
					&& lowestDPrice != 0 && lowestDPrice <= lowestUPrice
					&& (((lastIGS.endPrice - lastIGS.startPrice) / lastIGS.startPrice) * 100) <= (3.3 * ninetyPercentileBarChange)
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				IGraphSegment secondLastIGS = interpretedGSs.get(segmentsSize - 2);
				if (secondLastIGS.identifier.equals("d")
						&& (((secondLastIGS.startPrice - secondLastIGS.endPrice) / secondLastIGS.startPrice) * 100) <= (2.5 * ninetyPercentileBarChange)) {
					currrentState = GSInterpretationType.PULLBACK_LONG_IN_EARLY_STAGES;
				}
			}
			//
			if (currrentState != GSInterpretationType.STRONG_DIRECTIONAL_LONG_IN_EARLY_STAGES
					&& currrentState != GSInterpretationType.PULLBACK_LONG_IN_EARLY_STAGES
					&& time.compareTo(VolumeGraphPatternEntry.midMidTime) <= 0) {
				currrentState = GSInterpretationType.UNKNOWN_LONG_IN_EARLY_STAGES;
			}
		} else {
			currrentState = GSInterpretationType.NON_DIRECTIONAL;
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
