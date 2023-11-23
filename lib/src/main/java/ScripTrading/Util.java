package ScripTrading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Util {
	
	public static String timeNMinsAgo(String time, int N) {
    	int timeH = (time.charAt(0) == '0') ? Integer.parseInt(time.substring(1, 2)) : Integer.parseInt(time.substring(0, 2));
    	
    	int timeM = (time.charAt(3) == '0') ? Integer.parseInt(time.substring(4, 5)) : Integer.parseInt(time.substring(3, 5));
    	
    	int totalminutes = timeH * 60 + timeM;
    	totalminutes = totalminutes - N;
    	
    	int updatedH = totalminutes / 60;
    	int updatedM = totalminutes - updatedH * 60;
    	
    	String stringH = (updatedH < 10) ? "0" + updatedH : "" + updatedH;
    	String stringM = (updatedM < 10) ? "0" + updatedM : "" + updatedM;
    	
    	return stringH + ":" + stringM;
    }
	
	public static int diffTime(String time1, String time2) {
    	int time1H = (time1.charAt(0) == '0') ? Integer.parseInt(time1.substring(1, 2)) : Integer.parseInt(time1.substring(0, 2));
    	int time2H = (time2.charAt(0) == '0') ? Integer.parseInt(time2.substring(1, 2)) : Integer.parseInt(time2.substring(0, 2));
    	
    	int time1M = Integer.parseInt(time1.substring(3, 5));
    	int time2M = Integer.parseInt(time2.substring(3, 5));
    	
    	return (time2H - time1H) * 60 +  (time2M - time1M);
    }
	
	public static String findNearestFiveMinute(String time) {
    	int timeH = (time.charAt(0) == '0') ? Integer.parseInt(time.substring(1, 2)) : Integer.parseInt(time.substring(0, 2));
    	
    	int timeM = Integer.parseInt(time.substring(3, 5));
    	
    	int minutes = timeH * 60 + timeM;
    	
    	int nearestFiveMinute = minutes - (minutes % 5);
    	
    	int nearestFiveMinuteH = nearestFiveMinute / 60;
    	int nearestFiveMinuteM = nearestFiveMinute - (nearestFiveMinuteH * 60);
    	
    	String stringH = (nearestFiveMinuteH < 10) ? "0" + nearestFiveMinuteH : "" + nearestFiveMinuteH;
    	String stringM = (nearestFiveMinuteM < 10) ? "0" + nearestFiveMinuteM : "" + nearestFiveMinuteM;
    	
    	return stringH + ":" + stringM;
    }
	
	public static int diffTimeWithSec(String time1, String time2) {
    	int time1H = (time1.charAt(0) == '0') ? Integer.parseInt(time1.substring(1, 2)) : Integer.parseInt(time1.substring(0, 2));
    	int time2H = (time2.charAt(0) == '0') ? Integer.parseInt(time2.substring(1, 2)) : Integer.parseInt(time2.substring(0, 2));
    	
    	int time1M = Integer.parseInt(time1.substring(3, 5));
    	int time2M = Integer.parseInt(time2.substring(3, 5));
    	
    	int time1S = Integer.parseInt(time1.substring(6, 8));
    	int time2S = Integer.parseInt(time2.substring(6, 8));
    	
    	return (time2H - time1H) * 60 * 60 +  (time2M - time1M) * 60 + (time2S - time1S);
    }
	
	public static void reauthIfNeeded(Map<String, String> tickleMap, String time) {
		String sessionId = findSessionInLastNMins(tickleMap, 10, time);
		if (sessionId != null && !sessionId.equals("")) {
			return;
		} else {
			new ReauthenticateUtil().reauth();
		}
	}
	 
	public static String findSessionInLastNMins(Map<String, String> tickleMap, int nCounter, String time) {
		String sessionId = "";
		if(tickleMap == null) {
			return sessionId;
		}
		while (nCounter > 0) {
			sessionId = tickleMap.get(time);
			if (sessionId != null && !sessionId.equals("")) {
				break;
			}
			time = timeNMinsAgo(time, 1);
			
			nCounter--;
		}
		
		return sessionId;
	}
	
	public static Map<String, DayData> deserializeHashMap(String filename) {
		HashMap<String, DayData> cache = null;
		  
        try {
            FileInputStream fileInput = new FileInputStream(
            		filename);
  
            ObjectInputStream objectInput
                = new ObjectInputStream(fileInput);
  
            cache = (HashMap)objectInput.readObject();
  
            objectInput.close();
            fileInput.close();
        }
  
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return cache;
	}
	
	public static Map<String, Double> deserializeDoubleHashMap(String filename) {
		HashMap<String, Double> cache = null;
		  
        try {
            FileInputStream fileInput = new FileInputStream(
            		filename);
  
            ObjectInputStream objectInput
                = new ObjectInputStream(fileInput);
  
            cache = (HashMap)objectInput.readObject();
  
            objectInput.close();
            fileInput.close();
        }
  
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return cache;
	}
	
	public static Map<String, Map<String, MinuteData>> readVixRawData(String path){
		InputStreamReader is = null;
		BufferedReader br = null;
		Map<String, Map<String, MinuteData>> results = new LinkedHashMap<>();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File(path)));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split(",");
				String date = linsVals[0].split(" ")[0];
				String time = linsVals[0].split(" ")[1].substring(0, 5);
				time = Util.timeNMinsAgo(time, 180);
				
				MinuteData mData = new MinuteData();
				mData.setClosePrice(Double.parseDouble(linsVals[4]));
				
			    if (!results.containsKey(date)) {
			    	Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
			    	results.put(date, minuteDataMap);
			    }
			    
				Map<String, MinuteData> minuteDataMap = results.get(date);
				minuteDataMap.put(time, mData);
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
	
	public static void serializeHashMap(Map<String, DayData> cache, String filename) {
		try {
            FileOutputStream myFileOutStream
                = new FileOutputStream(
                		filename);
  
            ObjectOutputStream myObjectOutStream
                = new ObjectOutputStream(myFileOutStream);
  
            myObjectOutStream.writeObject(cache);
  
            // closing FileOutputStream and
            // ObjectOutputStream
            myObjectOutStream.close();
            myFileOutStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static void serializeDoubleHashMap(Map<String, Double> cache, String filename) {
		try {
            FileOutputStream myFileOutStream
                = new FileOutputStream(
                		filename);
  
            ObjectOutputStream myObjectOutStream
                = new ObjectOutputStream(myFileOutStream);
  
            myObjectOutStream.writeObject(cache);
  
            // closing FileOutputStream and
            // ObjectOutputStream
            myObjectOutStream.close();
            myFileOutStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static void serializeVixRawData(Map<String, Map<String, MinuteData>> vixRawData, String filename) {
		try {
            FileOutputStream myFileOutStream
                = new FileOutputStream(
                		filename);
  
            ObjectOutputStream myObjectOutStream
                = new ObjectOutputStream(myFileOutStream);
  
            myObjectOutStream.writeObject(vixRawData);
  
            // closing FileOutputStream and
            // ObjectOutputStream
            myObjectOutStream.close();
            myFileOutStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static List<String> getExpiryDates() {
		List<String> expiryDates = new ArrayList<>();
		
		expiryDates.add("2021-01-08");
		expiryDates.add("2021-01-15");
		expiryDates.add("2021-01-22");
		expiryDates.add("2021-01-29");
		expiryDates.add("2021-02-05");
		expiryDates.add("2021-02-12");
		expiryDates.add("2021-02-19");
		expiryDates.add("2021-02-26");
		expiryDates.add("2021-03-05");
		expiryDates.add("2021-03-12");
		expiryDates.add("2021-03-19");
		expiryDates.add("2021-03-26");
		expiryDates.add("2021-04-01");
		expiryDates.add("2021-04-09");
		expiryDates.add("2021-04-16");
		expiryDates.add("2021-04-23");
		expiryDates.add("2021-04-30");
		expiryDates.add("2021-05-07");
		expiryDates.add("2021-05-14");
		expiryDates.add("2021-05-21");
		expiryDates.add("2021-05-28");
		expiryDates.add("2021-06-04");
		expiryDates.add("2021-06-11");
		expiryDates.add("2021-06-18");
		expiryDates.add("2021-06-25");
		expiryDates.add("2021-07-02");
		expiryDates.add("2021-07-09");
		expiryDates.add("2021-07-16");
		expiryDates.add("2021-07-23");
		expiryDates.add("2021-07-30");
		expiryDates.add("2021-08-06");
		expiryDates.add("2021-08-13");
		expiryDates.add("2021-08-20");
		expiryDates.add("2021-08-27");
		expiryDates.add("2021-09-03");
		expiryDates.add("2021-09-10");
		expiryDates.add("2021-09-17");
		expiryDates.add("2021-09-24");
		expiryDates.add("2021-10-01");
		expiryDates.add("2021-10-08");
		expiryDates.add("2021-10-15");
		expiryDates.add("2021-10-22");
		expiryDates.add("2021-10-29");
		expiryDates.add("2021-11-05");
		expiryDates.add("2021-11-12");
		expiryDates.add("2021-11-19");
		expiryDates.add("2021-11-26");
		expiryDates.add("2021-12-03");
		expiryDates.add("2021-12-10");
		expiryDates.add("2021-12-17");
		expiryDates.add("2021-12-23");
		expiryDates.add("2021-12-31");
		
		
		expiryDates.add("2022-01-07");
		expiryDates.add("2022-01-14");
		expiryDates.add("2022-01-21");
		expiryDates.add("2022-01-28");
		expiryDates.add("2022-02-04");
		expiryDates.add("2022-02-11");
		expiryDates.add("2022-02-18");
		expiryDates.add("2022-02-25");
		expiryDates.add("2022-03-04");
		expiryDates.add("2022-03-11");
		expiryDates.add("2022-03-18");
		expiryDates.add("2022-03-25");
		expiryDates.add("2022-04-01");
		expiryDates.add("2022-04-08");
		expiryDates.add("2022-04-14");
		expiryDates.add("2022-04-22");
		expiryDates.add("2022-04-29");
		expiryDates.add("2022-05-06");
		expiryDates.add("2022-05-13");
		expiryDates.add("2022-05-20");
		expiryDates.add("2022-05-27");
		expiryDates.add("2022-06-03");
		expiryDates.add("2022-06-10");
		expiryDates.add("2022-06-17");
		expiryDates.add("2022-06-24");
		expiryDates.add("2022-07-01");
		expiryDates.add("2022-07-08");
		expiryDates.add("2022-07-15");
		expiryDates.add("2022-07-22");
		expiryDates.add("2022-07-29");
		expiryDates.add("2022-08-05");
		expiryDates.add("2022-08-12");
		expiryDates.add("2022-08-19");
		expiryDates.add("2022-08-26");
		expiryDates.add("2022-09-02");
		expiryDates.add("2022-09-09");
		expiryDates.add("2022-09-16");
		expiryDates.add("2022-09-23");
		expiryDates.add("2022-09-30");
		expiryDates.add("2022-10-07");
		expiryDates.add("2022-10-14");
		expiryDates.add("2022-10-21");
		expiryDates.add("2022-10-28");
		expiryDates.add("2022-11-04");
		expiryDates.add("2022-11-11");
		expiryDates.add("2022-11-18");
		expiryDates.add("2022-11-25");
		expiryDates.add("2022-12-02");
		expiryDates.add("2022-12-09");
		expiryDates.add("2022-12-16");
		expiryDates.add("2022-12-23");
		expiryDates.add("2022-12-30");
		
		expiryDates.add("2023-01-06");
		expiryDates.add("2023-01-13");
		expiryDates.add("2023-01-20");
		expiryDates.add("2023-01-27");
		expiryDates.add("2023-02-03");
		expiryDates.add("2023-02-10");
		expiryDates.add("2023-02-17");
		expiryDates.add("2023-02-24");
		expiryDates.add("2023-03-03");
		expiryDates.add("2023-03-10");
		expiryDates.add("2023-03-17");
		expiryDates.add("2023-03-24");
		expiryDates.add("2023-03-31");
		expiryDates.add("2023-04-06");
		expiryDates.add("2023-04-14");
		expiryDates.add("2023-04-21");
		expiryDates.add("2023-04-28");
		expiryDates.add("2023-05-05");
		expiryDates.add("2023-05-12");
		expiryDates.add("2023-05-19");
		expiryDates.add("2023-05-26");
		expiryDates.add("2023-06-02");
		expiryDates.add("2023-06-09");
		expiryDates.add("2023-06-16");
		expiryDates.add("2023-06-23");
		expiryDates.add("2023-06-30");
		expiryDates.add("2023-07-07");
		expiryDates.add("2023-07-14");
		expiryDates.add("2023-07-21");
		expiryDates.add("2023-07-28");
		expiryDates.add("2023-08-04");
		expiryDates.add("2023-08-11");
		expiryDates.add("2023-08-18");
		expiryDates.add("2023-08-25");
		expiryDates.add("2023-09-01");
		expiryDates.add("2023-09-08");
		expiryDates.add("2023-09-15");
		expiryDates.add("2023-09-22");
		expiryDates.add("2023-09-29");
		
		
		return expiryDates;
	}
	
	public static boolean goodTimeToLongBasedOffGraphSegments(List<GraphSegment> graphArray, double closeAtTime, double ninetyPercentileBarChange, double closeAt15MinsAgo) {
		int segmentsSize = graphArray.size();
		int cntr = segmentsSize - 1;
		if (!graphArray.get(cntr).identifier.equals("u")) {
			return false;
		}
		cntr--;
		while (cntr > 0) {
			GraphSegment graphSegment = graphArray.get(cntr);
			if ((graphSegment.identifier.equals("d") || graphSegment.identifier.equals("dr"))) {
				break;
			}
			cntr--;
		}
		
		if ((graphArray.get(cntr).identifier.equals("d") || graphArray.get(cntr).identifier.equals("dr"))) {
			double dSegment = graphArray.get(cntr).startPrice - graphArray.get(cntr).endPrice;
			double floorPrice = graphArray.get(cntr).endPrice;
			if (closeAtTime < (floorPrice + (0.3 * dSegment))) {
				return true;
			}
			
			if (closeAtTime > graphArray.get(cntr).startPrice && closeAt15MinsAgo < graphArray.get(cntr).startPrice) {
				return true;
			}
		} else {
			double floorPrice = graphArray.get(cntr).endPrice;
			return (closeAtTime < (floorPrice + (3 * ninetyPercentileBarChange * floorPrice / 100)));
		}
		
		return false;
	}

	static void calculateGraphSegments(List<GraphSegment> graphArray, double fiveMinVol,
			double fiveMinOpen, double fiveMinClose, double fiveMinHigh, double fiveMinLow, String time, double ninetyPercentileBarChange) {
		//time = timeNMinsAgo(time, 5);
		//priceWithTime.put(time, fiveMinClose);
    	if (fiveMinClose >= fiveMinOpen) {
          if (graphArray.size() == 0) {
              graphArray.add(new GraphSegment("c", "0", time, fiveMinOpen, fiveMinClose, fiveMinClose, 0));
              
          }
          GraphSegment prevGS = graphArray.get(graphArray.size() - 1);
          if (prevGS.identifier.equals("c")) {
              //double avgSegmentPrice = (prevGS.startPrice + prevGS.endPrice) / 2;
              if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 > ninetyPercentileBarChange) {
            	  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("u", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  //prevGS.startPrice = Math.min(fiveMinClose, prevGS.startPrice);
            	  //prevGS.priceWithTime.put(time, fiveMinClose);
            	  //if(prevGS.priceWithTime.size() >= 6) {
            	  if (Math.abs(((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100) < (ninetyPercentileBarChange / 10)) {
            		  //String time10MinsAgo = timeNMinsAgo(time, 25);
            		  //double fiveMinClose10MinsAgo = prevGS.priceWithTime.remove(time10MinsAgo);
            		  prevGS.endPrice = fiveMinClose; //fiveMinClose10MinsAgo;
            		  prevGS.endTime = time ;//time10MinsAgo;
            	  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("u")) {
              if (diffTime(prevGS.endTime, time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  if (fiveMinClose > prevGS.endPrice) {
                      prevGS.endTime = time;
                      prevGS.endPrice = fiveMinClose;
                  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("ur")) {
              if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 > ninetyPercentileBarChange) {
            	  prevGS.identifier = "u";
            	  if (fiveMinClose > prevGS.endPrice) {
                      prevGS.endTime = time;
                      prevGS.endPrice = fiveMinClose;
                  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
            	  //int barcount4NewSegment = diffTime(prevGS.pullbackTime, time) / 5;
            	  //prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  //graphArray.add(new GraphSegment("u", prevGS.pullbackTime, time, prevGS.pullbackPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time, prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("d")) {
              if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 > Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) * 0.3) {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.pullbackPrice.add(fiveMinClose);
                  prevGS.identifier = "dr";
                  prevGS.pullbackTime.add(time);
                  prevGS.barCount = prevGS.barCount + 1;
                  
              } else if (diffTime(prevGS.endTime, time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("dr")) {
              if (((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100 > Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) * 0.5) {
                  prevGS.identifier = "d";
                  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
                  graphArray.add(new GraphSegment("u", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time, prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  if (fiveMinClose > prevGS.pullbackPrice.peekLast()) {
                      prevGS.pullbackTime.removeLast(); prevGS.pullbackTime.add(time);
                      prevGS.pullbackPrice.removeLast(); prevGS.pullbackPrice.add(fiveMinClose);
                  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
    	} else if (fiveMinClose < fiveMinOpen) {
          if (graphArray.size() == 0) {
        	  graphArray.add(new GraphSegment("c", "0", time, fiveMinOpen, fiveMinClose, fiveMinClose, 0));
              
          }
          GraphSegment prevGS = graphArray.get(graphArray.size() - 1);
          if (prevGS.identifier.equals("c")) {
              //double avgSegmentPrice = (prevGS.startPrice + prevGS.endPrice) / 2;
              if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 > ninetyPercentileBarChange) {
            	  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("d", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
            	//prevGS.startPrice = Math.min(fiveMinClose, prevGS.startPrice);
            	  //prevGS.priceWithTime.put(time, fiveMinClose);
            	  //if(prevGS.priceWithTime.size() >= 6) {
            	  if (Math.abs(((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100) < (ninetyPercentileBarChange / 10)) {
            		  //String time10MinsAgo = timeNMinsAgo(time, 25);
            		  //double fiveMinClose10MinsAgo = prevGS.priceWithTime.remove(time10MinsAgo);
            		  prevGS.endPrice = fiveMinClose; //fiveMinClose10MinsAgo;
            		  prevGS.endTime = time; //time10MinsAgo;
            	  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("d")) {
        	  if (diffTime(prevGS.endTime, time) >= 30) { // Math.abs(((fiveMinClose - prevGS.endPrice) / prevGS.endPrice) * 100) < ninetyPercentileBarChange &&
        		  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
        		  graphArray.add(new GraphSegment("c", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  if (fiveMinClose < prevGS.endPrice) {
                      prevGS.endTime = time;
                      prevGS.endPrice = fiveMinClose;
                  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("dr")) {
              if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 > ninetyPercentileBarChange) {
            	  prevGS.identifier = "d";
            	  if (fiveMinClose < prevGS.endPrice) {
                      prevGS.endTime = time;
                      prevGS.endPrice = fiveMinClose;
                  }
                  prevGS.currentPrice = fiveMinClose;
            	  prevGS.barCount = prevGS.barCount + 1;
            	  //int barcount4NewSegment = diffTime(prevGS.pullbackTime, time) / 5;
            	  //prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  //graphArray.add(new GraphSegment("d", prevGS.pullbackTime, time, prevGS.pullbackPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time, prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("u")) {
              if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 > Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) * 0.3) {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.pullbackPrice.add(fiveMinClose);
                  prevGS.identifier = "ur";
                  prevGS.pullbackTime.add(time);
                  prevGS.barCount = prevGS.barCount + 1;
                  
              } else if (diffTime(prevGS.endTime, time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
          else if (prevGS.identifier.equals("ur")) {
              if (((prevGS.endPrice - fiveMinClose) / prevGS.endPrice) * 100 > Math.abs(((prevGS.endPrice - prevGS.startPrice) / prevGS.startPrice) * 100) * 0.5) {
                  prevGS.identifier = "u";
                  int barcount4NewSegment = diffTime(prevGS.endTime, time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
                  graphArray.add(new GraphSegment("d", prevGS.endTime, time, prevGS.endPrice, fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else if (diffTime(prevGS.pullbackTime.peekLast(), time) >= 30) {
            	  int barcount4NewSegment = diffTime(prevGS.pullbackTime.peekLast(), time) / 5;
            	  prevGS.barCount = prevGS.barCount - barcount4NewSegment + 1;
            	  graphArray.add(new GraphSegment("c", prevGS.pullbackTime.peekLast(), time, prevGS.pullbackPrice.peekLast(), fiveMinClose, fiveMinClose, barcount4NewSegment));
                  
              } else {
                  if (fiveMinClose < prevGS.pullbackPrice.peekLast()) {
                      prevGS.pullbackTime.removeLast(); prevGS.pullbackTime.add(time);
                      prevGS.pullbackPrice.removeLast(); prevGS.pullbackPrice.add(fiveMinClose);
                  }
                  prevGS.currentPrice = fiveMinClose;
                  prevGS.barCount = prevGS.barCount + 1;
                  
              }
          }
    	}
    	
    }

}
