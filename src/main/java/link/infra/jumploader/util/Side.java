package link.infra.jumploader.util;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(Side.Adapter.class)
public enum Side {
	CLIENT("client"),
	SERVER("server");

	public final String name;

	Side(String name) {
		this.name = name;
	}

	public static Side of(String serialisedName) {
		if (serialisedName.equals("server")) {
			return SERVER;
		}
		return CLIENT;
	}

	@Override
	public String toString() {
		return name;
	}

	public static class Adapter extends TypeAdapter<Side> {
		@Override
		public void write(JsonWriter out, Side value) throws IOException {
			if (value == null) {
				out.value("current");
			} else {
				out.value(value.name);
			}
		}

		@Override
		public Side read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			String name = in.nextString();
			if (name.equals("current")) {
				return null;
			}
			return Side.of(name);
		}
	}
}
