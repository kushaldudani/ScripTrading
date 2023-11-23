package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MinuteDataManager implements Runnable {
	
	private CountDownLatch latch;
	private Map<String, MinuteData> minuteDataMap;
	
	public MinuteDataManager(CountDownLatch latch, Map<String, MinuteData> minuteDataMap) {
		this.latch = latch;
		this.minuteDataMap = minuteDataMap;
	}

	@Override
	public void run() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		SimpleDateFormat secondsFormatter = new SimpleDateFormat("HH:mm:ss");
		String time = shorttimeFormatter.format(currentDate);
		
		while (time.compareTo("13:01") <= 0) {
			try {
				int timeToSleep = 3000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				time = shorttimeFormatter.format(currentDate);
				int seconds = Integer.parseInt(secondsFormatter.format(currentDate).substring(6, 8));
				
				if (seconds >= 5) {
					String fillTimeEnd = Util.findNearestFiveMinute(time);
					String fillTimeStart = "06:30";
					Map<String, MinuteData> mDataMap = null;
					while (fillTimeStart.compareTo(fillTimeEnd) < 0) {
						if (!minuteDataMap.containsKey(fillTimeStart)) {
							if (mDataMap == null) {
								mDataMap = new MarketHistory().data("https://localhost:5000/v1/api/iserver/marketdata/history", 320227571, null, "1d", "5min");
							}
						
							if (mDataMap.containsKey(fillTimeStart)) {
								MinuteData mData = mDataMap.get(fillTimeStart);
								minuteDataMap.put(fillTimeStart, mData);
								LoggerUtil.getLogger().info(mData + " New entry added in minuteDataMap " + fillTimeStart);
							} else {
								LoggerUtil.getLogger().info("Data is incomplete " + fillTimeStart);
								break;
							}
						}
					
						fillTimeStart = Util.timeNMinsAgo(fillTimeStart, -5);
					}
				}
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
		latch.countDown();
		
	}

}
