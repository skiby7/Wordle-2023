package Client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClientConfig(int tcpPort, int rmiPort, int maxRetries, int reconnectionTimeout, String networkInterface, String serverIp, String rmiRegistrationEndpoint, String rmiRankingEndpoint, String language, boolean prettyPrint) {
	public ClientConfig(@JsonProperty(value = "tcpPort", required = true) int tcpPort,
						@JsonProperty(value = "rmiPort", required = true) int rmiPort,
						@JsonProperty(value = "maxRetries", required = true) int maxRetries,
						@JsonProperty(value = "reconnectionTimeout", required = true) int reconnectionTimeout,
						@JsonProperty(value = "networkInterface", required = true) String networkInterface,
						@JsonProperty(value = "serverIp", required = true) String serverIp,
						@JsonProperty(value = "rmiRegistrationEndpoint", required = true) String rmiRegistrationEndpoint,
						@JsonProperty(value = "rmiRankingEndpoint", required = true) String rmiRankingEndpoint,
						@JsonProperty(value = "language", required = true) String language,
						@JsonProperty(value = "prettyPrint", required = true) boolean prettyPrint) {
		this.tcpPort = tcpPort;
		this.rmiPort = rmiPort;
		this.maxRetries = maxRetries;
		this.reconnectionTimeout = reconnectionTimeout;
		this.networkInterface = networkInterface;
		this.serverIp = serverIp;
		this.rmiRegistrationEndpoint = rmiRegistrationEndpoint;
		this.rmiRankingEndpoint = rmiRankingEndpoint;
		this.language = language;
		this.prettyPrint = prettyPrint;
	}


}
