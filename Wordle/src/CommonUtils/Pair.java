package CommonUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public record Pair<T, U>(@JsonProperty("a") T a, @JsonProperty("b") U b) implements Serializable{

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return a.equals(pair.a) && b.equals(pair.b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, b);
	}
}
