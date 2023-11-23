package playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import ScripTrading.LoggerUtil;
//import ScripTrading.MinuteData;
//import ScripTrading.Util;

public class UtilRun {
	
	public static void main(String[] args) {
		Map<String, String> masterLongResults = readResults("config/QQQMasterLongResult.txt");
		Map<String, String> masterShortResults = readResults("config/QQQMasterShortResult.txt");
		
		for (String key : masterLongResults.keySet()) {
			if (masterShortResults.containsKey(key)) {
				System.out.println(key);
			}
		}
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
	
	public static Map<String, Map<String, Double>> readVolumeData(String path){
		InputStreamReader is = null;
		BufferedReader br = null;
		Map<String, Map<String, Double>> results = new LinkedHashMap<>();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File(path)));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				
			    if (!results.containsKey(linsVals[0])) {
			    	Map<String, Double> minuteDataMap = new LinkedHashMap<>();
			    	results.put(linsVals[0], minuteDataMap);
			    }
			    
				Map<String, Double> minuteDataMap = results.get(linsVals[0]);
				minuteDataMap.put(linsVals[1], Double.parseDouble(linsVals[2]));
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
