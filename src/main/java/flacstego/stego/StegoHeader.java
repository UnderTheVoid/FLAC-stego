package flacstego.stego;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Arrays;

public final class StegoHeader {
    
    public static final String MAGIC = "FSTG";

    public static final int VERSION = 1;

    public static final int FIXED_SIZE = 19;

    private StegoHeader() {}

    public record ParsedHeader(String fileName, long fileSize, long crc32) {}

    public static byte[] create(String fileName, long fileSize, long crc32) {
        Objects.requireNonNull(fileName, "Имя файла не должно быть null");

        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);

        if (nameBytes.length == 0 || nameBytes.length > 65535){
            throw new IllegalArgumentException("Некорректная длина имени файла");
        }
        
        if (fileSize < 0){
            throw new IllegalArgumentException("Размер файла не может быть отрицательным");
        }

        if (crc32 < 0 || crc32 > 0xFFFFFFFFL){
            throw new IllegalArgumentException("Некорректное значение CRC32");
        }

        ByteBuffer buffer = ByteBuffer.allocate(FIXED_SIZE + nameBytes.length);

        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(MAGIC.getBytes(StandardCharsets.US_ASCII));

        buffer.put((byte) VERSION);

        buffer.putShort((short) nameBytes.length);

        buffer.putLong(fileSize);

        buffer.putInt((int) crc32);

        buffer.put(nameBytes);

        return buffer.array();
    }

    public static ParsedHeader parse(byte[] header) {

        Objects.requireNonNull(header, "Заголовок не должен быть null");
        
        if (header.length < FIXED_SIZE){
            throw new IllegalArgumentException("Заголовок слишком короткий");
        }

        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(ByteOrder.BIG_ENDIAN);

        byte[] magicBytes = new byte[4];
        buffer.get(magicBytes);

        if (!Arrays.equals(magicBytes, MAGIC.getBytes(StandardCharsets.US_ASCII))){
            throw new IllegalArgumentException("Неверная сигнатура заголовка");
        }

        int version = Byte.toUnsignedInt(buffer.get());

        if (version != VERSION){
            throw new IllegalArgumentException("Неподдерживаемая версия: " + version);
        }

        int nameLength = Short.toUnsignedInt(buffer.getShort());

        long fileSize = buffer.getLong();

        long crc32 = Integer.toUnsignedLong(buffer.getInt());

        if (fileSize < 0) {
            throw new IllegalArgumentException("НЕкорректный размер файла");
        }

        if (nameLength == 0 || header.length != FIXED_SIZE + nameLength){
            throw new IllegalArgumentException("Некорректная длина заголовка");
        }

        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);

        String fileName = new String(nameBytes, StandardCharsets.UTF_8);

        return new ParsedHeader(fileName, fileSize, crc32);
    }
}
