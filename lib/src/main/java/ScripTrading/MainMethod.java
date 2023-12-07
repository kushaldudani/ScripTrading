package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainMethod {
	
	
	public static void main(String[] args) {
		ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(6);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		//Map<String, String> tickleMap = Collections.synchronizedMap(new LinkedHashMap<>());
		TickleManager tickleManager = new TickleManager();
		WORKER_THREAD_POOL.submit(tickleManager);
		//TickleMapProvider.getInstance().setTickleMap(tickleMap);
		
		while (true) {
			Calendar calendar = Calendar.getInstance();
			String currentTime = sdf.format(calendar.getTime());
			int currentTimeH = (currentTime.charAt(0) == '0') ? Integer.parseInt(currentTime.substring(1, 2)) : Integer.parseInt(currentTime.substring(0, 2));
			int currentTimeM = (currentTime.charAt(3) == '0') ? Integer.parseInt(currentTime.substring(4, 5)) : Integer.parseInt(currentTime.substring(3, 5));
			int currentTimeS = (currentTime.charAt(6) == '0') ? Integer.parseInt(currentTime.substring(7, 8)) : Integer.parseInt(currentTime.substring(6, 8));
			System.out.println(currentTimeH + "  " + currentTimeM + "  " + currentTimeS);
			if (calendar.get(Calendar.DAY_OF_WEEK) > 1 && calendar.get(Calendar.DAY_OF_WEEK) < 7 
					&& ((currentTimeH == 6 && currentTimeM >= 30) || currentTimeH >= 7) && currentTimeH < 13) {
				try {
					CountDownLatch latch = new CountDownLatch(1);
					Map<String, MinuteData> minuteDataMap = Collections.synchronizedMap(new LinkedHashMap<>());
					Map<String, String> longnotifictionMap = Collections.synchronizedMap(new LinkedHashMap<>());
					Map<String, String> shortnotifictionMap = Collections.synchronizedMap(new LinkedHashMap<>());
					//Map<Long, Double> conIdToStrike = Collections.synchronizedMap(new LinkedHashMap<>());
					
					SendMail smail = new SendMail(longnotifictionMap, shortnotifictionMap);
					WORKER_THREAD_POOL.submit(smail);
					
					MinuteDataManager mdm = new MinuteDataManager(latch, minuteDataMap);
					WORKER_THREAD_POOL.submit(mdm);
					
					ExecutorService orderThreadPool = Executors.newFixedThreadPool(1);
					LongOrderTracker longorderTracker = new LongOrderTracker(minuteDataMap, longnotifictionMap, orderThreadPool);
					WORKER_THREAD_POOL.submit(longorderTracker);
					
					ShortOrderTracker shortorderTracker = new ShortOrderTracker(minuteDataMap, shortnotifictionMap, orderThreadPool);
					WORKER_THREAD_POOL.submit(shortorderTracker);
					
					OrderPoller orderPoller = new OrderPoller();
					WORKER_THREAD_POOL.submit(orderPoller);
					
					while (latch.getCount() > 0) {
						try {
							latch.await();
						} catch (Exception ie) {
						
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					LoggerUtil.getLogger().info(e.getMessage());
					//break;
					//e.printStackTrace();
				}
			} else {
				int timeToSleep = (60 - currentTimeS) * 1000;
				System.out.println("TimeTosleep in mainmethod: " + timeToSleep);
				LoggerUtil.getLogger().info("TimeTosleep in mainmethod: " + timeToSleep);
				try {
		            Thread.sleep(timeToSleep);
		        } catch (Exception e) {
		            //e.printStackTrace();
		            LoggerUtil.getLogger().info(e.getMessage());
		        }
			}
		}
		
		
		  //          Thread.currentThread().interrupt();
		

		// wait for the latch to be decremented by the two remaining threads
		
	}

}
