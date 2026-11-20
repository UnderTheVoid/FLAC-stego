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

    public static final long MAX_FILE_SIZE = 64L * 1024 * 1024;

    private StegoHeader() {}

    public record ParsedHeader(String fileName, long fileSize, long crc32) {}

    public record FixedHeader(int nameLength, long fileSize, long crc32) {}

    public static FixedHeader readFixed(byte[] bytes) {

        Objects.requireNonNull(bytes, "Заголовок не должен быть null");

        if (bytes.length < FIXED_SIZE) {
            throw new IllegalArgumentException("Недостаточно байтов для заголовка");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        byte[] magic = new byte[4];
        buffer.get(magic);
        if (!Arrays.equals(magic, MAGIC.getBytes(StandardCharsets.US_ASCII))){
            throw new IllegalArgumentException("Неверная сигнатура заголовка");
        }

        int version =  Byte.toUnsignedInt(buffer.get());
        if (version != VERSION) {
            throw new IllegalArgumentException("Неподдерживаемая версия: " + version);
        }

        int nameLength = Short.toUnsignedInt(buffer.getShort());
        
        long fileSize = buffer.getLong();

        long crc32 = Integer.toUnsignedLong(buffer.getInt());

        if (nameLength == 0 || fileSize < 0 || fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Некорректные параметры заголовка");
        }
        
        return new FixedHeader(nameLength, fileSize, crc32);
    }

    public static byte[] create(String fileName, long fileSize, long crc32) {
        Objects.requireNonNull(fileName, "Имя файла не должно быть null");

        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);

        if (nameBytes.length == 0 || nameBytes.length > 65535){
            throw new IllegalArgumentException("Некорректная длина имени файла");
        }
        
        if (fileSize < 0 || fileSize > MAX_FILE_SIZE){
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

        FixedHeader fixed = readFixed(header);

        int expectedLength = FIXED_SIZE + fixed.nameLength();

        if (header.length != expectedLength){
            throw new IllegalArgumentException("Некорректная длина заголовка");
        }

        String fileName = new String(header, FIXED_SIZE, fixed.nameLength(), StandardCharsets.UTF_8);

        return new ParsedHeader(fileName, fixed.fileSize(), fixed.crc32());
    }
}
