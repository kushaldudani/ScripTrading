package ScripTrading;


import io.polygon.kotlin.sdk.DefaultJvmHttpClientProvider;
import io.polygon.kotlin.sdk.rest.ComparisonQueryFilterParameters;
import io.polygon.kotlin.sdk.rest.ComparisonQueryFilterParametersBuilder;
import io.polygon.kotlin.sdk.rest.*;
import io.polygon.kotlin.sdk.rest.experimental.FinancialsParameters;
import io.polygon.kotlin.sdk.rest.experimental.FinancialsParametersBuilder;
import io.polygon.kotlin.sdk.rest.reference.*;
import io.polygon.kotlin.sdk.websocket.*;
import kotlinx.coroutines.channels.Channel;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WebsocketCopy {
	
	

	public static void main(String[] args) {

		// TODO Auto-generated method stub
    
        String polygonKey = "6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS";//System.getenv("POLYGON_API_KEY");
        if (polygonKey == null || polygonKey.isEmpty()) {
        	LoggerUtil.getLogger().info("Make sure you set your polygon API key in the POLYGON_API_KEY environment variable!");
            //System.exit(1);
            return;
        }
        
        Downloader downloader = new Downloader();
        Map<String, MinuteData> minuteDataMap = downloader.downloadStockPrice("https://api.polygon.io/v2/aggs/ticker/TSLA/range/1/minute/2023-07-02/2023-07-03?adjusted=true&sort=asc&apiKey=6xGq1Yv1LmfdH4fyoThIJphP7J3pdmRS");
        
        List<MinuteData> fiveMinuteList = new ArrayList<>();
		List<GraphSegment> graphSegments = new ArrayList<>();
		for (String time : minuteDataMap.keySet()) {
			if ((time.charAt(4) == '0' || time.charAt(4) == '5') && !fiveMinuteList.isEmpty()) {
				double fiveMinVol = 0;
				double fiveMinOpen = fiveMinuteList.get(0).getOpenPrice();
				double fiveMinClose = fiveMinuteList.get(fiveMinuteList.size() - 1).getClosePrice();
				double fiveMinHigh = 0;
				double fiveMinLow = fiveMinuteList.get(0).getLowPrice();
				for (MinuteData md : fiveMinuteList) {
					fiveMinVol = fiveMinVol + md.getVolume();
					if (md.getHighPrice() > fiveMinHigh) {
						fiveMinHigh = md.getHighPrice();
					}
					if (md.getLowPrice() < fiveMinLow) {
						fiveMinLow = md.getLowPrice();
					}
				}
				
				Util.calculateGraphSegments(graphSegments, fiveMinVol, fiveMinOpen, fiveMinClose, fiveMinHigh, fiveMinLow, time, 0.75);
				
				
				
				fiveMinuteList.clear();
				fiveMinuteList.add(minuteDataMap.get(time));
			} else {
				fiveMinuteList.add(minuteDataMap.get(time));
			}
		}
		// Print Graph Segments
		for (GraphSegment graphSegment : graphSegments) {
			LoggerUtil.getLogger().info(graphSegment.toString());
		}

        

        /*LoggerUtil.getLogger().info("Websocket sample:");
        Map<String, MinuteData> minuteDataMap = new LinkedHashMap<>();
        // List<GraphSegment> graphSegments = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Calendar calendar = Calendar.getInstance();
        websocketSample(polygonKey, minuteDataMap, calendar, simpleDateFormat);*/
        
        //latch.countDown();
    }

    public static void websocketSample(String polygonKey, Map<String, MinuteData> minuteDataMap, Calendar calendar, SimpleDateFormat sdf) {
        PolygonWebSocketClient client = new PolygonWebSocketClient(
                polygonKey,
                PolygonWebSocketCluster.Stocks,
                new DefaultPolygonWebSocketListener() {
                    @Override
                    public void onReceive(@NotNull PolygonWebSocketClient client, @NotNull PolygonWebSocketMessage message) {
                        //if (message instanceof PolygonWebSocketMessage.RawMessage) {
                        //	PolygonWebSocketMessage.RawMessage wsmessage = ((PolygonWebSocketMessage.RawMessage) message);
                        //    System.out.println(new String(wsmessage.getData()));
                    	if (message instanceof PolygonWebSocketMessage.StocksMessage) {
                    		PolygonWebSocketMessage.StocksMessage.Aggregate wsmessage = ((PolygonWebSocketMessage.StocksMessage.Aggregate) message);
                    		System.out.println(wsmessage);
                    		//LoggerUtil.getLogger().info(wsmessage.toString());
                    		System.out.println("It is coming here");
                    		calendar.setTimeInMillis(wsmessage.getStartTimestampMillis());
                    		MinuteData mData = new MinuteData();
                    		mData.setClosePrice(wsmessage.getClosePrice());
                    		mData.setOpenPrice(wsmessage.getOpenPrice());
                    		mData.setHighPrice(wsmessage.getHighPrice());
                    		mData.setLowPrice(wsmessage.getLowPrice());
                    		minuteDataMap.put(sdf.format(calendar.getTime()), mData);
                    		System.out.println(minuteDataMap);
                    		
                    		List<MinuteData> fiveMinuteList = new ArrayList<>();
                    		List<GraphSegment> graphSegments = new ArrayList<>();
                    		for (String time : minuteDataMap.keySet()) {
                    			if (time.charAt(4) == '0' || time.charAt(4) == '5') {
                    				double fiveMinVol = 0;
                    				double fiveMinOpen = fiveMinuteList.get(0).getOpenPrice();
                    				double fiveMinClose = fiveMinuteList.get(fiveMinuteList.size() - 1).getClosePrice();
                    				double fiveMinHigh = 0;
                    				double fiveMinLow = fiveMinuteList.get(0).getLowPrice();
                    				for (MinuteData md : fiveMinuteList) {
                    					fiveMinVol = fiveMinVol + md.getVolume();
                    					if (md.getHighPrice() > fiveMinHigh) {
                    						fiveMinHigh = md.getHighPrice();
                    					}
                    					if (md.getLowPrice() < fiveMinLow) {
                    						fiveMinLow = md.getLowPrice();
                    					}
                    				}
                    				
                    				Util.calculateGraphSegments(graphSegments, fiveMinVol, fiveMinOpen, fiveMinClose, fiveMinHigh, fiveMinLow, time, 0.25);
                    				
                    				fiveMinuteList.clear();
                    				fiveMinuteList.add(minuteDataMap.get(time));
                    			} else {
                    				fiveMinuteList.add(minuteDataMap.get(time));
                    			}
                    		}
                    		// Print Graph Segments
                    		for (GraphSegment graphSegment : graphSegments) {
                    			LoggerUtil.getLogger().info(graphSegment.toString());
                    		}
                    	} else {

                    		LoggerUtil.getLogger().info(message.toString());
                        }
                    }
                    
                    
					

					@Override
                    public void onError(@NotNull PolygonWebSocketClient client, @NotNull Throwable error) {
						LoggerUtil.getLogger().info("Error in websocket");
						LoggerUtil.getLogger().info(error.getMessage());
                    }
                },
                Channel.UNLIMITED,
                new DefaultJvmHttpClientProvider(),
                "delayed.polygon.io");

        client.connectBlocking();

        List<PolygonWebSocketSubscription> subs = Collections.singletonList(
                new PolygonWebSocketSubscription(PolygonWebSocketChannel.Stocks.AggPerMinute.INSTANCE, "QQQ"));
        client.subscribeBlocking(subs);

        try {
            Thread.sleep(23400000);
        } catch (InterruptedException e) {
        	LoggerUtil.getLogger().info("Error in JavaUsageThread main thread sleeping");
			LoggerUtil.getLogger().info(e.getMessage());
        }

        client.unsubscribeBlocking(subs);
        client.disconnectBlocking();
    }

    /*public static void supportedTickersSample(PolygonRestClient polygonRestClient) {
        System.out.println("3 Supported Tickers: ");
        SupportedTickersParameters params = new SupportedTickersParametersBuilder()
                .limit(3)
                .market("fx")
                .build();

        System.out.println(polygonRestClient
                .getReferenceClient()
                .getSupportedTickersBlocking(params)
        );
    }

    public static void tickerDetailsSample(PolygonRestClient polygonRestClient) {
        System.out.println("Redfin Ticker Details: ");
        System.out.println(polygonRestClient.getReferenceClient().getTickerDetailsBlocking("RDFN"));
    }

    public static void dividendsSample(PolygonRestClient polygonRestClient) {
        System.out.println("GE dividends:");
        DividendsParameters geParams = new DividendsParametersBuilder()
                .ticker(ComparisonQueryFilterParameters.equal("GE"))
                .limit(1)
                .build();
        System.out.println(polygonRestClient.getReferenceClient().getDividendsBlocking(geParams));

        System.out.println("Dividends with cash amounts between $1 and $10");
        DividendsParameters cashAmountFilterParams = new DividendsParametersBuilder()
                .cashAmount(new ComparisonQueryFilterParametersBuilder<Double>()
                        .greaterThanOrEqual(1.0)
                        .lessThanOrEqual(10.0)
                        .build())
                .limit(1)
                .build();
        System.out.println(polygonRestClient.getReferenceClient().getDividendsBlocking(cashAmountFilterParams));
    }

    public static void financialsSample(PolygonRestClient polygonRestClient) {
        System.out.println("RDFN Financials");
        FinancialsParameters params = new FinancialsParametersBuilder().ticker("RDFN").build();
        System.out.println(polygonRestClient.getExperimentalClient().getFinancialsBlocking(params));
    }*/

}
