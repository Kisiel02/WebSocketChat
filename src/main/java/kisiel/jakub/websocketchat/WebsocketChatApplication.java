package kisiel.jakub.websocketchat;

import javafx.application.Application;
import kisiel.jakub.websocketchat.client.GuiApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebsocketChatApplication {

    public static void main(String[] args) {
        //SpringApplication.run(WebsocketChatApplication.class, args);
        Application.launch(GuiApplication.class, args);
    }

}
