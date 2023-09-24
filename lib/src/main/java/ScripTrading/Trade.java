package ScripTrading;

public class Trade {
	
	private String orderid;
	
	private String executionInfo;	
	
	public Trade() {
		this.orderid="";
		this.executionInfo="";
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
	

}
