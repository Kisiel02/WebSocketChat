package kisiel.jakub.websocketchat.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import kisiel.jakub.websocketchat.messages.ConfigDTO;
import kisiel.jakub.websocketchat.messages.FileMessage;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.messages.CustomMessage;
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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
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
        String text = customMessage.getText();
        String encrypted = securityManager.encryptStringWithSessionKey(text, customMessage.getMode());
        customMessage.setText(encrypted);
        logger.info("Sending message {}", encrypted);
        this.stompSession.send(MESSAGES_LOCATION, gson.toJson(customMessage));
    }

    public void sendConfig(ConfigDTO configDTO) {
        String message = gson.toJson(configDTO);
        this.stompSession.send(CONFIG_LOCATION, message);
    }

    public void sendChunk(FileMessage fileMessage) {
        String message = gson.toJson(fileMessage);
        this.stompSession.send(FILE_LOCATION, message);
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

    public FileUpdateTask sendFile(File file, SecurityManager.BlockMode mode) {
        FileUpdateTask fileUpdateTask = new FileUpdateTask(BUFFER_SIZE, file,securityManager, mode, this);
        return fileUpdateTask;
    }

    public void exportSessionKey() {
        SecretKey sessionKey = this.securityManager.getSessionKey();
        ConfigDTO configDTO = new ConfigDTO();
        byte[] encryptedSessionKey = this.securityManager.encryptWithForeignKey(sessionKey.getEncoded());
        configDTO.setSessionKey(encryptedSessionKey);
        configDTO.setType(ConfigDTO.messageType.SESSION_KEY);
        configDTO.setIvVector(securityManager.getIv().getIV());
        String sessionKeyString = Base64.getEncoder().encodeToString(encryptedSessionKey);
        logger.debug("\nExporting encrypted session key: {}", sessionKeyString);
        sendConfig(configDTO);
    }

    private void exportPublicKeyAndPort(PublicKey publicKey, int ownPort) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.setPublicKey(publicKey.getEncoded());
        configDTO.setPort(ownPort);
        configDTO.setType(ConfigDTO.messageType.PUBLIC_KEY_AND_PORT);
        this.sendConfig(configDTO);
    }

    private void exportPublicKey(PublicKey publicKey) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.setPublicKey(publicKey.getEncoded());
        configDTO.setType(ConfigDTO.messageType.PUBLIC_KEY);
        this.sendConfig(configDTO);
    }
}
