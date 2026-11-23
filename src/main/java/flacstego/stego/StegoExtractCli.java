package flacstego.stego;

import flacstego.audio.FlacPcmDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class StegoExtractCli {
    
    private StegoExtractCli() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Использование: <файл.flac>");
            return;
        }

        Path inputPath = Path.of(args[0]);

        try {
            StegoStreamExtractor extractor = new StegoStreamExtractor();
            FlacPcmDecoder decoder = new FlacPcmDecoder();
            decoder.decode(inputPath, (samples, blockSize) -> {
                extractor.readBlock(samples, blockSize);
            });
            StegoPayloadParser.ExtractedFile file = extractor.getResult();
            String fileName = file.fileName();
            
            if (fileName.isBlank() || fileName.equals(".") || fileName.equals(". .") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("Недопустимое имя восстановленного файла");
            }
            for (int i = 0; i < fileName.length(); i++) {
                if (Character.isISOControl(fileName.charAt(i))) {
                    throw new IllegalArgumentException("Имя содержит управляющие символы");
                }
            }

            Path outputDirectory = Path.of("extracted");
            Files.createDirectories(outputDirectory);
            Path outputPath = outputDirectory.resolve(fileName);
            Files.write(outputPath, file.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            System.err.println("Файл восстановлен: " + outputPath);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("Ошибка извлечения: " + e.getMessage());
        }

    }

}
