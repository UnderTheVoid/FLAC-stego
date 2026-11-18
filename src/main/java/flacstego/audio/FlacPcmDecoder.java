package flacstego.audio;

import io.github.raphimc.libflac.StreamDecoder;
import io.github.raphimc.libflac.StreamDecoderInitStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class FlacPcmDecoder {
    public void decode (Path path, PcmBlockHandler handler) throws IOException{
        Objects.requireNonNull(path, "Путь не должен быть пустым");
        Objects.requireNonNull(handler, "Обработчик не должег быть null");
         try (StreamDecoder decoder = new StreamDecoder()){
            
            decoder.setMd5Checking(true);

            StreamDecoderInitStatus status = decoder.initFile(
                
                path.toString(),
                
                (d, frame, buffer) -> {
                    
                    handler.handle(buffer, frame.getHeader().getBlocksize());

                    return true;
                },

                null,

                (d,error) -> {
                    throw new IllegalStateException(
                        "Ошибка декодера" + error
                    );
                }

            );
            if (status != StreamDecoderInitStatus.OK){
                throw new IOException("Не удалось открыть FLAC: " + status);
            }
            if (!decoder.processUntilEndOfStream()){
                throw new IOException(
                    "Ошибка декодирования: " + decoder.getResolvedStateString());
            }
            if(!decoder.finish()){
                throw new IOException("Не удалось подтвердить целостность FLAC");
            } 
        }
    }
}
