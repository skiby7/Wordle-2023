package Server;

import Client.Requests;
import CommonUtils.PrettyPrinter;
import Server.WordleDB;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.rowset.spi.SyncProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TestMain {

	public static HashMap<String, Object> getJsonFromApi(String host) throws IOException {
		URL url = new URL(host);
		ObjectMapper mapper = new ObjectMapper();
		InetAddress addr = InetAddress.getByName(url.getHost());
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.setRequestMethod("GET");
		HashMap<String, Object> responseMap;
		BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
		String response = reader.readLine();
		System.out.println(response);
		responseMap = mapper.readValue(response, HashMap.class);
		reader.close();
		return responseMap;
	}
	public static void main(String[] args){
		WordleDB json = new WordleDB("/home/leonardo/Documents/University/Reti/Laboratorio/Wordle_Project/Wordle/src/TestDatabase" , true, true, WordleDB.DatabaseType.JSON);
		WordleDB sqlite = new WordleDB("/home/leonardo/Documents/University/Reti/Laboratorio/Wordle_Project/Wordle/src/testDb-3.db", true, true, WordleDB.DatabaseType.SQLITE);
		System.out.println(json.getWord(1));
		System.out.println(sqlite.getWord(2));
		System.exit(0);
		if (args.length != 2) {
			System.out.println("usage: java TestMain /path/to/db /path/to/words");
			return;
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
		int threadNum = 13, testSize = 300000;
		PrettyPrinter.prettyPrintln("[ @CTEST JSON DATABASE@0 ] - Inizio test database json");



		LinkedList<String> users = new LinkedList<>();
		Random random = new Random();

		for (int i = 0; i < 10; i++) users.add(String.valueOf(Math.abs(random.nextInt())));
		try {
			Scanner s = new Scanner(new File(args[1]));
			ArrayList<String> words = new ArrayList<>();
			while (s.hasNext()) words.add(s.next());

			final AtomicLong sqliteTime = new AtomicLong(), jsonTime = new AtomicLong();
			List<Long> jsonTimes = Collections.synchronizedList(new ArrayList<>());
			List<Long> sqliteTimes = Collections.synchronizedList(new ArrayList<>());

			System.out.printf("Starting test with %d threadMax and %d entries...\n", threadNum - 1,  testSize);
			ThreadPoolExecutor sqliteExecutor = null;
			ThreadPoolExecutor jsonExecutor = null;
			WordleDB jsonDatabase;
			WordleDB sqliteDatabase = null;
			for (int t = 4; t < threadNum; t += 2 ) {
				if (t == 3) t = 2;
				PrettyPrinter.prettyPrintln("@CStarting test with " + t + " workers@0");
				sqliteExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(t);
				jsonExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(t);
				jsonDatabase = new WordleDB(args[0] + "/Json-" + simpleDateFormat.format(new Timestamp(System.currentTimeMillis())) + "-" + t , true, true, WordleDB.DatabaseType.JSON);
				sqliteDatabase = new WordleDB(args[0] + "/sqlite-" + simpleDateFormat.format(new Timestamp(System.currentTimeMillis())) + "-" + t + ".db", true, true, WordleDB.DatabaseType.SQLITE);
				WordleDB finalJsonDatabase = jsonDatabase;
				Runnable jsonTask = () -> {
					long timer = 0;
					for (String user: users) {
						finalJsonDatabase.insertUser(user, user, "user");
						for (String word: words) {
							if (jsonTimes.size() >= testSize) return;
							timer = System.nanoTime();
							finalJsonDatabase.insertGame(user, word);
							timer = System.nanoTime() - timer;
							jsonTimes.add(timer);
						}
					}
				};
				WordleDB finalSqliteDatabase = sqliteDatabase;
				AtomicBoolean committing = new AtomicBoolean(false);
				Runnable sqliteTask = () -> {
					long timer = 0;
					for (String user: users) {
						finalSqliteDatabase.insertUser(user, user, "user");
						for (String word: words) {
							if (sqliteTimes.size() >= testSize) {
								if (!committing.get()) {
									committing.set(true);
									System.out.printf("Committing the transactions took %d ns\n", timer);
								}
								return;
							}
							timer = System.nanoTime();
							finalSqliteDatabase.insertGame(user, word);
							timer = System.nanoTime() - timer;
							sqliteTimes.add(timer);
						}
					}
				};
				PrettyPrinter.prettyPrintln("@GStarting sqlite test...@0");
				for (int i = 0; i < t; i++) {
					sqliteExecutor.execute(sqliteTask);
				}

				long lastJsonWords = 0, lastSqliteWords = 0;
				boolean jsonFinished = false, sqliteFinished = false, jsonTestStarted = false;

				while (!(jsonFinished && sqliteFinished)) {
					List<Long> jsonSeries = new ArrayList<>(jsonTimes);
					List<Long> sqliteSeries = new ArrayList<>(sqliteTimes);
					if (jsonSeries.size() >= testSize) {
						if ((t == 1 ? t + 1 : t + 2) < threadNum)
							PrettyPrinter.prettyPrintln("@MFinished json test, restarting with " + (t == 1 ? t + 1 : t + 2) + " workers...");
						jsonFinished = true;
					}
					if (sqliteSeries.size() >= testSize) {
						sqliteFinished = true;
						if (!jsonTestStarted) {
							PrettyPrinter.prettyPrintln("@MFinished sqlite test, starting json test...");
							Thread.sleep(1500);
							jsonTestStarted = true;
							for (int i = 0; i < t; i++) {
								jsonExecutor.execute(jsonTask);
							}
						}
					}
					double jsonAvg = (double) jsonSeries.stream().mapToLong(Long::longValue).sum() / jsonSeries.size();
					double sqliteAvg = (double) sqliteSeries.stream().mapToLong(Long::longValue).sum() / sqliteSeries.size();
					long jsonWords = jsonSeries.size();
					long sqliteWords = sqliteSeries.size();
					if (jsonTestStarted && jsonWords - lastJsonWords > 200) {
						PrettyPrinter.prettyPrintln(String.format("@C%d@0 jsonWordProcessed %d workers - @M%.2f@0 avg time per transaction (ms)", jsonWords, t, jsonAvg / 1000000));
						lastJsonWords = jsonWords;
					}
					if (!sqliteFinished && sqliteWords - lastSqliteWords > 500) {
						PrettyPrinter.prettyPrintln(String.format("@C%d@0 sqliteWordProcessed %d workers - @M%.2f@0 avg time per transaction (ms)", sqliteWords, t, sqliteAvg / 1000000));
						lastSqliteWords = sqliteWords;
					}
					Thread.sleep(300);
				}
				ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(new File(args[0] + "/jsonSeries-" + simpleDateFormat.format(new Timestamp(System.currentTimeMillis())) + "-" +  t + ".json"), jsonTimes.subList(0, testSize));
				mapper.writeValue(new File(args[0] + "/sqliteSeries-" + simpleDateFormat.format(new Timestamp(System.currentTimeMillis())) + "-" +  t + ".json"), sqliteTimes.subList(0, testSize));
				jsonTimes.clear();
				sqliteTimes.clear();
				PrettyPrinter.prettyPrintln("@YFinished test with " + t + " workers@0");
			}
			sqliteDatabase.closeDatabase();
			System.out.println("Qui");
			jsonExecutor.shutdownNow();
			sqliteExecutor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
