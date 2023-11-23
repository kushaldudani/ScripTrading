package ScripTrading;

public class Trade {
	
	private String orderid;
	
	private String executionInfo;
	
	private double strike;
	
	private long contract;
	
	public Trade() {
		this.orderid="";
		this.executionInfo="";
		this.strike=0;
		this.contract=0;
	}

	public void setOrderid(String orderid) {
		this.orderid = orderid;
	}

	public void setExecutionInfo(String executionInfo) {
		this.executionInfo = executionInfo;
	}

	public String getOrderId() {
		return orderid;
	}

	public String getExecutionInfo() {
		return executionInfo;
	}
	
	public double getStrike() {
		return strike;
	}

	public void setStrike(double strike) {
		this.strike = strike;
	}
	
	public long getContract() {
		return contract;
	}

	public void setContract(long contract) {
		this.contract = contract;
	}
	
	@Override
	public String toString() {
		return executionInfo + "  " + orderid + "  " + strike + "  " + contract;
	}

}
