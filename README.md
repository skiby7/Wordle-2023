# Utilizzo e parametri di configurazione

## Compilazione ed esecuzione

Per realizzare e testare il progetto è stata utilizzata la versione 16 di OpenJDK in ambiente Linux.
Una volta posizionati nella root del progetto, i comandi necessari alla compilazione sono:

```bash
find . -name "*.java" > ./files.txt
javac -d "./build" \ 
-cp "./libs/jackson-core-2.13.0.jar:./libs/jackson-annotations-2.13.0.jar:./libs/jackson-databind-2.13.0.jar:./libs/sqlite-jdbc-3.40.0.0.jar" \
 @files.txt
```

Per eseguire il progetto appena compilato, basterà dare i comandi:

```bash
# Client
java -cp "./libs/jackson-annotations-2.13.3.jar:./libs/jackson-core-2.13.3.jar:./libs/jackson-databind-2.13.3.jar:./build/" \
Client.ClientMain </path/to/config.json>

# Server
java -cp \
"./libs/jackson-annotations-2.13.3.jar:./libs/jackson-core-2.13.3.jar:./libs/jackson-databind-2.13.3.jar:./libs/sqlite-jdbc-3.40.0.0.jar:./build/" \
Server.ServerMain </path/to/config.json>
```

In alternativa è possibile spostarsi nella root del progetto, eseguire lo script si compilazione `compileWordle.sh` per poi eseguire `execServer.sh` e `execClient.sh` passando come argomento il path dei file di configurazione. Potrebbe essere necessario aggiungere i permessi di esecuzione con il comando `chmod +x *.sh`
Per quanto riguarda la configurazione degli applicativi ho deciso di utilizzare il formato JSON de-serializzato nelle classi `ServerConfig` e `ClientConfig`, entrambe implementate come Record.

### Problemi noti

Durante la fase di test del progetto ho riscontrato i seguenti problemi:

* Per far funzionare la comunicazione attraverso il gruppo multicast quando si esegue il client e il server sulla stessa macchina, è stato necessario, almeno nel mio caso su Fedora 37, disabilitare momentaneamente il firewall^[Si potrebbero creare anche nuove regole, ma per un semplice test ho preferito fare così.]. Se il client e il server sono in esecuzione su due computer diversi, invece, i pacchetti non vengono ricevuti dai client connessi.
* Lo stub esportato dal client per registrare la callback RMI sul server potrebbe bloccare l'esecuzione del client se si testa il progetto eseguendo il server e il/i client su pc diversi. Il client, se ha più interfacce di rete, non è detto che sceglierà quella corretta, infatti nel mio caso testando il progetto connesso alla rete wifi (interfaccia n. 2, `wlp59s0`), non avevo problemi, mentre adoperando un adattatore per l'utilizzo del cavo lan (interfaccia n. 46 `enp0s20f0u2u3c2`), il client si fermava sulla chiamata `remoteRankingObject.addListener(rankingListenerStub)` e tramite il debugger ho visto che l'indirizzo ip usato era quello relativo a un'interfaccia di rete virtuale  (interfaccia n. 8, `br-05695febda49`).


## Configurazione del Server

Di seguito un esempio di configurazione del server:
```json
{
  "tcpPort" : 6789,
  "rmiPort" : 3800,
  "multicastPort" : 7800,
  "multicastAddress" : "224.0.0.1",
  "corePoolSize" : 4, // corePoolSize del cachedTreadPool che gestirà le richieste
  "secretWordTimeout" : 120000, // Intervallo in millisecondi fra l'estrazione di due SW 
  "verbose" : false, // Se true, stampa a schermo ogni richiesta in arrivo dal client e relativa risposta
  "sqliteDatabasePath" : "/home/leonardo/Documents/University/Reti/Laboratorio/Wordle_Project/Wordle/src/testDb-3.db",
  "autoCreateDatabase" : true, // Se true, crea i file necessari per il funzionamento del DB
  "autoCommitDatabase" : false, // Se true viene attivato l'auto-commit del database, vedere sezione 2.4
  "wordsFilePath" : "/home/leonardo/Documents/University/Reti/Laboratorio/Wordle_Project/Wordle/src/words.txt",
  "useJsonDatabase" : true,
  "jsonDatabasePath" : "/home/leonardo/Documents/University/Reti/Laboratorio/Wordle_Project/Wordle/src/JsonDatabase",
  "debug" : "true" // Se true, viene abilitato l'endpoint getCurrentWord
}
```
Anche se può sembrare ridondante, ho deciso di mantenere due voci separate per i path dei database in modo da poter testare più agevolmente entrambe le soluzioni senza dover modificare la configurazione ogni volta che si va a cambiare il flag `useJsonDatabase`.

**N.B.**: Tutti i campi sono necessari affinché il server vada in esecuzione.

## Configurazione del Client

Di seguito un esempio di configurazione del client:
```json
{
  "tcpPort" : 6789,
  "rmiPort" : 3800,
  "maxRetries" : 5, // Numero massimo di tentativi di riconnessione
  "reconnectionTimeout" : 1500, // Timeout fra un tentativo e l'altro
  "networkInterface" : "wlp59s0", // Interfaccia su cui ricevere i DatagramPackets
  "serverIp" : "localhost",
  "rmiRegistrationEndpoint" : "REGISTRATION",
  "rmiRankingEndpoint" : "RANKING",
  "language" : "it-it",
  "prettyPrint" : true // Se true, saranno abilitati i colori nella CLI
}
```
Le lingue supportate sono l'italiano (`it-it`) e l'inglese (`en-en`). 

**N.B.**: Tutti i campi sono necessari affinché il client vada in esecuzione.