package kisiel.jakub.websocketchat.client;

import com.google.gson.Gson;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.messages.ConfigMessage;
import kisiel.jakub.websocketchat.messages.CustomMessage;
import kisiel.jakub.websocketchat.messages.FileMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

@Component
public class ConnectionManager {

    private final Logger logger = LogManager.getLogger(ConnectionManager.class);

    private static final String CONFIG_LOCATION = "/app/chat/config";

    private static final String FILE_LOCATION = "/app/chat/files";

    private static final String MESSAGES_LOCATION = "/app/chat/messages";

    private static final int BUFFER_SIZE = 1024 * 8;

    private ChatGuiController chatGuiController;

    private StompSession stompSession;

    private SecurityManager securityManager;

    private Gson gson;

    @Autowired
    public ConnectionManager(@Lazy ChatGuiController chatGuiController, SecurityManager securityManager, Gson gson) {
        this.chatGuiController = chatGuiController;
        this.securityManager = securityManager;
        this.gson = gson;
    }

    public void sendMessage(CustomMessage customMessage) {
        String text = customMessage.getContent();
        String encrypted = securityManager.encryptStringWithSessionKey(text, customMessage.getMode());
        customMessage.setContent(encrypted);
        logger.info("Sending message {}", encrypted);
        this.stompSession.send(MESSAGES_LOCATION, gson.toJson(customMessage));
    }

    public void sendConfig(ConfigMessage configMessage) {
        String message = gson.toJson(configMessage);
        this.stompSession.send(CONFIG_LOCATION, message);
    }

    public void sendChunk(FileMessage fileMessage) {
        String message = gson.toJson(fileMessage);
        this.stompSession.send(FILE_LOCATION, message);
    }

    public void initConnection(int anotherPort, int ownPort) throws ConnectionLostException {
        if (connect(anotherPort)) {
            try {
                securityManager.initializeSecurity("RSA", 1024, "AES", 128);
                exportPublicKeyAndPort(securityManager.getPublicKey(), ownPort);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Wrong algorithm name", e);
            }
        } else {
            throw new ConnectionLostException("Could not connect");
        }

    }

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

    public FileUploadTask<Void> sendFile(File file, SecurityManager.BlockMode mode) {
        return new FileUploadTask<>(BUFFER_SIZE, file,securityManager, mode, this);
    }

    public void exportSessionKey() {
        SecretKey sessionKey = this.securityManager.getSessionKey();
        ConfigMessage configMessage = new ConfigMessage();
        byte[] encryptedSessionKey = this.securityManager.encryptWithForeignKey(sessionKey.getEncoded());
        configMessage.setSessionKey(encryptedSessionKey);
        configMessage.setType(ConfigMessage.messageType.SESSION_KEY);
        configMessage.setIvVector(securityManager.getIv().getIV());
        String sessionKeyString = Base64.getEncoder().encodeToString(encryptedSessionKey);
        logger.debug("\nExporting encrypted session key: {}", sessionKeyString);
        sendConfig(configMessage);
    }

    private void exportPublicKeyAndPort(PublicKey publicKey, int ownPort) {
        ConfigMessage configMessage = new ConfigMessage();
        configMessage.setPublicKey(publicKey.getEncoded());
        configMessage.setPort(ownPort);
        configMessage.setType(ConfigMessage.messageType.PUBLIC_KEY_AND_PORT);
        this.sendConfig(configMessage);
    }

    private void exportPublicKey(PublicKey publicKey) {
        ConfigMessage configMessage = new ConfigMessage();
        configMessage.setPublicKey(publicKey.getEncoded());
        configMessage.setType(ConfigMessage.messageType.PUBLIC_KEY);
        this.sendConfig(configMessage);
    }
}
