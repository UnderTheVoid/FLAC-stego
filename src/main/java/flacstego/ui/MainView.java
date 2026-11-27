package flacstego.ui;

import flacstego.stego.StegoEmbedCli;
import flacstego.stego.StegoExtractCli;
import flacstego.analysis.StegoAnalyzer;
import flacstego.analysis.StegoAnalyzer.LsbBlock;
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
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public class MainView extends BorderPane {
    private final Label statusLabel = new Label("Приложение запущено");

    public MainView(){

        

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
            createEmbeddingTab(),
            createExtractionTab(),
            createAnalysisTab()
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
        audioField.setEditable(true);

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
        secretField.setEditable(true);

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
        outputField.setEditable(true);

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
        audioField.setEditable(true);

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
        directoryField.setEditable(true);

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

    private Tab createAnalysisTab() {
        TextField audioField = new TextField();
        audioField.setPromptText("FLAC не выбран");
        audioField.setEditable(false);

        Button audioButton = new Button("Обзор...");
        audioButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите FLAC для анализа");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FLAC (*.flac)", "*.flac"));

            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                audioField.setText(file.getAbsolutePath());
            }
        });

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(false);
        resultArea.setPromptText("Результат отобразится здесь");

        Button analyzeButton = new Button("Начать анализ");
        Button graphsButton = new Button("Показать графики");
        Button saveButton = new Button("Сохранить отчёт");

        saveButton.setDisable(true);
        graphsButton.setDisable(true);

        saveButton.setOnAction(event -> saveAnalysisReport(resultArea.getText()));

        audioField.textProperty().addListener((observable, oldValue, newValue) -> {
            resultArea.clear();
            graphsButton.setDisable(true);
            saveButton.setDisable(true);
            statusLabel.setText("FLAC-файл выбран");
        });

        analyzeButton.setOnAction(event -> {
            if (audioField.getText().isBlank()) {
                statusLabel.setText("Необходимо выбрать FLAC-файл");
                return;
            }

            Path audioPath = Path.of(audioField.getText());

            Task<StegoAnalyzer.AnalysisReport> task = new Task<>() {
                @Override 
                protected StegoAnalyzer.AnalysisReport call() throws Exception {
                    return StegoAnalyzer.analyzeReport(audioPath);
                }
            };

            analyzeButton.setDisable(true);
            audioButton.setDisable(true);
            graphsButton.setDisable(true);
            saveButton.setDisable(true);
            resultArea.clear();
            statusLabel.setText("Выполняется анализ");
                
            task.setOnSucceeded(done -> {
                analyzeButton.setDisable(false);
                audioButton.setDisable(false);
                saveButton.setDisable(false);
                resultArea.setText(formatAnalysisSummary(task.getValue()));
                resultArea.positionCaret(0);

                graphsButton.setOnAction(graphEvent -> showAnalysisCharts(task.getValue()));
                graphsButton.setDisable(false);
                statusLabel.setText("Анализ завершён");
            });

            task.setOnFailed(failed -> {
                analyzeButton.setDisable(false);
                audioButton.setDisable(false);
                Throwable error = task.getException();
                statusLabel.setText("Ошибка анализа: " + (error != null ? error.getMessage() : "Неизвестная ошибка"));
            });

            Thread worker = new Thread(task, "flac-stego-analysis");
            worker.setDaemon(true);
            worker.start();
            });
            
            HBox audioRow = new HBox(10, audioField, audioButton);
            HBox buttonsRow = new HBox(10, analyzeButton, graphsButton, saveButton);
            HBox.setHgrow(audioField, Priority.ALWAYS);


            VBox content = new VBox(10,
                new Label("FLAC для анализа: "),
                audioRow,
                buttonsRow,
                new Label("Результаты: "),
                resultArea
            );

            content.setPadding(new Insets(15));
            VBox.setVgrow(resultArea, Priority.ALWAYS);

            return new Tab("Стеганоанализ", content);
    }

    private String formatAnalysisSummary(StegoAnalyzer.AnalysisReport report) {
        StringBuilder text = new StringBuilder();
        text.append("ПРОВЕРКА ИЗВЕСТНОГО ФОРМАТА\n");

        if (report.format().confirmed()) {
            text.append("Скрытое сообщение подтверждено\n");
            text.append("Имя файла: ").append(report.format().fileName()).append("\n");
            text.append("Размер файла: ").append(report.format().fileSize()).append("\n");
        } else {
            text.append("Сообщение известного формата не подтверждено\n");
        }

        text.append("Подробности: ").append(report.format().details()).append("\n");

        text.append("\nLSB-СТАТИСТИКА\n");
        text.append("Всего отсчётов: ").append(report.lsb().total()).append('\n');
        text.append(String.format("Доля нулей: %.2f%%%n", report.lsb().zerosPercent()));
        text.append(String.format("Доля единиц: %.2f%%%n", report.lsb().onesPercent()));
        text.append(String.format("Энтропия: %.4f бит/символ%n", report.lsb().entropy()));
        text.append("\nχ²-АНАЛИЗ (первые 65536 кадров)\n");
        text.append(String.format("Статистика χ²: %.4f%n", report.chiSquare().chiSquare()));
        text.append("Использовано пар значений: ").append(report.chiSquare().pairCount()).append('\n');
        text.append(String.format("Охват отсчётов: %.2f%%%n", report.chiSquare().coveragePercent()));

        if (report.chiSquare().pairCount() > 0) {
            text.append(String.format("χ² на пару: %.4f%n", report.chiSquare().averagePerPair()));
        } else {
            text.append("Недостаточно данных для χ² на пару\n");
        }

        text.append("\nКОРРЕЛЯЦИЯ\n");
        text.append("Исследовано соседних пар: ").append(report.correlation().pairCount()).append('\n');

        if (Double.isNaN(report.correlation().coefficient())) {
            text.append("Коэффициент корреляции не определён\n");
        } else {
            text.append(String.format("Коэффициент Пирсона: %.12f%n", report.correlation().coefficient()));
        }

        text.append("\nSAMPLE PAIR ANALYSIS (первые 65536 кадров)\n");
        text.append("Исследовано соседних пар: ").append(report.spa().pairCount()).append('\n');

        if (Double.isNaN(report.spa().estimatedRate())) {
            text.append("Оценка доли встраивания не определена\n");
        } else {
            text.append(String.format("Оценка доли встраивания: %.2f%%%n", report.spa().estimatedRate() * 100));
        }

        text.append("\nБЛОЧНЫЙ LSB-АНАЛИЗ\n");
        text.append("Размер окна: 4096 кадров\n");
        text.append("Всего окон: ").append(report.lsbBlocks().size()).append('\n');

        for (int i = 0; i < report.lsbBlocks().size(); i++) {
            StegoAnalyzer.LsbBlock block = report.lsbBlocks().get(i);
            StegoAnalyzer.LsbStats stats = block.stats();

            text.append(String.format("Окно %d | Кадры %d–%d | Нули: %.2f%% | Единицы: %.2f%% | Энтропия: %.4f%n",
                i + 1, block.startSample(), block.startSample() + block.samplesPerChannel() - 1,
                stats.zerosPercent(), stats.onesPercent(), stats.entropy()));
        }

        text.append("\nБЛОЧНЫЙ χ²-АНАЛИЗ\n");
        text.append("Размер окна: 65536 кадров\n");
        text.append("Всего окон: ").append(report.chiSquareBlocks().size()).append('\n');

        for (int i = 0; i < report.chiSquareBlocks().size(); i++) {
            StegoAnalyzer.ChiSquareBlock block = report.chiSquareBlocks().get(i);
            StegoAnalyzer.ChiSquareStats stats = block.stats();

            text.append(String.format("Окно %d | Кадры %d–%d | χ²: %.4f | Пар значений: %d | Охват: %.2f%%",
                i + 1, block.startFrame(), block.startFrame() + block.frameCount() - 1,
                stats.chiSquare(), stats.pairCount(), stats.coveragePercent()));

            if (stats.pairCount() > 0) {
                text.append(String.format(" | χ² на пару: %.4f%n", stats.averagePerPair()));
            } else {
                text.append(" | χ² на пару не определено\n");
            }
        }

        text.append("\nБЛОЧНЫЙ SPA-АНАЛИЗ\n");
        text.append("Размер окна: 32768 кадров\n");
        text.append("Всего окон: ").append(report.spaBlocks().size()).append('\n');

        for (int i = 0; i < report.spaBlocks().size(); i++) {
            StegoAnalyzer.SpaBlock block = report.spaBlocks().get(i);
            StegoAnalyzer.SpaStats stats = block.stats();

            text.append(String.format("Окно %d | Кадры %d–%d | Пар: %d | ",
                i + 1, block.startFrame(), block.startFrame() + block.frameCount() - 1,
                stats.pairCount()));

            if (Double.isNaN(stats.estimatedRate())) {
                text.append("Оценка не определена\n");
            } else {
                text.append(String.format("Оценка SPA: %.2f%%%n", stats.estimatedRate() * 100));
            }
}
        text.append("\nПОИСК ГРАНИЦЫ ВСТРАИВАНИЯ\n");

        StegoAnalyzer.ChiSquarePattern pattern = report.chiSquarePattern();

        text.append(pattern.explanation()).append('\n');

        if (pattern.suspicious()) {
            text.append("Предполагаемая граница после окна: ")
                .append(pattern.boundaryAfterWindow()).append('\n');

            text.append(String.format("Среднее χ² на пару до границы: %.4f%n", pattern.beforeMean()));
            text.append(String.format("Среднее χ² на пару после границы: %.4f%n", pattern.afterMean()));
        }

        return text.toString();
    }

    private void showAnalysisCharts(StegoAnalyzer.AnalysisReport report) {
        ScatterChart<Number, Number> lsbChart = createChart("Блочный LSB-анализ", "Доля единиц, %");
        XYChart.Series<Number, Number> lsbSeries = new XYChart.Series<>();

        for (StegoAnalyzer.LsbBlock block : report.lsbBlocks()) {
            addChartPoint(lsbSeries, block.startSample(), block.stats().onesPercent());
        }
        lsbChart.getData().add(lsbSeries);

        ScatterChart<Number, Number> chiChart = createChart("Блочный χ²-анализ", "χ² на пару");
        XYChart.Series<Number, Number> chiSeries = new XYChart.Series<>();

        int outsideZoom = 0;

        for (StegoAnalyzer.ChiSquareBlock block : report.chiSquareBlocks()) {
            double value = block.stats().averagePerPair();

            if (Double.isFinite(value)) {
                addChartPoint(chiSeries, block.startFrame(), value);

                if (value < 0.8 || value > 1.2) {
                    outsideZoom++;
                }
            }
        }
        chiChart.getData().add(chiSeries);

        NumberAxis chiAxis = (NumberAxis) chiChart.getYAxis();
        CheckBox chiZoom = new CheckBox("Приблизить область χ² от 0.8 до 1.2");
        Label chiInfo = new Label("За пределами диапазона приближения: " + outsideZoom + " окон");

        chiZoom.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                chiAxis.setAutoRanging(false);
                chiAxis.setLowerBound(0.8);
                chiAxis.setUpperBound(1.2);
                chiAxis.setTickUnit(0.05);
            } else {
                chiAxis.setAutoRanging(true);
            }
        });

        chiZoom.setSelected(true);

        VBox chiContent = new VBox(5, chiZoom, chiInfo, chiChart);
        chiContent.setPadding(new Insets(5));
        VBox.setVgrow(chiChart, Priority.ALWAYS);

        ScatterChart<Number, Number> spaChart = createChart("Блочный SPA-анализ", "Оценка доли встраивания, %");
        XYChart.Series<Number, Number> spaSeries = new XYChart.Series<>();

        for (StegoAnalyzer.SpaBlock block : report.spaBlocks()) {
            double value = block.stats().estimatedRate();
            if (Double.isFinite(value)) {
                addChartPoint(spaSeries, block.startFrame(), value * 100);
            }
        }
        spaChart.getData().add(spaSeries);

        NumberAxis spaAxis = (NumberAxis) spaChart.getYAxis();
        spaAxis.setAutoRanging(false);
        spaAxis.setLowerBound(0);
        spaAxis.setUpperBound(100);
        spaAxis.setTickUnit(10);

        int totalWindows = report.spaBlocks().size();
        int definedWindows = spaSeries.getData().size();

        Label spaInfo = new Label("Определено окон: " + definedWindows + " из " + totalWindows
            + " | Не определено: " + (totalWindows - definedWindows));

        VBox spaContent = new VBox(5, spaInfo, spaChart);
        spaContent.setPadding(new Insets(5));
        VBox.setVgrow(spaChart, Priority.ALWAYS);

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(new Tab("LSB", lsbChart), new Tab("χ²", chiContent), new Tab("SPA", spaContent));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Stage window = new Stage();
        window.setTitle("Графики стегоанализа");
        window.initOwner(getScene().getWindow());
        window.setScene(new Scene(tabs, 1000, 600));
        window.show();
    }

    private ScatterChart<Number, Number> createChart(String title, String yLabel) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Начальный кадр окна, млн.");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setForceZeroInRange(false);

        ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        return chart;
    }

    private void saveAnalysisReport(String report) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить отчёт стегоанализа");
        chooser.setInitialFileName("analysis.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            Files.writeString(file.toPath(), report);
            statusLabel.setText("Отчёт сохранён");
        } catch (IOException error) {
            statusLabel.setText("Ошибка сохранения: " + error.getMessage());
        }
    }

    private void addChartPoint(XYChart.Series<Number, Number> series, long frame, double value) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(frame / 1_000_000.0, value);

        StackPane marker = new StackPane();
        marker.setStyle("-fx-background-color: #e76d32; -fx-background-radius: 3px; -fx-background-insets: 0; -fx-padding: 2px;");

        Tooltip.install(marker, new Tooltip(String.format("Кадр: %d%nЗначение: %.4f", frame, value)));

        point.setNode(marker);
        series.getData().add(point);
    }

}
