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
	Map<Double, Map<String, MinuteData>> callDataMap;
	Map<Double, Map<String, MinuteData>> putDataMap;
	
	
	public DayData(Map<String, MinuteData> minuteDataMap, Map<String, Double> premiumMap, Map<Double, Map<String, MinuteData>> callDataMap,
			Map<Double, Map<String, MinuteData>> putDataMap) {
		super();
		this.minuteDataMap = minuteDataMap;
		this.premiumMap = premiumMap;
		this.callDataMap = callDataMap;
		this.putDataMap = putDataMap;
	}

	public Map<Double, Map<String, MinuteData>> getCallDataMap() {
		return callDataMap;
	}

	public Map<Double, Map<String, MinuteData>> getPutDataMap() {
		return putDataMap;
	}
	
	public void setPutDataMap(Map<Double, Map<String, MinuteData>> putMap) {
		this.putDataMap = putMap;
	}

	public Map<String, MinuteData> getMinuteDataMap() {
		return minuteDataMap;
	}
	
	public Map<String, Double> getPremiumMap() {
		return premiumMap;
	}
	
}
