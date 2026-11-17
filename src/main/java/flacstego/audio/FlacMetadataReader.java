package flacstego.audio;

import io.github.raphimc.libflac.Metadata;
import io.github.raphimc.libflac.StreamInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class FlacMetadataReader {
    public AudioInfo read(Path path) throws IOException{
        Objects.requireNonNull(path, "Путь к файлу не найден");
        StreamInfo streamInfo = Metadata.getStreamInfo(path.toString());
        if (streamInfo == null){
            throw new IOException("Не удалось прочитать STREAMINFO из FLAC-файла");
        }
        return new AudioInfo(streamInfo.getSampleRate(), streamInfo.getChannels(), streamInfo.getBitsPerSample(), streamInfo.getTotalSamples());
    }
}
