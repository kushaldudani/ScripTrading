package playground;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import ScripTrading.LoggerUtil;
import ScripTrading.MinuteData;

public class OptionDownloader implements Runnable {

	public static void main(String[] args) {
		OptionDownloader od = new OptionDownloader(new LinkedHashMap<>(), "12:55", 328570);
		
		od.run();
	}
	
	
	
	private Map<String, MinuteData> callMap;
	private String time;
	private int strike;
	
	public OptionDownloader(Map<String, MinuteData> callMap, String time, int strike /* should be multiplied by 1000 */) {
		this.callMap = callMap;
		this.time = time;
		this.strike = strike;
	}
	
	@Override
	public void run() {
		try{
		Downloader downloader = new Downloader();
		//int increment = 1000;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		String nowDate = sdf.format(calendar.getTime());
		
		//int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		//calendar.add(Calendar.DATE, (6 - dayOfWeek));
	    //String fridayDate = sdf.format(calendar.getTime());
	    String expiryDate = nowDate.substring(2, 4) + nowDate.substring(5, 7) + nowDate.substring(8, 10);
		String sym = "O:QQQ"+expiryDate;
		//int strike = closePrice - (int) (closePrice % increment);

		
		Map<String, MinuteData> callDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"C00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		//strike = strike + increment;
		//Map<String, MinuteData> putDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"P00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		
		/*if (!callDatamap.containsKey(time)) {
			calendar.add(Calendar.DATE, -1);
		    String thursDate = sdf.format(calendar.getTime());
		    thursDate = thursDate.substring(2, 4) + thursDate.substring(5, 7) + thursDate.substring(8, 10);
			sym = "O:MSFT"+thursDate;
			strike = closePrice - (int) (closePrice % increment);

			callDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"C00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			strike = strike + increment;
			putDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"P00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		}*/
		if (callDatamap.containsKey(time)) {
			callMap.put(time, callDatamap.get(time));
		}
		//double totalPremium = callDatamap.get(time).getClosePrice() + putDatamap.get(time).getClosePrice();
		//System.out.println(totalPremium);
		//premiumMap.put(time, totalPremium);
		
		} catch(Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
	}

}
