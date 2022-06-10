package kisiel.jakub.websocketchat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private TextField textField;

    @FXML
    private TextField port;

    @FXML
    private VBox messages;

    @FXML
    private ProgressBar fileProgress;

    @FXML
    private ScrollPane scrollPane;

    private SecurityManager.BlockMode mode = SecurityManager.BlockMode.CBC;

    @FXML
    public void connectButtonAction(ActionEvent event) {
        try {
            int portNumber = Integer.parseInt(port.getText());
            this.scrollPane.setPrefWidth(messages.getWidth());
            connectionManager.initConnection(portNumber, portOwn);
        } catch (NumberFormatException e) {
            logger.info(portAnother);
            connectionManager.initConnection(portAnother, portOwn);
        } catch (Exception e) {
            addOwnLine("Could not connect");
        }
    }

    @FXML
    public void sendButtonAction(ActionEvent event) {
        String text = textField.getText();
        CustomMessage customMessage = new CustomMessage(
            text, CustomMessage.Type.TEXT, mode
        );
        this.connectionManager.sendMessage(customMessage);
        addOwnLine(text);
        textField.clear();
    }

    public void addOwnLine(String line) {
        Platform.runLater(() -> messages.getChildren().add(createText(line, MessageDisplay.LEFT)));
    }

    public void addForeignLine(String line) {
        Platform.runLater(() -> messages.getChildren().add(createText(line, MessageDisplay.RIGHT)));
    }

    public void addInfoLine(String line) {
        Platform.runLater(() -> messages.getChildren().add(createText(line, MessageDisplay.CENTER)));
    }

    public void notifyAboutConnection() {
        this.addInfoLine("Connected");
    }

    @FXML
    private void cbcAction(ActionEvent actionEvent) {
        this.mode = SecurityManager.BlockMode.CBC;
    }

    @FXML
    private void ecbAction(ActionEvent actionEvent) {
        this.mode = SecurityManager.BlockMode.ECB;
    }

    public void fileAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        FileUpdateTask<Void> fileUpdateTask = this.connectionManager.sendFile(file, mode);
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

    private static HBox createText(String text, MessageDisplay alignment) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(200);
        label.setPadding(new Insets(5));
        label.setStyle("-fx-background-radius: 6");
        HBox pane = new HBox(label);
        switch (alignment) {
            case LEFT -> {
                pane.setAlignment(Pos.CENTER_LEFT);
                label.setStyle("-fx-background-color: white");
            }
            case RIGHT -> {
                pane.setAlignment(Pos.CENTER_RIGHT);
                label.setStyle("-fx-background-color: #DCF8C6");
            }
            case CENTER -> {
                pane.setAlignment(Pos.CENTER);
                label.setStyle("-fx-background-color: #f5e689");
            }
        }
        return pane;
    }

    private enum MessageDisplay {
        CENTER,
        LEFT,
        RIGHT
    }
}
