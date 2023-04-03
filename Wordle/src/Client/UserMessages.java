package Client;

import java.util.*;

public class UserMessages {
	public static class LanguageNotSupported extends Exception {
		public LanguageNotSupported(String errorMessage) {
			super(errorMessage);
		}
	}
	private final HashMap<String, HashMap<String, String>> messages;
	private final String currentLanguage;
	public UserMessages(boolean prettyPrinter, String currentLanguage) throws LanguageNotSupported {
		switch (currentLanguage) {
			case "it-it", "en-en" -> this.currentLanguage = currentLanguage;
			default -> throw new LanguageNotSupported(currentLanguage + " is not supported yet!");
		}
		messages = new HashMap<>();
		initIT_ITMessages(prettyPrinter);
		initEN_ENMessages(prettyPrinter);
	}

	public String message(String identifier) {
		switch (currentLanguage) {
			case "it-it" -> {
				return italian().get(identifier) != null ? italian().get(identifier) : "";
			}
			case "en-en" -> {
				return english().get(identifier) != null ? english().get(identifier) : "";
			}
		}
		return "";
	}
	private Map<String, String> italian(){
		return messages.get("it-it");
	}

	private Map<String, String> english(){
		return messages.get("en-en");
	}

	private void removePrettyCharFromMap(HashMap<String,String> map) {
		Set<String> keys = map.keySet();
		for (String key: keys) {
			map.put(key, map.get(key).replaceAll("@BK", "")
					.replaceAll("@R", "")
					.replaceAll("@G", "")
					.replaceAll("@Y", "")
					.replaceAll("@BL", "")
					.replaceAll("@M", "")
					.replaceAll("@C", "")
					.replaceAll("@W", "")
					.replaceAll("@BO", "")
					.replaceAll("@I", "")
					.replaceAll("@0", ""));
		}
	}

	private void initIT_ITMessages(boolean prettyPrinter) {
		messages.put("it-it", new HashMap<>());
		messages.get("it-it").put("unable_to_connect", "[ @RErrore@0 ] - Impossibile connettersi! Controlla l'IP e la porta o riprova più tardi!");
		messages.get("it-it").put("unable_to_connect_ex", "[ Errore ] - Impossibile connettersi! Controlla l'IP e la porta o riprova più tardi!");
		messages.get("it-it").put("unable_to_connect_rmi", "[ @RErrore@0 ] - Impossibile connettersi ai servizi RMI! Controlla l'IP e la porta o riprova più tardi!");
		messages.get("it-it").put("unable_to_connect_rmi_ex", "[ Errore ] - Impossibile connettersi ai servizi RMI! Controlla l'IP e la porta o riprova più tardi!");
		messages.get("it-it").put("banner", """


				 @M__    __                   _  _      \s
				/ / /\\ \\ \\  ___   _ __   __| || |  ___\s
				\\ \\/  \\/ / / _ \\ | '__| / _` || | / _ \\
				 \\  /\\  / | (_) || |   | (_| || ||  __/
				  \\/  \\/   \\___/ |_|    \\__,_||_| \\___|@0
				                                      \s""");
		messages.get("it-it").put("login_menu", "1. Registrati 2. Esegui il login 0. Esci\n@C>>>@0 ");
		messages.get("it-it").put("game_menu", "1. Gioca 2. Invia parola 3. Visualizza classifica 4. Leggi notifiche (@C%d@0) 5. Abilita/disabilita statistiche 0. Logout\n@C>>>@0 ");
		messages.get("it-it").put("enter_valid_number", "[ @RErrore@0 ] - Inserisci un numero valido\n@C>>>@0 ");
		messages.get("it-it").put("enter_username", "Inserire nome utente\n@C>>>@0 ");
		messages.get("it-it").put("enter_password", "Inserire password\n@C>>>@0 ");
		messages.get("it-it").put("fatal_error", "[ @RErrore@0 ] - Errore fatale, esco...");
		messages.get("it-it").put("response_not_valid", "[ @RErrore@0 ] - Non ho ottenuto una risposta valida, riprova");
		messages.get("it-it").put("registration_successful", "[ @GSuccesso@0 ] - Registrazione avvenuta con successo!");
		messages.get("it-it").put("already_registered", "[ @RErrore@0 ] - Utente già iscritto!");
		messages.get("it-it").put("login_successful", "[ @GSuccesso@0 ] - Benvenuto @M%s@0!");
		messages.get("it-it").put("already_logged_in", "[ @RErrore@0 ] - Accesso già effettuato!");
		messages.get("it-it").put("login_unsuccessful", "[ @RErrore@0 ] - Le credenziali sono errate");
		messages.get("it-it").put("internal_server_error", "[ @RErrore@0 ] - Errore interno al server");
		messages.get("it-it").put("word_timeout", "[ @YTEMPO SCADUTO@0 ] - Hai terminato il tempo per indovinare la parola numero %d, inizia un nuovo gioco!");
		messages.get("it-it").put("game_started_successfully", "[ @GSuccesso@0 ] - Iniziato il gioco numero %d!");
		messages.get("it-it").put("game_restarted_successfully", "[ @GSuccesso@0 ] - Ripreso il gioco numero %d!");
		messages.get("it-it").put("game_closed", "[ @RErrore@0 ] - Impossibile riprendere il gioco, aspetta che venga estratta una nuova parola!");
		messages.get("it-it").put("game_already_won", "[ @RErrore@0 ] - Hai già vinto, aspetta che venga estratta una nuova parola!");
		messages.get("it-it").put("game_lost", "[ @RGame over!@0 ] - Hai finito i tentativi! Traduzione -> %s");
		messages.get("it-it").put("already_playing", "[ @RErrore@0 ] - Stai già giocando!");
		messages.get("it-it").put("not_authorized", "[ @RErrore@0 ] - Non autorizzato!");
		messages.get("it-it").put("start_new_game", "[ @RErrore@0 ] - Prima fai partire un gioco!");
		messages.get("it-it").put("insert_word", "Inserisci una parola di 10 lettere\n@C>>>@0 ");
		messages.get("it-it").put("victory", "[ @GVittoria!@0 ] - Hai indovinato la parola! Traduzione -> %s");
		messages.get("it-it").put("try_again", "[ @YRiprova!@0 ] - Non hai indovinato la parola, ti rimangono %d/12 tentativi!");
		messages.get("it-it").put("try_again_not_exists", "[ @YRiprova!@0 ] - La parola non è presente nel dizionario, ti rimangono %d/12 tentativi!");
		messages.get("it-it").put("logout_successful", "[ @GSuccesso@0 ] - Arrivederci @M%s@0!");
		messages.get("it-it").put("press_enter_to_continue", "@IPremi invio per continuare@0");
		messages.get("it-it").put("notifications_enabled", "[ @GSuccesso@0 ] - Notifiche attivate!");
		messages.get("it-it").put("notifications_disabled", "[ @GSuccesso@0 ] - Notifiche disattivate!");
		messages.get("it-it").put("ranking_notification", "\n[ @CClassifica aggiornata@0 ] - La classifica è stata aggiornata!");
		messages.get("it-it").put("trying_to_reconnect", "[ @MRiconnessione@0 ] - Sto provando a riconnettermi al server...");
		messages.get("it-it").put("reconnection_successful", "[ @GRiconnesso@0 ] - Riconnesso al server!");
		messages.get("it-it").put("cannot_join_multicast", "[ @RErrore@0 ] - Non sono riuscito a connettermi al gruppo multicast!");
		messages.get("it-it").put("cannot_receive_multicast", "[ @RErrore@0 ] - Non sono riuscito a leggere dal MulticastSocket, chiudo il thread!");
		messages.get("it-it").put("stats", "@BOStatistiche:@0\nPartite giocate: @M%d@0 - Percentuale partite vinte: @M%.2f%c@0 - Ultima streak: @M%d@0 - Streak più lunga: @M%d@0");
		messages.get("it-it").put("time", "Prossima parola alle ore: @BO%s@0");
		messages.get("it-it").put("user", "Utente -> @G%s@0");
		messages.get("it-it").put("token_expired", "[ @RErrore@0 ] - La sessione è scaduta, devi effettuare il login!");
		messages.get("it-it").put("share_prompt", "Vuoi condividere la partita appena conclusa? [s/n] ");
		messages.get("it-it").put("share_successful", "[ @GSuccesso@0 ] - Risultati inviati agli altri utenti!");
		messages.get("it-it").put("share_body", "--------------------\nUtente: %s, tentativi usati: %d/12, vinta: %s\n\n%s\n--------------------");
		messages.get("it-it").put("bye", "Arrivederci!");

		if(!prettyPrinter) removePrettyCharFromMap(messages.get("it-it"));

	}

	private void initEN_ENMessages(boolean prettyPrinter) {
		messages.put("en-en", new HashMap<>());
		messages.get("en-en").put("unable_to_connect", "[ @RError@0 ] - Unable to connect to the server! Check IP and tcpPort or try again later!");
		messages.get("en-en").put("unable_to_connect_ex", "[ Error ] - Unable to connect to the server! Check IP and tcpPort or try again later!");
		messages.get("en-en").put("unable_to_connect_rmi", "[ @RError@0 ] - Unable to connect to RMI services! Check IP and tcpPort or try again later!");
		messages.get("en-en").put("unable_to_connect_rmi_ex", "[ Error ] - Unable to connect to RMI services! Check IP and tcpPort or try again later!");
		messages.get("en-en").put("banner", """


				 @M__    __                   _  _      \s
				/ / /\\ \\ \\  ___   _ __   __| || |  ___\s
				\\ \\/  \\/ / / _ \\ | '__| / _` || | / _ \\
				 \\  /\\  / | (_) || |   | (_| || ||  __/
				  \\/  \\/   \\___/ |_|    \\__,_||_| \\___|@0
				                                      \s""");
		messages.get("en-en").put("login_menu", "1. Sign up 2. Login 0. Exit\n@C>>>@0 ");
		messages.get("en-en").put("game_menu", "1. Play 2. Send word 3. Show ranking 4. Read notifications (@C%d@0) 5. Enable/disable statistics 0. Logout\n@C>>>@0 ");
		messages.get("en-en").put("enter_valid_number", "[ @RError@0 ] - Enter a valid number\n@C>>>@0 ");
		messages.get("en-en").put("enter_username", "Enter username\n@C>>>@0 ");
		messages.get("en-en").put("enter_password", "Enter password\n@C>>>@0 ");
		messages.get("en-en").put("fatal_error", "[ @RError@0 ] - Fatal error, exiting...");
		messages.get("en-en").put("response_not_valid", "[ @RError@0 ] - Response not valid, try again later");
		messages.get("en-en").put("registration_successful", "[ @GSuccess@0 ] - Registration successful!");
		messages.get("en-en").put("already_registered", "[ @RError@0 ] - User already registered!");
		messages.get("en-en").put("login_successful", "[ @GSuccess@0 ] - Welcome @M%s@0!");
		messages.get("en-en").put("login_unsuccessful", "[ @RError@0 ] - Wrong credentials");
		messages.get("en-en").put("already_logged_in", "[ @RError@0 ] - Already logged in!");
		messages.get("en-en").put("internal_server_error", "[ @RError@0 ] - Internal server error");
		messages.get("en-en").put("word_timeout", "[ @YWord Timeout@0 ] - Time for guessing word %d is over, start a new game!");
		messages.get("en-en").put("game_started_successfully", "[ @GSuccess@0 ] - Started game number %d!");
		messages.get("en-en").put("game_restarted_successfully", "[ @GSuccess@0 ] - Restarted game number %d!");
		messages.get("en-en").put("game_closed", "[ @RError@0 ] - Unable to restart game, wait for a new word!");
		messages.get("en-en").put("game_already_won", "[ @RError@0 ] - Game already won, wait for a new word!");
		messages.get("en-en").put("game_lost", "[ @RGame over!@0 ] - Better luck next time! Translation -> %s");
		messages.get("en-en").put("already_playing", "[ @RError@0 ] - Game already started!");
		messages.get("en-en").put("not_authorized", "[ @RError@0 ] - Not authorized!");
		messages.get("en-en").put("start_new_game", "[ @RError@0 ] - Start a new game first!");
		messages.get("en-en").put("insert_word", "Enter a 10-letter word\n@C>>>@0 ");
		messages.get("en-en").put("victory", "[ @GVictory!@0 ] - You guessed the word! Translation -> %s");
		messages.get("en-en").put("try_again", "[ @YTry again!@0 ] - You have %d/12 attempts remaining!");
		messages.get("en-en").put("try_again_not_exists", "[ @YTry again!@0 ] - The word you sen is not in the dictionary, you have %d/12 attempts!");
		messages.get("en-en").put("logout_successful", "[ @GSuccess@0 ] - See you @M%s@0!");
		messages.get("en-en").put("press_enter_to_continue", "@IPress enter to continue@0");
		messages.get("en-en").put("notifications_enabled", "[ @GSuccess@0 ] - Notifications enabled!");
		messages.get("en-en").put("notifications_disabled", "[ @GSuccess@0 ] - Notifications disabled!");
		messages.get("en-en").put("ranking_notification", "\n[ @CRanking update@0 ] - The ranking has been updated!");
		messages.get("en-en").put("trying_to_reconnect", "[ @MReconnecting@0 ] - Trying to reconnect...");
		messages.get("en-en").put("reconnection_successful", "[ @GReconnected@0 ] - Reconnected to the server!");
		messages.get("en-en").put("cannot_join_multicast", "[ @RError@0 ] - Cannot connect to multicast group!");
		messages.get("en-en").put("cannot_receive_multicast", "[ @RError@0 ] - Cannot read from MulticastSocket, closing the thread!");
		messages.get("en-en").put("stats", "@BOUserStatistics:@0Games played: @M%d@0 - Percent games won: @M%.2f%c@0 - Last streak: @M%d@0 - Longest streak: @M%d@0");
		messages.get("en-en").put("time", "Next word extracted at: @BO%s@0");
		messages.get("en-en").put("token_expired", "[ @RError@0 ] - Session has expired, please login!");
		messages.get("en-en").put("user", "User -> @G%s@0");
		messages.get("en-en").put("share_prompt", "Do you want to share this game? [y/n] ");
		messages.get("en-en").put("share_successful", "[ @GSuccess@0 ] - Results shared successfully!");
		messages.get("en-en").put("share_body", "--------------------\nUser: %s, attempts: %d/12, won: %s\n\n%s\n--------------------");
		messages.get("en-en").put("bye", "See you!");

		if(!prettyPrinter) removePrettyCharFromMap(messages.get("en-en"));
	}

}
