package flacstego.analysis;

import flacstego.audio.FlacPcmDecoder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

        public double entropy() {
            if (total() == 0) {
                return 0;
            }

            double p0 = (double) zeros / total();
            double p1 = (double) ones / total();
            double h = 0;
            if (p0 > 0) {
                h -= p0 * Math.log(p0) / Math.log(2); //Math.log - натуральный логариф log2(x) = ln(x)/ln(2) = Math.log(x)/Math.log(2) 
            }

            if (p1 > 0) {
                h -= p1 * Math.log(p1) / Math.log(2);
            }

            return h;
        }
    }

    public record LsbBlock(long startSample, int samplesPerChannel, LsbStats stats) {

    }

    public record ChiSquareStats(double chiSquare, int pairCount, long coveredSamples, long analyzedSamples) {
        
        public double averagePerPair() {
            if(pairCount == 0) {
                return Double.NaN;
            }
            return chiSquare / pairCount;
        }

        public double coveragePercent() {
            if (analyzedSamples == 0) {
                return 0;
            }
            return  (double) coveredSamples / analyzedSamples * 100;
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

    public static ChiSquareStats analyzeChiSquare(Path audioPath, int maxFrames) throws IOException {
        if (maxFrames <= 0) {
            throw new IllegalArgumentException("Кол-во кадров должно быть положительным");
        }

        Map<Integer, long[]> frequencies = new HashMap<>();
        long[] processedFrames = new long[1];
        long[] analyzedSamples = new long[1];

        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(audioPath, (samples, blocksize) -> {
            for (int i = 0; i < blocksize && processedFrames[0] < maxFrames; i++) {
                for (int channel = 0; channel < samples.length; channel++) {
                    int sample = samples[channel][i];
                    int pairKey = sample & ~1;
                    long[] pair = frequencies.get(pairKey);

                    if (pair == null) {
                        pair = new long[2];
                        frequencies.put(pairKey,pair);
                    } 

                    pair[sample & 1]++;
                    analyzedSamples[0]++;
                }
                processedFrames[0]++;
            }
        });

        double chiSquare = 0;
        int pairCount = 0;
        long coveredSamples = 0;

        for (long[] pair : frequencies.values()) {
            long total = pair[0] + pair[1];

            if (total < 10) {
                continue;
            }

            double diff = (double) pair[0] - pair[1];
            chiSquare += diff * diff / total;

            pairCount++;
            coveredSamples += total;
        }
        
        return new ChiSquareStats(chiSquare, pairCount, coveredSamples, analyzedSamples[0]);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equals("--blocks") && !args[1].equals("--chi2"))) {
            System.out.println("Использование: <файл.flac> [--blocks | --chi2]");
            return;
        }
        if (args.length == 2 && args[1].equals("--blocks")) {
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
                System.out.printf("Энтропия окна: %.4f бит/символ%n", block.stats.entropy());
            }
            System.out.println("Всего окон: " + blocks.size());
            return;
        }

        if (args.length == 2 && args[1].equals("--chi2")) {
            ChiSquareStats stats = analyzeChiSquare(Path.of(args[0]), 65536);
            
            System.out.printf("Статистика χ²: %.4f%n", stats.chiSquare());
            System.out.println("Использовано пар: " + stats.pairCount());
            System.out.printf("Охват отсчётов: %.2f%%%n", stats.coveragePercent());

            if (stats.pairCount() == 0) {
                System.out.println("Недостаточно повторений значений для χ²-анализа");
            } else {
                System.out.printf("χ² на одну пару: %.4f%n", stats.averagePerPair());
            }
            return;
        }

        LsbStats stats = analyzeLsb(Path.of(args[0]));
        System.out.println("Всего отсчётов: " + stats.total());
        System.out.println("Кол-во нулей: " + stats.zeros());
        System.out.println("Кол-во единиц: " + stats.ones());
        System.out.printf("Доля нулей: %.2f%%%n", stats.zerosPercent());
        System.out.printf("Энтропия LSB: %.4f бит/символ%n", stats.entropy());
    }
}
