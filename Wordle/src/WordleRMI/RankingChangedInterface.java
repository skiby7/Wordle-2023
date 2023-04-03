package WordleRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface RankingChangedInterface extends Remote {
	void notifyRanking() throws RemoteException;
	void addListener(RankingListenerInterface listener) throws RemoteException;
	void removeListener(RankingListenerInterface listener) throws RemoteException;
}
