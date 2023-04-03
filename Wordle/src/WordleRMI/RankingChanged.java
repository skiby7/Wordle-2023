package WordleRMI;

import CommonUtils.PrettyPrinter;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RankingChanged extends RemoteServer implements RankingChangedInterface {
	private final List<RankingListenerInterface> listeners;
	public RankingChanged() throws RemoteException {
		super();
		listeners = new ArrayList<>();
	}

	@Override
	public void notifyRanking() throws RemoteException {
		LinkedList<RankingListenerInterface> toRemove = new LinkedList<>();
		for (RankingListenerInterface listener : listeners) {
			try {
				listener.rankingChanged();
			}
			catch (RemoteException e) {
				toRemove.add(listener);
			}
		}
		for (RankingListenerInterface listener : toRemove) {
			listeners.remove(listener);
		}
	}

	@Override
	public void addListener(RankingListenerInterface listener) {
		listeners.add(listener);
		PrettyPrinter.prettyPrintln("[ @CRMI@0 ] - New client added to notification service!");
	}

	@Override
	public void removeListener(RankingListenerInterface listener) {
		listeners.remove(listener);
		PrettyPrinter.prettyPrintln("[ @CRMI@0 ] - Client removed from notification service!");
	}


}

