package Client;

import CommonUtils.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Session {
	private String username;
	private String token;
	private String multicastIp;
	private int multicastPort;
	private boolean isPlaying;
	private boolean loggedIn;
	private int currentWordId;
	private long wordExpiration;
	private boolean notification;
	private boolean showStatistics;
	private final List<String> shares;
	private NetworkInterface networkInterface;
	public Session(String networkInterface) throws SocketException {
		this.username = "";
		this.token = "";
		this.isPlaying = false;
		this.loggedIn = false;
		this.currentWordId = -1;
		this.wordExpiration = 0;
		this.multicastIp = "";
		this.multicastPort = -1;
		this.networkInterface = NetworkInterface.getByName(networkInterface);
		this.shares = new LinkedList<>();
	}

	public String username() {
		return username;
	}

	public void username(String username) {
		this.username = username;
	}

	public String token() {
		return token;
	}

	public void token(String token) {
		this.token = token;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void isPlaying(boolean playing) {
		isPlaying = playing;
	}

	public boolean loggedIn() {
		return loggedIn;
	}

	public void loggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public int currentWordId() {
		return currentWordId;
	}

	public void currentWordId(int currentWordId) {
		this.currentWordId = currentWordId;
	}

	public long wordExpiration() {
		return wordExpiration;
	}

	public void wordExpiration(long wordExpiration) {
		this.wordExpiration = wordExpiration;
	}

	public boolean notification() { return this.notification; }
	public void notification(boolean notification) { this.notification = notification; }

	public String multicastIp() { return this.multicastIp; }
	public void multicastIp(String multicastIp) { this.multicastIp = multicastIp; }
	public int multicastPort() { return this.multicastPort; }
	public void multicastPort(int multicastPort) { this.multicastPort = multicastPort; }
	public boolean showStatistics() { return this.showStatistics; }
	public void showStatistics(boolean showStatistics) { this.showStatistics = showStatistics; }
	public NetworkInterface networkInterface() { return this.networkInterface; }
	public void networkInterface(NetworkInterface networkInterface) { this.networkInterface = networkInterface; }

	public void addShare (String username, String share) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> message = mapper.readValue(share, HashMap.class);
			String senderUsername = (String) message.get("username");
			if (senderUsername.equals(username)) return; // This prevents that the sender receives the notification
		} catch (Exception ignored) {}

		synchronized (shares) {
			this.shares.add(share);
		}
	}

	public void readNotifications(UserMessages msg) {
		LinkedList<String> toRead = null;
		synchronized (shares) {
			toRead = new LinkedList<>(shares);
			shares.clear();
		}
		ObjectMapper mapper = new ObjectMapper();

		for (String share : toRead) {
			try {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> message = mapper.readValue(share, HashMap.class);
				String[] rawHints = ((String) message.get("hints")).split(":");
				StringBuilder stringBuilder = new StringBuilder();
				char[][] hints = new char[12][10];
				for (int i = 0; i < rawHints.length; i++)
					hints[i] = !rawHints[i].equals("") ? rawHints[i].toCharArray() : new char[10];
				for (char[] hint : hints) {
					stringBuilder.append("[ ");
					for (char c : hint) {
						switch (c) {
							case 'X', '?', '+' -> stringBuilder.append(c).append(" ");
							default -> stringBuilder.append("X ");
						}
					}
					stringBuilder.append("]\n");
				}
				PrettyPrinter.prettyPrintln(String.format(msg.message("share_body"), message.get("username"), (int)  message.get("remainingGuesses"),  message.get("won"), stringBuilder));
			} catch (Exception ignored) {}
		}

	}

	public int getNotificationNumber() {
		int notifications;
		synchronized (shares) {
			notifications = shares.size();
		}
		return notifications;
	}

	public void resetSession(){
		this.username = "";
		this.token = "";
		this.isPlaying = false;
		this.loggedIn = false;
		this.showStatistics = false;
		this.currentWordId = -1;
		this.wordExpiration = 0;
		this.multicastIp = "";
		this.multicastPort = -1;
		synchronized (shares) {
			shares.clear();
		}
	}
}
