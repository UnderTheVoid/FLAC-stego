package flacstego;

import flacstego.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application{
    @Override 
    public void start(Stage stage){
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 650, 400);
        stage.setTitle("FLAC-stego");
        stage.setScene(scene);
        stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("/images/image.png"))));
        stage.show();
    }
    public static void main(String[] args){
        launch(args);
    }
}