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
        long[] decodedBlocks = {0};
        long[] shownSamples = {0};

        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(path, (samples, blockSize) -> {
            
            for (int i = 0; i < blockSize && shownSamples[0] < 12; i++){
                for (int channel =0; channel < samples.length && shownSamples[0] < 12; channel++){

                    System.out.println("Канал: " + channel + ", индекс: " + i + ", отсчёт: " + samples[channel][i]);
                    shownSamples[0]++;
                }
            }
            decodedBlocks[0]++;
            if (decodedBlocks[0] <= 3){

                System.out.println("Блок №" + decodedBlocks[0] + ": " + blockSize + " Отсчётов на канал");
            }
            
            decodedSamplesPerChannel[0] += blockSize;
 
        });
        
        FlacMetadataReader reader = new FlacMetadataReader();

        AudioInfo info = reader.read(path);

        long capacityBits = decodedSamplesPerChannel[0] * info.channels();
        long capacityBytes = capacityBits / 8;

        System.out.println("Кол-во каналов: " + info.channels());
        System.out.println("Теоретическая вместимость бит: " + capacityBytes);

        System.out.println("Декодировано отсчётов на канал: " + decodedSamplesPerChannel[0]);
        System.out.println("Всего PCM блоков: " + decodedBlocks[0]);
    }
}
