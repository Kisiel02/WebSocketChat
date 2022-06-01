package kisiel.jakub.websocketchat.server.textMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class TextMessageController {

    Logger logger = LoggerFactory.getLogger(TextMessageController.class);

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String textMessage(String message) {
        Gson gson = new Gson();
        CustomMessage textMessage = gson.fromJson(message, CustomMessage.class);
        logger.info(textMessage.getText());
        return gson.toJson(textMessage);
    }
}
