package test.so.audio_led.fft;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import test.so.audio_led.AudioDataReceivedListener;

public class RecordingThread {
    private String TAG = "RecordThread";
    private AnalyzerParameters analyzerParam;
    private AudioDataReceivedListener mListener;
    private boolean mShouldContinue;
    private Thread mThread;
    private double[] spectrumDBcopy;
    private STFT stft;
    AudioRecord record;

    public RecordingThread(AudioDataReceivedListener listener, AnalyzerParameters analyzerParam) {
        this.mListener = listener;
        this.analyzerParam = analyzerParam;
    }

    public boolean recording() {
        return this.mThread != null;
    }

    public void startRecording() {
        if (this.mThread == null) {
            this.mShouldContinue = true;
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    record1();
                }
            });
            this.mThread.start();
        }
    }

    public void stopRecording() {
        if (mThread != null) {
            mShouldContinue = false;
            mThread = null;
        }
    }

    private void record1() {



        long tStart = SystemClock.uptimeMillis();
        long tEnd = SystemClock.uptimeMillis();
        if (tEnd - tStart < 500) {
            Log.i(this.TAG, "wait more.." + (500 - (tEnd - tStart)) + " ms");
            SleepWithoutInterrupt(500 - (tEnd - tStart));
        }
        int minBytes = AudioRecord.getMinBufferSize(this.analyzerParam.sampleRate, 16, 2);
        if (minBytes == -2) {
            Log.e(this.TAG, "SamplingLoop::run(): Invalid AudioRecord parameter.\n");
            return;
        }

          /*
          Develop -> Reference -> AudioRecord
             Data should be read from the audio hardware in chunks of sizes
             inferior to the total recording buffer size.
         */
        // Determine size of buffers for AudioRecord and AudioRecord::read()
        int readChunkSize = analyzerParam.hopLen;  // Every hopLen one fft result (overlapped analyze window)
        readChunkSize = Math.min(readChunkSize, 2048);  // read in a smaller chunk, hopefully smaller delay
        int bufferSampleSize = Math.max(minBytes / analyzerParam.BYTE_OF_SAMPLE, analyzerParam.fftLen / 2) * 2;
        // tolerate up to about 1 sec.
        bufferSampleSize = (int) Math.ceil(1.0 * analyzerParam.sampleRate / bufferSampleSize) * bufferSampleSize;

        // Use the mic with AGC turned off. e.g. VOICE_RECOGNITION for measurement
        // The buffer size here seems not relate to the delay.
        // So choose a larger size (~1sec) so that overrun is unlikely.

        try {
            if (analyzerParam.audioSourceId < 1000) {
                record = new AudioRecord(analyzerParam.audioSourceId, analyzerParam.sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, analyzerParam.BYTE_OF_SAMPLE * bufferSampleSize);
            } else {
                record = new AudioRecord(analyzerParam.RECORDER_AGC_OFF, analyzerParam.sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, analyzerParam.BYTE_OF_SAMPLE * bufferSampleSize);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Fail to initialize recorder.");
            return;
        }

        Log.i(TAG, "SamplingLoop::Run(): Starting recorder... \n" +
                "  source          : " + analyzerParam.getAudioSourceName() + "\n" +
                String.format("  sample rate     : %d Hz (request %d Hz)\n", record.getSampleRate(), analyzerParam.sampleRate) +
                String.format("  min buffer size : %d samples, %d Bytes\n", minBytes / analyzerParam.BYTE_OF_SAMPLE, minBytes) +
                String.format("  buffer size     : %d samples, %d Bytes\n", bufferSampleSize, analyzerParam.BYTE_OF_SAMPLE * bufferSampleSize) +
                String.format("  read chunk size : %d samples, %d Bytes\n", readChunkSize, analyzerParam.BYTE_OF_SAMPLE * readChunkSize) +
                String.format("  FFT length      : %d\n", analyzerParam.fftLen) +
                String.format("  nFFTAverage     : %d\n", analyzerParam.nFFTAverage));

        analyzerParam.sampleRate = record.getSampleRate();
        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "SamplingLoop::run(): Fail to initialize AudioRecord()");
            // If failed somehow, leave user a chance to change preference.
            return;
        }
        short[] audioSamples = new short[readChunkSize];
        int numOfReadShort = 0;

        stft = new STFT(analyzerParam);
        stft.setAWeighting(analyzerParam.isAWeighting);
        if (spectrumDBcopy == null || spectrumDBcopy.length != analyzerParam.fftLen / 2 + 1) {
            spectrumDBcopy = new double[analyzerParam.fftLen / 2 + 1];
        }

        RecorderMonitor recorderMonitor = new RecorderMonitor(analyzerParam.sampleRate, bufferSampleSize, "SamplingLoop::run()");
        recorderMonitor.start();

// Start recording
        try {
            record.startRecording();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Fail to start recording.");
            return;
        }

        // Main loop
        // When running in this loop (including when paused), you can not change properties
        // related to recorder: e.g. audioSourceId, sampleRate, bufferSampleSize
        // TODO: allow change of FFT length on the fly.
        while (mShouldContinue) {
            // Read data

            numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling

            if (recorderMonitor.updateState(numOfReadShort) && recorderMonitor.getLastCheckOverrun())
                    Log.d(this.TAG, "Recorder buffer overrun!\nYour cell phone is too slow.\nTry lower sampling rate or higher average number.");

            mListener.onAudioDataReceived(audioSamples);

            stft.feedData(audioSamples, numOfReadShort);

            // If there is new spectrum data, do plot
            if (stft.nElemSpectrumAmp() >= analyzerParam.nFFTAverage) {
                // Update spectrum or spectrogram
                final double[] spectrumDB = stft.getSpectrumAmpDB();
                System.arraycopy(spectrumDB, 0, spectrumDBcopy, 0, spectrumDB.length);
                stft.calculatePeak();
                Log.d("maxAmpFreq", String.valueOf(stft.maxAmpFreq));
                Log.d("maxAmpDB", String.valueOf(stft.maxAmpDB));
                Log.d("dtRMS", String.valueOf(stft.getRMS()));
                Log.d("dtRMSFromFT", String.valueOf(stft.getRMSFromFT()));
                mListener.sampleValues(stft.maxAmpDB, stft.maxAmpFreq, stft.getRMS(), stft.getRMSFromFT());
            }
        }
        Log.i(TAG, "SamplingLoop::Run(): Actual sample rate: " + recorderMonitor.getSampleRate());
        Log.i(TAG, "SamplingLoop::Run(): Stopping and releasing recorder.");

        record.stop();
        record.release();


    }

    private void SleepWithoutInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
