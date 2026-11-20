package flacstego.stego;

public final class LsbCodec{

    private LsbCodec(){
    }

    public static int embedBit(int sample, int bit){

        if (bit != 0 && bit != 1){
            throw new IllegalArgumentException("Бит должен быть 0 или 1");
        }

        return (sample & ~1) | bit;
    }

    public static int extractBit(int sample){
        return sample & 1;
    }

    public static void embedByte(int[] samples, int start, int value){

        if (value < 0 || value > 255){
            throw new IllegalArgumentException("Значение байта должно быть от 0 до 255");
        }

        if (start < 0 || start > samples.length - 8){
            throw new IndexOutOfBoundsException("Недостаточно PCM-отсчётов");
        }
        for (int i = 0; i < 8; i++){

            int bit = (value >>> (7 - i)) & 1;

            samples[start + i] = embedBit(samples[start + i], bit);
        }
    }

    public static int extractByte(int[] samples, int start){

        if (start < 0 || start > samples.length - 8){
            throw new IndexOutOfBoundsException("Недостаточно PCM-отсчётов");
        }

        int value = 0;

        for (int i = 0; i < 8; i++){

            int bit = extractBit(samples[start + i]);

            value = (value << 1) | bit;
        }

        return value;
    }

}

