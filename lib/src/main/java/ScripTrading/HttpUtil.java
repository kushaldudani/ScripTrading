package ScripTrading;

import java.util.Arrays;

import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpUtil {
	
	public static CloseableHttpResponse get(String baseUrl, String paramString, CloseableHttpClient client) throws Exception {
		
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
		CloseableHttpResponse response=null;
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
	
	public static CloseableHttpResponse post(String baseUrl, CloseableHttpClient client) throws Exception {
		
		System.out.println(baseUrl);
		HttpPost post = new HttpPost(baseUrl);

		//StringEntity requestEntity = new StringEntity(
		//		jsonString,
		//	    ContentType.APPLICATION_JSON);
        // add request parameter, form parameters
        //List<NameValuePair> urlParameters = new ArrayList<>();
        //urlParameters.add(new BasicNameValuePair("username", "abc"));
        //urlParameters.add(new BasicNameValuePair("password", "123"));
        //urlParameters.add(new BasicNameValuePair("custom", "secret"));

        //spost.setEntity(requestEntity);
		//request.setHeader("User-Agent", "runscope/0.1");
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		//request.setHeader("Accept", "*/*");
		int responsecode=0;
		//int nooftries = 1;
		CloseableHttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(post);
				responsecode = response.getStatusLine().getStatusCode();
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
	
	public static CloseableHttpResponse post(String baseUrl, String jsonString, CloseableHttpClient client) throws Exception {
		
		System.out.println(baseUrl);
		HttpPost post = new HttpPost(baseUrl);

		StringEntity requestEntity = new StringEntity(
				jsonString,
			    ContentType.APPLICATION_JSON);
        // add request parameter, form parameters
        //List<NameValuePair> urlParameters = new ArrayList<>();
        //urlParameters.add(new BasicNameValuePair("username", "abc"));
        //urlParameters.add(new BasicNameValuePair("password", "123"));
        //urlParameters.add(new BasicNameValuePair("custom", "secret"));

        post.setEntity(requestEntity);
		//request.setHeader("User-Agent", "runscope/0.1");
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		//request.setHeader("Accept", "*/*");
		int responsecode=0;
		//int nooftries = 1;
		CloseableHttpResponse response=null;
		//while(responsecode != 200 && nooftries <= 5){
			try{
				response = client.execute(post);
				responsecode = response.getStatusLine().getStatusCode();
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
	
	public static CloseableHttpClient createHttpClient() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(5*1000).setConnectTimeout(5*1000).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(10);
		connManager.setDefaultMaxPerRoute(10);
		return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connManager).build();
	}

}
