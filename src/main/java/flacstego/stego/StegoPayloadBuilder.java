package flacstego.stego;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.CRC32;

public final class StegoPayloadBuilder {
    private StegoPayloadBuilder() {}

    public static byte[] fromFile(Path path) throws IOException {

        Objects.requireNonNull(path, "Путь не должен быть null");

        if (Files.size(path) > StegoHeader.MAX_FILE_SIZE) {
            throw new IOException("Скрываемый файл превышает допустимый размер");
        }

        byte[] fileData = Files.readAllBytes(path);

        if (fileData.length > StegoHeader.MAX_FILE_SIZE) {
            throw new IOException("Скрываемый файл превышает допустимый размер");
        }

        Path fileNamePath = path.getFileName();

        if (fileNamePath == null){
            throw new IllegalArgumentException("Путь н содержит имени файла");
        }

        String fileName = fileNamePath.toString();

        CRC32 crc = new CRC32();

        crc.update(fileData);

        long checksum = crc.getValue();

        byte[] header = StegoHeader.create(fileName, fileData.length, checksum);

        long totalSize = (long) header.length + fileData.length;

        if (totalSize > Integer.MAX_VALUE){
            throw new IOException("Скрываемые данные слишком велики для текущей реализации :( ");
        }

        byte[] payload = new byte[(int) totalSize];

        System.arraycopy(header, 0, payload, 0, header.length);
        System.arraycopy(fileData, 0, payload, header.length, fileData.length);

        return payload;
    }
}
