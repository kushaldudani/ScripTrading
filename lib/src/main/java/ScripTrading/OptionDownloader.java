package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class OptionDownloader implements Runnable {

	public static void main(String[] args) {
		OptionDownloader od = new OptionDownloader(new LinkedHashMap<>(), "12:55", 328570);
		
		od.run();
	}
	
	
	
	private Map<String, Double> premiumMap;
	private String time;
	private int closePrice;
	
	public OptionDownloader(Map<String, Double> premiumMap, String time, int closePrice) {
		this.premiumMap = premiumMap;
		this.time = time;
		this.closePrice = closePrice;
	}
	
	@Override
	public void run() {
		try{
		Downloader downloader = new Downloader();
		int increment = 2500;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		String nowDate = sdf.format(calendar.getTime());
		
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		calendar.add(Calendar.DATE, (6 - dayOfWeek));
	    String fridayDate = sdf.format(calendar.getTime());
	    fridayDate = fridayDate.substring(2, 4) + fridayDate.substring(5, 7) + fridayDate.substring(8, 10);
		String sym = "O:MSFT"+fridayDate;
		int strike = closePrice - (int) (closePrice % increment);

		
		Map<String, MinuteData> callDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"C00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		strike = strike + increment;
		Map<String, MinuteData> putDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"P00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		
		if (!callDatamap.containsKey(time)) {
			calendar.add(Calendar.DATE, -1);
		    String thursDate = sdf.format(calendar.getTime());
		    thursDate = thursDate.substring(2, 4) + thursDate.substring(5, 7) + thursDate.substring(8, 10);
			sym = "O:MSFT"+thursDate;
			strike = closePrice - (int) (closePrice % increment);

			callDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"C00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
			strike = strike + increment;
			putDatamap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/"+sym+"P00"+strike+"/range/5/minute/"+nowDate+"/"+nowDate+"?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
		}
		
		double totalPremium = callDatamap.get(time).getClosePrice() + putDatamap.get(time).getClosePrice();
		//System.out.println(totalPremium);
		premiumMap.put(time, totalPremium);
		
		} catch(Exception e) {
			LoggerUtil.getLogger().info(e.getMessage());
		}
	}

}
