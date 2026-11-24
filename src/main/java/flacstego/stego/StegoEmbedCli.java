package flacstego.stego;

import flacstego.audio.AudioInfo;
import flacstego.audio.FlacMetadataReader;
import flacstego.audio.FlacPcmTranscoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StegoEmbedCli {
    
    private  StegoEmbedCli() {}

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println(
                    "Использование: StegoEmbedCli "
                    + "<исходный.flac> <секретный-файл> <результат.flac>"
            );
            return;
        }

        try {

            Path inputPath = Path.of(args[0]);
            Path secretPath = Path.of(args[1]);
            Path outputPath = Path.of(args[2]);

            long writtenBits = embed(
                    inputPath,
                    secretPath,
                    outputPath
            );

            System.out.println("Скрытый файл успешно встроен");
            System.out.println("Результат: " + outputPath);
            System.out.println("Записано битов: " + writtenBits);

        } catch (IOException | IllegalArgumentException
                | IllegalStateException e) {

            System.err.println(
                    "Ошибка встраивания: " + e.getMessage()
            );
        }
    }

    public static long embed(Path inputPath, Path secretPath, Path outputPath) throws IOException {
        if (inputPath.toAbsolutePath().normalize().equals(outputPath.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Исходный и выходной FLAC должны быть различными файлами");
        }

        byte[] payload = StegoPayloadBuilder.fromFile(secretPath);
        long requiredSamples = (long) payload.length * 8;
        FlacMetadataReader metadataReader = new FlacMetadataReader();
        AudioInfo info = metadataReader.read(inputPath);

        long totalSamples = info.totalSamplesPerChannel();
        if (totalSamples > 0) {
            long availableSamples = totalSamples * info.channels();
            if (requiredSamples > availableSamples) {
                throw new IllegalArgumentException("Недостаточно место в аудио. Требуется: " + requiredSamples + ", Доступно:" + availableSamples);
            }
        }

        LsbStreamWriter writer = new LsbStreamWriter(payload);
        FlacPcmTranscoder.transcode(inputPath, outputPath, (samples, blockSize) -> {
            writer.writeBlock(samples, blockSize);
        });

        if (!writer.isFinished()) {
            Files.deleteIfExists(outputPath);
            throw new IOException("Аудиофайл закончился до завершения встраивания");
        }

        return writer.getWrittenBits();
    }

}
