package Server;

import CommonUtils.PrettyPrinter;
import WordleRMI.RankingChanged;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WordFactory implements Runnable{
	private final ReentrantReadWriteLock currentWordLock;
	private final ReentrantReadWriteLock wordChangedTimestampLock;
	private long wordChangedTimestamp;
	private final long newWordTimeout;
	private final AtomicBoolean isRunning;
	private final List<String> words;
	private String currentWord;
	private final WordleDB database;
	private final RankingChanged rankingService;
	private LinkedList<String> ranking;
	
	public WordFactory(long newWordTimeout, List<String> words, WordleDB database, RankingChanged rankingService) {
		this.newWordTimeout = newWordTimeout;
		this.wordChangedTimestamp = 0;
		this.isRunning = new AtomicBoolean(true);
		this.words =  new ArrayList<>(words);
		this.database = database;
		this.currentWordLock = new ReentrantReadWriteLock();
		this.wordChangedTimestampLock = new ReentrantReadWriteLock();
		this.rankingService = rankingService;
		this.ranking = database.getRanking();
	}
	public void setIsRunning(boolean isRunning) {
		this.isRunning.set(isRunning);
	}

	public boolean wordExists(String word) {
		return this.words.contains(word);
	}

	public String getCurrentWord() {
		String result;
		currentWordLock.readLock().lock();
		result = currentWord;
		currentWordLock.readLock().unlock();
		return result;
	}
	public Long getWordChangedTimestamp() {
		long timestamp;
		wordChangedTimestampLock.readLock().lock();
		timestamp = wordChangedTimestamp;
		wordChangedTimestampLock.readLock().unlock();
		return timestamp;
	}
	public long getTimeUntilNewWord() {
		return (getWordChangedTimestamp() + newWordTimeout) - System.currentTimeMillis();
	}

	public char[] getHint(String word) {
		char[] hint = new char[word.length()];
		char[] curWord = currentWord.toCharArray();
		char[] sentWord = word.toCharArray();
		for (int i = 0; i < word.length(); i++) {
			if(sentWord[i] == curWord[i]) hint[i] = '+';
			else if(currentWord.contains(String.format("%c", sentWord[i]))) hint[i] = '?';
			else hint[i] = 'X';
		}
		return hint;
	}


	@Override
	public void run() {
		PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - GameFactory ready");
		Random random = new Random();
		String wordCandidate;
		String wordToReset;
		boolean firstWord = true;
		while (isRunning.get()) {
			if (System.currentTimeMillis() - wordChangedTimestamp >= newWordTimeout) {
				wordCandidate = words.get(random.nextInt(words.size()));
				while(database.wordAlreadyExtracted(wordCandidate))
					wordCandidate = words.get(random.nextInt(words.size()));

				currentWordLock.writeLock().lock();
				wordToReset = currentWord;
				currentWord = wordCandidate;
				currentWordLock.writeLock().unlock();

				wordChangedTimestampLock.writeLock().lock();
				wordChangedTimestamp = System.currentTimeMillis();
				wordChangedTimestampLock.writeLock().unlock();

				database.insertExtractedWord(currentWord);
				PrettyPrinter.prettyPrintln(String.format("[ @MNew Word@0 ] - Extracted new word: @C%s@0", wordCandidate));
				if(firstWord) {
					firstWord = false;
					continue;
				}
				database.resetStreaks(wordToReset);
			}

			// If the top 3 changed, then notify the clients
			LinkedList<String> newRanking = database.getRanking();
			try {
				for (int i = 0; i < ranking.size() && i < 3; i++) {
					if (!ranking.get(i).equals(newRanking.get(i))) {
						rankingService.notifyRanking();
						ranking = newRanking;
						break;
					}
				}
			} catch (RemoteException e) {
				PrettyPrinter.prettyPrintln("[ @RError@0 ] - Cannot notify ranking!");
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException ignored) {
				return;
			}
		}
		PrettyPrinter.prettyPrintln("[ @GInfo@0 ] - GameFactory closed");


	}
}

