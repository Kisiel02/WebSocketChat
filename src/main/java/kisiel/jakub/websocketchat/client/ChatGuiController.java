package kisiel.jakub.websocketchat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import kisiel.jakub.websocketchat.server.textMessage.CustomMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ChatGuiController {

    private final Logger logger = LogManager.getLogger(ChatGuiController.class);

    @Autowired
    private ConnectionManager connectionManager;

    @FXML
    private Button connectButton;

//    @FXML
//    private ScrollPane scrollField;

    @FXML
    private Button sendButton;

    @FXML
    private TextField textField;

    @FXML
    private TextField port;

    @FXML
    private TextArea messages;

    @FXML
    public void connectButtonAction(ActionEvent event) {
        try {
            int portNumber = Integer.parseInt(port.getText());
            connectionManager.connect(portNumber);
        } catch (NumberFormatException e) {
            logger.error("Wrong port number", e);
        } catch (Exception e) {
            this.messages.setText("Could not connect");
        }
    }

    @FXML
    public void sendButtonAction(ActionEvent event) {
        String text = textField.getText();
        CustomMessage customMessage = new CustomMessage(
                text, CustomMessage.Type.TEXT
        );
        this.connectionManager.send(customMessage);
        textField.clear();
    }

    @FXML
    public void textFieldAction(ActionEvent event) {
        //TODO
        // enter to send
    }

    public void addLine(String line) {
        messages.setText(messages.getText() + line);

    }
}
