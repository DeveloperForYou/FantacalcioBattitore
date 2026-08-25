package it.fantacalcio.battitore;

import it.fantacalcio.battitore.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainView root = new MainView(stage);
        Scene scene = new Scene(root, 980, 680);
        scene.getStylesheets().add(
                Main.class.getResource("/it/fantacalcio/battitore/app.css").toExternalForm()
        );

        stage.setTitle("Battitore Fantacalcio");
        stage.setMinWidth(860);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void StartApplication(String[] args) {
        launch(args);
    }
}
