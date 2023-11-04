package ScripTrading;

public class TradeConfirmation {
	
	private String date;
	
	private boolean hasOrderFilled;
	
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

	@Override
	public String toString() {
		return date + "  " + hasOrderFilled;
	}


}
