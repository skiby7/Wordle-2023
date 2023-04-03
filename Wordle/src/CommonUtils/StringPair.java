package CommonUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public record StringPair(@JsonProperty("a") String a, @JsonProperty("b") String b) implements Serializable {

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringPair that = (StringPair) o;
		return Objects.equals(a, that.a) && Objects.equals(b, that.b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, b);
	}
}