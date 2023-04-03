package Server;

import CommonUtils.PrettyPrinter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordleDB {
	public enum DatabaseType {
		JSON,
		SQLITE
	}

	public enum Authorization {
		NOT_AUTHORIZED,
		AUTHORIZED_USER,
		AUTHORIZED_ADMIN
	}

	private final String databasePath;
	private final boolean autoCreateDatabase;
	private final boolean autoCommitDatabase;
	private final DatabaseType databaseType;
	private WordleJsonDB jsonDatabase;
	private Connection db;
	private Thread commitRoutine;

	public WordleDB(String databasePath, boolean autoCreateDatabase, boolean autoCommit, DatabaseType type) {
		this.databasePath = databasePath;
		this.databaseType = type;
		this.autoCreateDatabase = autoCreateDatabase;
		this.autoCommitDatabase = autoCommit;
		try {
			switch (type) {
				case JSON -> jsonDatabase = new WordleJsonDB(databasePath, autoCreateDatabase, autoCommit);
				case SQLITE -> {
					Class.forName("org.sqlite.JDBC");
					String connectionUrl = "jdbc:sqlite:" + databasePath;
					db = DriverManager.getConnection(connectionUrl);
					db.setAutoCommit(autoCommit);
				}
			}
			// this routine saves the state of the server on the storage when the autoCommit flag is set to false
			commitRoutine = new Thread(() -> {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
				while (!Thread.interrupted()) {
					try {
						Thread.sleep(20000);
						switch (databaseType) {
							case JSON -> {
								jsonDatabase.writeGamesToDisk();
								jsonDatabase.writeUsersToDisk();
								jsonDatabase.writeWordsToDisk();
							}
							case SQLITE -> db.commit();
						}
						PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Transactions committed at " + simpleDateFormat.format(new Timestamp(System.currentTimeMillis())));
					} catch (InterruptedException ignored) {
						return;
					}
					catch (IOException | SQLException e) {
						PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
						System.exit(1);
					}
				}
			});
		} catch (SQLException | ClassNotFoundException e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		checkDatabase();
		if (!autoCommit) {
			commitRoutine.start();
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Auto-commit is off, starting commit routine to preserve consistency");
		}
	}

	/**
	 * If the autoCommit flag is set to false, this method commits the data for the last time, then, if needed, closes the db connection
	 */
	public void closeDatabase() {
			try {
				if (!autoCommitDatabase){
					commitRoutine.interrupt();
					switch (databaseType) {
						case JSON -> {
							jsonDatabase.writeGamesToDisk();
							jsonDatabase.writeUsersToDisk();
							jsonDatabase.writeWordsToDisk();
						}
						case SQLITE -> db.commit();

					}
					PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Transactions committed before exiting");
				}
				if (databaseType == DatabaseType.SQLITE) db.close();
			} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}


	/**
	 * This method initiates the database
	 */
	public void checkDatabase() {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.checkDatabase(db, databasePath, autoCreateDatabase);
				case JSON -> jsonDatabase.checkDatabase();
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param currentWord the word to check
	 * @return true if there is a game in the games table identified by <username, currentWord> with the flag closed set to false
	 */
	public boolean isPlaying(String username, String currentWord) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result =  WordleSqliteDB.isPlaying(db, username, currentWord);
				case JSON -> result = jsonDatabase.isPlaying(username, currentWord);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}


		return result;
	}

	/**
	 *
	 * @param word to search
	 * @return return the id of the word, -1 if the word is not in the extracted words database
	 */
	public int getWordId(String word) {
		int result = -1;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getWordId(db, word);
				case JSON -> result = jsonDatabase.getWordId(word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}


	/**
	 *
	 * @param username the user identified by username
	 * @param wordId the id of the word that identifies the game to search
	 * @return the number of guesses made by the user trying win the word identified by wordId
	 */
	public int getGuessesNumber(String username, int wordId) {
		int result = -1;
		try {
			switch (databaseType) {
				case SQLITE -> result =  WordleSqliteDB.getGuessesNumber(db, username, wordId);
				case JSON -> result = jsonDatabase.getGuessesNumber(username, wordId);
			}
		}  catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @return the statistics of the user (gamesPlayed, gamesWonPct, lastStreak, maxStreak, guessDistribution)
	 */
	public HashMap<String, Object> getUserStatistics(String username) {
		HashMap<String, Object> result = new HashMap<>();
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getUserStatistics(db, username);
				case JSON -> result = jsonDatabase.getUserStatistics(username);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param wordId the id of the word that identifies the game to search
	 * @return the history of the hints given by the server about the word identified by wordId
	 */
	public String getHintsHistory(String username, int wordId) {
		String result = "";
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getHintsHistory(db, username, wordId);
				case JSON -> result = jsonDatabase.getHintsHistory(username, wordId);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param wordId the id of the word that identifies the game to search
	 * @return the history of the guesses made by the user trying win the word identified by wordId
	 */
	public String getGuessesHistory(String username, int wordId) {
		String result = "";
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getGuessesHistory(db, username, wordId);
				case JSON -> result = jsonDatabase.getGuessesHistory(username, wordId);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 * This method increments the guess counter and saves the guess and the hint
	 * @param username the user identified by username
	 * @param word the word that identifies the game
	 * @param guess the guess made by the user
	 * @param hint the hint given by the server
	 */
	public void incrementGameGuesses(String username, String word, String guess, String hint) {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.incrementGameGuesses(db, username, word, guess, hint);
				case JSON -> jsonDatabase.incrementGameGuesses(username, word, guess, hint);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * This method closes the game identified by <username, word> and sets the victory flag to true
	 * @param username the user identified by username
	 * @param word the word that identifies the game user won
	 */
	public void setUserVictory(String username, String word) {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.setUserVictory(db, username, word);
				case JSON -> jsonDatabase.setUserVictory(username, word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * This method resets the streak counter of the user
	 * @param username the user identified by username
	 */
	public void resetUserStreaks(String username) {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.resetUserStreaks(db, username);
				case JSON -> jsonDatabase.resetUserStreaks(username);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * This method resets the streak counter of all the users that didn't guess the SW word
	 * @param word the last word played
	 */
	public void resetStreaks(String word) {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.resetStreaks(db, word);
				case JSON -> jsonDatabase.resetStreaks(word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * @param word the word to be inserted
	 * @return true if the word is inserted inside the database
	 */
	public boolean insertExtractedWord(String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.insertExtractedWord(db, word);
				case JSON -> result = jsonDatabase.insertExtractedWord(word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}
	/**
	 *
	 * @param username the user identified by username that is trying to guess "word"
	 * @param word the word that identifies the game
	 * @return true if the word is inserted inside the database
	 */
	public boolean insertGame(String username, String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.insertGame(db, username, word);
				case JSON -> result = jsonDatabase.insertGame(username, word);
			}
		}  catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param password the password chosen by the user
	 * @param role the role of the user
	 * @return true if the word is inserted inside the database
	 */
	public boolean insertUser(String username, String password, String role) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.insertUser(db, username, password, role);
				case JSON -> result = jsonDatabase.insertUser(username, password, role);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param word the word that identifies the game
	 * @return true if user guessed the word passed as argument correctly
	 */
	public boolean isGameWon(String username, String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.isGameWon(db, username, word);
				case JSON -> result = jsonDatabase.isGameWon(username, word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 * this method set the flag closes the game identified by <username, word>by setting the closed flag to true
	 * @param username the user identified by username
	 * @param word the word that identifies the game
	 */
	public void closeGame(String username, String word) {
		try {
			switch (databaseType) {
				case SQLITE -> WordleSqliteDB.closeGame(db, username, word);
				case JSON -> jsonDatabase.closeGame(username, word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param word the word that identifies the game
	 * @return true if the game identified by <username, word> is set to true
	 */
	public boolean isGameClosed(String username, String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.isGameClosed(db, username, word);
				case JSON -> result = jsonDatabase.isGameClosed(username, word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param username the user identified by username
	 * @param word the word that identifies the game
	 * @return true if the game identified by <username, word> exists
	 */
	public boolean gameExists(String username, String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.gameExists(db, username, word);
				case JSON -> result = jsonDatabase.gameExists(username, word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param word the word to check
	 * @return  true if the word identified by word has been extracted
	 */
	public boolean wordAlreadyExtracted(String word) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.wordAlreadyExtracted(db, word);
				case JSON -> result = jsonDatabase.wordAlreadyExtracted(word);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}
	/**
	 *
	 * @param username the user identified by username
	 * @return true if the user identified by username
	 */
	public boolean userExists(String username) {
		boolean result = false;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.userExists(db, username);
				case JSON -> result = jsonDatabase.userExists(username);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	public Authorization validateUser(String username, String password) {
		Authorization result = Authorization.NOT_AUTHORIZED;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.validateUser(db, username, password);
				case JSON -> result = jsonDatabase.validateUser(username, password);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @param wordId the wordId to search
	 * @return get the word identified by wordId
	 */
	public String getWord(int wordId) {
		String result = "";
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getWord(db, wordId);
				case JSON -> result = jsonDatabase.getWord(wordId);
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}

	/**
	 *
	 * @return an ordered list containing the ranking
	 */
	public LinkedList<String> getRanking() {
		LinkedList<String> result = null;
		try {
			switch (databaseType) {
				case SQLITE -> result = WordleSqliteDB.getRanking(db);
				case JSON -> result = jsonDatabase.getRanking();
			}
		} catch (Exception e) {
			PrettyPrinter.prettyPrintln("[ @RDatabase exception@0 ] - " + e.getMessage());
			System.exit(1);
		}
		return result;
	}
}
