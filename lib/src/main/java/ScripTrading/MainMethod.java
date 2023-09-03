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
		ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(2);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
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
					Map<String, Double> premiumMap = Collections.synchronizedMap(new LinkedHashMap<>());
					JavaUsageSample jus = new JavaUsageSample(latch, minuteDataMap, premiumMap);
					WORKER_THREAD_POOL.submit(jus);
					OrderTracker orderTracker = new OrderTracker(minuteDataMap, premiumMap);
					WORKER_THREAD_POOL.submit(orderTracker);
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
		        } catch (InterruptedException e) {
		            //e.printStackTrace();
		            LoggerUtil.getLogger().info(e.getMessage());
		        }
			}
		}
		
		
		  //          Thread.currentThread().interrupt();
		

		// wait for the latch to be decremented by the two remaining threads
		
	}

}
