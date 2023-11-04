package ScripTrading;

import java.util.LinkedList;

public class IGraphSegment {
	
	public String identifier;
	public String startTime;
	public String endTime;
	public double startPrice;
	public double endPrice;
	
	public LinkedList<PullBackLevel> pullBackLevels = new LinkedList<>();
    
    
	public IGraphSegment(String identifier, String startTime, String endTime, double startPrice, double endPrice) {
		super();
		this.identifier = identifier;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startPrice = startPrice;
		this.endPrice = endPrice;
	}


	@Override
	public String toString() {
		return "IGraphSegment [identifier=" + identifier + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", startPrice=" + startPrice + ", endPrice=" + endPrice + "  " + pullBackLevels + "]";
	}
	
	public static class PullBackLevel {
		
		public String identifier;
		public PriceTime up;
		public PriceTime down;
		
		@Override
		public String toString() {
			return "PullBackLevel [identifier=" + identifier + " up=" + up.price + "  " + up.time + ", down=" + down.price
					+ "  " + down.time + "]";
		}
		
	}
	
	public static class PriceTime {
		public double price;
		public String time;
		
		public PriceTime(double price, String time) {
			this.price = price;
			this.time = time;
		}
		
	}

}
