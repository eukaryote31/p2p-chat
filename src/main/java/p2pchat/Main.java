package p2pchat;

import java.net.InetSocketAddress;

import org.java_websocket.drafts.Draft_17;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	public static void main(String[] args) {
		log.info("Started main");
		P2PServer p2p = new P2PServer(new ServerHandler(), 1943, new Draft_17());
		p2p.start();
	}

}
