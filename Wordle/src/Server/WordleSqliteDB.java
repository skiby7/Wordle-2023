package Server;

import CommonUtils.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

public class WordleSqliteDB {
	private static String bytesToHex(byte[] hash) {
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

	public static WordleDB.Authorization validateUser(Connection db, String username, String password) throws NoSuchAlgorithmException, SQLException {
		if (!userExists(db, username)) return  WordleDB.Authorization.NOT_AUTHORIZED;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String hashedPasswd, salt, role;
		MessageDigest digest;
		WordleDB.Authorization authLevel =  WordleDB.Authorization.NOT_AUTHORIZED;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			String query = "SELECT hashed_password, role FROM users WHERE username = ?";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			resultSet.next();
			hashedPasswd = resultSet.getString(1);
			role = resultSet.getString(2);
			salt = hashedPasswd.split(":")[0];
			hashedPasswd = hashedPasswd.split(":")[1];
			if(hashedPasswd.equals(bytesToHex(digest.digest((salt + password).getBytes(StandardCharsets.UTF_8))))) {
				switch (role){
					case "admin" -> authLevel =  WordleDB.Authorization.AUTHORIZED_ADMIN;
					case "user" -> authLevel =  WordleDB.Authorization.AUTHORIZED_USER;
				}
			}
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return authLevel;
	}

	public static boolean userExists(Connection db, String username) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			String query = "SELECT COUNT(*) FROM users WHERE username = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			resultSet.next();
			if (resultSet.getInt(1) == 0) return false;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean wordAlreadyExtracted(Connection db, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			String query = "SELECT COUNT(*) FROM words_extracted WHERE word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, word);
			resultSet = statement.executeQuery();
			resultSet.next();
			if (resultSet.getInt(1) == 0) return false;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean gameExists(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			String query = "SELECT COUNT(*) FROM games WHERE username = ? AND word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			resultSet = statement.executeQuery();
			resultSet.next();
			if (resultSet.getInt(1) == 0) return false;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean isGameWon(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			String query = "SELECT COUNT(*) FROM games WHERE username = ? AND word = ? AND won = 1;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			resultSet = statement.executeQuery();
			resultSet.next();
			if (resultSet.getInt(1) == 0) return false;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean insertUser(Connection db, String username, String password, String role) throws SQLException, NoSuchAlgorithmException {
		if (username.isBlank() || password.isBlank() || role.isBlank()) return false;
		if (userExists(db, username)) return false;
		PreparedStatement statement = null;
		MessageDigest digest = null;
		byte[] hash;
		SecureRandom random = new SecureRandom();
		String salt = String.valueOf((int) (random.nextDouble()*100000));
		try {
			digest = MessageDigest.getInstance("SHA-256");
			String query = "INSERT INTO users (username, hashed_password, role) VALUES (?,?,?)";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, salt + ":" + bytesToHex(digest.digest((salt + password).getBytes(StandardCharsets.UTF_8))));
			statement.setString(3, role);
			statement.executeUpdate();
			statement.close();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean insertGame(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		try {
			String query = "INSERT INTO games (username, word, guesses) VALUES (?,?,0)";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			statement.executeUpdate();
			statement.close();
			query = "UPDATE users SET last_game_timestamp = (datetime('now')),  games_played = games_played + 1 WHERE username = ?";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
		return true;
	}


	public static boolean resetStreaks(Connection db, String word) throws SQLException {
		PreparedStatement statement = null;
		try {
			// users that tried to guess {word} but lost
			String query = "UPDATE users SET current_streak = 0 WHERE username IN (SELECT username FROM games WHERE word = ? AND won = 0);";
			statement = db.prepareStatement(query);
			statement.setString(1, word);
			statement.executeUpdate();
			// users that haven't played {word}
			query = "UPDATE users SET current_streak = 0 WHERE username NOT IN (SELECT username FROM games WHERE word = ?);";
			statement = db.prepareStatement(query);
			statement.setString(1, word);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
		return true;
	}
	public static boolean resetUserStreaks(Connection db, String username) throws SQLException {
		PreparedStatement statement = null;
		try {
			String query = "UPDATE users SET current_streak = 0 WHERE username = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static boolean isGameClosed(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		boolean result;
		try {
			String query = "SELECT closed FROM games WHERE username = ? AND word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			resultSet = statement.executeQuery();
			result = resultSet.getInt(1) == 1;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return result;
	}

	public static void closeGame(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		try {
			String query = "UPDATE games SET closed = 1 WHERE username = ? AND word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
	}

	public static void setUserVictory(Connection db, String username, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int currentStreak, longestStreak;
		try {
			String query = "UPDATE games SET won = 1, closed = 1 WHERE username = ? AND word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			statement.executeUpdate();
			statement.close();
			query = "SELECT current_streak, longest_streak FROM users WHERE username = ?";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			currentStreak = resultSet.getInt(1) + 1;
			longestStreak = resultSet.getInt(2);
			query = "UPDATE users SET current_streak = ? WHERE username = ?;";
			statement = db.prepareStatement(query);
			statement.setInt(1, currentStreak);
			statement.setString(2, username);
			statement.executeUpdate();
			statement.close();
			if (currentStreak > longestStreak) {
				query = "UPDATE users SET longest_streak = ? WHERE username = ?;";
				statement = db.prepareStatement(query);
				statement.setInt(1, currentStreak);
				statement.setString(2, username);
				statement.executeUpdate();
				statement.close();
			}
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
	}

	public static void incrementGameGuesses(Connection db, String username, String word, String guess, String hint) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int currentGuesses;
		String guessesHistory, hintsHistory;
		try {
			String query = "SELECT guesses, guesses_history, hints_history FROM games WHERE username = ? AND word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, word);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				currentGuesses = resultSet.getInt(1);
				guessesHistory = resultSet.getString(2);
				hintsHistory = resultSet.getString(3);
				statement.close();
				if (currentGuesses == 12) return;
				if (guessesHistory == null || guessesHistory.equals("")) guessesHistory = guess;
				else guessesHistory += String.format(":%s", guess);
				if (hintsHistory == null || hintsHistory.equals("")) hintsHistory = hint;
				else hintsHistory += String.format(":%s", hint);
				query = "UPDATE games SET guesses = guesses + 1, guesses_history = ?, hints_history = ? WHERE username = ? AND word = ?;";
				statement = db.prepareStatement(query);
				statement.setString(1, guessesHistory);
				statement.setString(2, hintsHistory);
				statement.setString(3, username);
				statement.setString(4, word);
				statement.executeUpdate();
			}
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
	}

	public static String getGuessesHistory(Connection db, String username, int wordId) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String guesses = "";
		try {
			String query = "SELECT guesses_history FROM games WHERE username = ? AND word = (SELECT word from words_extracted WHERE ID = ?);";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setInt(2, wordId);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				guesses = resultSet.getString(1);
				guesses = guesses == null ? "" : guesses;
			}
			statement.close();
			resultSet.close();
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return guesses;
	}
	public static String getHintsHistory(Connection db, String username, int wordId) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String hints = "";
		try {
			String query = "SELECT hints_history FROM games WHERE username = ? AND word = (SELECT word from words_extracted WHERE ID = ?);";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setInt(2, wordId);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				hints = resultSet.getString(1);
				hints = hints == null ? "" : hints;
			}
			statement.close();
			resultSet.close();
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return hints;
	}

	public static HashMap<String, Object> getUserStatistics(Connection db, String username) throws SQLException {
		int gamesPlayed = 0, lastStreak = 0, maxStreak = 0, gamesWon = 0;
		double gamesWonPct = 0.0;
		int[] guessDistribution = new int[12];
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		HashMap<String, Object> result = new HashMap<>();
		try {
			String query = "SELECT COUNT(*) FROM games WHERE username = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				gamesPlayed = resultSet.getInt(1);
			}
			statement.close();
			resultSet.close();

			query = "SELECT current_streak, longest_streak FROM users WHERE username = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				lastStreak = resultSet.getInt(1);
				maxStreak = resultSet.getInt(2);
			}
			statement.close();
			resultSet.close();

			query = "SELECT COUNT(*) FROM games WHERE username = ? AND won = 1;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				gamesWon = resultSet.getInt(1);
				gamesWonPct = gamesPlayed != 0 ? (double) gamesWon/gamesPlayed : 0;
			}
			statement.close();
			resultSet.close();

			query = """
					SELECT username, guesses, count(username) FROM games
					WHERE won = 1 AND username = ?
					GROUP BY username, guesses
					ORDER BY guesses;
					""";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			resultSet = statement.executeQuery();

			while (resultSet.next())
				guessDistribution[resultSet.getInt(2) - 1] = resultSet.getInt(3);

		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		result.put("details", "");
		result.put("gamesPlayed", gamesPlayed);
		result.put("gamesWonPct", gamesWonPct);
		result.put("lastStreak", lastStreak);
		result.put("maxStreak", maxStreak);
		result.put("guessDistribution", guessDistribution);
		return result;
	}

	public static int getGuessesNumber(Connection db, String username, int wordId) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int currentGuesses = -1;
		try {
			String query = "SELECT guesses FROM games WHERE username = ? AND word = (SELECT word FROM words_extracted WHERE ID = ?);";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setInt(2, wordId);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				currentGuesses = resultSet.getInt(1);
			} else { return currentGuesses; }
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return currentGuesses;
	}

	public static boolean insertExtractedWord(Connection db, String word) throws SQLException {
		if (wordAlreadyExtracted(db, word)) return false;
		PreparedStatement statement = null;
		try {
			String query = "INSERT INTO words_extracted (word) VALUES (?)";
			statement = db.prepareStatement(query);
			statement.setString(1, word);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (Exception ignored) {}
		}
		return true;
	}

	public static String getWord(Connection db, int wordId) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String word = "";
		try {
			String query = "SELECT word FROM words_extracted WHERE ID = ?;";
			statement = db.prepareStatement(query);
			statement.setInt(1, wordId);
			resultSet = statement.executeQuery();
			if (resultSet.next()) word = resultSet.getString(1);
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return word;
	}

	public static int getWordId(Connection db, String word) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int ID = -1;
		try {
			String query = "SELECT ID FROM words_extracted WHERE word = ?;";
			statement = db.prepareStatement(query);
			statement.setString(1, word);
			resultSet = statement.executeQuery();
			if (resultSet.next()) ID = resultSet.getInt(1);
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return ID;
	}
	public static boolean isPlaying(Connection db, String username, String currentWord) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		boolean userIsPlaying = false;
		try {
			String query = "SELECT COUNT(*) FROM games WHERE username = ? AND word = ? AND closed = 0;";
			statement = db.prepareStatement(query);
			statement.setString(1, username);
			statement.setString(2, currentWord);
			resultSet = statement.executeQuery();
			if (resultSet.next()) userIsPlaying = resultSet.getInt(1) != 0;
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return userIsPlaying;
	}

	/**
	 * @author Matteo Loporchio
	 */
	private static double computeScore(int numPlayed, int[] guessDist) {
		int sum = 0, numGuessed = 0;
		for (int i = 0; i < guessDist.length; i++) {
			sum += (i + 1) * guessDist[i];
			numGuessed += guessDist[i];
		}
		sum += (12 + 1) * (numPlayed - numGuessed);
		return ((double) sum / (double) numPlayed);
	}
	public static LinkedList<String> getRanking(Connection db) throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		LinkedList<String> users = new LinkedList<>();
		LinkedList<Pair<String, Double>> ranking = new LinkedList<>();
		int correctionFactor = 0;
		try {
			String query = """
					SELECT username FROM users;
					""";
			statement = db.prepareStatement(query);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				users.add(resultSet.getString(1));
			}

			statement.close();
			resultSet.close();
			for (String user: users) {
				HashMap<String, Object> userStatistics = WordleSqliteDB.getUserStatistics(db, user);
				double score =  computeScore((int) userStatistics.get("gamesPlayed"), (int[]) userStatistics.get("guessDistribution"));
				if (Double.isNaN(score)) score = Double.MAX_VALUE;
				ranking.add(new Pair<>(user, score));
			}
			// lower is better
			ranking.sort((p1, p2) -> (int) (p1.b() - p2.b()));
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();
			} catch (Exception ignored) {}
		}
		return new LinkedList<>(ranking.stream().limit(10).map(Pair::a).toList());
	}

	public static void checkDatabase(Connection db, String path, boolean autoCreateDatabase) throws IOException, SQLException, NoSuchAlgorithmException, ClassNotFoundException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		if (autoCreateDatabase) {
			File dbFile = new File(path);
			if(!(dbFile.exists() && dbFile.isFile())){
				dbFile.getParentFile().mkdirs();
				if(!dbFile.createNewFile()) {
					throw new IOException("Cannot create database " + path);
				}
			}
		}
		try {
			Class.forName("org.sqlite.JDBC");
			String connectionUrl = "jdbc:sqlite:" + path;
			db = DriverManager.getConnection(connectionUrl);
			String query = """
					CREATE TABLE IF NOT EXISTS
						users ( ID INTEGER PRIMARY KEY AUTOINCREMENT,
								username TEXT NOT NULL,
								hashed_password TEXT NOT NULL,
								role VARCHAR(20) DEFAULT 'user',
								subscription_date datetime DEFAULT (datetime('now')),
								last_game_timestamp datetime DEFAULT NULL,
								games_played INTEGER DEFAULT 0,
								current_streak INTEGER DEFAULT 0,
								longest_streak INTEGER DEFAULT 0
								);
					""";
			statement = db.prepareStatement(query);
			statement.executeUpdate();
			statement.close();
			query = """
					SELECT COUNT(*) FROM users;
					""";
			statement = db.prepareStatement(query);
			resultSet = statement.executeQuery();
			resultSet.next();
			int totalUsers = resultSet.getInt(1);
			resultSet.close();
			statement.close();
			if (totalUsers == 0)
				insertUser(db, "admin", "changeme", "admin");

			query = """
					CREATE TABLE IF NOT EXISTS
								games ( ID INTEGER PRIMARY KEY AUTOINCREMENT,
											username TEXT NOT NULL,
											word TEXT NOT NULL,
											guesses INTEGER NOT NULL,
											won INTEGER NOT NULL DEFAULT 0,
											guesses_history TEXT DEFAULT NULL,
											hints_history TEXT DEFAULT NULL,
											game_date datetime DEFAULT (datetime('now')),
											closed INTEGER NOT NULL DEFAULT 0
											);
					""";
			statement = db.prepareStatement(query);
			statement.executeUpdate();
			query = """
					CREATE TABLE IF NOT EXISTS
								words_extracted ( ID INTEGER PRIMARY KEY AUTOINCREMENT,
											word TEXT NOT NULL,
											date datetime DEFAULT (datetime('now'))
											);
					""";
			statement = db.prepareStatement(query);
			statement.executeUpdate();
		} finally {
			try {
				if (statement != null) statement.close();
				if (resultSet != null) resultSet.close();

			} catch (Exception ignored) {}
		}
	}
}
