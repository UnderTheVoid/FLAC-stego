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
            System.err.println("Использование: " + "<исходный.flac> <срываемый файл> <результ.flac>");
        }

        try {
            Path inputPath = Path.of(args[0]);
            Path secretPath = Path.of(args[1]);
            Path outputPath = Path.of(args[2]);

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

            System.out.println("Скрываемый файл успешно встроен");
            System.out.println("Результат: " + outputPath);
            System.err.println("Записано битов: " + writer.getWrittenBits());
            
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Ошибка встраивания " + e.getMessage());
        }
    }

}
