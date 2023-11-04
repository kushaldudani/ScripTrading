package ScripTrading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		} finally{
			 try {
				 bw.close();
				out.close();
			} catch (Exception e) {}
		}		
	}
	
	public synchronized void writeTradeData(String currentDate, double strike, int qty) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File("/home/kushaldudani/qqq/metadata.txt"),false));
			bw =  new BufferedWriter(out);
			bw.write(currentDate + "  " + strike + "  " + qty);
			bw.write("\n");
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		} finally{
			 try {
				 bw.close();
				out.close();
			} catch (Exception e) {}
		}		
	}
	
	public synchronized void writeTradeConfirmation(String currentDate, boolean hasOrderFilled) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File("/home/kushaldudani/qqq/tradeconfirmation.txt"),false));
			bw =  new BufferedWriter(out);
			bw.write(currentDate + "  " + hasOrderFilled);
			bw.write("\n");
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		} finally{
			 try {
				 bw.close();
				out.close();
			} catch (Exception e) {}
		}		
	}
	
	public synchronized void writeVolatilityData(String currentDate, double avgVolatility) {
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try{
			out = new OutputStreamWriter(new FileOutputStream(new 
					File("/home/kushaldudani/qqq/volatilitydata.txt"),false));
			bw =  new BufferedWriter(out);
			bw.write(currentDate + "  " + avgVolatility);
			bw.write("\n");
		} catch (Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		} finally{
			 try {
				 bw.close();
				out.close();
			} catch (Exception e) {}
		}		
	}
	
	public synchronized double readVolatilityData(){
		InputStreamReader is = null;
		BufferedReader br = null;
		double avgVolatility = 0;
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File("/home/kushaldudani/qqq/volatilitydata.txt")));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				if (linsVals.length == 2) {
					avgVolatility = Double.parseDouble(linsVals[1]);
				}
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
		return avgVolatility;
	}
	
	public synchronized TradeConfirmation readTradeConfirmation(String currentDate){
		InputStreamReader is = null;
		BufferedReader br = null;
		TradeConfirmation tc = new TradeConfirmation();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File("/home/kushaldudani/qqq/tradeconfirmation.txt")));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				if (linsVals.length == 2 && currentDate.equals(linsVals[0])) {
					tc.setDate(linsVals[0]);
					tc.setHasOrderFilled(Boolean.parseBoolean(linsVals[1]));
				}
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
		return tc;
	}
	
	public synchronized TradeData readTradeData(String currentDate){
		InputStreamReader is = null;
		BufferedReader br = null;
		TradeData tradeData = new TradeData();
		try {
			is = new InputStreamReader(new FileInputStream(new 
					File("/home/kushaldudani/qqq/metadata.txt")));
			br =  new BufferedReader(is);
			String line; 
			while ((line = br.readLine()) != null) {
				String[] linsVals = line.split("  ");
				if (linsVals.length == 3 && currentDate.equals(linsVals[0])) {
					tradeData.setDate(linsVals[0]);
					tradeData.setStrike(Double.parseDouble(linsVals[1]));
					tradeData.setQty(Integer.parseInt(linsVals[2]));
					//tradeData.setHasOrderFilled(Boolean.parseBoolean(linsVals[3]));
				}
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
			LoggerUtil.getLogger().info(e.getMessage());
			//System.exit(1);
		}finally{
			 try {
				br.close();
				is.close();
				
			} catch (Exception e) {}
		}
		return trade;
	}

}
