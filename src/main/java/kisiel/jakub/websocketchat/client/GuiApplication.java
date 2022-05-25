package kisiel.jakub.websocketchat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kisiel.jakub.websocketchat.WebsocketChatApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.*;

public class GuiApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        this.context = new SpringApplicationBuilder(WebsocketChatApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
       // ClassPathResource fxml = new ClassPathResource(");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatGUI.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        this.context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() throws Exception {
        this.context.stop();
        Platform.exit();
    }

    static class StageReadyEvent extends ApplicationEvent {
        public StageReadyEvent(Stage stage) {
            super(stage);
        }

        public Stage getStage() {
            return ((Stage) getSource());
        }
    }
}

/*@Component
class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final String applicationTitle;
    private final ApplicationContext applicationContext;

    StageInitializer(@Value("${spring.application.ui.title}") String applicationTitle,
                     ApplicationContext applicationContext) {
        this.applicationTitle = applicationTitle;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent stageReadyEvent) {
        try {
            Stage stage = stageReadyEvent.getStage();
            ClassPathResource fxml = new ClassPathResource("/ui.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxml.getURL());
            fxmlLoader.setControllerFactory(this.applicationContext::getBean);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle(this.applicationTitle);
            stage.show();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class StageReadyEvent extends ApplicationEvent {

    private final Stage stage;

    StageReadyEvent(Stage stage) {
        super(stage);
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}*/
