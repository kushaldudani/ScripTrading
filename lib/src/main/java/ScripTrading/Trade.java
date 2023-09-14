package ScripTrading;

public class Trade {
	
	private String time;
	
	private double closePriceAtTime;
	
	private String id;
	
	private String message;
	
	private int qty;
	
	private double enterPrice;
	
	
	public Trade(String time, double closePriceAtTime, String id, String message, int qty) {
		super();
		this.time = time;
		this.closePriceAtTime = closePriceAtTime;
		this.id = id;
		this.message = message;
		this.qty = qty;
	}

	public int getQty() {
		return qty;
	}

	public String getId() {
		return id;
	}

	public String getMessage() {
		return message;
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
