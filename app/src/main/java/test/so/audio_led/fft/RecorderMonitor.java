package test.so.audio_led.fft;

import android.os.SystemClock;
import android.util.Log;

class RecorderMonitor {
    private static final String TAG0 = "RecorderMonitor:";
    private final String TAG;
    private int bufferSampleSize;
    private boolean lastCheckOverrun = false;
    private long lastOverrunTime;
    private long nSamplesRead;
    private int sampleRate;
    private double sampleRateReal;
    private long timeStarted;
    private long timeUpdateInterval;
    private long timeUpdateOld;

    RecorderMonitor(int sampleRateIn, int bufferSampleSizeIn, String TAG1) {
        this.sampleRate = sampleRateIn;
        this.bufferSampleSize = bufferSampleSizeIn;
        this.timeUpdateInterval = 2000;
        this.TAG = TAG1 + TAG0;
    }

    void start() {
        this.nSamplesRead = 0;
        this.lastOverrunTime = 0;
        this.timeStarted = SystemClock.uptimeMillis();
        this.timeUpdateOld = this.timeStarted;
        this.sampleRateReal = (double) this.sampleRate;
    }

    boolean updateState(int numOfReadShort) {
        long timeNow = SystemClock.uptimeMillis();
        if (this.nSamplesRead == 0) {
            this.timeStarted = timeNow - ((long) ((numOfReadShort * 1000) / this.sampleRate));
        }
        this.nSamplesRead += (long) numOfReadShort;
        if (this.timeUpdateOld + this.timeUpdateInterval > timeNow) {
            return false;
        }
        this.timeUpdateOld += this.timeUpdateInterval;
        if (this.timeUpdateOld + this.timeUpdateInterval <= timeNow) {
            this.timeUpdateOld = timeNow;
        }
        long nSamplesFromTime = (long) ((((double) (timeNow - this.timeStarted)) * this.sampleRateReal) / 1000.0d);
        double f1 = ((double) this.nSamplesRead) / this.sampleRateReal;
        double f2 = ((double) nSamplesFromTime) / this.sampleRateReal;
        if (nSamplesFromTime > ((long) this.bufferSampleSize) + this.nSamplesRead) {
            Log.w(this.TAG, "SamplingLoop::run(): Buffer Overrun occurred !\n should read " + nSamplesFromTime + " (" + (((double) Math.round(1000.0d * f2)) / 1000.0d) + "s), actual read " + this.nSamplesRead + " (" + (((double) Math.round(1000.0d * f1)) / 1000.0d) + "s)\n diff " + (nSamplesFromTime - this.nSamplesRead) + " (" + (((double) Math.round((f2 - f1) * 1000.0d)) / 1000.0d) + "s) sampleRate = " + (((double) Math.round(this.sampleRateReal * 100.0d)) / 100.0d) + "\n Overrun counter reset.");
            this.lastOverrunTime = timeNow;
            this.nSamplesRead = 0;
        }
        if (this.nSamplesRead > ((long) (this.sampleRate * 10))) {
            this.sampleRateReal = (0.9d * this.sampleRateReal) + (0.1d * ((((double) this.nSamplesRead) * 1000.0d) / ((double) (timeNow - this.timeStarted))));
            if (Math.abs(this.sampleRateReal - ((double) this.sampleRate)) > 0.0145d * ((double) this.sampleRate)) {
                Log.w(this.TAG, "SamplingLoop::run(): Sample rate inaccurate, possible hardware problem !\n should read " + nSamplesFromTime + " (" + (((double) Math.round(1000.0d * f2)) / 1000.0d) + "s), actual read " + this.nSamplesRead + " (" + (((double) Math.round(1000.0d * f1)) / 1000.0d) + "s)\n diff " + (nSamplesFromTime - this.nSamplesRead) + " (" + (((double) Math.round((f2 - f1) * 1000.0d)) / 1000.0d) + "s) sampleRate = " + (((double) Math.round(this.sampleRateReal * 100.0d)) / 100.0d) + "\n Overrun counter reset.");
                this.nSamplesRead = 0;
            }
        }
        this.lastCheckOverrun = this.lastOverrunTime == timeNow;
        return true;
    }

    boolean getLastCheckOverrun() {
        return this.lastCheckOverrun;
    }

    long getLastOverrunTime() {
        return this.lastOverrunTime;
    }

    double getSampleRate() {
        return this.sampleRateReal;
    }
}
