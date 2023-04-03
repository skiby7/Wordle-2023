package WordleRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface RankingListenerInterface extends Remote {
	void rankingChanged() throws RemoteException;
}
