package ScripTrading;


public class TickleManager implements Runnable {
	
	//private Map<String, String> tickleMap;
	
	public TickleManager() {
		//this.tickleMap = tickleMap;
	}

	@Override
	public void run() {
		//Calendar calendar = Calendar.getInstance();
		//calendar.setTimeInMillis(System.currentTimeMillis());
		//Date currentDate = calendar.getTime();
		
		//SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		//String time = shorttimeFormatter.format(currentDate);
		
		while (true) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				//calendar.setTimeInMillis(System.currentTimeMillis());
				//currentDate = calendar.getTime();
				//time = shorttimeFormatter.format(currentDate);
				Tickle tickleInstance = new Tickle();
				boolean isAuthenticated = tickleInstance.tickle();
				//tickleMap.put(time, sessionId);
				//tickleInstance.accountPing();
				if (!isAuthenticated) {
					new ReauthenticateUtil().reauth();
				}
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}

}
