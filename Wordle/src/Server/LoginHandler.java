package Server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LoginHandler {
	public static class TokenSession {
		// After a new TokenSession object is created, the token field is only read by the threads
		private final String token;
		private final AtomicLong loginTime;
		private final WordleDB.Authorization loginType;
		public TokenSession(String token, long loginTime, WordleDB.Authorization loginType) {
			this.token = token;
			this.loginTime = new AtomicLong(loginTime);
			this.loginType = loginType;
		}

		public String getToken() {
			return token;
		}
		public long getLoginTime() {
			return loginTime.get();
		}

		public void updateLoginTime() {
			loginTime.set(System.currentTimeMillis());
		}
		public WordleDB.Authorization getLoginType() {
			return loginType;
		}
	}
	private final ConcurrentHashMap<String, TokenSession> userSessions;
	public LoginHandler() {
		this.userSessions = new ConcurrentHashMap<>();
	}
	public TokenSession getUserSession(String username) {
		return userSessions.get(username);
	}


	public void addSession(WordleDB.Authorization sessionType, String username, String token) {
		TokenSession newSession = new TokenSession(token, System.currentTimeMillis(), sessionType);
		if (sessionType != WordleDB.Authorization.NOT_AUTHORIZED)  userSessions.putIfAbsent(username, newSession);
	}

	public void removeSession(String username) {
		userSessions.remove(username);
	}

	public void renewSession(String username) {
		if(userSessions.containsKey(username)) userSessions.get(username).updateLoginTime();
	}





}
