package kisiel.jakub.websocketchat.server.textMessage;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Controller
public class TextMessageController {

    private final Logger logger = LoggerFactory.getLogger(TextMessageController.class);

    private final TextMessageService service;

    @Autowired
    public TextMessageController(TextMessageService service) {
        this.service = service;
    }

    @MessageMapping("/chat/messages")
    @SendTo("/topic/messages")
    public void textMessage(String message) {
        Gson gson = new Gson();
        CustomMessage textMessage = gson.fromJson(message, CustomMessage.class);
        logger.info("Received message: {}", textMessage.getText());
        this.service.handleTextMessage(textMessage);

       // return gson.toJson(textMessage);
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
}
