package ScripTrading;

public class TradeData {
	
	private String date;
	
	private double strike;
	
	private int qty;
	
	public TradeData() {
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public double getStrike() {
		return strike;
	}

	public void setStrike(double strike) {
		this.strike = strike;
	}

	public int getQty() {
		return qty;
	}

	public void setQty(int qty) {
		this.qty = qty;
	}

	@Override
	public String toString() {
		return date + "  " + strike + "  " + qty;
	}

}
