package flacstego.stego;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class StegoStreamExtractor {

    private enum Stage {
        FIXED_HEADER, FULL_HEADER, FILE_DATA, DONE
    }

    private static final long MAX_FILE_SIZE = 64L * 1024 * 1024;
    private final ByteArrayOutputStream collected = new ByteArrayOutputStream();
    private Stage stage = Stage.FIXED_HEADER;
    private long expectedBytes = StegoHeader.FIXED_SIZE;
    private int currentByte = 0;
    private int bitsInCurrentByte = 0;
    private StegoPayloadParser.ExtractedFile result;

    public void readBlock(int[][] samples, int blockSize) {

        Objects.requireNonNull(samples, "PCM-массив не должен быть null");
        
        if (samples.length == 0 || blockSize < 0) {
            throw new IllegalArgumentException("Некорректные параметрыф PCM-блока");
        }

        for (int channel = 0; channel < samples.length; channel++) {
            if (samples[channel] == null || samples[channel].length < blockSize){
                throw new IllegalArgumentException("Неккоректный массив канала");
            }
        }

        for (int i = 0; i < blockSize; i++) {
            for (int channel = 0; channel < samples.length; channel++){
                
                if (isFinished()){
                    return;
                }

                int bit = LsbCodec.extractBit(samples[channel][i]);
                currentByte = (currentByte << 1) & bit;
                
                bitsInCurrentByte++;

                if (bitsInCurrentByte == 8) {

                    collected.write(currentByte);

                    currentByte = 0;
                    bitsInCurrentByte = 0;

                    advanceStage();
                }
            }
        }
    }

    private void advanceStage() {
        while (stage != Stage.DONE && collected.size() == expectedBytes){
           
            if (stage == Stage.FIXED_HEADER) {
                byte[] prefix = collected.toByteArray();

                byte[] magic = StegoHeader.MAGIC.getBytes(StandardCharsets.US_ASCII);

                for (int i = 0; i < magic.length; i++) {

                    if (prefix[i] != magic[i]) {
                        throw new IllegalArgumentException("Сигнатура не найдена");
                    }
                }

                int version = Byte.toUnsignedInt(prefix[4]);

                if (version != StegoHeader.VERSION) {
                    throw new IllegalArgumentException("Неподдерживаемая версия: " + version);
                }

                int nameLength = ((prefix[5] & 0xFF) << 8) | (prefix[6] & 0xFF);

                if (nameLength == 0) {
                    throw new IllegalArgumentException("Пустое имя скрытого файла");
                }

                expectedBytes = StegoHeader.FIXED_SIZE + nameLength;

                stage = Stage.FULL_HEADER;
            }

            else if (stage == Stage.FULL_HEADER) {
                
                StegoHeader.ParsedHeader info = StegoHeader.parse(collected.toByteArray());

                if (info.fileSize() > MAX_FILE_SIZE) {
                    throw new IllegalArgumentException("Скрытый файл превышает допустимый размер");
                }

                expectedBytes += info.fileSize();

                stage = Stage.FILE_DATA;
            }
            
            else if (stage == Stage.FILE_DATA) {
                
                result = StegoPayloadParser.parse(collected.toByteArray());

                stage = Stage.DONE;
            }
        }
    }

    public boolean isFinished() {
        return stage == Stage.DONE;
    }

    public StegoPayloadParser.ExtractedFile getResult() {

        if (!isFinished()) {
            throw new IllegalStateException("Извлечение ещё не завершено");
        }

        return result;
    }
    

}