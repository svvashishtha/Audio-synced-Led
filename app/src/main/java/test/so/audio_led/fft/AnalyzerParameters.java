package test.so.audio_led.fft;

import android.content.res.Resources;
import android.util.Log;

import test.so.audio_led.R;

public class AnalyzerParameters {
    final int BYTE_OF_SAMPLE = 2;
    final int RECORDER_AGC_OFF = 6;
    final double SAMPLE_VALUE_MAX = 32767.0d;
    int[] audioSourceIDs;
    int audioSourceId = 6;
    String[] audioSourceNames;
    int fftLen = 2048;
    int hopLen = 1024;
    boolean isAWeighting = false;
    double[] micGainDB = null;
    int nFFTAverage = 2;
    double overlapPercent = 50.0d;
    int sampleRate = 8000;
    double spectrogramDuration = 4.0d;
    String wndFuncName;

    public AnalyzerParameters(Resources res) {
        getAudioSourceNameFromIdPrepare(res);
    }

    private void getAudioSourceNameFromIdPrepare(Resources res) {
        this.audioSourceNames = res.getStringArray(R.array.audio_source);
        String[] sasid = res.getStringArray(R.array.audio_source_id);
        this.audioSourceIDs = new int[this.audioSourceNames.length];
        for (int i = 0; i < this.audioSourceNames.length; i++) {
            this.audioSourceIDs[i] = Integer.parseInt(sasid[i]);
        }
    }

    String getAudioSourceNameFromId(int id) {
        for (int i = 0; i < this.audioSourceNames.length; i++) {
            if (this.audioSourceIDs[i] == id) {
                return this.audioSourceNames[i];
            }
        }
        Log.i("AnalyzerParameters", "getAudioSourceName(): non-standard entry.");
        return Integer.valueOf(id).toString();
    }

    String getAudioSourceName() {
        return getAudioSourceNameFromId(this.audioSourceId);
    }

    public int getRECORDER_AGC_OFF() {
        return 6;
    }

    public int getBYTE_OF_SAMPLE() {
        return 2;
    }

    public double getSAMPLE_VALUE_MAX() {
        return 32767.0d;
    }

    public int getAudioSourceId() {
        return this.audioSourceId;
    }

    public void setAudioSourceId(int audioSourceId) {
        this.audioSourceId = audioSourceId;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getFftLen() {
        return this.fftLen;
    }

    public void setFftLen(int fftLen) {
        this.fftLen = fftLen;
    }

    public int getHopLen() {
        return this.hopLen;
    }

    public void setHopLen(int hopLen) {
        this.hopLen = hopLen;
    }

    public double getOverlapPercent() {
        return this.overlapPercent;
    }

    public void setOverlapPercent(double overlapPercent) {
        this.overlapPercent = overlapPercent;
    }

    public String getWndFuncName() {
        return this.wndFuncName;
    }

    public void setWndFuncName(String wndFuncName) {
        this.wndFuncName = wndFuncName;
    }

    public int getnFFTAverage() {
        return this.nFFTAverage;
    }

    public void setnFFTAverage(int nFFTAverage) {
        this.nFFTAverage = nFFTAverage;
    }

    public boolean isAWeighting() {
        return this.isAWeighting;
    }

    public void setAWeighting(boolean AWeighting) {
        this.isAWeighting = AWeighting;
    }

    public double getSpectrogramDuration() {
        return this.spectrogramDuration;
    }

    public void setSpectrogramDuration(double spectrogramDuration) {
        this.spectrogramDuration = spectrogramDuration;
    }

    public double[] getMicGainDB() {
        return this.micGainDB;
    }

    public void setMicGainDB(double[] micGainDB) {
        this.micGainDB = micGainDB;
    }

    public String[] getAudioSourceNames() {
        return this.audioSourceNames;
    }

    public void setAudioSourceNames(String[] audioSourceNames) {
        this.audioSourceNames = audioSourceNames;
    }

    public int[] getAudioSourceIDs() {
        return this.audioSourceIDs;
    }

    public void setAudioSourceIDs(int[] audioSourceIDs) {
        this.audioSourceIDs = audioSourceIDs;
    }
}
