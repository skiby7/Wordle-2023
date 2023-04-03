package Models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameModel implements Serializable {
	private int id;
	private String username;
	private String word;
	private final AtomicInteger guesses;
	private final AtomicBoolean won;
	private String guessesHistory;
	private String hintsHistory;
	private Timestamp gameDate;
	private final AtomicBoolean closed;
	@JsonIgnore
	private final Object guessesHistorySync;
	@JsonIgnore
	private final Object hintsHistorySync;


	@JsonCreator
	public GameModel(@JsonProperty("id") int id,
					 @JsonProperty("username") String username,
					 @JsonProperty("word") String word,
					 @JsonProperty("guesses") int guesses,
					 @JsonProperty("won") boolean won,
					 @JsonProperty("guessesHistory") String guessesHistory,
					 @JsonProperty("hintsHistory") String hintsHistory,
					 @JsonProperty("gameDate") long gameDate,
					 @JsonProperty("closed") boolean closed) {
		this.id = id;
		this.username = username;
		this.word = word;
		this.guesses = new AtomicInteger(guesses);
		this.won = new AtomicBoolean(won);
		this.closed = new AtomicBoolean(closed);
		this.guessesHistory = guessesHistory;
		this.hintsHistory = hintsHistory;
		this.gameDate = new Timestamp(gameDate);
		this.guessesHistorySync = new Object();
		this.hintsHistorySync = new Object();
	}

	public GameModel(@JsonProperty("id") int id,
					 @JsonProperty("username") String username,
					 @JsonProperty("word") String word) {
		this.id = id;
		this.username = username;
		this.word = word;
		this.guesses = new AtomicInteger(0);
		this.won = new AtomicBoolean(false);
		this.closed = new AtomicBoolean(false);
		this.guessesHistory = "";
		this.hintsHistory = "";
		this.gameDate = new Timestamp(System.currentTimeMillis());
		this.guessesHistorySync = new Object();
		this.hintsHistorySync = new Object();
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getGuesses() {
		return guesses.get();
	}

	public void setGuesses(int guesses) {
		this.guesses.set(guesses);
	}

	public void incrementGuesses() { this.guesses.addAndGet(1); }

	public boolean isWon() {
		return won.get();
	}

	public void setWon(boolean won) {
		this.won.set(won);
	}

	public boolean isClosed() {
		return closed.get();
	}

	public void setClosed(boolean closed) {
		this.closed.set(closed);
	}

	public String getGuessesHistory() {
		String result;
		synchronized (guessesHistorySync){
			result = guessesHistory;
		}
		return result;
	}

	public void setGuessesHistory(String guessesHistory) {
		synchronized (guessesHistorySync) {
			this.guessesHistory = guessesHistory;
		}
	}

	public String getHintsHistory() {
		String result;
		synchronized (hintsHistorySync){
			result = hintsHistory;
		}
		return result;
	}

	public void setHintsHistory(String hintsHistory) {
		synchronized (hintsHistorySync) {
			this.hintsHistory = hintsHistory;
		}
	}

	public Timestamp getGameDate() {
		return gameDate;
	}

	public void setGameDate(Timestamp gameDate) {
		this.gameDate = gameDate;
	}

	// a user can try to guess an SW only once, so the couple (username, word) uniquely identifies a game
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GameModel gameModel = (GameModel) o;
		return username.equals(gameModel.username) && word.equals(gameModel.word);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username, word);
	}
}
