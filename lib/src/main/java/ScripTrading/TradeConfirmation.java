package ScripTrading;

public class TradeConfirmation {
	
	private String date;
	
	private boolean hasOrderFilled;
	
	private String tradeTime;
	
	private double strike;
	
	public TradeConfirmation() {
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public boolean getHasOrderFilled() {
		return hasOrderFilled;
	}

	public void setHasOrderFilled(boolean hasOrderFilled) {
		this.hasOrderFilled = hasOrderFilled;
	}
	
	public String getTradeTime() {
		return tradeTime;
	}

	public void setTradeTime(String tradeTime) {
		this.tradeTime = tradeTime;
	}

	public double getStrike() {
		return strike;
	}

	public void setStrike(double strike) {
		this.strike = strike;
	}
	
	@Override
	public String toString() {
		return date + "  " + hasOrderFilled + "  " + tradeTime + "  " + strike;
	}


}
