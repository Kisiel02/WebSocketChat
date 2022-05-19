package kisiel.jakub.websocketchat.server.textMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class TextMessageController {

    Logger logger = LoggerFactory.getLogger(TextMessageController.class);

//    @Autowired
//    SimpUserRegistry userRegistry;

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String textMessage(CustomMessage textMessage) throws InterruptedException, JsonProcessingException {
        //ObjectMapper mapper = new ObjectMapper();

        logger.info(textMessage.getText());
        //return mapper.writeValueAsString(textMessage);
        return textMessage.getText();
    }
}
