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
		
		while (true) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				time = shorttimeFormatter.format(currentDate);
				Tickle tickleInstance = new Tickle();
				String sessionId = tickleInstance.tickle();
				tickleMap.put(time, sessionId);
				tickleInstance.accountPing();
				
				new ReauthenticateUtil().reauth();
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}

}
