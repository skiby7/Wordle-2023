package Models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class WordsExtractedModel implements Serializable {
	private int ID;
	private Timestamp date;
	private String word;

	public WordsExtractedModel(@JsonProperty("ID") int ID,
							   @JsonProperty("date") long date,
							   @JsonProperty("word") String word) {
		this.ID = ID;
		this.date = new Timestamp(date);
		this.word = word;
	}

	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public Timestamp getDate() {
		return date;
	}

	public void setDate(Timestamp date) {
		this.date = date;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WordsExtractedModel that = (WordsExtractedModel) o;
		return word.equals(that.word);
	}

	@Override
	public int hashCode() {
		return Objects.hash(word);
	}
}
