package flacstego.stego;

import flacstego.audio.FlacPcmDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class StegoExtractCli {
    
    private StegoExtractCli() {}

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Использование: <файл.flac>");
            return;
        }

        try {
            Path inputPath = Path.of(args[0]);
            Path result = extract(inputPath,Path.of("extracted"));
            System.out.println("Файл восстановлен: " + result);

        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("Ошибка извлечения: " + e.getMessage());
        }
    }

    public static Path extract(Path inputPath, Path outputDirectory) throws IOException {
        Objects.requireNonNull(inputPath, "Путь к файлу не может быть null");
        Objects.requireNonNull(outputDirectory, "Путь конечной директории не может быть null");

        StegoStreamExtractor extractor = new StegoStreamExtractor();
        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(inputPath, (samples, blockSize) -> {
            extractor.readBlock(samples, blockSize);
        });
        StegoPayloadParser.ExtractedFile file = extractor.getResult();
        
        String fileName = file.fileName();    
        if (fileName.isBlank() || 
            fileName.equals(".") || 
            fileName.equals(". .") || 
            fileName.contains("/") || 
            fileName.contains("\\")
        ) {
            throw new IllegalArgumentException("Недопустимое имя восстановленного файла");
        }
        for (int i = 0; i < fileName.length(); i++) {
            if (Character.isISOControl(fileName.charAt(i))) {
                throw new IllegalArgumentException("Имя содержит управляющие символы");
            }
        }

        Path directory = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path outputPath = directory.resolve(fileName).normalize();
        if (!outputPath.getParent().equals(directory)) {
            throw new IllegalArgumentException("Недопустимый путь восстановленного файла");
        }

        Files.write(outputPath, file.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        return outputPath;
    }

}
