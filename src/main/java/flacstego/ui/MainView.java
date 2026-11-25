package flacstego.ui;

import flacstego.stego.StegoEmbedCli;
import flacstego.stego.StegoExtractCli;
import flacstego.analysis.StegoAnalyzer;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;

public class MainView extends BorderPane {
    private final Label statusLabel = new Label("Приложение запущено");

    public MainView(){

        

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
            createEmbeddingTab(),
            createExtractionTab(),
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
        
        TextField audioField = new TextField();
        audioField.setPromptText("Flac не выбран");
        audioField.setEditable(false);

        Button audioButton = new Button("Обзор...");

        audioButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите исходный FLAC");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FLAC (*.flac)","*.flac"));

            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                audioField.setText(file.getAbsolutePath());
            }
        });

        TextField secretField = new TextField();
        secretField.setPromptText("Скрываемый файл не выбран");
        secretField.setEditable(false);

        Button secretButton = new Button("Обзор...");
        secretButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите скрываемый файл");
            
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                secretField.setText(file.getAbsolutePath());
            }
        });

        TextField outputField = new TextField();
        outputField.setPromptText("Путь сохранения готового файла не выбран");
        outputField.setEditable(false);

        Button outputButton = new Button("Сохранить как...");
        outputButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Сохранить новый FLAC");
            chooser.setInitialFileName("encoded.flac");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FLAC (*.flac)", "*.flac"));

            File file = chooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                outputField.setText(file.getAbsolutePath());
            }
        });

        HBox audioRow = new HBox(10, audioField, audioButton);
        HBox secretRow = new HBox(10, secretField, secretButton);
        HBox outputRow = new HBox(10, outputField, outputButton);

        HBox.setHgrow(audioField, Priority.ALWAYS);
        HBox.setHgrow(secretField, Priority.ALWAYS);
        HBox.setHgrow(outputField, Priority.ALWAYS);

        Button embedButton = new Button("Встроить файл");
        embedButton.setOnAction(event -> {
            if (audioField.getText().isBlank() || secretField.getText().isBlank() || outputField.getText().isBlank()) {
                statusLabel.setText("Необходимо выбрать все файлы");
                return;
            }

            Path inputPath = Path.of(audioField.getText());
            Path secretPath = Path.of(secretField.getText());
            Path outputPath = Path.of(outputField.getText());

            Task<Long> task = new Task<Long>() {
                @Override 
                protected Long call() throws Exception {
                    return StegoEmbedCli.embed(inputPath,secretPath,outputPath);
                }
            };
            
            embedButton.setDisable(true);
            statusLabel.setText("Выполняется встраивание...");

            task.setOnSucceeded(done -> {
                embedButton.setDisable(false);
                statusLabel.setText("Встраивание завершено. Записано битов: " + task.getValue());
            });

            task.setOnFailed(failed -> {
                embedButton.setDisable(false);
                Throwable error = task.getException();
                statusLabel.setText("Ошибка встраивания: " + (error != null ? error.getMessage() : "Неизвестная ошибка"));
            });

            Thread worker = new Thread(task, "flac-stego-embed");
            worker.setDaemon(true);
            worker.start();
        });
        VBox content = new VBox(
            10,
            new Label("Исходный FLAC:"),
            audioRow, new Label("Скрываемый файл:"),
            secretRow, new Label("Новый FLAC:"),
            outputRow,
            embedButton
        );
        content.setPadding(new Insets(15));

        return new Tab("Встраивание", content);
    }

    private Tab createExtractionTab() {

        TextField audioField = new TextField();
        audioField.setPromptText("FLAC не выбран");
        audioField.setEditable(false);

        Button audioButton = new Button("Обзор...");
        audioButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите FLAC со скрытыми данными");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FLAC (*.flac)", "*.flac"));
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                audioField.setText(file.getAbsolutePath());
            }
        });

        TextField directoryField = new TextField();
        directoryField.setPromptText("Папка сохранения не выбрана");
        directoryField.setEditable(false);

        Button directoryButton = new Button("Обзор...");
        directoryButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Выберите папку для извлечния");
            File directory = chooser.showDialog(getScene().getWindow());
            if (directory != null) {
                directoryField.setText(directory.getAbsolutePath());
            }
        });

        HBox audioRow = new HBox(10, audioField, audioButton);
        HBox directoryRow = new HBox(10, directoryField, directoryButton);

        HBox.setHgrow(audioField, Priority.ALWAYS);
        HBox.setHgrow(directoryField, Priority.ALWAYS);

        Button extractButton = new Button("Извлечь файл");
        extractButton.setOnAction(event -> {
            if (audioField.getText().isBlank() || directoryField.getText().isBlank()) {
                statusLabel.setText("Необходимо выбрать все файлы");
                return;
            }
            
            Path inputPath = Path.of(audioField.getText());
            Path outputDirectory = Path.of(directoryField.getText());

            Task<Path> task = new Task<Path>() {
                @Override 
                protected Path call() throws Exception {
                    return StegoExtractCli.extract(inputPath, outputDirectory);
                }
            };

            extractButton.setDisable(true);
            statusLabel.setText("Выполняется извлечение...");
            
            task.setOnSucceeded(done -> {
                extractButton.setDisable(false);
                statusLabel.setText("Файл восстановлен");
            });

            task.setOnFailed(failed -> {
                extractButton.setDisable(false);
                Throwable error = task.getException();
                statusLabel.setText("Ошибка извлечения: " + (error != null ? error.getMessage() : "неизвестная ошибка"));
            });

            Thread worker = new Thread(task, "flac-stego-extract");
            worker.setDaemon(true);
            worker.start();
        });

        VBox content = new VBox(
            10,
            new Label("FLAC со скрытыми данными: "),
            audioRow,
            new Label("Папка сохранения"),
            directoryRow,
            extractButton
        );
        content.setPadding(new Insets(15));

        return new Tab("Извлечение", content);
    }

    private Tab createPlaceholderTab(String name, String message){
        Label label = new Label(message);
        
        VBox content = new VBox(label);
        content.setPadding(new Insets(15));
        
        return new Tab(name, content);
    }
}
