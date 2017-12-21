package test.so.audio_led;

public interface AudioDataReceivedListener {
    void onAudioDataReceived(short[] sArr);

    void sampleValues(double maxAmpDB, double maxAmpFreq, double rms, double rmsFromFFt);
}
