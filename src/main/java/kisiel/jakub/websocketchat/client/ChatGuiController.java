package kisiel.jakub.websocketchat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.messages.CustomMessage;
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

    @FXML
    private ProgressBar fileProgress;

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
        addLine(text);
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

    public void fileAction(ActionEvent actionEvent) throws Exception {
        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage)((Node) actionEvent.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        FileUpdateTask fileUpdateTask = this.connectionManager.sendFile(file, mode);
        fileProgress.setStyle("-fx-accent: #16507E");
        fileProgress.progressProperty().bind(fileUpdateTask.progressProperty());
        final Thread thread = new Thread(fileUpdateTask, "task-thread");
        thread.setDaemon(true);
        fileProgress.progressProperty().addListener(observable -> {
            if (fileProgress.getProgress() >= 0.99d) {
                fileProgress.setStyle("-fx-accent: forestgreen;");
            }
        });
        thread.start();
    }
}
