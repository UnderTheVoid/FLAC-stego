package flacstego.stego;

import java.util.Arrays;
import java.util.Objects;

public final class LsbStreamReader {

    private final byte[] data;

    private long bitIndex;

    public LsbStreamReader(int expectedBytes) {

        if (expectedBytes < 0) {
            throw new IllegalArgumentException("Кол-во байтов не может быть отриацтельным");
        }

        this.data = new byte[expectedBytes];

        this.bitIndex = 0;
    }

    public void readBlock(int[][] samples, int blockSize) {
        
        Objects.requireNonNull(samples, "PCM-массив не должен быть null");

        if (samples.length == 0){
            throw new IllegalArgumentException("Нет аудиоканалов");
        }

        if (blockSize < 0) {
            throw new IllegalArgumentException("Отрицательный blockSize");
        }

        for (int channel = 0; channel < samples.length; channel++){
            if (samples[channel] == null || samples[channel].length < blockSize){
                throw new IllegalArgumentException("Некорректный массив канала: " + channel);
            }
        }

        if (isFinished()){
            return;
        }

        for (int i = 0; i < blockSize; i++){
            for (int channel = 0; channel < samples.length; channel++){

                if (isFinished()){
                    return;
                }

                int byteIndex = (int) (bitIndex / 8);
                int bit = LsbCodec.extractBit(samples[channel][i]);
                int current = Byte.toUnsignedInt(data[byteIndex]);

                current = (current << 1) | bit;

                data[byteIndex] = (byte) current;

                bitIndex++;
            }
        }
    }

    public boolean isFinished() {
        return bitIndex == (long) data.length * 8;
    }

    public long getReadBits() {
        return bitIndex;
    }

    public long getTotalBits() {
        return (long) data.length * 8;
    }

    public byte[] getData() {

        if (!isFinished()){
            throw new IllegalStateException("Извлечение ещё не завершено");
        }

        return Arrays.copyOf(data, data.length);
    }
}

