package Models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public class UserModel implements Serializable {
	private int id;
	private String username;
	private Timestamp subscriptionDate;
	private String role;
	private Timestamp lastGameTimestamp;
	@JsonIgnore
	private final Object lastGameSync;
	private AtomicInteger gamesPlayed;
	private String hashedPassword;
	private final AtomicInteger currentStreak;
	private final AtomicInteger longestStreak;



	@JsonCreator
	public UserModel(@JsonProperty("id") int id,
					 @JsonProperty("currentStreak") int currentStreak,
					 @JsonProperty("gamesPlayed") int gamesPlayed,
					 @JsonProperty("hashedPassword") String hashedPassword,
					 @JsonProperty("lastGameTimestamp") long lastGameTimestamp,
					 @JsonProperty("longestStreak") int longestStreak,
					 @JsonProperty("role") String role,
					 @JsonProperty("subscriptionDate") long subscriptionDate,
					 @JsonProperty("username") String username) {

		this.id = id;
		this.username = username;
		this.hashedPassword = hashedPassword;
		this.role = role;
		this.gamesPlayed = new AtomicInteger(gamesPlayed);
		this.currentStreak = new AtomicInteger(currentStreak);
		this.longestStreak = new AtomicInteger(longestStreak);
		this.lastGameTimestamp = new Timestamp(lastGameTimestamp);
		this.subscriptionDate = new Timestamp(subscriptionDate);
		this.lastGameSync = new Object();
	}
	public UserModel(@JsonProperty("id") int id,
					 @JsonProperty("username") String username,
					 @JsonProperty("hashedPassword") String hashedPassword,
					 @JsonProperty("role") String role
					 ) {

		this.id = id;
		this.username = username;
		this.hashedPassword = hashedPassword;
		this.role = role;
		this.gamesPlayed = new AtomicInteger(0);
		this.currentStreak = new AtomicInteger(0);
		this.longestStreak = new AtomicInteger(0);
		this.lastGameTimestamp = null;
		this.subscriptionDate = new Timestamp(System.currentTimeMillis());
		this.lastGameSync = new Object();
	}

	public int getID() {
		return id;
	}

	public void setID(int ID) {
		this.id = ID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public void setHashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Timestamp getSubscriptionDate() {
		return this.subscriptionDate;
	}

	public void setSubscriptionDate(Timestamp subscriptionDate) {
		this.subscriptionDate = subscriptionDate;
	}

	public Timestamp getLastGameTimestamp() {
		Timestamp result = null;
		synchronized (lastGameSync){
			result = lastGameTimestamp;
		}
		return result;
	}

	public void setLastGameTimestamp(Timestamp lastGameTimestamp) {
		synchronized (lastGameSync) {
			this.lastGameTimestamp = lastGameTimestamp;
		}
	}

	public int getGamesPlayed() {
		return gamesPlayed.get();
	}

	public void setGamesPlayed(int gamesPlayed) {
		this.gamesPlayed.set(gamesPlayed);
	}

	public void incrementGamesPlayed() {
		this.gamesPlayed.addAndGet(1);
	}

	public int getCurrentStreak() {
		return currentStreak.get();
	}

	public void setCurrentStreak(int currentStreak) {
		this.currentStreak.set(currentStreak);
	}
	public void incrementCurrentStreak() {
		this.currentStreak.addAndGet(1);
	}

	public int getLongestStreak() {
		return longestStreak.get();
	}

	public void setLongestStreak(int longestStreak) {
		this.longestStreak.set(longestStreak);
	}
	public void incrementLongestStreak() {
		this.longestStreak.addAndGet(1);
	}

	// two users cannot have the same username, so it can be used to uniquely identify a user
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserModel userModel = (UserModel) o;
		return username.equals(userModel.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}
}
