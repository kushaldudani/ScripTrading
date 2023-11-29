package playground;

import java.util.List;
import java.util.Map;

import ScripTrading.GraphSegment;
import ScripTrading.Util;

public class GSUtil2 {

	/*
	 * private static int diffTime(String time1, String time2) { int time1H =
	 * (time1.charAt(0) == '0') ? Integer.parseInt(time1.substring(1, 2)) :
	 * Integer.parseInt(time1.substring(0, 2)); int time2H = (time2.charAt(0) ==
	 * '0') ? Integer.parseInt(time2.substring(1, 2)) :
	 * Integer.parseInt(time2.substring(0, 2));
	 * 
	 * int time1M = Integer.parseInt(time1.substring(3, 5)); int time2M =
	 * Integer.parseInt(time2.substring(3, 5));
	 * 
	 * return (time2H - time1H) * 60 + (time2M - time1M); }
	 * 
	 * public static void calculateGraphSegments(List<GraphSegment> graphArray,
	 * double fiveMinVol, double fiveMinOpen, double fiveMinClose, double
	 * fiveMinHigh, double fiveMinLow, String time, double
	 * ninetyPercentileBarChange, Map<String, Double> priceWithTime) { //time =
	 * timeNMinsAgo(time, 5); priceWithTime.put(time, fiveMinClose); String
	 * time5MinsAgo = Util.timeNMinsAgo(time, 5); double fiveMinPrevClose =
	 * (priceWithTime.containsKey(time5MinsAgo)) ? priceWithTime.get(time5MinsAgo) :
	 * fiveMinOpen; if (fiveMinClose >= fiveMinPrevClose) { if (graphArray.size() ==
	 * 0) { graphArray.add(new GraphSegment("c", "06:30", time, fiveMinPrevClose,
	 * fiveMinClose, fiveMinClose, 0));
	 * 
	 * } GraphSegment prevGS = graphArray.get(graphArray.size() - 1); if
	 * (prevGS.identifier.equals("c")) { //double avgSegmentPrice =
	 * (prevGS.startPrice + prevGS.endPrice) / 2; int timeCntr = 5; String
	 * timeAtTCntr = Util.timeNMinsAgo(time, timeCntr); prevGS.endPrice =
	 * (priceWithTime.containsKey(timeAtTCntr)) ? priceWithTime.get(timeAtTCntr) :
	 * prevGS.endPrice; prevGS.endTime = (priceWithTime.containsKey(timeAtTCntr)) ?
	 * timeAtTCntr : time; while (timeAtTCntr.compareTo(prevGS.startTime) > 0) {
	 * timeCntr = timeCntr + 5; timeAtTCntr = Util.timeNMinsAgo(time, timeCntr); if
	 * (priceWithTime.containsKey(timeAtTCntr) && priceWithTime.get(timeAtTCntr) <
	 * prevGS.endPrice) { prevGS.endPrice = priceWithTime.get(timeAtTCntr);
	 * prevGS.endTime = timeAtTCntr; } } if (((fiveMinClose - prevGS.endPrice) /
	 * prevGS.endPrice) * 100 > ninetyPercentileBarChange) { int barcount4NewSegment
	 * = diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("u", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { //prevGS.startPrice = Math.min(fiveMinClose, prevGS.startPrice);
	 * //prevGS.priceWithTime.put(time, fiveMinClose);
	 * //if(prevGS.priceWithTime.size() >= 6) { //if (Math.abs(((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100) < (ninetyPercentileBarChange /
	 * 10)) { //String time10MinsAgo = timeNMinsAgo(time, 25); //double
	 * fiveMinClose10MinsAgo = prevGS.priceWithTime.remove(time10MinsAgo); //
	 * prevGS.endPrice = fiveMinClose; //fiveMinClose10MinsAgo; // prevGS.endTime =
	 * time ;//time10MinsAgo; //} prevGS.currentPrice = fiveMinClose;
	 * prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("u")) { if (diffTime(prevGS.endTime,
	 * time) >= 30) { int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("c", prevGS.endTime, time, prevGS.endPrice,
	 * fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 >
	 * (ninetyPercentileBarChange / 10)) { //if (fiveMinClose > prevGS.endPrice) {
	 * prevGS.endTime = time; prevGS.endPrice = fiveMinClose; } prevGS.currentPrice
	 * = fiveMinClose; prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("ur")) { if (((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100 > (ninetyPercentileBarChange / 10))
	 * { //if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 >
	 * ninetyPercentileBarChange) { prevGS.identifier = "u"; prevGS.endTime = time;
	 * prevGS.endPrice = fiveMinClose; prevGS.currentPrice = fiveMinClose;
	 * prevGS.barCount = prevGS.barCount + 1; //int barcount4NewSegment =
	 * diffTime(prevGS.pullbackTime, time) / 5; //prevGS.barCount = prevGS.barCount
	 * - barcount4NewSegment + 1; //graphArray.add(new GraphSegment("u",
	 * prevGS.pullbackTime, time, prevGS.pullbackPrice, fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 20) { int
	 * barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time,
	 * prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else { prevGS.currentPrice = fiveMinClose; prevGS.barCount =
	 * prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("d")) { if (((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100 >
	 * Math.max(ninetyPercentileBarChange, Math.abs(((prevGS.endPrice -
	 * prevGS.startPrice) / prevGS.startPrice) * 100) * 0.5)) { //prevGS.identifier
	 * = "d"; int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("u", prevGS.endTime, time, prevGS.endPrice,
	 * fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 >
	 * Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) *
	 * 0.33) { prevGS.currentPrice = fiveMinClose;
	 * prevGS.pullbackPrice.add(fiveMinClose); prevGS.identifier = "dr";
	 * prevGS.pullbackTime.add(time); prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } else if (diffTime(prevGS.endTime, time) >= 30) { int barcount4NewSegment =
	 * diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("c", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { prevGS.currentPrice = fiveMinClose; prevGS.barCount =
	 * prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("dr")) { if (((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100 >
	 * Math.max(ninetyPercentileBarChange, Math.abs(((prevGS.endPrice -
	 * prevGS.startPrice) / prevGS.startPrice) * 100) * 0.5)) { prevGS.identifier =
	 * "d"; int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("u", prevGS.endTime, time, prevGS.endPrice,
	 * fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 20) { int
	 * barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time,
	 * prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else { if (((fiveMinClose - prevGS.pullbackPrice.peekLast()) /
	 * prevGS.pullbackPrice.peekLast()) * 100 > (ninetyPercentileBarChange / 10)) {
	 * //if (fiveMinClose > prevGS.pullbackPrice.peekLast()) {
	 * prevGS.pullbackTime.removeLast(); prevGS.pullbackTime.add(time);
	 * prevGS.pullbackPrice.removeLast(); prevGS.pullbackPrice.add(fiveMinClose); }
	 * prevGS.currentPrice = fiveMinClose; prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } } else if (fiveMinClose < fiveMinPrevClose) { if (graphArray.size() == 0)
	 * { graphArray.add(new GraphSegment("c", "06:30", time, fiveMinPrevClose,
	 * fiveMinClose, fiveMinClose, 0));
	 * 
	 * } GraphSegment prevGS = graphArray.get(graphArray.size() - 1); if
	 * (prevGS.identifier.equals("c")) { //double avgSegmentPrice =
	 * (prevGS.startPrice + prevGS.endPrice) / 2; int timeCntr = 5; String
	 * timeAtTCntr = Util.timeNMinsAgo(time, timeCntr); prevGS.endPrice =
	 * (priceWithTime.containsKey(timeAtTCntr)) ? priceWithTime.get(timeAtTCntr) :
	 * prevGS.endPrice; prevGS.endTime = (priceWithTime.containsKey(timeAtTCntr)) ?
	 * timeAtTCntr : time; while (timeAtTCntr.compareTo(prevGS.startTime) > 0) {
	 * timeCntr = timeCntr + 5; timeAtTCntr = Util.timeNMinsAgo(time, timeCntr); if
	 * (priceWithTime.containsKey(timeAtTCntr) && priceWithTime.get(timeAtTCntr) >
	 * prevGS.endPrice) { prevGS.endPrice = priceWithTime.get(timeAtTCntr);
	 * prevGS.endTime = timeAtTCntr; } } if (((prevGS.endPrice - fiveMinClose) /
	 * prevGS.endPrice) * 100 > ninetyPercentileBarChange) { int barcount4NewSegment
	 * = diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("d", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { //prevGS.startPrice = Math.min(fiveMinClose, prevGS.startPrice);
	 * //prevGS.priceWithTime.put(time, fiveMinClose);
	 * //if(prevGS.priceWithTime.size() >= 6) { //if (Math.abs(((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100) < (ninetyPercentileBarChange /
	 * 10)) { //String time10MinsAgo = timeNMinsAgo(time, 25); //double
	 * fiveMinClose10MinsAgo = prevGS.priceWithTime.remove(time10MinsAgo); //
	 * prevGS.endPrice = fiveMinClose; //fiveMinClose10MinsAgo; // prevGS.endTime =
	 * time; //time10MinsAgo; //} prevGS.currentPrice = fiveMinClose;
	 * prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("d")) { if (diffTime(prevGS.endTime,
	 * time) >= 30) { // Math.abs(((fiveMinClose - prevGS.endPrice) /
	 * prevGS.endPrice) * 100) < ninetyPercentileBarChange && int
	 * barcount4NewSegment = diffTime(prevGS.endTime, time) / 5; prevGS.barCount =
	 * prevGS.barCount - barcount4NewSegment + 1; graphArray.add(new
	 * GraphSegment("c", prevGS.endTime, time, prevGS.endPrice, fiveMinClose,
	 * fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 < (-1
	 * * ninetyPercentileBarChange / 10)) { //if (fiveMinClose < prevGS.endPrice) {
	 * prevGS.endTime = time; prevGS.endPrice = fiveMinClose; } prevGS.currentPrice
	 * = fiveMinClose; prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("dr")) { if (((fiveMinClose -
	 * prevGS.endPrice) / prevGS.endPrice) * 100 < (-1 * ninetyPercentileBarChange /
	 * 10)) { //if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 >
	 * ninetyPercentileBarChange) { prevGS.identifier = "d"; prevGS.endTime = time;
	 * prevGS.endPrice = fiveMinClose; prevGS.currentPrice = fiveMinClose;
	 * prevGS.barCount = prevGS.barCount + 1; //int barcount4NewSegment =
	 * diffTime(prevGS.pullbackTime, time) / 5; //prevGS.barCount = prevGS.barCount
	 * - barcount4NewSegment + 1; //graphArray.add(new GraphSegment("d",
	 * prevGS.pullbackTime, time, prevGS.pullbackPrice, fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 20) { int
	 * barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time,
	 * prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else { prevGS.currentPrice = fiveMinClose; prevGS.barCount =
	 * prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("u")) { if (((prevGS.endPrice -
	 * fiveMinClose) / prevGS.endPrice) * 100 > Math.max(ninetyPercentileBarChange,
	 * Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) *
	 * 0.5)) { //prevGS.identifier = "u"; int barcount4NewSegment =
	 * diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("d", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 >
	 * Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) *
	 * 0.33) { prevGS.currentPrice = fiveMinClose;
	 * prevGS.pullbackPrice.add(fiveMinClose); prevGS.identifier = "ur";
	 * prevGS.pullbackTime.add(time); prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } else if (diffTime(prevGS.endTime, time) >= 30) { int barcount4NewSegment =
	 * diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("c", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else { prevGS.currentPrice = fiveMinClose; prevGS.barCount =
	 * prevGS.barCount + 1;
	 * 
	 * } } else if (prevGS.identifier.equals("ur")) { if (((prevGS.endPrice -
	 * fiveMinClose) / prevGS.endPrice) * 100 > Math.max(ninetyPercentileBarChange,
	 * Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) *
	 * 0.5)) { prevGS.identifier = "u"; int barcount4NewSegment =
	 * diffTime(prevGS.endTime, time) / 5; prevGS.barCount = prevGS.barCount -
	 * barcount4NewSegment + 1; graphArray.add(new GraphSegment("d", prevGS.endTime,
	 * time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
	 * 
	 * } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 20) { int
	 * barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
	 * prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
	 * graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time,
	 * prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose,
	 * barcount4NewSegment));
	 * 
	 * } else { if (((fiveMinClose - prevGS.pullbackPrice.peekLast()) /
	 * prevGS.pullbackPrice.peekLast()) * 100 < (-1 * ninetyPercentileBarChange /
	 * 10)) { //if (fiveMinClose < prevGS.pullbackPrice.peekLast()) {
	 * prevGS.pullbackTime.removeLast(); prevGS.pullbackTime.add(time);
	 * prevGS.pullbackPrice.removeLast(); prevGS.pullbackPrice.add(fiveMinClose); }
	 * prevGS.currentPrice = fiveMinClose; prevGS.barCount = prevGS.barCount + 1;
	 * 
	 * } } }
	 * 
	 * }
	 */
}
