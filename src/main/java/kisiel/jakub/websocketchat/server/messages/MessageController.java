package kisiel.jakub.websocketchat.server.messages;

import com.google.gson.Gson;
import kisiel.jakub.websocketchat.messages.CustomMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Controller
public class MessageController {

    private final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService service;

    @Autowired
    public MessageController(MessageService service) {
        this.service = service;
    }

    @MessageMapping("/chat/messages")
    @SendTo("/topic/messages")
    public void textMessage(String message) {
        Gson gson = new Gson();
        CustomMessage textMessage = gson.fromJson(message, CustomMessage.class);
        logger.info("Received message: {}", textMessage.getContent());
        this.service.handleCustomMessage(textMessage);
    }

    @MessageMapping("/chat/config")
    @SendTo("/topic/messages")
    public void config(String message) {
        try {
            this.service.handleConfigMessage(message);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Wrong algorithm", e);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat/files")
    @SendTo("/topic/messages")
    public void receiveFile(String message) {
        this.service.handleChunk(message);
    }
}
