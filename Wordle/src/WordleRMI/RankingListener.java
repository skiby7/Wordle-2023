package WordleRMI;

import Client.UserMessages;
import CommonUtils.PrettyPrinter;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;


public class RankingListener extends RemoteObject implements RankingListenerInterface {
	private final UserMessages messages;
	public RankingListener(UserMessages messages) throws RemoteException {
		super();
		this.messages = messages;
	}

	@Override
	public void rankingChanged() throws RemoteException {
		PrettyPrinter.prettyPrintln(messages.message("ranking_notification"));
	}
}
