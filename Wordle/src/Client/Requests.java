package Client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
public class Requests {
	private enum ResponseType {
		SIMPLE,
		AUTH,
		STARTED,
		STATUS,
		HINTS,
		STATISTICS
	}
	public static final int BUFFER_SIZE = 1024 * 1024; // 1MB should be plenty of space


	public static HashMap<String, Object> get(SocketChannel socket, String url, String queryString, String token) throws IOException {
		URL url1 = new URL(url);
		ObjectMapper mapper = new ObjectMapper();
		String request = String.format("%s\r\n%s\r\n%s%s\r\n%s\r\n\r\n",
				String.format("GET %s%s HTTP/1.1", url1.getPath(), !queryString.equals("") ? "?" + queryString : ""),
				String.format("Host: %s", url1.getHost()),
				!token.equals("") ? String.format("Authorization: %s\r\n", token) : "",
				"User-Agent: Wordle Java Client",
				"Accept: */*");
		int bytesProcessed = 0;
		ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
		bytesProcessed = socket.write(buffer);
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		bytesProcessed = socket.read(buffer);
		if(bytesProcessed < 0) throw new IOException("Broken pipe - server unreachable");
		buffer.flip();
		BufferedReader reader = new BufferedReader(new StringReader(new String(buffer.array())));
		StringBuilder responseHeader = new StringBuilder();
		StringBuilder responseBody = new StringBuilder();
		String response = reader.readLine();
		String tmp;
		int responseStatusCode;
		while (!(tmp = reader.readLine()).equals("")) {
			responseHeader.append(tmp);
		}
		while((tmp = reader.readLine()) != null) {
			responseBody.append(tmp);
		}
		@SuppressWarnings("unchecked")
		HashMap<String, Object> responseMap = mapper.readValue(responseBody.toString(), HashMap.class);
		try {
			responseStatusCode = Integer.parseInt(response.split(" ")[1]);
		} catch (Exception e) { return null;}
		responseMap.put("status", responseStatusCode);
		reader.close();
		return responseMap;
	}
	public static HashMap<String, Object> post(SocketChannel socket, String url, String queryString, String body, String token) throws IOException {
		URL url1 = new URL(url);
		ObjectMapper mapper = new ObjectMapper();
		String request = String.format("%s\r\n%s\r\n%s\r\n%s%s\r\n\r\n%s\r\n\r\n",
				String.format("POST %s%s HTTP/1.1", url1.getPath(), !queryString.equals("") ? "?" + queryString : ""),
				String.format("Host: %s", url1.getHost()),
				"User-Agent: Wordle Java Client",
				!token.equals("") ? String.format("Authorization: %s\r\n", token) : "",
				"Accept: */*",
				body);

		int bytesProcessed = 0;
		ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
		bytesProcessed = socket.write(buffer);
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		bytesProcessed = socket.read(buffer);
		if(bytesProcessed < 0) throw new IOException("Broken pipe - server unreachable");
		buffer.flip();
		BufferedReader reader = new BufferedReader(new StringReader(new String(buffer.array())));
		StringBuilder responseHeader = new StringBuilder();
		StringBuilder responseBody = new StringBuilder();
		String response = reader.readLine();
		String tmp;
		int responseStatusCode;
		while (!(tmp = reader.readLine()).equals("")) {
			responseHeader.append(tmp);
		}
		while((tmp = reader.readLine()) != null) {
			responseBody.append(tmp);
		}
		@SuppressWarnings("unchecked")
		HashMap<String, Object> responseMap = mapper.readValue(responseBody.toString(), HashMap.class);
		try {
			responseStatusCode = Integer.parseInt(response.split(" ")[1]);
		} catch (Exception e) { return null; }
		responseMap.put("status", responseStatusCode);
		reader.close();
		return responseMap;
	}


}
