package flacstego.audio;

import java.io.IOException;
import java.nio.file.Path;

public final class FlacInfoCli{
    public static void main(String[] args) throws IOException {
        if (args.length != 1){
            System.err.println("Укажите путь к FLAC-файлу");
            return;
        }

        Path path = Path.of(args[0]);

        FlacMetadataReader reader = new FlacMetadataReader();
        AudioInfo info = reader.read(path);

        System.out.println("Частота: " + info.sampleRate() + " Гц");
        System.out.println("Каналы: " + info.channels());
        System.out.println("Разрядность: " + info.bitsPerSample() + " бит");
        System.out.println("Отсчётов на канал: " + info.totalSamplesPerChannel());
        
        if (info.totalSamplesPerChannel() > 0){
            System.out.printf("Продолжительность: %.2f с %n", info.durationSeconds());
        }
        else{
            System.out.println("Продолжительность не определена");
        }
    }
}