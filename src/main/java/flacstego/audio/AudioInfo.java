package flacstego.audio;

public record AudioInfo(int sampleRate, int channels, int bitsPerSample, long totalSamplesPerChannel){
    public AudioInfo{
        if (sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0 || totalSamplesPerChannel < 0){
            throw new IllegalArgumentException("Некорректные параметры аудио");
        }
    }
    public double durationSeconds(){
        return (double) totalSamplesPerChannel / sampleRate;
    }
}