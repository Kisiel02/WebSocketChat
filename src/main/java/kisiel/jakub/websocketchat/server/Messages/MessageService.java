package kisiel.jakub.websocketchat.server.Messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import kisiel.jakub.websocketchat.messages.ConfigDTO;
import kisiel.jakub.websocketchat.messages.CustomMessage;
import kisiel.jakub.websocketchat.messages.FileMessage;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.client.ChatGuiController;
import kisiel.jakub.websocketchat.client.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

@Service
public class MessageService {

    private final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final ChatGuiController chatGuiController;

    private final SecurityManager securityManager;

    private final ConnectionManager connectionManager;

    private Gson gson;

    @Autowired
    public MessageService(ChatGuiController chatGuiController, SecurityManager securityManager, ConnectionManager connectionManager) {
        this.chatGuiController = chatGuiController;
        this.securityManager = securityManager;
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void initGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context) -> new JsonPrimitive(Base64.getEncoder().encodeToString(src)));
        builder.registerTypeAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) -> Base64.getDecoder().decode(json.getAsString()));
        this.gson = builder.create();
    }

    public void handleTextMessage(CustomMessage message) {
        String text = securityManager.decryptStringWithSessionKey(message.getText(), message.getMode());
        this.chatGuiController.addForeignLine(text);
    }

    public void handleConfigMessage(String message) throws NoSuchAlgorithmException, InvalidKeySpecException {
        ConfigDTO configDTO = gson.fromJson(message, ConfigDTO.class);

        if (configDTO.getType().equals(ConfigDTO.messageType.PUBLIC_KEY_AND_PORT)) {
            //First connection message, need to generate RSA keys and save received public key and connect back
            this.securityManager.initializeSecurity("RSA", 1024);
            this.securityManager.saveForeignKey(configDTO);
            this.connectionManager.backConnection(configDTO.getPort(), securityManager.getPublicKey());
        } else if (configDTO.getType().equals(ConfigDTO.messageType.PUBLIC_KEY)) {
            //Received a back connection, need to send generated session key and store received public key
            this.securityManager.saveForeignKey(configDTO);
            this.connectionManager.exportSessionKey();
        } else if (configDTO.getType().equals(ConfigDTO.messageType.SESSION_KEY)) {
            //Received session key, decrypt it and store
            byte[] encryptedSessionKey = configDTO.getSessionKey();
            String sessionKeyString = Base64.getEncoder().encodeToString(configDTO.getSessionKey());
            logger.debug("\nReceived encrypted session key: {}", sessionKeyString);
            byte[] decryptedSessionKey = this.securityManager.decryptWithPrivateKey(encryptedSessionKey);
            this.securityManager.setSessionKeyFromBytes(decryptedSessionKey);
            this.securityManager.setIv(new IvParameterSpec(configDTO.getIvVector()));
            String ivString = Base64.getEncoder().encodeToString(securityManager.getIv().getIV());
            logger.info("IV vector key: {}", ivString);
        }
    }

    public void handleChunk(String message) {
        FileMessage chunk = gson.fromJson(message, FileMessage.class);
        File file = new File(chunk.getFileName());

        // if first chunk, create file, otherwise append
        boolean append = chunk.getCounter() != 0;

        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, append))) {
            byte[] decrypted = securityManager.decryptFileWithSessionKey(chunk.getChunk(), chunk.getBlockMode());
            output.write(decrypted);

        } catch (FileNotFoundException e) {
            logger.error("Could not create file", e);
        } catch (IOException e) {
            logger.error("Could not write to file", e);
        }

//        if(chunk.isDone())
    }
}

