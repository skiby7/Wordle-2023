package Server;

import CommonUtils.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class RequestHandler implements Runnable{
	private enum Endpoint {
		NOT_SUPPORTED,
		REGISTER,
		LOGIN,
		VERIFY,
		LOGOUT,
		PLAY,
		GET_GAME_STATUS,
		GET_GAME_HISTORY,
		WORD_TIMER,
		SEND_WORD,
		GET_STATISTICS,
		SHARE,
		GET_RANKING,
		GET_MULTICAST,
		GET_CURRENT_WORD,
		OPTIONS
	}
	private final WordleDB database;
	private final LoginHandler loginHandler;
	private final WordFactory wordFactory;
	private final String requestString;
	private final SelectionKey key;
	private final Selector selector;
	private final ServerConfig config;
	public RequestHandler(Selector selector, SelectionKey key, String requestString, WordleDB database, LoginHandler loginHandler, WordFactory wordFactory, ServerConfig config) {
		this.database = database;
		this.loginHandler = loginHandler;
		this.wordFactory = wordFactory;
		this.requestString = requestString;
		this.key = key;
		this.selector = selector;
		this.config = config;
	}

	private boolean credentialsBlankOrNull(String a, String b) {
		return a == null || b == null || a.isBlank() || b.isBlank();
	}

	private void printRequestResponse(String apiEndpoint, String rawEndpoint, HashMap<String, String> request, HashMap<String, Object> response) {
		StringBuilder text = new StringBuilder();
		text.append(String.format("~~~~~[ @YNew connection from %s@0 ]~~~~~\n", request.get("username") != null ? request.get("username") : ""));
		text.append(String.format("[ @CRequest %s@0 ] - ", apiEndpoint.equals(Endpoint.NOT_SUPPORTED.toString()) ? rawEndpoint : apiEndpoint));
		int i = request.size();
		for (Map.Entry<String, String> entry : request.entrySet()) {
			if (entry.getKey().equals("password")) {
				text.append("\n");
				continue;
			}
			String toPrint = entry.getValue();
			text.append("(").append(entry.getKey()).append(", ").append(toPrint).append(")");
			i--;
			if(i != 0) text.append(", ");
			else text.append("\n");
		}
		i = response.size();
		text.append("[ @MResponse@0 ] - ");

		for (Map.Entry<String, Object> entry : response.entrySet()) {
			String toPrint = entry.getValue().toString();
			text.append("(").append(entry.getKey()).append(", ").append(toPrint).append(")");
			i--;
			if(i != 0) text.append(", ");
			else text.append("\n");
		}
		if (response.entrySet().isEmpty()) text.append("\n");
		text.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		PrettyPrinter.prettyPrintln(text.toString());
	}


	private void handleRequest(String request, String header, String body) {
		HashMap<String,String> params = getParams(request.split(" ")[1], header, body);
		Endpoint endpoint = getEndpoint(request, params);

		try {
			HashMap<String, Object> response = new HashMap<>();
			switch (endpoint){
				case NOT_SUPPORTED -> {
					response.put("details", "Method not allowed");
					attachJson("405", response);
				}
				case OPTIONS -> attachJson("200", response); // Handle CORS preflight request

				case REGISTER -> {
					String username = params.get("username");
					String password = params.get("password");
					if(credentialsBlankOrNull(username, password)){
						response.put("details", "Invalid registration");
						attachJson("400", response);
					}

					if(database.insertUser(username, password, "user")) {
						response.put("details", "Registration successful");
						attachJson("200", response);
					}

					else {
						response.put("details", "User already exists");
						attachJson("409",  response);
					}

				}
				case LOGIN -> {
					String username = params.get("username");
					String password = params.get("password");
					if(credentialsBlankOrNull(username, password)) {
						response.put("details", "Invalid login");
						attachJson("400", response);
					}

					WordleDB.Authorization loginType = database.validateUser(username, password);
					if (loginType == WordleDB.Authorization.NOT_AUTHORIZED) {
						response.put("details", "Not authorized");
						attachJson("401", response);
					} else {
						response.put("multicastIp", config.multicastAddress());
						response.put("multicastPort", config.multicastPort());
						if(loginHandler.getUserSession(username) != null) {
							loginHandler.renewSession(username);
							response.put("details", "Already logged in!");
							response.put("token", loginHandler.getUserSession(username).getToken());
							attachJson("400", response);
						} else {
							String sessionToken = generateSessionToken(username);
							if (sessionToken.isBlank()) {
								response.put("details", "Internal server error");
								attachJson("500", response);

							} else {
								loginHandler.addSession(loginType, username, sessionToken);
								response.put("details", "Login successful");
								response.put("token", sessionToken);
								attachJson("200", response);
							}
						}
					}
				}

				case VERIFY -> {
					String username = params.get("username");
					String token = params.get("token");
					if(credentialsBlankOrNull(username, token) || (loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token))) {
						response.put("details", "Not authorized");
						attachJson("401", response);
					}
					else {
						loginHandler.renewSession(username);
						response.put("details", "Session renewed");
						response.put("token", "True");
						attachJson("200", response);
					}
				}

				case LOGOUT -> {
					String username = params.get("username");
					String token = params.get("token");
					String currentWord = wordFactory.getCurrentWord();
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)) {
						response.put("details", "Not authorized");
						attachJson("401", response);
					}
					else {
						loginHandler.removeSession(username);
						response.put("details", "Logout successful");
						if (database.isPlaying(username, currentWord)) {
							database.closeGame(username, currentWord);
							// If I close a game before winning, the streak is interrupted
							if (!database.isGameWon(username, currentWord)) database.resetUserStreaks(username);
						}
						attachJson("200", response);
					}
				}

				case PLAY -> {
					String username = params.get("username");
					String token = params.get("token");
					String currentWord = wordFactory.getCurrentWord();

					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)) {
						response.put("details", "Not authorized");
						attachJson("401", response);
					} else if (!database.gameExists(username, currentWord)) {
						if(database.insertGame(username, currentWord)) {
							response.put("details", "Game started!");
							response.put("wordId", database.getWordId(currentWord));
							attachJson("200", response);
						}
						else {
							response.put("details", "Internal server error");
							attachJson("500", response);
						}
					} else{
						if (!database.isGameClosed(username, currentWord)) {
							response.put("details", "Bad request - Game already started");
							response.put("wordId", database.getWordId(currentWord));
						} else {
							response.put("details", "Bad request - Game closed");
							response.put("victory", database.isGameWon(username, currentWord));
						}
						attachJson("400", response);
					}
				}
				// Used by the bew app
				case GET_GAME_STATUS -> {
					String username = params.get("username");
					String token = params.get("token");
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)) {
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					boolean isPlaying =  database.isPlaying(username, wordFactory.getCurrentWord());
					response.put("details", "");
					response.put("isPlaying", isPlaying);
					response.put("wordId", isPlaying ? database.getWordId(wordFactory.getCurrentWord()) : -1);
					attachJson("200", response);
				}

				case WORD_TIMER -> {
					String username = params.get("username");
					String token = params.get("token");
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					response.put("details", "");
					response.put("time", wordFactory.getTimeUntilNewWord());
					attachJson("200", response);
				}

				// Returns all the guesses and the hints of the game identified by wordId
				case GET_GAME_HISTORY -> {
					String username = params.get("username");
					String token = params.get("token");
					int wordId;
					try {
						wordId = Integer.parseInt(params.get("wordId"));
					} catch (Exception ignored) {
						response.put("details", "wordId not valid");
						attachJson("400", response);
						break;
					}
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					String guesses = database.getGuessesHistory(username, wordId);
					String hints = database.getHintsHistory(username, wordId);
					response.put("details", "");
					response.put("guesses", guesses);
					response.put("hints", hints);

					attachJson("200",response);
				}

				case SEND_WORD -> {
					String username = params.get("username");
					String token = params.get("token");
					String sentWord = params.get("word");
					String currentWord = wordFactory.getCurrentWord();
					int wordId;
					try {
						wordId = Integer.parseInt(params.get("wordId"));
					} catch (Exception ignored) {
						response.put("details", "WordId not valid");
						response.put("code", 100);
						attachJson("400", response);
						break;
					}
					int userGuesses = database.getGuessesNumber(username, wordId);
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)) {
						response.put("details", "Not authorized");
						attachJson("401", response);

					}
					else if(wordId != database.getWordId(currentWord)) {
						response.put("details", "Word changed, start a new game");
						response.put("code", 100);
						attachJson("400", response);
					}
					else if (!database.gameExists(username, wordFactory.getCurrentWord())) {
						response.put("details", "Bad request - Start a game first");
						response.put("code", 200);
						attachJson("400", response);
					}
					else if (database.isGameWon(username, wordFactory.getCurrentWord())){
						response.put("details", "Bad request - Game already won");
						response.put("code", 300);
						attachJson("400", response);
					}
					else if (userGuesses < 12){
						if(sentWord.equals(currentWord)) {
							String translatedWord;
							try {
								HashMap<String, Object> translationResponse = httpGet(String.format("https://api.mymemory.translated.net/get?q=%s&langpair=en|it", currentWord));
								translatedWord = (String) ((HashMap<?, ?>) translationResponse.get("responseData")).get("translatedText");
							} catch (Exception ignored) {
								translatedWord = "-";
							}
							response.put("translatedWord", translatedWord);
							response.put("details", "Victory!");
							response.put("hint", wordFactory.getHint(sentWord));
							response.put("victory", true);
							response.put("wordExists", true);
							database.setUserVictory(username, sentWord);
							database.incrementGameGuesses(username, currentWord, sentWord, String.valueOf(wordFactory.getHint(sentWord)));
						} else if (!wordFactory.wordExists(sentWord)) {
							response.put("details", "Try again!");
							response.put("hint", wordFactory.getHint(sentWord));
							response.put("victory", false);
							response.put("wordExists", false);
						}
						else {
							response.put("details", "Try again!");
							response.put("hint", wordFactory.getHint(sentWord));
							response.put("victory", false);
							response.put("wordExists", true);
							database.incrementGameGuesses(username, currentWord, sentWord, String.valueOf(wordFactory.getHint(sentWord)));
						}
						if (database.getGuessesNumber(username, wordId) == 12) {
							String translatedWord;
							try {
								HashMap<String, Object> translationResponse = httpGet(String.format("https://api.mymemory.translated.net/get?q=%s&langpair=en|it", currentWord));
								translatedWord = (String) ((HashMap<?, ?>) translationResponse.get("responseData")).get("translatedText");
							} catch (Exception ignored) {
								translatedWord = "-";
							}
							response.put("translatedWord", translatedWord);
							response.put("details", "Finished guesses");
							response.put("remainingGuesses", 0);
							attachJson("400", response);
							database.closeGame(username, currentWord);
							database.resetUserStreaks(username);
						} else {
							response.put("remainingGuesses", 12 - database.getGuessesNumber(username, wordId));
							attachJson("200", response);
						}
					} else {
						String translatedWord;
						try {
							HashMap<String, Object> translationResponse = httpGet(String.format("https://api.mymemory.translated.net/get?q=%s&langpair=en|it", currentWord));
							translatedWord = (String) ((HashMap<?, ?>) translationResponse.get("responseData")).get("translatedText");
						} catch (Exception ignored) {
							translatedWord = "-";
						}
						response.put("translatedWord", translatedWord);
						response.put("details", "Finished guesses");
						response.put("remainingGuesses", 0);
						attachJson("400", response);
						database.resetUserStreaks(username);
					}
				}

				case GET_STATISTICS -> {
					String username = params.get("username");
					String token = params.get("token");
					HashMap<String, Object> statistics;
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}

					statistics = database.getUserStatistics(username);
					if (statistics != null){
						attachJson("200", database.getUserStatistics(username));
						response = statistics;
					}
					else {
						response.put("details", "Internal error");
						attachJson("500", response);
					}
				}

				case GET_RANKING -> {
					String username = params.get("username");
					String token = params.get("token");
					List<String> ranking;
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					ranking = database.getRanking();
					if (ranking != null){
						response.put("ranking", ranking);
						attachJson("200", response);
					}
					else {
						response.put("details", "Internal error");
						attachJson("500", response);
					}
				}

				case SHARE -> {
					String username = params.get("username");
					String token = params.get("token");
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					int wordId;
					try {
						wordId = Integer.parseInt(params.get("wordId"));
					} catch (Exception ignored) {
						response.put("details", "WordId not valid");
						attachJson("400", response);
						break;
					}
					String word = database.getWord(wordId);
					if (!word.equals("") && !database.gameExists(username, word) && !database.isGameClosed(username, word)) {
						response.put("details", "WordId not valid");
						attachJson("400", response);
						break;
					}

					DatagramSocket socket = new DatagramSocket();
					String gameHints = database.getHintsHistory(username, wordId);
					int remainingGuesses = database.getGuessesNumber(username, wordId);
					boolean won = database.isGameWon(username, word);
					response.put("username", username);
					response.put("hints", gameHints);
					response.put("remainingGuesses", remainingGuesses);
					response.put("won", won);
					ObjectMapper mapper = new ObjectMapper();
					String serializedMessage = mapper.writeValueAsString(response);
					response.clear();
					byte[] buffer = serializedMessage.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(config.multicastAddress()), config.multicastPort());
					socket.send(packet);
					response.put("details", "Success");
					attachJson("200", response);
				}

				// Used by the CLI client to get the multicast parameters in order to join the multicast group
				case GET_MULTICAST -> {
					String username = params.get("username");
					String token = params.get("token");
					if(credentialsBlankOrNull(username, token) || loginHandler.getUserSession(username) == null || !loginHandler.getUserSession(username).getToken().equals(token)){
						response.put("details", "Not authorized");
						attachJson("401", response);
						break;
					}
					response.put("multicastIp", config.multicastAddress());
					response.put("multicastPort", config.multicastPort());
					attachJson("200", response);
				}
				case GET_CURRENT_WORD -> {
					response.put("currentWord", wordFactory.getCurrentWord());
					attachJson("200", response);
				}
			}

			if (endpoint != Endpoint.OPTIONS && config.verbose()) printRequestResponse(String.valueOf(endpoint), request, params, response);
		} catch (Exception e) {
			try {
				HashMap<String, Object> response = new HashMap<>();
				response.put("details", "Internal server error");
				attachJson("500", response);
				PrettyPrinter.prettyPrintln("[ @RException@0 -> @M"+ e.getStackTrace()[0].getFileName() + ":@C" + e.getStackTrace()[0].getLineNumber() + "@0 ] - " + e.getMessage());
				e.printStackTrace();
			} catch (IOException ignored) {}
		}
	}

	@Override
	public void run() {
		try{
			BufferedReader reader = new BufferedReader(new StringReader(requestString));
			String request = reader.readLine();
			StringBuilder headerBuilder = new StringBuilder();
			String tmp;
			StringBuilder bodyBuilder = new StringBuilder();
			if (request == null || request.contains("/favicon.ico")) return;

			while ((tmp = reader.readLine()) != null && !tmp.equals("")) {
					headerBuilder.append(tmp).append("\n");
			}
			if (request.contains("POST")) {
				while((tmp = reader.readLine()) != null) {
					bodyBuilder.append(tmp);
				}
			}
			String body = bodyBuilder.toString();
			String header = headerBuilder.substring(0, headerBuilder.length()-1);
			handleRequest(request, header, body);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private static HashMap<String, Object> httpGet(String host) throws IOException {
		URL url = new URL(host);
		ObjectMapper mapper = new ObjectMapper();
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
		String response = reader.readLine();
		@SuppressWarnings("unchecked")
		HashMap<String, Object> responseMap = mapper.readValue(response, HashMap.class);
		reader.close();
		return responseMap;
	}

	public static void sendAttachment(SelectionKey key) {
		SocketChannel com = (SocketChannel) key.channel();
		String response = (String) key.attachment();
		ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
		try {
			do{
				com.write(responseBuffer);
			} while (responseBuffer.hasRemaining());
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			try {
				PrettyPrinter.prettyPrintln("[ @RError thread " + Thread.currentThread().getName().toUpperCase() + "@0 ] - IOException thrown while responding to" + (com.getRemoteAddress().toString().substring(1)));
				com.close();
				key.cancel();
			} catch (Exception ignored) {}
		}
	}

	private String generateSessionToken(String username) {
		SecureRandom random = new SecureRandom();
		MessageDigest digest = null;
		byte[] hash;
		String hashString;
		String time = String.valueOf(System.currentTimeMillis());
		String rand = String.valueOf((int) (random.nextDouble()*100000));
		String toHash = time + username + rand;
		int low, high;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ignored) {}
		if (digest != null){
			hash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder(2 * hash.length);
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			hashString = hexString.toString();
			low = random.nextInt((hashString.length() - 1)/ 2);
			high = low + (hashString.length() / 2);
			return hashString.substring(low, high);
		}
		return "";
	}

	private HashMap<String,String> getParams(String requestParams, String header, String bodyParams) {
		HashMap<String, String> requestParamsMap = new HashMap<>();
		HashMap<String, String> bodyParamsMap = new HashMap<>();
		HashMap<String, String> params = new HashMap<>();

		String queryString = requestParams.substring(requestParams.indexOf("?") + 1);
		String[] splitParams = queryString.split("&");
		for (String param: splitParams) {
			if (param.split("=").length == 2)
				requestParamsMap.put(param.split("=")[0], URLDecoder.decode(param.split("=")[1], StandardCharsets.UTF_8));
		}
		splitParams = header.split("\n");
		for (String param: splitParams) if (param.toLowerCase().contains("authorization:")) requestParamsMap.put("token", URLDecoder.decode(param.split(": ")[1].replaceAll("Bearer ", ""), StandardCharsets.UTF_8));
		splitParams = bodyParams.split("&");
		for (String param: splitParams) {
			if (param.split("=").length == 2)
				bodyParamsMap.put(param.split("=")[0], URLDecoder.decode(param.split("=")[1], StandardCharsets.UTF_8));
		}
		params.putAll(requestParamsMap);
		params.putAll(bodyParamsMap);
		return params;
	}
	private Endpoint getEndpoint(String request, HashMap<String, String> params) {
		String action;
		String requestType = request.split(" ")[0];

		try {
			String rawAction = request.split(" ")[1].split("/")[1];
			if (rawAction.contains("?"))
				action = rawAction.substring(0, rawAction.indexOf("?"));
			else action = rawAction;
		} catch (Exception ignored) { return Endpoint.NOT_SUPPORTED; }

		if (requestType.equals("OPTIONS")) return Endpoint.OPTIONS;

		if (action.equals("register") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("password")) return Endpoint.REGISTER;

		if (action.equals("login") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("password")) return Endpoint.LOGIN;

		if (action.equals("verify") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.VERIFY;

		if (action.equals("logout") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.LOGOUT;

		if (action.equals("playWordle") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.PLAY;

		if(action.equals("sendWord") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("token") && params.containsKey("word") && params.containsKey("wordId")) return Endpoint.SEND_WORD;

		if (action.equals("getGameStatus") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.GET_GAME_STATUS;

		if (action.equals("getGameHistory") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token") && params.containsKey("wordId")) return Endpoint.GET_GAME_HISTORY;

		if(action.equals("showMeRanking") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.GET_RANKING;

		if(action.equals("getMulticast") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.GET_MULTICAST;

		if (action.equals("wordTimer") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.WORD_TIMER;

		if(action.equals("sendMeStatistics") && requestType.equals("GET"))
			if (params.containsKey("username") && params.containsKey("token")) return Endpoint.GET_STATISTICS;

		if(action.equals("share") && requestType.equals("POST"))
			if (params.containsKey("username") && params.containsKey("token") && params.containsKey("wordId")) return Endpoint.SHARE;

		if(action.equals("getCurrentWord") && requestType.equals("GET"))
			if(config.debug()) return Endpoint.GET_CURRENT_WORD;


		return Endpoint.NOT_SUPPORTED;
	}
	private void attachJson(String status, HashMap<String, Object> response) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String serializedOutput;
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		serializedOutput = mapper.writeValueAsString(response);
		String responseString = String.format("""
				HTTP/1.1 %s OK\r
				Server: wordle-project\r
				Date: %s\r
				Access-Control-Allow-Origin: *\r
				Access-Control-Allow-Methods: *\r
				Access-Control-Allow-Headers: *\r
				Content-Type: application/json; charset=utf-8\r
				Content-Length: %d\r
				\r
				%s\r
				\r
								
				""", status, new Date(), serializedOutput.length(), serializedOutput);
		key.attach(responseString);
		key.interestOps(SelectionKey.OP_WRITE);
		selector.wakeup();
	}
}
