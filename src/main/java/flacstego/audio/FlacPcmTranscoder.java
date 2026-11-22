package flacstego.audio;

import io.github.raphimc.libflac.StreamEncoder;
import io.github.raphimc.libflac.StreamEncoderInitStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FlacPcmTranscoder {
    
    private FlacPcmTranscoder() {}

    public static void transcode(Path inputPath, Path outputPath, PcmBlockHandler processor) throws IOException {

        Objects.requireNonNull(inputPath, "Исходный путь не может быть null");
        Objects.requireNonNull(outputPath, "Выходной путь не может быть null");
        Objects.requireNonNull(processor, "Обработчик не должен быть null");

        FlacMetadataReader reader = new FlacMetadataReader();

        AudioInfo info = reader.read(inputPath);

        Files.createFile(outputPath);

        boolean success = false;

        try {

            try (StreamEncoder encoder = new StreamEncoder()) {

                encoder.setChannels(info.channels());
                encoder.setSampleRate(info.sampleRate());
                encoder.setBitsPerSample(info.bitsPerSample());
                encoder.setTotalSamplesEstimate(info.totalSamplesPerChannel());

                StreamEncoderInitStatus status = encoder.initFile(outputPath.toString(), null);

                if (status != StreamEncoderInitStatus.OK) {
                    throw new IOException("Не удалось инициализировать FLAC-кодировщик: " + status);
                }

                FlacPcmDecoder decoder = new FlacPcmDecoder();
                decoder.decode(inputPath, (samples, blockSize) -> {

                    processor.handle(samples, blockSize);

                    if (!encoder.process(samples, blockSize)) {
                        throw new IOException("Ошибка кодирования FLAC: " + encoder.getResolvedStateString());
                    }
                });

                encoder.finish();

            }

            success = true;
        } finally {

            if(!success) {
                Files.deleteIfExists(outputPath);
            }
        }
    }
}
