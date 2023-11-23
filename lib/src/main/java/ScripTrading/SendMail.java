package ScripTrading;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail implements Runnable {
	
	public static void main(String[] args) {
		SendMail sm = new SendMail(null, null);
		sm.generateAndSendEmail("Test", "Test");
	}
	
	private Map<String, String> longnotifictionMap;
	private Map<String, String> shortnotifictionMap;
	private Set<String> emailSentTracker = new HashSet<>();
	
	public SendMail(Map<String, String> longnotifictionMap, Map<String, String> shortnotifictionMap) {
		this.longnotifictionMap = longnotifictionMap;
		this.shortnotifictionMap = shortnotifictionMap;
	}
	
	@Override
	public void run() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		Date currentDate = calendar.getTime();
		
		SimpleDateFormat shorttimeFormatter = new SimpleDateFormat("HH:mm");
		String time = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
		time = Util.timeNMinsAgo(time, 5);
		
		while (time.compareTo("13:02") <= 0) {
			try {
				int timeToSleep = 60000;
				Thread.sleep(timeToSleep);
				
				calendar.setTimeInMillis(System.currentTimeMillis());
				currentDate = calendar.getTime();
				time = Util.findNearestFiveMinute(shorttimeFormatter.format(currentDate));
				time = Util.timeNMinsAgo(time, 5);
				
				if (longnotifictionMap.containsKey(time) && shortnotifictionMap.containsKey(time) && !emailSentTracker.contains(time)) {
					generateAndSendEmail(longnotifictionMap.get(time), shortnotifictionMap.get(time));
					emailSentTracker.add(time);
				}
			} catch (Exception e) {
				LoggerUtil.getLogger().info(e.getMessage());
			}
		}
		
	}
	
	private void executeMail(String longmessage, String shortmessage, String subject) {
		try{		
			
			Properties mailServerProperties = System.getProperties();
			mailServerProperties.put("mail.smtp.port", "587");
			mailServerProperties.put("mail.smtp.auth", "true");
			mailServerProperties.put("mail.smtp.starttls.enable", "true");
			mailServerProperties.put("mail.smtp.ssl.protocols", "TLSv1.2");
			
	 
	//Step2		
			
			Session getMailSession = Session.getDefaultInstance(mailServerProperties, null);
			MimeMessage generateMailMessage = new MimeMessage(getMailSession);
			generateMailMessage.setFrom(new InternetAddress("escapeplan555@gmail.com"));
			generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("kushal.kush12@gmail.com"));
			//generateMailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress("sakshi.dalmia@gmail.com"));
			generateMailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress("agarwa27@gmail.com"));
			generateMailMessage.setSubject(subject);
			String emailBody = "Details for Long side. <br><br> " + longmessage + " <br><br> " + "Details for Short side. <br><br>" + shortmessage;
			generateMailMessage.setContent(emailBody, "text/html");
			
	 
	//Step3		
			//LoggerUtil.getLogger().info("Get Session and Send mail");
			Transport transport = getMailSession.getTransport("smtp");
			
			// Enter your correct gmail UserID and Password (XXXApp Shah@gmail.com)
			transport.connect("smtp.gmail.com", "escapeplan555", "gxqs saoj opyu zwxt");
			transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
			transport.close();
			LoggerUtil.getLogger().info("Mail sent ...");
		}catch(Exception e){
			e.printStackTrace();
			LoggerUtil.getLogger().log(Level.SEVERE, "sendmail failed", e);
		}
	}
	
	private void generateAndSendEmail(String longmessage, String shortmessage)  {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar today = Calendar.getInstance();
		executeMail(longmessage, shortmessage, "QQQ Day Trading "+sdf.format(today.getTime()));
	}

}
