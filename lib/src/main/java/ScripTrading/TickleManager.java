package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class TickleManager implements Runnable {
	
	private Map<String, String> tickleMap;
	
	public TickleManager(Map<String, String> tickleMap) {
		this.tickleMap = tickleMap;
	}

	@Override
	public void run() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String time = shorttimeFormatter.format(currentDate);
		
		while (time.compareTo("13:02") <= 0) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				time = shorttimeFormatter.format(currentDate);
				String sessionId = new Tickle().tickle();
				tickleMap.put(time, sessionId);
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}

}
