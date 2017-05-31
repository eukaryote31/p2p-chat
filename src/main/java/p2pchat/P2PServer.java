package p2pchat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonSyntaxException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class P2PServer extends WebSocketServer {
	public static final InetSocketAddress[] bootstrap_hosts = { new InetSocketAddress("eukaryote.duckdns.org", 1943) };

	Map<InetSocketAddress, WebSocket> clients = Collections.synchronizedMap(new HashMap<>());
	Set<Request> messages = Collections.synchronizedSet(new HashSet<>());
	@Getter
	private Map<InetSocketAddress, WebSocket> knownhosts = Collections.synchronizedMap(new HashMap<>());

	ServerHandler handler;

	public P2PServer(ServerHandler handler, int port, Draft d) {
		super(new InetSocketAddress(port), Collections.singletonList(d));
		this.handler = handler;
		for (InetSocketAddress addr : bootstrap_hosts)
			this.addHost(addr);
		log.debug("Constructed");
	}

	@Override
	public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
		log.info("{} disconnected", arg0.getRemoteSocketAddress().getHostString());
		clients.remove(arg0.getRemoteSocketAddress());

	}

	@Override
	public void onError(WebSocket arg0, Exception arg1) {
		if (arg0 != null)
			log.error("Error with ip {}", arg0.getRemoteSocketAddress());
		log.error("Error: ", arg1);
	}

	@Override
	public void onMessage(WebSocket arg0, String arg1) {
		try {
			Request req = Request.fromJson(arg1);
			String reqtype = req.type;

			try {

				Method m = handler.getClass().getMethod("request_" + reqtype, Request.class, P2PServer.class,
						WebSocket.class);

				m.invoke(handler, req, this, arg0);
			} catch (NoSuchMethodException e) {
				log.warn("IP {} requested invalid request type `{}`", arg0.getRemoteSocketAddress(), reqtype);
			} catch (SecurityException e) {
				log.error("Something went wrong (ip={}, request={})", arg0.getRemoteSocketAddress(), req);
			} catch (IllegalAccessException e) {
				log.error("Unexpected error!", e);
			} catch (IllegalArgumentException e) {
				log.error("Unexpected error!", e);
			} catch (InvocationTargetException e) {
				log.error("Unexpected error!", e);
			}
		} catch (JsonSyntaxException e) {
			log.warn("Invalid json recieved from IP " + arg0.getRemoteSocketAddress(), e);
		}

	}

	@Override
	public void onOpen(WebSocket arg0, ClientHandshake arg1) {
		log.info("{} connected", arg0.getRemoteSocketAddress().getHostString());
		clients.put(arg0.getRemoteSocketAddress(), arg0);
	}

	@Override
	public void onStart() {
		log.info("Server started");
	}

	public void broadcast(String s) {
		log.info("Broadcasting to {} clients and {} servers", clients.size(), getKnownhosts().size());
		for (WebSocket ws : clients.values()) {
			ws.send(s);
		}

		for (WebSocket sc : getKnownhosts().values()) {
			sc.send(s);
		}
	}

	protected void writeToSocket(Socket sc, String s) {
		try {
			OutputStream stream = sc.getOutputStream();
			stream.write(s.getBytes());
			stream.flush();
		} catch (IOException e) {
			log.warn("Error writing to socket {}: {}", sc, e);
		}
	}

	public void putHost(InetSocketAddress addr, WebSocket ws) {
		this.knownhosts.put(addr, ws);
	}

	public void addHost(InetSocketAddress host) {
		log.info("Adding host {}", "ws://" + host.getAddress().getHostAddress() + ":" + host.getPort());
		try {
			ClientRelay cr = new ClientRelay(this,
					new URI("ws://" + host.getAddress().getHostAddress() + ":" + host.getPort()));
			cr.connectBlocking();

			Request r = new Request();
			r.type = "get_version";

			cr.send(r.toJson());
		} catch (Exception e) {
			log.warn("Error while adding host {}: {}", host, e);
		}
	}

	class ClientRelay extends WebSocketClient {

		P2PServer parent;

		public ClientRelay(P2PServer server, URI arg0) {
			super(arg0, new Draft_17(), null, 3000);

			parent = server;
		}

		@Override
		public void onClose(int arg0, String arg1, boolean arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onError(Exception arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMessage(String arg0) {
			parent.onMessage(this, arg0);
		}

		@Override
		public void onOpen(ServerHandshake arg0) {
			log.info("Successfully established connection! ", this.getRemoteSocketAddress());
		}

		public void write(String str) {
			this.send(str);
		}
	}

}
