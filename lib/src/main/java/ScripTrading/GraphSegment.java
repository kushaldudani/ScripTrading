package ScripTrading;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class GraphSegment {
	
	public String identifier;
	public String startTime;
	public String endTime;
	public double startPrice;
	public double endPrice;
	public double currentPrice;
	public LinkedList<String> pullbackTime = new LinkedList<>();
	public LinkedList<Double> pullbackPrice = new LinkedList<>();
	
	public int barCount;
	//public Map<String, Double> priceWithTime = new LinkedHashMap<>();
    
    
    
    
    
	public GraphSegment(String identifier, String startTime, String endTime, double startPrice, double endPrice,
			double currentPrice, int barCount) {
		super();
		this.identifier = identifier;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startPrice = startPrice;
		this.endPrice = endPrice;
		this.currentPrice = currentPrice;
		this.barCount = barCount;
	}
	
	
	
	



	@Override
	public String toString() {
		return "GraphSegment [identifier=" + identifier + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", startPrice=" + startPrice + ", endPrice=" + endPrice + ", currentPrice=" + currentPrice
				+ ", pullbackTime=" + pullbackTime + ", pullbackPrice=" + pullbackPrice + ", barCount=" + barCount
				+ "]";
	}



	

}
