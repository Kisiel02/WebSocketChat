package kisiel.jakub.websocketchat.client;

import com.google.gson.Gson;
import kisiel.jakub.websocketchat.server.textMessage.CustomMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;

@Component
public class ConnectionManager {

    private ChatGuiController chatGuiController;

    private StompSession stompSession;

    public ConnectionManager(@Lazy ChatGuiController chatGuiController) {
        this.chatGuiController = chatGuiController;
    }

    public void send(CustomMessage customMessage) {
        this.stompSession.send("/app/chat/messages", new Gson().toJson(customMessage));
    }

    public void connect(int port) throws ExecutionException, InterruptedException {
        WebSocketClient client = new StandardWebSocketClient();

        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new GsonMessageConverter());

        StompSessionHandler sessionHandler = new MyStompSessionHandler(chatGuiController);
        this.stompSession = stompClient.connect(String.format(
                        "ws://localhost:%d/chat", port), sessionHandler).get();

    }
}
