package test.so.audio_led.utils;

public final class AudioUtils {
    public static int calculateAudioLength(int samplesCount, int sampleRate, int channelCount) {
        return ((samplesCount / channelCount) * 1000) / sampleRate;
    }
}
