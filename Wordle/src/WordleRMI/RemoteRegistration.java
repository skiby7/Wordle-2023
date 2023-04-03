package WordleRMI;

import CommonUtils.PrettyPrinter;
import Server.WordleDB;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

public class RemoteRegistration extends RemoteServer implements RemoteRegistrationInterface{
	private final WordleDB database;
	public RemoteRegistration(WordleDB database) throws RemoteException {
		super();
		this.database = database;
	}
	@Override
	public boolean registration(String user, String password) {
		if (user.isBlank() || password.isBlank()) return false;
		boolean registrationResult = database.insertUser(user, password, "user");
		PrettyPrinter.prettyPrintln(String.format("[ @CRMI REGISTRATION@0 ] - %s", user));
		return registrationResult;
	}
}
