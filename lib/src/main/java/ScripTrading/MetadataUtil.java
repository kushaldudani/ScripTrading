package ScripTrading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MetadataUtil {
	
	private static MetadataUtil singleInstance = null;
	
	public static synchronized MetadataUtil getInstance() {
        if (singleInstance == null) {
        	singleInstance = new MetadataUtil();
        }
  
        return singleInstance;
    }
	
	public synchronized void write(String path, String currentDate, String value, String orderId) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File(path),false));
			bw =  new BufferedWriter(out);
			bw.write(currentDate + "  " + value + "  " + orderId);
			bw.write("\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			 try {
				 bw.close();
				out.close();
			} catch (IOException e) {}
		}		
	}
	
	public synchronized TradeData read(String currentDate){
		InputStreamReader is = null;
		BufferedReader br = null;
		TradeData tradeData = new TradeData();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File("/Users/kushd/qqq/metadata.txt")));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				if (linsVals.length == 3 && currentDate.equals(linsVals[0])) {
					tradeData.setDate(linsVals[0]);
					tradeData.setStrike(Double.parseDouble(linsVals[1]));
					//tradeData.setBuyBackPremium(Double.parseDouble(linsVals[2]));
					tradeData.setQty(Integer.parseInt(linsVals[2]));
					//tradeData.setEnterPrice(Double.parseDouble(linsVals[4]));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			//System.exit(1);
		}finally{
			 try {
				br.close();
				is.close();
				
			} catch (IOException e) {}
		}
		return tradeData;
	}
	
	public synchronized Trade readTrade(String currentDate, String path){
		InputStreamReader is = null;
		BufferedReader br = null;
		Trade trade = new Trade();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File(path)));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				if (linsVals.length == 3 && currentDate.equals(linsVals[0])) {
					trade.setExecutionInfo(linsVals[1]);
					trade.setOrderid(linsVals[2]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			//System.exit(1);
		}finally{
			 try {
				br.close();
				is.close();
				
			} catch (IOException e) {}
		}
		return trade;
	}

}
