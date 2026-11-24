package flacstego.analysis;

import flacstego.audio.FlacPcmDecoder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public record LsbBlock(long startSample, int samplesPerChannel, LsbStats stats) {

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

    public static List<LsbBlock> analyzeLsbBlocks(Path audioPath, int windowSamples) throws IOException {
        if (windowSamples <= 0) {
            throw new IllegalArgumentException("Размер окна должен быть положительным");
        }

        List<LsbBlock> blocks = new ArrayList<>();
        long[] counts = new long[2];
        long[] processedSamples = new long[1];
        int[] inWindow = new int[1];

        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(audioPath, (samples, blockSize) -> {
            for (int i = 0; i < blockSize; i++) {
                for (int channel = 0; channel < samples.length; channel++) {
                    int bit = samples[channel][i] & 1;
                    counts[bit]++;
                }

                processedSamples[0]++;
                inWindow[0]++;

                if (inWindow[0] == windowSamples) {
                    long start = processedSamples[0] - inWindow[0];
                    blocks.add(new LsbBlock(start, inWindow[0], new LsbStats(counts[0], counts[1])));

                    counts[0] = 0;
                    counts[1] = 0;
                    inWindow[0] = 0;
                }
            }
        });

        if (inWindow[0] > 0) {
            long start = processedSamples[0] - inWindow[0];
            blocks.add(new LsbBlock(start, inWindow[0], new LsbStats(counts[0], counts[1])));
        }

        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("В аудиофайле нет PCM-отсчётов для анализа");
        }

        return blocks;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equals("--blocks"))) {
            System.out.println("Использование: <файл.flac> [--blocks]");
            return;
        }
        if (args.length == 2) {
            List<LsbBlock> blocks = analyzeLsbBlocks(Path.of(args[0]), 4096);
            int displayCount = Math.min(10, blocks.size());

            for (int i = 0; i < displayCount; i++) {
                LsbBlock block = blocks.get(i);
                System.out.printf("Окно %d | Отсчёты %d-%d | " + "Нули: %d | Единицы %d | " + "Доля единиц: %.2f%%%n", 
                    i + 1, 
                    block.startSample, 
                    block.startSample + block.samplesPerChannel - 1, 
                    block.stats().zeros, 
                    block.stats().ones(), 
                    block.stats().onesPercent()
                );
            }
            System.out.println("Всего окон: " + blocks.size());
            return;
        }
        LsbStats stats = analyzeLsb(Path.of(args[0]));
        System.out.println("Всего отсчётов: " + stats.total());
        System.out.println("Кол-во нулей: " + stats.zeros());
        System.out.println("Кол-во единиц: " + stats.ones());
        System.out.printf("Доля нулей: %.2f%%%n", stats.zerosPercent());
    }
}
