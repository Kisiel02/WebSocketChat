package kisiel.jakub.websocketchat.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import kisiel.jakub.websocketchat.ConfigDTO;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.server.textMessage.CustomMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Base64;

@Component
public class ConnectionManager {

    private final Logger logger = LogManager.getLogger(ConnectionManager.class);

    private static final String CONFIG_LOCATION = "/app/chat/config";

    private static final String MESSAGES_LOCATION = "/app/chat/messages";

    private ChatGuiController chatGuiController;

    private StompSession stompSession;

    private SecurityManager securityManager;

    private Gson gson;

    @Autowired
    public ConnectionManager(@Lazy ChatGuiController chatGuiController, SecurityManager securityManager) {
        this.chatGuiController = chatGuiController;
        this.securityManager = securityManager;
    }

    @PostConstruct
    public void initGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context) -> new JsonPrimitive(Base64.getEncoder().encodeToString(src)));
        builder.registerTypeAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) -> Base64.getDecoder().decode(json.getAsString()));
        this.gson = builder.create();
    }

    public void sendMessage(CustomMessage customMessage) {
        this.stompSession.send(MESSAGES_LOCATION, gson.toJson(customMessage));
    }

    public void sendConfig(ConfigDTO configDTO) {
        String message = gson.toJson(configDTO);
        //logger.info("\n Wysyłam strina z configiem:\n" + message);
        this.stompSession.send(CONFIG_LOCATION, message);
    }

    public void initConnection(int anotherPort, int ownPort) {
        if (connect(anotherPort)) {
            try {
                securityManager.initializeSecurity("RSA", 1024, "AES", 128);
                exportPublicKeyAndPort(securityManager.getPublicKey(), ownPort);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Wrong algorithm name", e);
            }
        }
    }

/*    public void backConnection(int anotherPort, PublicKey publicKey, SecretKey sessionKey) {
        if (anotherPort != 0 && connect(anotherPort)) {
            exportPublicAndSessionKeys(publicKey, sessionKey);
        }
    }*/

    //Connect back to the sender of config message, export your public key
    public void backConnection(int anotherPort, PublicKey publicKey) {
        if (connect(anotherPort)) {
            exportPublicKey(publicKey);
        }
    }

    //connect to given port
    public boolean connect(int port) {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(client);
            stompClient.setMessageConverter(new GsonMessageConverter());
            StompSessionHandler sessionHandler = new MyStompSessionHandler(chatGuiController);
            this.stompSession = stompClient.connect(String.format(
                "ws://localhost:%d/chat", port), sessionHandler).get();
            return stompSession.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public void exportSessionKey() {
        SecretKey sessionKey = this.securityManager.getSessionKey();
        String sessionKeyString = Base64.getEncoder().encodeToString(sessionKey.getEncoded());
        //logger.error("\nKlucz sesji przed enkrypcją: " + sessionKey.getEncoded());
        logger.error("\nKlucz sesji przed enkrypcją: " + sessionKeyString);
        ConfigDTO configDTO = new ConfigDTO();
        byte[] encryptedSessionKey =  this.securityManager.encrypt(sessionKey.getEncoded());
        configDTO.setSessionKey(sessionKey.getEncoded());
        //configDTO.setSessionKey(encryptedSessionKey);
        //configDTO.setSessionKey(sessionKeyString);
        configDTO.setType(ConfigDTO.messageType.SESSION_KEY);
        //logger.info("\nEksportuje klucz sesji: " + encryptedSessionKey);
        //logger.info("\nEksportuje klucz sesji: " + sessionKey.getEncoded());
        logger.info("\nEksportuje klucz sesji: " + sessionKeyString);
        sendConfig(configDTO);
    }

    private void exportPublicKeyAndPort(PublicKey publicKey, int ownPort) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.setPublicKey(publicKey.getEncoded());
        configDTO.setPort(ownPort);
        configDTO.setType(ConfigDTO.messageType.PUBLIC_KEY_AND_PORT);
        //logger.info("Wysyłam " + configDTO.getPublicKey());
        this.sendConfig(configDTO);
    }

   /* private void exportPublicAndSessionKeys(PublicKey publicKey, SecretKey secretKey) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.setPublicKey(publicKey.getEncoded());
        configDTO.setSessionKey(secretKey.getEncoded());
        this.stompSession.send(CONFIG_LOCATION, new Gson().toJson(
            configDTO));
    }*/

    private void exportPublicKey(PublicKey publicKey) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.setPublicKey(publicKey.getEncoded());
        configDTO.setType(ConfigDTO.messageType.PUBLIC_KEY);
        this.sendConfig(configDTO);
    }
}
