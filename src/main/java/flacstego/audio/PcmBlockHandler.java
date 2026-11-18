package flacstego.audio;

import java.io.IOException;

@FunctionalInterface 
public interface PcmBlockHandler {
    void handle(int[][] samples, int blockSize) throws IOException;
}
