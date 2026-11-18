package flacstego.audio;

import java.io.IOException;
import java.nio.file.Path;

public final class FlacPcmCli{
    public static void main(String[] args) throws IOException {
        if (args.length != 1){
            System.err.println("Укажите путь к FLAC-файлу");
            return;
        }
        
        Path path = Path.of(args[0]);
        long[] decodedSamplesPerChannel = {0};
        
        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(path, (samples, blockSize) -> {

            if(decodedSamplesPerChannel[0] == 0 && blockSize > 0){

                int count = Math.min(8, blockSize);

                System.out.println("Первые отсчёты канала 0: ");

                for (int i = 0; i < count; i++){
                    System.out.println(i + ": " + samples[0][i]);
                }
            }

            decodedSamplesPerChannel[0] += blockSize;
 
        });
        
        System.out.println("Декодировано отсчётов на канал: " + decodedSamplesPerChannel[0]);

    }
}
