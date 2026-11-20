package flacstego.stego;

import java.util.Arrays;
import java.util.Objects;

public final class LsbStreamWriter {
    
    private final byte[] data;

    private long bitIndex;

    public LsbStreamWriter(byte[] data){

        Objects.requireNonNull(data, "Данные не должны быть null");

        this.data = Arrays.copyOf(data, data.length);

        this.bitIndex = 0;
    }

    public void writeBlock(int[][] samples, int blockSize){

        Objects.requireNonNull(samples, "PCM-массив не должен быть null");

        if (samples.length == 0){
            throw new IllegalArgumentException("Нет аудиоканалов");
        }

        if (blockSize < 0){
            throw new IllegalArgumentException("Отрицательный blockSize");
        }

        for (int channel = 0; channel < samples.length; channel++){

            if(samples[channel] == null || samples[channel].length < blockSize){
                throw new IllegalArgumentException("Некорректный массив канала " + channel);
            }
        }

        if (isFinished()){
            return;
        }

        for (int i = 0; i < blockSize; i++){
            for(int channel = 0; channel < samples.length; channel++){
                
                if(isFinished()){
                    return;
                }

                int byteIndex = (int) (bitIndex / 8);
                int bitPosition = 7 - (int) (bitIndex % 8);
                int unsignedByte = data[byteIndex] & 0xFF;

                int bit = (unsignedByte >>> bitPosition) & 1;

                samples[channel][i] = LsbCodec.embedBit(samples[channel][i], bit);

                bitIndex++;
            }
        }
    }

    public boolean isFinished() {
        return bitIndex == (long) data.length * 8;
    }

    public long getWrittenBits() {
        return bitIndex;
    }

    public long getTotalBits() {
        return (long) data.length * 8;
    }
}
