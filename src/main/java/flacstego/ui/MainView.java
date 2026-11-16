package flacstego.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {
    private final Label statusLabel = new Label("Приложение запущено");

    public MainView(){

        Label title  = new Label("FLAC-stego");
        BorderPane.setMargin(title, new Insets(10));
        setTop(title);

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
            createEmbeddingTab(),
            createPlaceholderTab(
                "Извлечение",
                "In work"
            ),
            createPlaceholderTab(
                "Стеганоанализ",
                "In work"
            )
        );
        tabs.setTabClosingPolicy(
            TabPane.TabClosingPolicy.UNAVAILABLE
        );
        setCenter(tabs);  
        
        BorderPane.setMargin(statusLabel, new Insets(10));
        setBottom(statusLabel);
    }

    private Tab createEmbeddingTab(){
        Label description = new Label(
            "In work"
        );

        Button testButton = new Button("Проверить интерфейс");

        testButton.setOnAction(event -> {
            statusLabel.setText("Кнопка работает");
        });

        VBox content = new VBox(10, description, testButton);
        content.setPadding(new Insets(15));

        return new Tab("Встраивание", content);
    }
    private Tab createPlaceholderTab(String name, String message){
        Label label = new Label(message);
        
        VBox content = new VBox(label);
        content.setPadding(new Insets(15));
        
        return new Tab(name, content);
    }
}
