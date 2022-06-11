package kisiel.jakub.websocketchat.server.messages;

import com.google.gson.Gson;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.client.ChatGuiController;
import kisiel.jakub.websocketchat.client.ConnectionManager;
import kisiel.jakub.websocketchat.messages.ConfigMessage;
import kisiel.jakub.websocketchat.messages.CustomMessage;
import kisiel.jakub.websocketchat.messages.FileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class MessageService {

    private final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final ChatGuiController chatGuiController;

    private final SecurityManager securityManager;

    private final ConnectionManager connectionManager;

    private Gson gson;

    private Instant start;

    @Autowired
    public MessageService(ChatGuiController chatGuiController, SecurityManager securityManager, ConnectionManager connectionManager, Gson gson) {
        this.chatGuiController = chatGuiController;
        this.securityManager = securityManager;
        this.connectionManager = connectionManager;
        this.gson = gson;
    }

    public void handleCustomMessage(CustomMessage message) {
        switch (message.getType()) {
            case TEXT -> {
                String text = securityManager.decryptStringWithSessionKey(message.getContent(), message.getMode());
                this.chatGuiController.addForeignLine(text);
            }
            case FILE_CONFIRM -> {
                String filename = securityManager.decryptStringWithSessionKey(message.getContent(), message.getMode());
                this.chatGuiController.addInfoLine("File " + filename + " send successfully");
            }
        }

    }

    public void handleConfigMessage(String message) throws NoSuchAlgorithmException, InvalidKeySpecException {
        ConfigMessage configMessage = gson.fromJson(message, ConfigMessage.class);

        if (configMessage.getType().equals(ConfigMessage.messageType.PUBLIC_KEY_AND_PORT)) {
            //First connection message, need to generate RSA keys and save received public key and connect back
            this.securityManager.initializeSecurity("RSA", 1024);
            this.securityManager.saveForeignKey(configMessage);
            this.connectionManager.backConnection(configMessage.getPort(), securityManager.getPublicKey());
        } else if (configMessage.getType().equals(ConfigMessage.messageType.PUBLIC_KEY)) {
            //Received a back connection, need to send generated session key and store received public key
            this.securityManager.saveForeignKey(configMessage);
            this.connectionManager.exportSessionKey();
        } else if (configMessage.getType().equals(ConfigMessage.messageType.SESSION_KEY)) {
            //Received session key, decrypt it and store
            byte[] encryptedSessionKey = configMessage.getSessionKey();
            String sessionKeyString = Base64.getEncoder().encodeToString(configMessage.getSessionKey());
            logger.debug("\nReceived encrypted session key: {}", sessionKeyString);
            byte[] decryptedSessionKey = this.securityManager.decryptWithPrivateKey(encryptedSessionKey);
            this.securityManager.setSessionKeyFromBytes(decryptedSessionKey);
            this.securityManager.setIv(new IvParameterSpec(configMessage.getIvVector()));
            String ivString = Base64.getEncoder().encodeToString(securityManager.getIv().getIV());
            logger.debug("IV vector key: {}", ivString);
        }
    }

    public void handleChunk(String message) {
        FileMessage chunk = gson.fromJson(message, FileMessage.class);
        File file = new File(chunk.getFileName());

        // if first chunk, create file, otherwise append
        boolean append = chunk.getCounter() != 0;
        //Instant start = Instant.now();

        if(chunk.getCounter() == 0) {
            start = Instant.now();
        }

        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, append))) {
            byte[] decrypted = securityManager.decryptFileWithSessionKey(chunk.getChunk(), chunk.getBlockMode());
            output.write(decrypted);

        } catch (FileNotFoundException e) {
            logger.error("Could not create file", e);
        } catch (IOException e) {
            logger.error("Could not write to file", e);
        }

        if (chunk.isDone()) {
            Instant end = Instant.now();
            long timeElapsed = Duration.between(start, end).toMillis();
            logger.info("Time of file saving with {} mode, size {} bytes: {} ms", chunk.getBlockMode(), file.length(), timeElapsed);
            CustomMessage customMessage = new CustomMessage();
            customMessage.setType(CustomMessage.Type.FILE_CONFIRM);
            customMessage.setContent(chunk.getFileName());
            customMessage.setMode(chunk.getBlockMode());

            connectionManager.sendMessage(customMessage);
            chatGuiController.addInfoLine("File " + chunk.getFileName() + " saved successfully");
        }
    }
}

