package kisiel.jakub.websocketchat.server.textMessage;

import kisiel.jakub.websocketchat.client.ChatGuiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TextMessageService {

    private final ChatGuiController chatGuiController;

    @Autowired
    public TextMessageService(ChatGuiController chatGuiController) {
        this.chatGuiController = chatGuiController;
    }

    public void handleTextMessage(CustomMessage message) {
        this.chatGuiController.addLine(message.getText());
    }

}
