package Client;

import CommonUtils.PrettyPrinter;
import WordleRMI.*;

import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client {
	private RemoteRegistrationInterface remoteRegistrationObject = null;
	private RankingChangedInterface remoteRankingObject = null;
	private RankingListener rankingListener = null;
	private RankingListenerInterface rankingListenerStub;
	private final UserMessages msg;
	private final ClientConfig config;
	private SocketChannel socketChannel;
	private final Session session;
	private MulticastSocket multicastSocket = null;
	private Thread multicastListener = null;
	private InetSocketAddress multicastGroupAddress = null;

	public Client(ClientConfig clientConfig) throws RemoteException, NotBoundException, UserMessages.LanguageNotSupported, SocketException {
		this.msg = new UserMessages(clientConfig.prettyPrint(), clientConfig.language());
		this.session = new Session(clientConfig.networkInterface());
		this.config = clientConfig;
	}

	private String getUserAndPass(Scanner scan, Console console) {
		String password;
		do {
			PrettyPrinter.prettyPrint(msg.message("enter_username"));
			session.username(scan.nextLine());
		} while (session.username().isBlank());
		do {
			PrettyPrinter.prettyPrint(msg.message("enter_password"));
			if (console != null) password = String.valueOf(console.readPassword());
			else password = scan.nextLine();
		} while (password.isBlank());
		return password;
	}

	private boolean verifyToken(Scanner scan) throws IOException {
		HashMap<String, Object> response = null;
		// If the client manages to reconnect to the server, it verifies the session token
		response = Requests.get(socketChannel, String.format("http://%s:%d/verify", config.serverIp(), config.tcpPort()), String.format("username=%s", URLEncoder.encode(session.username(), StandardCharsets.UTF_8)), session.token());

		if (!validateResponse(response, new String[]{"status"})) {
			PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
			return false;
		}
		int statusCode = (int) response.get("status");
		if (statusCode == 401) {
			// token expired, so the client resets the session, stop the multicast listener thread and closes all the sockets
			PrettyPrinter.prettyPrintln(msg.message("token_expired"));
			PrettyPrinter.prettyPrintln(msg.message("press_enter_to_continue"));
			scan.nextLine();
			handleLogout();
		}
		return true;
	}


	/**
	 * This method handles the client reconnection to the server after an exception thrown by the SocketChannel or the RMI services.
	 * If the server is still not reachable after config.maxRetries(), the client stops.
	 */
	private void handleConnectionException(Scanner scan) {
		boolean reconnected = false;
		Exception error = null;
		for (int i = 0; i < config.maxRetries() && !reconnected; i++) {
			try {
				PrettyPrinter.prettyPrintln(msg.message("trying_to_reconnect"));
				Thread.sleep(config.reconnectionTimeout());
				connect(true);
				if (socketChannel != null)
					reconnected = true;
			} catch (Exception e) {
				error = e;
			}
		}
		if (!reconnected) {
			PrettyPrinter.prettyPrintln(msg.message("fatal_error"));
			if (error != null) error.printStackTrace();
			System.exit(1);
		} else {
			if (session.loggedIn()) {
				try {
					verifyToken(scan);
				} catch (IOException e) {
					PrettyPrinter.prettyPrintln(msg.message("fatal_error"));
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}


	/**
	 * This method handles the creation of the multicast group and
	 * starts a thread that waits for incoming DataPackets.
	 * All the messages sent by the server are stored inside session.shares
	 */
	private void joinMulticast() {
		if (session.multicastIp().equals("") || session.multicastPort() < 0) {
			PrettyPrinter.prettyPrintln(msg.message("cannot_join_multicast"));
			return;
		}
		// This method works, BUT ON LINUX YOU NEED TO DISABLE FIREWALL IN ORDER TO RECEIVE/SEND PACKETS
		try {
			multicastSocket = new MulticastSocket();
			multicastGroupAddress = new InetSocketAddress(session.multicastIp(), session.multicastPort());
			multicastSocket.joinGroup(multicastGroupAddress, session.networkInterface());
			multicastListener = new Thread(() -> {
			byte[] buffer = new byte[Requests.BUFFER_SIZE];
			while (!Thread.interrupted()) {
				DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
				try {
					this.multicastSocket.receive(datagramPacket);
				} catch (IOException e) {
					if (!Thread.interrupted()) PrettyPrinter.prettyPrintln(msg.message("cannot_receive_multicast"));
					return;
				}
				String msg = new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
				session.addShare(session.username(), msg);
			}
		});
		multicastListener.setDaemon(true);
		multicastListener.start();
		} catch (IOException e) {
			PrettyPrinter.prettyPrintln(msg.message("cannot_join_multicast"));
		}
	}

	private void connectToRMIServices() throws RemoteException, NotBoundException {
		Registry r = LocateRegistry.getRegistry(config.serverIp(), config.rmiPort());
		this.remoteRegistrationObject = (RemoteRegistrationInterface) r.lookup(config.rmiRegistrationEndpoint());
		this.remoteRankingObject = (RankingChangedInterface) r.lookup(config.rmiRankingEndpoint());
	}

	/**
	 * Connects to the sever specified in the configuration file
	 * @param reconnecting if reconnecting is false and the server is not reachable, the client stops.
	 *                     If reconnecting is false, the client re-throws the exception.
	 *                     This method is called during the startup or by handleConnectionException(),
	 *                     so if the client has never connected to the server stops immediately, otherwise
	 *                     throws an exception that is catch by the caller.
	 * @throws IOException the client cannot establish a connection with the server
	 * @throws NotBoundException the RMI endpoint specified in the config is not registered
	 */
	private void connect(boolean reconnecting) throws IOException, NotBoundException {
		try {
			socketChannel = SocketChannel.open(new InetSocketAddress(config.serverIp(), config.tcpPort()));
			if (reconnecting) connectToRMIServices();
		} catch (IOException e) {
			if (!reconnecting) {
				PrettyPrinter.prettyPrintln(msg.message("unable_to_connect"));
				System.exit(1);
			} else throw new IOException(msg.message("unable_to_connect_ex"));
		} catch (NotBoundException e) {
			if (!reconnecting){
				PrettyPrinter.prettyPrintln(msg.message("unable_to_connect_rmi"));
				System.exit(1);
			} else throw new NotBoundException(msg.message("unable_to_connect_rmi_ex"));
		}
	}

	private boolean validateResponse(HashMap<String, Object> response, String[] parameters) {
		if (response == null) return false;
		for (String param: parameters) {
			if (response.get(param) == null) return false;
		}
		return true;
	}

	/**
	 * Called after a successful logout or if the session token expires, this method
	 * removes the rankingListener associated with this client from the server, stop the
	 * multicast listener thread and closes the socket channel. Finally, resets the session.
	 */
	public void handleLogout() {
		if (session.notification()) {
			rankingListener = null;
			if (remoteRankingObject != null && rankingListenerStub != null) {
				try {
					remoteRankingObject.removeListener(rankingListenerStub);
				} catch (Exception ignored) {}
			}
		}
		if(multicastListener != null && multicastListener.isAlive()) {
			multicastListener.interrupt();
			if (multicastSocket != null) {
				try {
					multicastSocket.leaveGroup(multicastGroupAddress, session.networkInterface());
				} catch (IOException ignore) {}
				multicastSocket.close();
				multicastSocket = null;
			}
			multicastListener = null;
		}
		session.resetSession();
		try {
			if (socketChannel.isConnected()) socketChannel.close();
			socketChannel = null;
		} catch (IOException ignored) {}

	}

	public void start() {
		// Add shutdown hook to gracefully close the client
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (session.loggedIn()) {
					Requests.post(socketChannel, String.format("http://%s:%d/logout", config.serverIp(), config.tcpPort()), "", String.format("username=%s", session.username()), session.token());
					handleLogout();
				}
				if (socketChannel != null) socketChannel.close();
			} catch (IOException ignored) {}
		}));
		String password;
		Console console = System.console();
		int choice;
		boolean refresh;
		// Init RMI services
		try {
			connectToRMIServices();
		} catch (RemoteException | NotBoundException e) {
			PrettyPrinter.prettyPrintln(msg.message("unable_to_connect_rmi"));
			System.exit(1);
		}

		try (Scanner scan = new Scanner(System.in)) {
			do {
				clearScreen();
				PrettyPrinter.prettyPrintln(msg.message("banner"));
				PrettyPrinter.prettyPrint(msg.message("login_menu"));
				while (!scan.hasNextInt()) {
					PrettyPrinter.prettyPrint(msg.message("enter_valid_number"));
					scan.next();
				}
				choice = scan.nextInt();
				scan.nextLine();
				switch (choice) {
					// Registration
					case 1 -> {
						password = getUserAndPass(scan, console);
						try {
							if (remoteRegistrationObject == null){
								PrettyPrinter.prettyPrintln(msg.message("unable_to_connect_rmi"));
								System.exit(1);
							}
							else if (remoteRegistrationObject.registration(session.username(), password))
								PrettyPrinter.prettyPrintln(msg.message("registration_successful"));
							else PrettyPrinter.prettyPrintln(msg.message("already_registered"));
						} catch (RemoteException e) {
							PrettyPrinter.prettyPrintln(msg.message("unable_to_connect_rmi"));
							System.exit(1);
						}
					}
					// Login
					case 2 -> {
						password = getUserAndPass(scan, console);
						try {
							connect(false); // Init TCP connection and RMI ranking notification service
						} catch (Exception ignored) {}
						try {
							HashMap<String, Object> response = Requests.post(socketChannel, String.format("http://%s:%d/login", config.serverIp(), config.tcpPort()), "", String.format("username=%s&password=%s", URLEncoder.encode(session.username(), StandardCharsets.UTF_8), password), "");
							if (!validateResponse(response, new String[]{"status"})) {
								PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
								break;
							}
							int statusCode = (int) response.get("status");
							switch (statusCode) {
								case 200, 400 -> {
									if (!validateResponse(response, new String[]{"token", "multicastIp", "multicastPort"})) {
										PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
										break;
									}
									session.token((String) response.get("token"));
									session.multicastIp((String) response.get("multicastIp"));
									session.multicastPort((int) response.get("multicastPort"));
									PrettyPrinter.prettyPrintln(statusCode == 200 ? String.format(msg.message("login_successful"), session.username()) : msg.message("already_logged_in"));
									joinMulticast();
									session.notification(true);
									try {
										rankingListener = new RankingListener(msg);
										rankingListenerStub = (RankingListenerInterface) UnicastRemoteObject.exportObject(rankingListener, 0);
										remoteRankingObject.addListener(rankingListenerStub);
									} catch (RemoteException e) {
										handleConnectionException(scan);
									}
									PrettyPrinter.prettyPrintln(msg.message("notifications_enabled"));
								}
								case 401 -> PrettyPrinter.prettyPrintln(msg.message("login_unsuccessful"));

								case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));

							}
							if (!session.token().isBlank()) session.loggedIn(true);

						} catch (IOException e) {
							handleConnectionException(scan);
						}
						PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
						scan.nextLine();
					}
					// Exit
					case 0 -> {
						PrettyPrinter.prettyPrintln(msg.message("bye"));
						System.exit(0);
					}
				}
				// Game menu
				if (session.loggedIn()) {
					do {
						try {
							verifyToken(scan);
						} catch (IOException e) {
							handleConnectionException(scan);
							break;
						}
						clearScreen();
						PrettyPrinter.prettyPrintln(msg.message("banner"));
						try {
							getWordTimer(false);
						} catch (IOException e) {
							handleConnectionException(scan);
							break;
						}
						// If the user is playing, it checks whether the wordId is still valid or not
						if (session.isPlaying()) {
							if (System.currentTimeMillis() > session.wordExpiration()) {
								PrettyPrinter.prettyPrintln(String.format(msg.message("word_timeout"), session.currentWordId()));
								try {
									sharePrompt(scan);
								} catch (IOException e) {
									handleConnectionException(scan);
									break;
								}
								session.isPlaying(false);
								session.currentWordId(-1);
								PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
								scan.nextLine();
								continue;
							} else {
								try {
									printWordHistory();
								} catch (IOException e) {
									handleConnectionException(scan);
									break;
								}
							}
						}

						if (session.showStatistics()) try {
							printStatistics();
						} catch (IOException e) {
							handleConnectionException(scan);
						}
						PrettyPrinter.prettyPrint(String.format(msg.message("game_menu"), session.getNotificationNumber()));
						refresh = false;
						choice = -1;
						do {

							String line = scan.nextLine();
							if (line.equals("")) {
								refresh = true;
								break;
							}
							try {
								choice = Integer.parseInt(line);
							} catch (Exception ignored) { PrettyPrinter.prettyPrint(msg.message("enter_valid_number")); }
						} while (choice == -1);

						if (refresh) continue; // Refresh the CLI by pressing "Enter"

						if (session.isPlaying()) {
							if (System.currentTimeMillis() > session.wordExpiration()) {
								PrettyPrinter.prettyPrintln(String.format(msg.message("word_timeout"), session.currentWordId()));
								try {
									sharePrompt(scan);
								} catch (IOException e) {
									handleConnectionException(scan);
								}
								session.isPlaying(false);
								session.currentWordId(-1);
								PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
								scan.nextLine();
								continue;
							}
						}
						switch (choice) {
							// Play wordle
							case 1 -> {
								try {
									HashMap<String, Object> response = Requests.post(socketChannel, String.format("http://%s:%d/playWordle", config.serverIp(), config.tcpPort()),"",  String.format("username=%s", session.username()), session.token());
									if (!validateResponse(response, new String[]{"status"})) {
										PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
										break;
									}
									int statusCode = (int) response.get("status");
									switch (statusCode) {
										case 200 -> {
											session.currentWordId((int) response.get("wordId"));
											session.isPlaying(true);
											PrettyPrinter.prettyPrintln(String.format(msg.message("game_started_successfully"), session.currentWordId()));
											try {
												getWordTimer(true);
											} catch (IOException e) {
												handleConnectionException(scan);
											}
										}
										case 400 -> {
											if (response.containsKey("wordId")) {
												// This is particularly useful on the web app, where the user can close the browser without logging out
												// Here there isn't an option to close the CLI and remember the session data.
												if (!session.isPlaying()) {
													session.isPlaying(true);
													session.currentWordId((int) response.get("wordId"));
													PrettyPrinter.prettyPrintln(String.format(msg.message("game_restarted_successfully"), session.currentWordId()));
													try {
														getWordTimer(true);
													} catch (IOException e) {
														handleConnectionException(scan);
													}
												} else
													PrettyPrinter.prettyPrintln(msg.message("already_playing"));
											} else if (response.containsKey("victory")){
												// If the response contains this key, it means that the game is closed
												if ((boolean) response.get("victory"))
													PrettyPrinter.prettyPrintln(msg.message("game_already_won"));
												else
													PrettyPrinter.prettyPrintln(msg.message("game_closed"));
												session.isPlaying(false);
												session.currentWordId(-1);
											}

										}
										case 401 -> {
											PrettyPrinter.prettyPrintln(msg.message("not_authorized"));
											handleLogout();

										}

										case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));
									}
								} catch (IOException e) {
									handleConnectionException(scan);
								}
							}
							// Send word
							case 2 -> {
								try {
									String word;
									if (session.currentWordId() < 0 || !session.isPlaying()) {
										PrettyPrinter.prettyPrintln(msg.message("start_new_game"));
										break;
									}
									do {
										PrettyPrinter.prettyPrint(msg.message("insert_word"));
										word = scan.nextLine();
									} while (word.isBlank() || word.length() != 10);
									HashMap<String, Object> response = Requests.post(socketChannel, String.format("http://%s:%d/sendWord", config.serverIp(), config.tcpPort()), "", String.format("username=%s&word=%s&wordId=%d", session.username(), word, session.currentWordId()), session.token());
									if (!validateResponse(response, new String[]{"status"})) {
										PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
										break;
									}
									int statusCode = (int) response.get("status");
									switch (statusCode) {
										case 200 -> {
											if (!validateResponse(response, new String[]{"victory", "remainingGuesses", "wordExists"})) {
												PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
												break;
											}
											int remainingGuesses = (int) response.get("remainingGuesses");
											if ((boolean) response.get("victory")) {
												String translatedWord = response.containsKey("translatedWord") ? (String) response.get("translatedWord") : "-";
												PrettyPrinter.prettyPrintln(String.format(msg.message("victory"), translatedWord));
												sharePrompt(scan);
												session.isPlaying(false);
												session.currentWordId(-1);
												printStatistics();
											} else {
												if ((boolean) response.get("wordExists")) PrettyPrinter.prettyPrintln(String.format(msg.message("try_again"), remainingGuesses));
												else PrettyPrinter.prettyPrintln(String.format(msg.message("try_again_not_exists"), remainingGuesses));
											}
										}
										case 400 -> {
											if (response.containsKey("remainingGuesses") && (int) response.get("remainingGuesses") == 0){
												String translatedWord = response.containsKey("translatedWord") ? (String) response.get("translatedWord") : "-";
												PrettyPrinter.prettyPrintln(String.format(msg.message("game_lost"), translatedWord));
												sharePrompt(scan);
												printStatistics();
											}
											else if (response.containsKey("code")) {
												switch ((int) response.get("code")) {
													case 100 -> PrettyPrinter.prettyPrintln(String.format(msg.message("word_timeout"), session.currentWordId()));
													case 200 -> PrettyPrinter.prettyPrintln(String.format(msg.message("start_new_game"), session.currentWordId()));
													case 300 -> PrettyPrinter.prettyPrintln(String.format(msg.message("game_already_won"), session.currentWordId()));
												}
											}
											session.isPlaying(false);
											session.currentWordId(-1);
										}

										case 401 -> {
											PrettyPrinter.prettyPrintln(msg.message("not_authorized"));
											handleLogout();
										}

										case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));

									}

								} catch (Exception e) {
									handleConnectionException(scan);
								}
							}
							// Get ranking
							case 3 -> {
								try {
									HashMap<String, Object> response = Requests.get(socketChannel, String.format("http://%s:%d/showMeRanking", config.serverIp(), config.tcpPort()), String.format("username=%s", session.username()), session.token());
									if (!validateResponse(response, new String[]{"status", "ranking"})) {
										PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
										break;
									}
									int statusCode = (int) response.get("status");
									switch (statusCode) {
										case 200 -> {
											@SuppressWarnings("unchecked")
											ArrayList<String> ranking = (ArrayList<String>) response.get("ranking");
											printRanking(ranking);
										}

										case 401 -> {
											PrettyPrinter.prettyPrintln(msg.message("not_authorized"));
											handleLogout();
										}

										case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));

									}
								} catch (IOException e) {
									handleConnectionException(scan);
								}
							}

							// Prints all the notifications
							case 4 -> session.readNotifications(msg);

							// Fetch the statistics at every CLI refresh
							case 5 -> session.showStatistics(!session.showStatistics());

							// Logout
							case 0 -> {
								try {
									HashMap<String, Object> response = Requests.post(socketChannel, String.format("http://%s:%d/logout", config.serverIp(), config.tcpPort()), "", String.format("username=%s", session.username()), session.token());
									if (!validateResponse(response, new String[]{"status"})) {
										PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
										break;
									}
									int statusCode = (int) response.get("status");
									switch (statusCode) {
										case 200, 401 -> PrettyPrinter.prettyPrintln(String.format(msg.message("logout_successful"), session.username()));
										case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));

									}

								} catch (IOException e) {
									handleConnectionException(scan);
								} finally {
									handleLogout();
								}
							}
						}
						// If the client gets 401 NOT AUTHORIZED, the session is reset and the CLI exits from the game menu
						if (!session.loggedIn()) choice = 0;
						PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
						scan.nextLine();
					} while (choice != 0);
				} else {
					PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
					scan.nextLine();
				}
			} while (true);
		}
	}

	/**
	 * Asks if the user wants to share the current game
	 * @param scan reads System.in
	 * @throws IOException if the host is not reachable
	 */
	public void sharePrompt(Scanner scan) throws IOException {
		String choice;
		do {
			PrettyPrinter.prettyPrint(msg.message("share_prompt"));
			choice = scan.nextLine();
		} while ((choice.isBlank() || choice.length() != 1) && (!choice.equals("s") && !choice.equals("y") && !choice.equals("n")));

		switch (choice) {
			case "s", "y" -> {
				HashMap<String, Object> response = Requests.post(socketChannel, String.format("http://%s:%d/share", config.serverIp(), config.tcpPort()), "", String.format("username=%s&wordId=%s", session.username(), session.currentWordId()), session.token());
				if (!validateResponse(response, new String[]{"status"})) {
					PrettyPrinter.prettyPrintln(msg.message("response_not_valid"));
					break;
				}
				int statusCode = (int) response.get("status");
				switch (statusCode) {
					case 200 -> PrettyPrinter.prettyPrintln(msg.message("share_successful"));
					case 500 -> PrettyPrinter.prettyPrintln(msg.message("internal_server_error"));

				}
			}
		}
		PrettyPrinter.prettyPrint(msg.message("press_enter_to_continue"));
		scan.nextLine();
	}

	public void clearScreen() {
		System.out.print("\033[H\033[2J");
		System.out.flush();
	}

	private void printGuessDistribution(ArrayList<Integer> guessDistribution) {
		int maxGuesses = 12;
		String[] index = new String[maxGuesses];
		String[] val = new String[maxGuesses];
		for (int i = 0; i < maxGuesses; i++) index[i] = String.format("@Y%03d@0", i + 1);
		for (int i = 0; i < maxGuesses; i++) val[i] = String.format("@I%03d@0", guessDistribution.get(i));
		PrettyPrinter.prettyPrintln("Guess distribution:");
		PrettyPrinter.prettyPrintln(Arrays.toString(index));
		PrettyPrinter.prettyPrintln(Arrays.toString(val));
	}

	/**
	 * Prints the time at which the word changes
	 *  @throws IOException if the host is not reachable
	 */
	public void getWordTimer(boolean setTimer) throws IOException {
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
		HashMap<String, Object> response;
		if (!setTimer) PrettyPrinter.prettyPrintln(String.format(msg.message("user"), session.username()));
		response = Requests.get(socketChannel, String.format("http://%s:%d/wordTimer", config.serverIp(), config.tcpPort()), String.format("username=%s", session.username()), session.token());
		if (validateResponse(response, new String[]{"status", "time"})) {
			if (!setTimer) PrettyPrinter.prettyPrintln(String.format(msg.message("time"), time.format(new Date(System.currentTimeMillis() + (int) response.get("time")))));
			if (setTimer) session.wordExpiration(System.currentTimeMillis() + (int) response.get("time"));
		}
	}

	private void printStatistics() throws IOException {
		HashMap<String, Object> response;
		response = Requests.get(socketChannel, String.format("http://%s:%d/sendMeStatistics", config.serverIp(), config.tcpPort()), String.format("username=%s", session.username()), session.token());
		if (validateResponse(response, new String[]{"status"})) {
			PrettyPrinter.prettyPrintln(String.format(msg.message("stats"),
					(int) response.get("gamesPlayed"), ((double) response.get("gamesWonPct") * 100), '%', (int) response.get("lastStreak"), (int) response.get("maxStreak")));
			@SuppressWarnings("unchecked")
			ArrayList<Integer> guessDistribution = (ArrayList<Integer>) response.get("guessDistribution");
			printGuessDistribution(guessDistribution);
		}
	}

	private void printRanking(ArrayList<String> ranking) {
		if (ranking == null || ranking.size() == 0) return;
		int i = 1;
		for (String user: ranking) PrettyPrinter.prettyPrintln(String.format("@C%d.@0 %s", i++, user));
	}

	/**
	 * Print a wordle-like table.
	 * @throws IOException if the server is not reachable
	 */
	private void printWordHistory() throws IOException {
		HashMap<String, Object> response = null;
		response = Requests.get(socketChannel, String.format("http://%s:%d/getGameHistory", config.serverIp(), config.tcpPort()), String.format("username=%s&wordId=%d", session.username(), session.currentWordId()), session.token());
		if (validateResponse(response, new String[]{"status", "guesses", "hints"})){
			String[] rawGuesses = ((String) response.get("guesses")).split(":");
			String[] rawHints = ((String) response.get("hints")).split(":");
			char[][] guesses = new char[12][10];
			char[][] hints = new char[12][10];
			if (rawGuesses.length != 0 && rawHints.length != 0) {
				for (int i = 0; i < rawGuesses.length && i < rawHints.length; i++) {
					guesses[i] = !rawGuesses[i].equals("") ? rawGuesses[i].toCharArray() : new char[10];
					hints[i] = !rawHints[i].equals("") ? rawHints[i].toCharArray() : new char[10];
				}
				for (int i = 0; i < guesses.length; i++) {
					PrettyPrinter.prettyPrint("[ ");
					for (int j = 0; j < guesses[i].length && j < hints[i].length; j++) {
						switch (hints[i][j]) {
							case 'X' -> PrettyPrinter.prettyPrint(guesses[i][j] + " ");
							case '?' -> PrettyPrinter.prettyPrint(String.format("@Y%c@0 ", guesses[i][j]));
							case '+' -> PrettyPrinter.prettyPrint(String.format("@G%c@0 ", guesses[i][j]));
							default -> PrettyPrinter.prettyPrint("0 ");
						}
					}
					PrettyPrinter.prettyPrint("] - [ ");
					for (int j = 0; j < guesses[i].length && j < hints[i].length; j++) {
						switch (hints[i][j]) {
							case 'X', '?', '+' -> PrettyPrinter.prettyPrint(hints[i][j] + " ");
							default -> PrettyPrinter.prettyPrint("X ");
						}
					}
					PrettyPrinter.prettyPrintln("]");
				}
			}
		}
	}
}
