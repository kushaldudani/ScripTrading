package ScripTrading;

import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

public class HttpUtil {
	
	public static HttpResponse get(String baseUrl, String paramString, HttpClient client) throws Exception {
		
		System.out.println(baseUrl + paramString);
		//HttpPost post = new HttpPost(baseUrl);
		HttpGet get = new HttpGet(baseUrl + paramString);

		RequestConfig defaultRequestConfig = RequestConfig.custom()
			    .setCookieSpec(CookieSpecs.BEST_MATCH)
			    .setExpectContinueEnabled(true)
			    .setStaleConnectionCheckEnabled(true)
			    .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
			    .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
			    .build();
		RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig)
			    .setSocketTimeout(5000)
			    .setConnectTimeout(5000)
			    .setConnectionRequestTimeout(5000)
			    .build();
		get.setConfig(requestConfig);
		//int nooftries = 1;
		HttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(get);
				//responsecode = response.getStatusLine().getStatusCode();
			}catch(Exception e){
				e.printStackTrace();
				LoggerUtil.getLogger().info(e.getMessage());	
			}
		//	try {
		//		Thread.sleep(nooftries * 1000);
		//	} catch (InterruptedException e) {}
		//	nooftries++;
		//}
			//System.out.println(responsecode);
		
		return response;
	}

}
