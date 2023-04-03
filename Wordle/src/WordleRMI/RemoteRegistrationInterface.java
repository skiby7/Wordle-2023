package WordleRMI;

import java.rmi.*;
public interface RemoteRegistrationInterface extends Remote {
	boolean registration (String user, String hashedPassword) throws RemoteException;
}
