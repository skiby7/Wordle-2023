package Server;

import WordleRMI.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import CommonUtils.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerMain {
	public static ServerConfig loadConfig(String path) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(new File(path), ServerConfig.class);
	}

	public static void main(String[] args) {

		ServerConfig serverConfig = null;
		if(args.length == 0) {
			PrettyPrinter.prettyPrintln("[ @RError@0 ] - Missing argument </path/to/config>");
			System.exit(1);
		}
		PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Configuration file: " + args[0]);

		try {
			serverConfig = loadConfig(args[0]);
		} catch (IOException e) {
			PrettyPrinter.prettyPrintln("[ @RError@0 ] - Cannot read configuration. Exiting...\n-----\nStacktrace:\n" + e.getMessage());
			System.exit(1);
		}
		if(serverConfig == null) {
			PrettyPrinter.prettyPrintln("[ @RError@0 ] - Config file is empty. Exiting...");
			System.exit(1);
		}

		Scanner s = null;
		try {
			s = new Scanner(new File(serverConfig.wordsFilePath()));
		} catch (FileNotFoundException e) {
			PrettyPrinter.prettyPrintln("[ @RError@0 ] - Cannot find words.txt file. Exiting...");
			System.exit(1);
		}
		ArrayList<String> words = new ArrayList<>();
		while (s.hasNext()){
			words.add(s.next());
		}
		s.close();

		try {
			PrettyPrinter.prettyPrintln("@M~~~~~~~~~~~~[ @CConfig@0 @M]~~~~~~~~~~~~@0");
			PrettyPrinter.prettyPrintln("@C" + serverConfig.toString() + "@0");
			PrettyPrinter.prettyPrintln("@M~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~@0");

			WordleDB database = new WordleDB(serverConfig.useJsonDatabase() ? serverConfig.jsonDatabasePath() : serverConfig.sqliteDatabasePath(), serverConfig.autoCreateDatabase(), serverConfig.autoCommitDatabase(), serverConfig.useJsonDatabase() ? WordleDB.DatabaseType.JSON : WordleDB.DatabaseType.SQLITE);
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Database ready");

			RemoteRegistration regService = new RemoteRegistration(database);
			RemoteRegistrationInterface registrationStub = (RemoteRegistrationInterface) UnicastRemoteObject.exportObject(regService, 0); // 0 -> dynamically assign the port
			RankingChanged rankingService = new RankingChanged();
			RankingChangedInterface rankingStub = (RankingChangedInterface) UnicastRemoteObject.exportObject(rankingService, 0);
			LocateRegistry.createRegistry(serverConfig.rmiPort());
			Registry r = LocateRegistry.getRegistry(serverConfig.rmiPort());
			r.bind("REGISTRATION", registrationStub);
			r.bind("RANKING", rankingStub);
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - RMI ready for incoming connections");
			ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

			executor.setCorePoolSize(serverConfig.corePoolSize());
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - ThreadPool alive");

			LoginHandler loginHandler = new LoginHandler();


			WordFactory wordFactory = new WordFactory(serverConfig.secretWordTimeout(), words, database, rankingService);
			Thread wordFactoryThread = new Thread(wordFactory);
			wordFactoryThread.setDaemon(true);
			wordFactoryThread.start();

			Selector selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverSocketChannel.socket();
			serverSocket.bind(new InetSocketAddress(serverConfig.tcpPort()));
			serverSocketChannel.configureBlocking(false);

			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Listening on port " + serverConfig.tcpPort());
			AtomicBoolean mainRunning = new AtomicBoolean(true);
			Object shutdownSync = new Object();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				executor.shutdown();
				PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Threadpool closed");

				if (wordFactoryThread.isAlive()) {
					wordFactory.setIsRunning(false);
					wordFactoryThread.interrupt();
				}
				Iterator<SelectionKey> iterator = new HashSet<>(selector.keys()).iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					key.cancel();
				}
				PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - All keys cancelled");
				database.closeDatabase();
				PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Database closed");
				PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Waiting main thread to close");
				mainRunning.set(false);
				selector.wakeup();

				try {
					synchronized (shutdownSync) {
						shutdownSync.wait(2000);
					}
				} catch (InterruptedException ignored) {}

			}));

			while(mainRunning.get()){
				 if (selector.select() == 0)
                    continue;
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();
				while(iterator.hasNext()) {
					SelectionKey selectedKey = iterator.next();
					iterator.remove();
					if(!selectedKey.isValid()) {
						selectedKey.channel().close();
						selectedKey.cancel();
						continue;
					}
					if (selectedKey.isAcceptable()) {
						ServerSocketChannel server = (ServerSocketChannel) selectedKey.channel();
						SocketChannel com = server.accept();
						if (com == null) continue;
						com.configureBlocking(false);
						PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Connected client " + (com.getRemoteAddress().toString().substring(1)));
						com.register(selector, SelectionKey.OP_READ);
					} else if (selectedKey.isReadable()) {
						ByteBuffer buffer = ByteBuffer.allocate(ServerConfig.BUFFER_SIZE);
						SocketChannel com = (SocketChannel) selectedKey.channel();
						int bytesRead = com.read(buffer);
						if(bytesRead == -1) {
							PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Disconnected client " + (com.getRemoteAddress().toString().substring(1)));
							selectedKey.cancel();
							try {
								com.close();
							} catch (Exception ignored) {}
						}
						buffer.flip();
						executor.execute(new RequestHandler(selector, selectedKey, new String(buffer.array()).trim(), database, loginHandler, wordFactory, serverConfig));
					} else if (selectedKey.isWritable())
						RequestHandler.sendAttachment(selectedKey);
				}
			}
			selector.close();
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Selector closed");
			synchronized (shutdownSync) {
				shutdownSync.notifyAll();
			}
			PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - Shutdown completed, bye!");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}