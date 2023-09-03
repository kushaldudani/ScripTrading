package ScripTrading;

public class Trade {
	
	private String time;
	
	private double closePriceAtTime;
	
	private double enterPrice;
	
	
	public Trade(String time, double closePriceAtTime) {
		super();
		this.time = time;
		this.closePriceAtTime = closePriceAtTime;
	}

	public String getTime() {
		return time;
	}

	public double getClosePriceAtTime() {
		return closePriceAtTime;
	}

	public double getEnterPrice() {
		return enterPrice;
	}

	public void setEnterPrice(double enterPrice) {
		this.enterPrice = enterPrice;
	}	
	
	
	

}
