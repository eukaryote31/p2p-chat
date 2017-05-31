package p2pchat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

import org.java_websocket.WebSocket;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerHandler {
	public void request_get_version(Request req, P2PServer server, WebSocket socket) {
		Request ret = new Request();
		ret.type = "version";
		ret.data.put("id", "p2pserver");
		
		if(!server.getKnownhosts().containsKey(socket.getRemoteSocketAddress()))
			server.addHost(socket.getRemoteSocketAddress());
		
		socket.send(ret.toJson());
	}
	
	public void request_get_hosts(Request req, P2PServer server, WebSocket socket) {
		Request ret = new Request();
		ret.type = "put_hosts";
		
		ret.data.put("hosts", server.getKnownhosts());
		
		socket.send(ret.toJson());
	}
	
	public void request_put_hosts(Request req, P2PServer server, WebSocket socket) {
		Map<String, Object> data = req.data;
		
		if(data == null)
			return;
		
		@SuppressWarnings("unchecked")
		Set<InetSocketAddress> hosts = (Set<InetSocketAddress>) data.get("hosts");
		
		for(InetSocketAddress addr : hosts) {
			try {
				@Cleanup
				Socket nsocket = new Socket();
				
				Request reqn = new Request();
				reqn.type = "get_version";
				
				nsocket.connect(addr);
				
				nsocket.getOutputStream().write(reqn.toJson().getBytes());
			} catch (IOException e) {
				return;
			}
		}
	}
	
	public void request_version(Request req, P2PServer server, WebSocket socket) {
		Map<String, Object> data = req.data;
		
		log.info("Got version response from {}", socket.getRemoteSocketAddress()	);
		
		if(data == null || !data.get("id").equals("p2pserver"))
			return;
		
		server.putHost(socket.getRemoteSocketAddress(), socket);

	}
	
	public void request_message(Request req, P2PServer server, WebSocket socket) {
		
		Map<String, Object> data = req.data;
		
		if(data == null)
			return;
		
		final int timeout = 600000;
		
		String message = (String) data.get("msg");
		String channel = (String) data.get("channel");
		long timestamp = ((Number) data.get("time_sent")).longValue();
		log.info("Recieved message `{}` in channel `{}` sent at {}", message, channel, timestamp);
		
		if(message == null || channel == null)
			return;
		
		// dont process messages sent more than `timestamp` ms ago or in the future
		long now = System.currentTimeMillis();
		if(now - timestamp < 0 ||
				now - timestamp > timeout) {
			log.info("Recieved message timed out {}ms ago", now - timestamp);
			return;
		}
		
		boolean isduplicate = !server.messages.add(req);
		
		if(isduplicate)
			return;
		
		log.info("Relaying message `{}` in channel `{}` sent at {}", message, channel, timestamp);
		
		server.broadcast(req.toJson());
	}
	
	// note to self: remove this later
	@Deprecated
	protected boolean hostAlive(InetSocketAddress addr) {
		try {
			@Cleanup
			Socket socket = new Socket();
			
			Request req = new Request();
			req.type = "get_version";
			
			socket.connect(addr);
			
			socket.getOutputStream().write(req.toJson().getBytes());
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String line = br.readLine();
			Request from = Request.fromJson(line);
			
			return from.type.equals("version") && from.data.get("id").equals("p2pserver");
		} catch (IOException e) {
			return false;
		}
		
	}
}
