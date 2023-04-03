package CommonUtils;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class StringPairDeserializer extends KeyDeserializer {
	@Override
	public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
		if(!s.contains("StringPair"))
			return null;
		String result = s.replaceAll("StringPair", "").replaceAll("\\[", "").replaceAll("\\]", "");
		String[] fields = result.split(",");
		return new StringPair(fields[0].split("=")[1], fields[1].split("=")[1]);
	}
}
