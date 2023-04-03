package Server;

import Models.*;
import CommonUtils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class WordleJsonDB {
	private final ReentrantLock userFileLock;
	private final ReentrantLock gamesFileLock;
	private final ReentrantLock wordsFileLock;
	private final String databasePath;
	private final boolean autoCreateDatabase;
	private volatile ConcurrentHashMap<String, UserModel> userTable;
	private volatile ConcurrentHashMap<StringPair, GameModel> gamesTable;
	private volatile ConcurrentHashMap<String, WordsExtractedModel> wordsExtractedTable;
	private final boolean autoCommit;

	public WordleJsonDB(String databasePath, boolean autoCreateDatabase, boolean autoCommit){
		this.databasePath = databasePath;
		this.autoCreateDatabase = autoCreateDatabase;
		this.autoCommit = autoCommit;
		this.userTable = new ConcurrentHashMap<>();
		this.gamesTable = new ConcurrentHashMap<>();
		this.wordsExtractedTable = new ConcurrentHashMap<>();
		this.userFileLock = new ReentrantLock();
		this.gamesFileLock = new ReentrantLock();
		this.wordsFileLock = new ReentrantLock();
	}

	private String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	public void writeUsersToDisk() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		userFileLock.lock();
		try {
			mapper.writeValue(new File(databasePath + "/users.json"), userTable);
		} finally {
			if (userFileLock.isLocked()) userFileLock.unlock();
		}
	}

	public void writeGamesToDisk() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		gamesFileLock.lock();
		try {
			mapper.writeValue(new File(databasePath + "/games.json"), gamesTable);
		} finally {
			if (gamesFileLock.isLocked()) gamesFileLock.unlock();
		}
	}

	public void writeWordsToDisk() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		wordsFileLock.lock();
		try {
			mapper.writeValue(new File(databasePath + "/words.json"), wordsExtractedTable);
		} finally {
			if (wordsFileLock.isLocked()) wordsFileLock.unlock();
		}
	}

	public WordleDB.Authorization validateUser(String username, String password) throws NoSuchAlgorithmException {
		if (!userExists(username)) return WordleDB.Authorization.NOT_AUTHORIZED;
		String hashedPasswd, salt, role;
		MessageDigest digest;
		WordleDB.Authorization authLevel = WordleDB.Authorization.NOT_AUTHORIZED;
		UserModel user = userTable.get(username);
		digest = MessageDigest.getInstance("SHA-256");
		hashedPasswd = user.getHashedPassword();
		role = user.getRole();
		salt = hashedPasswd.split(":")[0];
		hashedPasswd = hashedPasswd.split(":")[1];
		if(hashedPasswd.equals(bytesToHex(digest.digest((salt + password).getBytes(StandardCharsets.UTF_8))))) {
			switch (role){
				case "admin" -> authLevel = WordleDB.Authorization.AUTHORIZED_ADMIN;
				case "user" -> authLevel = WordleDB.Authorization.AUTHORIZED_USER;
			}
		}
		return authLevel;
	}

	public boolean userExists(String username) {
		return userTable.containsKey(username);
	}

	public boolean wordAlreadyExtracted(String word) {
		return wordsExtractedTable.containsKey(word);
	}

	public boolean gameExists(String username, String word) { return gamesTable.containsKey(new StringPair(username, word)); }

	public boolean isGameWon(String username, String word) {
		StringPair key = new StringPair(username, word);
		if(!gamesTable.containsKey(key)) return false;
		return gamesTable.get(key).isWon();
	}

	public boolean insertUser(String username, String password, String role) throws IOException, NoSuchAlgorithmException {
		if (username.isBlank() || password.isBlank() || role.isBlank()) return false;
		MessageDigest digest = null;
		byte[] hash;
		SecureRandom random = new SecureRandom();
		String salt = String.valueOf((int) (random.nextDouble()*100000));
		String hashedPassword = "";
		digest = MessageDigest.getInstance("SHA-256");
		hashedPassword = salt + ":" + bytesToHex(digest.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
		if (userTable.putIfAbsent(username , new UserModel(
				userTable.size() + 1,
				username,
				hashedPassword,
				role)) != null) return false;
		if (autoCommit) writeUsersToDisk();
		return true;
	}

	public boolean insertGame(String username, String word) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		StringPair key = new StringPair(username, word);
		if (gamesTable.putIfAbsent(key, new GameModel(
				gamesTable.size() + 1,
				username,
				word)) != null) return false;
		userTable.get(username).setLastGameTimestamp(new Timestamp(System.currentTimeMillis()));
		if (autoCommit) writeGamesToDisk();
		return true;
	}

	public boolean insertExtractedWord(String word) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		if (wordsExtractedTable.putIfAbsent(word ,new WordsExtractedModel(
				wordsExtractedTable.size() + 1,
				System.currentTimeMillis(),
				word)) != null) return false;
		if (autoCommit) writeWordsToDisk();
		return true;
	}

	public boolean isGameClosed(String username, String word) {
		StringPair key = new StringPair(username, word);
		return gamesTable.get(key).isClosed();
	}

	public void closeGame(String username, String word) throws IOException {
		StringPair key = new StringPair(username, word);
		gamesTable.get(key).setClosed(true);
		if (autoCommit) writeGamesToDisk();
	}

	public void resetStreaks(String word) throws IOException {
		// getting the username of the users that have tried to guess {word} but lost
		List<String> usersToReset = gamesTable.keySet().stream().filter(s -> gamesTable.get(s).getWord().equals(word) && !gamesTable.get(s).isWon()).map(StringPair::a).toList();
		for (String user: usersToReset)
			userTable.get(user).setCurrentStreak(0);

		// getting the username of the users that have played {word}
		usersToReset = gamesTable.keySet().stream().filter(s -> gamesTable.get(s).getWord().equals(word)).map(StringPair::a).toList();
		// calculating the difference between all users and the one who have tried to guess {word}
		List<String> diff = new LinkedList<>(userTable.keySet().stream().toList());
		diff.removeAll(usersToReset);
		// diff now contains users that haven't tried to guess {word}
		for (String user: diff)
			userTable.get(user).setCurrentStreak(0);
		if (autoCommit) writeUsersToDisk();
	}
	public void resetUserStreaks(String username) throws IOException {
		userTable.get(username).setCurrentStreak(0);
		if (autoCommit) writeUsersToDisk();
	}

	public void setUserVictory(String username, String word) throws IOException {
		StringPair key = new StringPair(username, word);
		gamesTable.get(key).setWon(true);
		gamesTable.get(key).setClosed(true);
		if (autoCommit) writeGamesToDisk();
		userTable.get(username).incrementCurrentStreak();
		if (userTable.get(username).getCurrentStreak() > userTable.get(username).getLongestStreak())
			userTable.get(username).incrementLongestStreak();
		if (autoCommit) writeUsersToDisk();
	}

	public void incrementGameGuesses(String username, String word, String guess, String hint) throws IOException {
		StringPair key = new StringPair(username, word);
		if(gamesTable.get(key).getGuesses() == 12) return;
		gamesTable.get(key).incrementGuesses();
		String guessesHistory = gamesTable.get(key).getGuessesHistory();
		String hintsHistory = gamesTable.get(key).getHintsHistory();
		if(guessesHistory == null || guessesHistory.equals(""))
			gamesTable.get(key).setGuessesHistory(guess);
		else gamesTable.get(key).setGuessesHistory(guessesHistory + String.format(":%s", guess));
		if(hintsHistory == null || hintsHistory.equals(""))
			gamesTable.get(key).setHintsHistory(hint);
		else gamesTable.get(key).setHintsHistory(hintsHistory + String.format(":%s", hint));
		if (autoCommit) writeGamesToDisk();
	}

	public String getGuessesHistory(String username, int wordId) {
		String word = "";
		for (WordsExtractedModel item: wordsExtractedTable.values()) {
			if (item.getID() == wordId) {
				word = item.getWord();
				break;
			}
		}
		StringPair key = new StringPair(username, word);
		return gamesTable.get(key).getGuessesHistory();
	}

	public String getHintsHistory(String username, int wordId) {
		String word = "";
		for (WordsExtractedModel item: wordsExtractedTable.values()) {
			if (item.getID() == wordId) {
				word = item.getWord();
				break;
			}
		}
		StringPair key = new StringPair(username, word);
		return gamesTable.get(key).getHintsHistory();
	}

	public HashMap<String, Object> getUserStatistics(String username) {
		int gamesPlayed = 0, lastStreak = 0, maxStreak = 0, gamesWon = 0;
		double gamesWonPct = 0.0;
		int[] guessDistribution = getGuessDistribution(username);
		HashMap<String, Object> result = new HashMap<>();
		List<String> wordsPlayed = gamesTable.keySet().stream().filter(s -> s.a().equals(username)).map(StringPair::b).toList();
		gamesPlayed = wordsPlayed.size();
		lastStreak = userTable.get(username).getCurrentStreak();
		maxStreak = userTable.get(username).getLongestStreak();
		gamesWon = (int) gamesTable.keySet().stream().filter(s -> s.a().equals(username) && gamesTable.get(s).isWon()).count();
		gamesWonPct = gamesPlayed != 0 ? (double) gamesWon/gamesPlayed : 0;

		result.put("details", "");
		result.put("gamesPlayed", gamesPlayed);
		result.put("gamesWonPct", gamesWonPct);
		result.put("lastStreak", lastStreak);
		result.put("maxStreak", maxStreak);
		result.put("guessDistribution", guessDistribution);
		return result;
	}

	public int getGuessesNumber(String username, int wordId) {
		String word = getWord(wordId);
		StringPair key = new StringPair(username, word);
		return gamesTable.containsKey(key) ? gamesTable.get(key).getGuesses() : -1;
	}

	public int[] getGuessDistribution(String username) {
		int[] guessDistribution = new int[12];
		List<Integer> gamesPlayedList = new LinkedList<>(gamesTable.values().stream().filter(s -> s.getUsername().equals(username) && s.isWon()).map(GameModel::getGuesses).toList());
		for (Integer i : gamesPlayedList) guessDistribution[i-1]++;
		return guessDistribution;
	}

	public String getWord(int wordId) {
		return wordsExtractedTable.values().stream().filter(s -> s.getID() == wordId).findFirst().map(WordsExtractedModel::getWord).orElse("");
	}
	public int getWordId(String word) {
		return wordsExtractedTable.get(word).getID();
	}
	public boolean isPlaying(String username, String currentWord) {
		StringPair key = new StringPair(username, currentWord);
		return gamesTable.containsKey(key) && !gamesTable.get(key).isClosed();
	}

	/**
	 * @author Matteo Loporchio
	 */
	private double computeScore(int numPlayed, int[] guessDist) {
		int sum = 0, numGuessed = 0;
		for (int i = 0; i < guessDist.length; i++) {
			sum += (i + 1) * guessDist[i];
			numGuessed += guessDist[i];
		}
		sum += (12 + 1) * (numPlayed - numGuessed);
		return ((double) sum / (double) numPlayed);
	}

	public LinkedList<String> getRanking() {
		LinkedList<String> ranking;
		LinkedList<Pair<String, Double>> rankingTmp = new LinkedList<>();
		Set<String> users = userTable.keySet();
		for (String user: users) {
			HashMap<String, Object> statistics = getUserStatistics(user);
			double score = computeScore((int) statistics.get("gamesPlayed"), (int[]) statistics.get("guessDistribution"));
			if (Double.isNaN(score)) score = Double.MAX_VALUE; // if someone hasn't played at least one game, then it should be at the end of the ranking
			rankingTmp.add(new Pair<>(user, score));
		}
		// lower is better
		rankingTmp.sort((p1, p2) -> (int) (p1.b() - p2.b()));
		// returning top 10
		ranking = new LinkedList<>(rankingTmp.stream().limit(10).map(Pair::a).toList());
		return ranking;
	}

	private boolean checkDbFiles(File file) throws IOException {
		if(!file.exists()){
			if(autoCreateDatabase) return file.createNewFile();
			else return false;
		}
		return true;
	}

	public void checkDatabase() throws IOException, NoSuchAlgorithmException {
		File dbDirectory = new File(databasePath);
		if(!dbDirectory.exists()) {
			if(autoCreateDatabase) {
				dbDirectory.getParentFile().mkdirs();
				if (!dbDirectory.mkdir()) throw new IOException("Cannot create database");
			}
		}
		File userDb = new File(databasePath + "/users.json");
		File gamesDb = new File(databasePath + "/games.json");
		File wordsDb = new File(databasePath + "/words.json");
		if (!(checkDbFiles(userDb) && checkDbFiles(gamesDb) && checkDbFiles(wordsDb))){
			throw new IOException("Cannot create database");
		}

		ObjectMapper mapper = new ObjectMapper();
		// if the file is empty do not update the table
		if(userDb.length() != 0) userTable = mapper.readValue(userDb, mapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, UserModel.class));
		if(wordsDb.length() != 0) wordsExtractedTable = mapper.readValue(wordsDb, mapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, WordsExtractedModel.class));
		if(gamesDb.length() != 0) {
			SimpleModule module = new SimpleModule();
			module.addKeyDeserializer(StringPair.class, new StringPairDeserializer());
			mapper.registerModule(module);
			gamesTable = mapper.readValue(gamesDb, mapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, StringPair.class, GameModel.class));
		}
		insertUser("admin", "changeme", "admin");
	}
}
