package kisiel.jakub.websocketchat.client;

import com.google.gson.Gson;
import kisiel.jakub.websocketchat.messages.CustomMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private final Logger logger = LogManager.getLogger(MyStompSessionHandler.class);

    private ChatGuiController chatGuiController;

    public MyStompSessionHandler(ChatGuiController chatGuiController) {
        this.chatGuiController = chatGuiController;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("New session established : " + session.getSessionId());
        session.subscribe("/topic/messages", this);
        logger.info("Subscribed to /topic/messages");
        chatGuiController.notifyAboutConnection();
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        Gson gson = new Gson();
        CustomMessage message = (CustomMessage)gson.fromJson(String.valueOf(payload), CustomMessage.class);
        this.logger.info(message.getText());
        this.chatGuiController.addOwnLine(message.getText());
    }
}
