package ScripTrading;

import java.io.Serializable;

public class MinuteData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6611010599628145503L;
	private double openPrice;
	private double closePrice;
	private double highPrice;
	private double lowPrice;
	private double volume;
	
	
	public double getOpenPrice() {
		return openPrice;
	}
	public void setOpenPrice(Double openPrice) {
		this.openPrice = openPrice;
	}
	public double getClosePrice() {
		return closePrice;
	}
	public void setClosePrice(Double closePrice) {
		this.closePrice = closePrice;
	}
	public double getHighPrice() {
		return highPrice;
	}
	public void setHighPrice(Double highPrice) {
		this.highPrice = highPrice;
	}
	public double getLowPrice() {
		return lowPrice;
	}
	public void setLowPrice(Double lowPrice) {
		this.lowPrice = lowPrice;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}
	
	
	@Override
	public String toString() {
		return "MinuteData [closePrice=" + closePrice + "]";
	}

}
