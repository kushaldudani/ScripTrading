package ScripTrading;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class LoggerUtil {
	
private static Logger logger;
	
	public static synchronized Logger getLogger(){
		if(logger == null){
			logger = Logger.getLogger(LoggerUtil.class.getSimpleName());
			Handler fileHandler = null;
			SimpleFormatter simpleFormatter = null;
			try{
				fileHandler = new FileHandler("/Users/kushd/msft/"+getLocation());
				simpleFormatter = new SimpleFormatter();
				logger.addHandler(fileHandler);
				fileHandler.setFormatter(simpleFormatter);
				fileHandler.setLevel(Level.ALL);
				logger.setLevel(Level.ALL);
			}catch(IOException exception){
				logger.log(Level.SEVERE, "Error occur in FileHandler.", exception);
				System.exit(2);
			}
		}
		
		return logger;
	}
	
	private static String getLocation(){
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		month = month + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		//int sec = cal.get(Calendar.SECOND);
		//int minute = cal.get(Calendar.MINUTE);
		//int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		String location = year+"/"+month+"/"+day;//+"-"+hour+"-"+minute+"-"+sec+"-nettyclient";
		return location;
	}

}