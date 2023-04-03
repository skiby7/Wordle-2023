package Client;
import Server.ServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.rmi.*;

public class ClientMain  {

	public static ClientConfig loadConfig(String path) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(new File(path), ClientConfig.class);
	}
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please provide a valid config file");
			System.exit(1);
		}
		ClientConfig clientConfig = null;
		try {
			clientConfig = loadConfig(args[0]);
		} catch (IOException e) {
			System.out.println("Client configuration not valid, exiting...");
			System.exit(1);
		}

		Client client = null;
		try {
			client = new Client(clientConfig);
			client.start();
		} catch (RemoteException e) {
			System.out.println("RMI tcpPort not valid!");
			e.printStackTrace();
		} catch (NotBoundException e) {
			System.out.println("RMI endpoints not valid!");
			e.printStackTrace();
		} catch (UserMessages.LanguageNotSupported e) {
			System.out.printf("Language %s not supported!", clientConfig.language());
			e.printStackTrace();
		} catch (SocketException e) {
			System.out.printf("Cannot find network interface %s!", clientConfig.networkInterface());
			e.printStackTrace();
		}
	}
}
