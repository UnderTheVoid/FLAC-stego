package flacstego.analysis;

import flacstego.audio.FlacPcmDecoder;
import flacstego.audio.FlacMetadataReader;
import flacstego.audio.AudioInfo;

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

    public record CorrelationStats(long pairCount, double coefficient) {

    }
    
    public record ChiSquareBlock(long startFrame, int frameCount, ChiSquareStats stats) {

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

        return calculateChiSquare(frequencies, analyzedSamples[0]);
    }

    public static CorrelationStats analyzeCorrelation(Path audioPath) throws IOException {
        AudioInfo info = new FlacMetadataReader().read(audioPath);
        int channels = info.channels();
        
        int[] previous = new int[channels];
        boolean[] hasPrevious = new boolean[channels];

        double[] sumX = new double[1];
        double[] sumY = new double[1];
        double[] sumXX = new double[1];
        double[] sumYY = new double[1];
        double[] sumXY = new double[1];
        long[] pairCount = new long[1];           
    
        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(audioPath, (samples, blockSize) -> {
            for (int i = 0; i < blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    int current = samples[channel][i];
                    if (hasPrevious[channel]) {
                        double x = previous[channel];
                        double y = current;

                        sumX[0] += x;
                        sumY[0] += y;
                        sumXX[0] += x * x;
                        sumYY[0] += y * y;
                        sumXY[0] += x * y;

                        pairCount[0]++;
                    }
                    previous[channel] = current;
                    hasPrevious[channel] = true;
                }
            }
        });

        long n = pairCount[0];
        if (n < 2) {
            return new CorrelationStats(n, Double.NaN);
        }

        double covariance = sumXY[0] - sumX[0] * sumY[0] / n;
        double varianceX = sumXX[0] - sumX[0] * sumX[0] / n;
        double varianceY = sumYY[0] - sumY[0] * sumY[0] / n;

        if (varianceX <= 0 || varianceY <= 0) {
            return new CorrelationStats(n, Double.NaN);
        }

        double r = covariance / Math.sqrt(varianceX * varianceY);
        r = Math.max(-1.0, Math.min(1.0, r));

        return new CorrelationStats(n, r);
    }

    private static ChiSquareStats calculateChiSquare(Map<Integer, long[]> frequencies, long analyzedSamples) {
        double chiSquare = 0;

        int pairCount = 0;
        long coveredSamples = 0;

        for (long[] pair : frequencies.values()) {

            long total = pair[0] + pair[1];

            if (total < 10) {
                continue;
            }

            double difference = (double) pair[0] - pair[1];

            chiSquare += difference * difference / total;

            pairCount++;
            coveredSamples += total;
        }

        return new ChiSquareStats(chiSquare, pairCount, coveredSamples, analyzedSamples);
    }
    
    public static List<ChiSquareBlock> analyzeChiSquareBlocks(Path audioPath, int windowFrames) throws IOException {

        if (windowFrames <= 0) {
            throw new IllegalArgumentException("Размер окна должен быть положительным");
        }

        List<ChiSquareBlock> blocks = new ArrayList<>();
        Map<Integer, long[]> frequencies = new HashMap<>();
        long[] processedFrames = new long[1];
        int[] framesInWindow = new int[1];
        long[] analyzedSamples = new long[1];

        FlacPcmDecoder decoder = new FlacPcmDecoder();
        decoder.decode(audioPath, (samples, blockSize) -> {
            for (int i = 0; i < blockSize; i++) {
                for (int channel = 0; channel < samples.length; channel++) {
                    int sample = samples[channel][i];
                    int pairKey = sample * ~1;
                    long[] pair = frequencies.get(pairKey);

                    if (pair == null) {
                        pair = new long[2];
                        frequencies.put(pairKey, pair);
                    }

                    pair[sample & 1]++;
                    analyzedSamples[0]++;
                }

                processedFrames[0]++;
                framesInWindow[0]++;

                if (framesInWindow[0] == windowFrames) {
                    long start = processedFrames[0] - framesInWindow[0];
                    ChiSquareStats stats = calculateChiSquare(frequencies, analyzedSamples[0]);

                    blocks.add(new ChiSquareBlock(start, framesInWindow[0], stats));

                    frequencies.clear();

                    framesInWindow[0] = 0;
                    analyzedSamples[0] = 0;
                }
            }
        });

        if (framesInWindow[0] > 0) {
            long start = processedFrames[0] - framesInWindow[0];
            ChiSquareStats stats = calculateChiSquare(frequencies, analyzedSamples[0]);
            
            blocks.add(new ChiSquareBlock(start, framesInWindow[0], stats));
        }

        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("В аудиофайлк нет PCM-отсчётов для анализа");
        }
        return blocks;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 || 
            (args.length == 2 && 
            !args[1].equals("--blocks") && 
            !args[1].equals("--chi2") &&
            !args[1].equals("--corr") &&
            !args[1].equals("--chi2-blocks")
            )
        ) {
            System.out.println("Использование: <файл.flac> [--blocks | --chi2 | --corr | --chi2-blocks]");
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

        if (args.length == 2 && args[1].equals("--corr")) {
            CorrelationStats stats = analyzeCorrelation(Path.of(args[0]));
            System.out.println("Кол-во пар: " + stats.pairCount());
            if (Double.isNaN(stats.coefficient())) {
                System.out.println("Корреляция не определена");
            } else {
                System.out.printf("Коэффицент корреаляции: %.12f%n", stats.coefficient());
            }
            return;
        }

        if (args.length == 2 && args[1].equals("--chi2-blocks")) {
            List<ChiSquareBlock> blocks = analyzeChiSquareBlocks(Path.of(args[0]), 65536);
            int displayCount = Math.min(10, blocks.size());

            for (int i = 0; i < displayCount; i++) {
                ChiSquareBlock block = blocks.get(i);
                ChiSquareStats stats = block.stats();
                
                System.out.println("Окно " + (i + 1) + " | Начальный кадр: " + block.startFrame());
                System.out.println("Использовано пар: " + stats.pairCount());
                System.out.printf("Охват отсчётов: %.2f%%%n", stats.coveragePercent());

                if (stats.pairCount() == 0) {
                    System.out.println("Недостаточно данных для χ²");
                } else {
                    System.out.printf("χ²: %.4f | χ² на пару: %.4f%n", stats.chiSquare(), stats.averagePerPair());
                }
            }
            System.out.println("Всего окон: " + blocks.size());
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
