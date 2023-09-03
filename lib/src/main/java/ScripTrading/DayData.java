package ScripTrading;

import java.io.Serializable;
import java.util.Map;

public class DayData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7975266192664496482L;
	
	private Map<String, MinuteData> minuteDataMap;
	private Map<String, Double> premiumMap;
	
	
	public DayData(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap) {
		super();
		this.minuteDataMap = minuteDataMap;
		this.premiumMap = premiumMap;
	}

	public Map<String, MinuteData> getMinuteDataMap() {
		return minuteDataMap;
	}
	
	public Map<String, Double> getPremiumMap() {
		return premiumMap;
	}
	
}
