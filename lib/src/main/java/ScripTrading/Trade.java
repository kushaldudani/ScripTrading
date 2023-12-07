package ScripTrading;

public class Trade {
	
	private String orderid;
	
	private String executionInfo;
	
	private double strike;
	
	private long contract;
	
	private String localOId;
	
	public Trade() {
		this.orderid="";
		this.executionInfo="";
		this.strike=0;
		this.contract=0;
		this.localOId="";
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

	public String getLocalOId() {
		return localOId;
	}

	public void setLocalOId(String localOId) {
		this.localOId = localOId;
	}

	public void setContract(long contract) {
		this.contract = contract;
	}
	
	@Override
	public String toString() {
		return executionInfo + "  " + orderid + "  " + strike + "  " + contract + "  " + localOId;
	}

}
