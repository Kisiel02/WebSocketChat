package kisiel.jakub.websocketchat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class ChatGuiController {

    @FXML
    private Button connectButton;

    @FXML
    private ScrollPane scrollField;

    @FXML
    private Button sendButton;

    @FXML
    private TextField textField;

    @FXML
    public void connectButtonAction(ActionEvent event) {

    }

    @FXML
    public void sendButtonAction(ActionEvent event) {

    }

    @FXML
    public void textFieldAction(ActionEvent event) {

    }
}
