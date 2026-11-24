package flacstego.analysis;

import flacstego.audio.FlacPcmDecoder;

import java.io.IOException;
import java.nio.file.Path;

public final class StegoAnalyzer {
    
    private StegoAnalyzer() {}

    public record LsbStats(long zeros, long ones) {
        
        public long total() {
            return zeros + ones;
        }

        public double onesPercent() {
            if (total() == 0) {
                return 0;
            } 
            return (double) ones / total() * 100;
        }

        public double zerosPercent() {
            if (total() == 0) {
                return 0;
            }
            return (double) zeros / total() * 100;
        }
    }

    public static LsbStats analyzeLsb(Path audioPath) throws IOException {
        long[] counts = new long[2];
        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(audioPath, (samples, blockSize) -> {
            for (int i = 0; i < blockSize; i++) {
                for (int channel = 0; channel < samples.length; channel++) {
                    int bit = samples[channel][i] & 1;
                    counts[bit]++;
                }
            }
        });

        if (counts[0] + counts[1] == 0) {
            throw new IllegalArgumentException("В аудиофайле нет PCM-отсчётов для анализа");
        }
        return new LsbStats(counts[0], counts[1]);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Использование: <файл.flac>");
        }

        LsbStats stats = analyzeLsb(Path.of(args[0]));
        System.out.println("Всего отсчётов: " + stats.total());
        System.out.println("Кол-во нулей: " + stats.zeros());
        System.out.println("Кол-во единиц: " + stats.ones());
        System.out.printf("Доля нулей: %.2f%%%n", stats.zerosPercent());
    }
}
