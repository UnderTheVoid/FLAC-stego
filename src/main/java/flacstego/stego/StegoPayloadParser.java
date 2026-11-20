package flacstego.stego;

import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;

public final class StegoPayloadParser {
    
    private StegoPayloadParser() {}

    public record  ExtractedFile(String fileName, byte[] content) {}

    public static ExtractedFile parse(byte[] payload){

        Objects.requireNonNull(payload, "Скрытые данные не должны быть null");

        if (payload.length < StegoHeader.FIXED_SIZE) {
            throw new IllegalArgumentException("Недостаточно данных для заголовка");
        }

        int nameLength = ((payload[5] & 0xFF) << 8) | (payload[6] & 0xFF);
        int headerLength = StegoHeader.FIXED_SIZE + nameLength;

        if (nameLength == 0 || payload.length < headerLength) {
            throw new IllegalArgumentException("Некорректная длина заголовка");
        }

        byte[] headerBytes = Arrays.copyOf(payload, headerLength);
        StegoHeader.ParsedHeader info  = StegoHeader.parse(headerBytes);
        long actualSize = (long) payload.length - headerLength;

        if (info.fileSize() != actualSize) {
            throw new IllegalArgumentException("Размер скрытого файла не совпадает с заголовком");
        }

        byte[] content = Arrays.copyOfRange(payload, headerLength, payload.length);

        CRC32 crc = new CRC32();
        crc.update(content);
        long actualCrc = crc.getValue();

        if (actualCrc != info.crc32()) {
            throw new IllegalArgumentException("Ошибка CRC32: скрытые данные повреждены");
        }

        return new ExtractedFile(info.fileName(), content);
    }
}
