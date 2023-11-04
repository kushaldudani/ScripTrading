package ScripTrading;

import java.util.Map;

public class TickleMapProvider {
	
	private static TickleMapProvider singleInstance = null;
	private Map<String, String> tickleMap;
	
	public static synchronized TickleMapProvider getInstance() {
        if (singleInstance == null) {
        	singleInstance = new TickleMapProvider();
        }
  
        return singleInstance;
    }
	
	public synchronized void setTickleMap(Map<String, String> tickleMap) {
		this.tickleMap = tickleMap;
	}
	
	public synchronized Map<String, String> getTickleMap() {
		return tickleMap;
	}

}
