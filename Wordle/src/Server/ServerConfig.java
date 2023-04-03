package Server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

public record ServerConfig(int tcpPort, int rmiPort, int multicastPort, String multicastAddress, int corePoolSize, int secretWordTimeout, boolean verbose, String sqliteDatabasePath, boolean autoCreateDatabase,
						   boolean autoCommitDatabase, String wordsFilePath, boolean useJsonDatabase, String jsonDatabasePath, boolean debug) {
	public static int BUFFER_SIZE = 4096;
	public ServerConfig(@JsonProperty(value = "tcpPort", required = true) int tcpPort,
						@JsonProperty(value = "rmiPort", required = true) int rmiPort,
						@JsonProperty(value = "multicastPort", required = true) int multicastPort,
						@JsonProperty(value = "multicastAddress", required = true) String multicastAddress,
						@JsonProperty(value = "corePoolSize", required = true) int corePoolSize,
						@JsonProperty(value = "secretWordTimeout", required = true) int secretWordTimeout,
						@JsonProperty(value = "verbose", required = true) boolean verbose,
						@JsonProperty(value = "sqliteDatabasePath", required = true) String sqliteDatabasePath,
						@JsonProperty(value = "autoCreateDatabase", required = true) boolean autoCreateDatabase,
						@JsonProperty(value = "autoCommitDatabase", required = true) boolean autoCommitDatabase,
						@JsonProperty(value = "wordsFilePath", required = true) String wordsFilePath,
						@JsonProperty(value = "useJsonDatabase", required = true) boolean useJsonDatabase,
						@JsonProperty(value = "jsonDatabasePath", required = true) String jsonDatabasePath,
						@JsonProperty(value = "debug", required = true) boolean debug) {
		this.tcpPort = tcpPort;
		this.rmiPort = rmiPort;
		this.multicastPort = multicastPort;
		this.multicastAddress = multicastAddress;
		this.corePoolSize = corePoolSize > 0 ? corePoolSize : 1;
		this.secretWordTimeout = secretWordTimeout;
		this.verbose = verbose;
		this.sqliteDatabasePath = sqliteDatabasePath;
		this.autoCreateDatabase = autoCreateDatabase;
		this.autoCommitDatabase = autoCommitDatabase;
		this.wordsFilePath = wordsFilePath;
		this.useJsonDatabase = useJsonDatabase;
		this.jsonDatabasePath = jsonDatabasePath;
		this.debug = debug;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		String config = "";
		try {
			config = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return config;
	}
}
