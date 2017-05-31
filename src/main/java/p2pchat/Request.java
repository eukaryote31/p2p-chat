package p2pchat;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Request {
	String type;
	
	Map<String, Object> data = new HashMap<>();
	
	private static Gson gson;
	
	static {
		gson = new GsonBuilder().registerTypeAdapter(InetSocketAddress.class, new AddressAdapter()).create();
	}
	
	public String toJson() {
		return gson.toJson(this);
	}
	
	public static Request fromJson(String json) {
		return gson.fromJson(json, Request.class);
	}
}
