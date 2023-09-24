package ScripTrading;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;

public class IBWebsocketClient extends WebSocketClient {

	  public IBWebsocketClient(URI serverUri, Draft draft) {
	    super(serverUri, draft);
	  }

	  public IBWebsocketClient(URI serverURI) {
	    super(serverURI);
	  }

	  public IBWebsocketClient(URI serverUri, Map<String, String> httpHeaders) {
	    super(serverUri, httpHeaders);
	  }

	  @Override
	  public void onOpen(ServerHandshake handshakedata) {
		  Tickle tm = new Tickle();
		  String sessionId = tm.tickle("https://localhost:5000/v1/api/tickle");
		  JSONObject authJson = new JSONObject();
		  authJson.put("session", sessionId);
		  send(authJson.toJSONString());
	  }

	  @Override
	  public void onMessage(String message) {
	    
	  }

	  @Override
	  public void onClose(int code, String reason, boolean remote) {
	    // The close codes are documented in class org.java_websocket.framing.CloseFrame
	    System.out.println(
	        "IB Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
	            + reason);
	  }

	  @Override
	  public void onError(Exception ex) {
		  LoggerUtil.getLogger().info("Error in websocket");
			LoggerUtil.getLogger().info(ex.getMessage());
	  }

	  public static void main(String[] args) throws URISyntaxException {
		  IBWebsocketClient c = new IBWebsocketClient(new URI(
	        "wss://localhost:5000/v1/api/ws"));
		  c.connect();
	    
	  }

}
