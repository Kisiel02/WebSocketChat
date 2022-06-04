package kisiel.jakub.websocketchat.server.textMessage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import kisiel.jakub.websocketchat.ConfigDTO;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.client.ChatGuiController;
import kisiel.jakub.websocketchat.client.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class TextMessageService {

    private final Logger logger = LoggerFactory.getLogger(TextMessageService.class);

    private final ChatGuiController chatGuiController;

    private final SecurityManager securityManager;

    private final ConnectionManager connectionManager;

    private Gson gson;

    @Autowired
    public TextMessageService(ChatGuiController chatGuiController, SecurityManager securityManager, ConnectionManager connectionManager) {
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
        this.chatGuiController.addLine(message.getText());
    }

    public void handleConfigMessage(String message) throws NoSuchAlgorithmException, InvalidKeySpecException {;
        ConfigDTO configDTO = gson.fromJson(message, ConfigDTO.class);

        if (configDTO.getType().equals(ConfigDTO.messageType.PUBLIC_KEY_AND_PORT)) {
            //First connection message, need to generate RSA keys and save received public key and connect back
            //logger.info("Odebrałem " + configDTO.getPublicKey());
            this.securityManager.initializeSecurity("RSA", 1024);
            this.securityManager.saveForeignKey(configDTO);
            this.connectionManager.backConnection(configDTO.getPort(), securityManager.getPublicKey());
        } else if (configDTO.getType().equals(ConfigDTO.messageType.PUBLIC_KEY)) {
            //Received a back connection, need to send generated session key and store received public key
            this.securityManager.saveForeignKey(configDTO);
            this.connectionManager.exportSessionKey();
        }
        else if (configDTO.getType().equals(ConfigDTO.messageType.SESSION_KEY)) {
            //Received session key, decrypt it and store
            //byte[] encryptedSessionKey = configDTO.getSessionKey();
            //logger.error("\nOdebrane bajty klucza sesji: " + encryptedSessionKey);
            //logger.error("\nOdebrane bajty klucza sesji: " + configDTO.getSessionKey());
            logger.error("\nOdebrany klucz sesji: " +  Base64.getEncoder().encodeToString(configDTO.getSessionKey()));
            //this.securityManager.setSessionKeyFromBytes(this.securityManager.decrypt(encryptedSessionKey));
//            this.securityManager.setSessionKeyFromBytes(
//                Base64.getDecoder().decode(configDTO.getSessionKey()));
            this.securityManager.setSessionKeyFromBytes(
                configDTO.getSessionKey());
        }
    }

   /* public void handleSessionKey(String message) {
        ConfigDTO configDTO = gson.fromJson(message, ConfigDTO.class);
        byte[] sessionKeyEncrypted = configDTO.getSessionKey();
        byte[] sessionKey = securityManager.decrypt(sessionKeyEncrypted);
        securityManager.setSessionKeyFromBytes(sessionKey);

    }*/


}
