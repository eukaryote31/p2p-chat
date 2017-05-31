package p2pchat;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AddressAdapter implements JsonSerializer<InetSocketAddress>, JsonDeserializer<InetSocketAddress> {

	@Override
	public InetSocketAddress deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject obj = json.getAsJsonObject();
		String ip = obj.get("ip").getAsString();
		int port = obj.get("ip").getAsInt();
		return new InetSocketAddress(ip, port);
	}

	@Override
	public JsonElement serialize(InetSocketAddress src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject ret = new JsonObject();
		ret.addProperty("ip", src.getHostString());
		ret.addProperty("port", src.getPort());
		return ret;
	}

}
