package kisiel.jakub.websocketchat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.Messages.CustomMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.ExecutionException;


@Component
public class ChatGuiController {

    private final Logger logger = LogManager.getLogger(ChatGuiController.class);

    @Value("${another.port}")
    private int portAnother;

    @Value("${server.port}")
    private int portOwn;

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
    private Button cbc;

    @FXML
    private Button ecb;

    @FXML
    private Button file;

    private SecurityManager.BlockMode mode = SecurityManager.BlockMode.CBC;

    @FXML
    public void connectButtonAction(ActionEvent event) throws ExecutionException, InterruptedException {
        try {
            int portNumber = Integer.parseInt(port.getText());
            connectionManager.initConnection(portNumber, portOwn);
        } catch (NumberFormatException e) {
            logger.info(portAnother);
            connectionManager.initConnection(portAnother, portOwn);;
        } catch (Exception e) {
            this.messages.setText("Could not connect");
        }
    }

    @FXML
    public void sendButtonAction(ActionEvent event) {
        String text = textField.getText();
        CustomMessage customMessage = new CustomMessage(
                text, CustomMessage.Type.TEXT, mode
        );
        this.connectionManager.sendMessage(customMessage);
        textField.clear();
    }

    @FXML
    public void textFieldAction(ActionEvent event) {
        //TODO
        // enter to send
    }

    public void addLine(String line) {
        messages.setText(messages.getText() + "\n" + line);

    }

    public void notifyAboutConnection() {
        this.addLine("Connected");
    }

    public void cbcAction(ActionEvent actionEvent) {
        this.mode = SecurityManager.BlockMode.CBC;
    }

    public void ecbAction(ActionEvent actionEvent) {
        this.mode = SecurityManager.BlockMode.ECB;
    }

    public void fileAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage)((Node) actionEvent.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        this.connectionManager.sendFile(file, mode);
    }
}
