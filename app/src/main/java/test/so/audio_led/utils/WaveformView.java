package test.so.audio_led.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;

import test.so.audio_led.R;

public class WaveformView extends View {
    private static final int HISTORY_SIZE = 6;
    public static final int MODE_PLAYBACK = 2;
    public static final int MODE_RECORDING = 1;
    private int brightness;
    private float centerY;
    private int colorDelta = 36;
    private Rect drawRect;
    private int height;
    private int mAudioLength;
    private Picture mCachedWaveform;
    private Bitmap mCachedWaveformBitmap;
    private int mChannels;
    private Paint mFillPaint;
    private LinkedList<float[]> mHistoricalData;
    private Paint mMarkerPaint;
    private int mMarkerPosition;
    private int mMode;
    private int mSampleRate;
    private short[] mSamples;
    private Paint mStrokePaint;
    private TextPaint mTextPaint;
    private boolean showTextAxis = true;
    private int width;
    private float xStep;

    public WaveformView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.WaveformView, defStyle, 0);
        this.mMode = a.getInt(0, 2);
        float strokeThickness = a.getFloat(5, 1.0f);
        int mStrokeColor = a.getColor(3, ContextCompat.getColor(context, R.color.default_waveform));
        int mFillColor = a.getColor(4, ContextCompat.getColor(context, R.color.default_waveformFill));
        int mMarkerColor = a.getColor(1, ContextCompat.getColor(context, R.color.default_playback_indicator));
        int mTextColor = a.getColor(2, ContextCompat.getColor(context, R.color.default_timecode));
        a.recycle();
        this.mTextPaint = new TextPaint();
        this.mTextPaint.setFlags(1);
        this.mTextPaint.setTextAlign(Align.CENTER);
        this.mTextPaint.setColor(mTextColor);
        this.mTextPaint.setTextSize(TextUtils.getFontSize(getContext(), 16842818));
        this.mStrokePaint = new Paint();
        this.mStrokePaint.setColor(mStrokeColor);
        this.mStrokePaint.setStyle(Style.STROKE);
        this.mStrokePaint.setStrokeWidth(strokeThickness);
        this.mStrokePaint.setAntiAlias(true);
        this.mFillPaint = new Paint();
        this.mFillPaint.setStyle(Style.FILL);
        this.mFillPaint.setAntiAlias(true);
        this.mFillPaint.setColor(mFillColor);
        this.mMarkerPaint = new Paint();
        this.mMarkerPaint.setStyle(Style.STROKE);
        this.mMarkerPaint.setStrokeWidth(0.0f);
        this.mMarkerPaint.setAntiAlias(true);
        this.mMarkerPaint.setColor(mMarkerColor);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.width = getMeasuredWidth();
        this.height = getMeasuredHeight();
        this.xStep = ((float) this.width) / (((float) this.mAudioLength) * 1.0f);
        this.centerY = ((float) this.height) / 2.0f;
        this.drawRect = new Rect(0, 0, this.width, this.height);
        if (this.mHistoricalData != null) {
            this.mHistoricalData.clear();
        }
        if (this.mMode == 2) {
            createPlaybackWaveform();
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        LinkedList<float[]> temp = this.mHistoricalData;
        if (this.mMode == 1 && temp != null) {
            this.brightness = this.colorDelta;
            Iterator it = temp.iterator();
            while (it.hasNext()) {
                float[] p = (float[]) it.next();
                this.mStrokePaint.setAlpha(this.brightness);
                canvas.drawLines(p, this.mStrokePaint);
                this.brightness += this.colorDelta;
            }
        } else if (this.mMode == 2) {
            if (this.mCachedWaveform != null) {
                canvas.drawPicture(this.mCachedWaveform);
            } else if (this.mCachedWaveformBitmap != null) {
                canvas.drawBitmap(this.mCachedWaveformBitmap, null, this.drawRect, null);
            }
            if (this.mMarkerPosition > -1 && this.mMarkerPosition < this.mAudioLength) {
                Canvas canvas2 = canvas;
                canvas2.drawLine(((float) this.mMarkerPosition) * this.xStep, 0.0f, ((float) this.mMarkerPosition) * this.xStep, (float) this.height, this.mMarkerPaint);
            }
        }
    }

    public int getMode() {
        return this.mMode;
    }

    public void setMode(int mMode) {
    }

    public short[] getSamples() {
        return this.mSamples;
    }

    public void setSamples(short[] samples) {
        this.mSamples = samples;
        calculateAudioLength();
        onSamplesChanged();
    }

    public int getMarkerPosition() {
        return this.mMarkerPosition;
    }

    public void setMarkerPosition(int markerPosition) {
        this.mMarkerPosition = markerPosition;
        postInvalidate();
    }

    public int getAudioLength() {
        return this.mAudioLength;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
        calculateAudioLength();
    }

    public int getChannels() {
        return this.mChannels;
    }

    public void setChannels(int channels) {
        this.mChannels = channels;
        calculateAudioLength();
    }

    public boolean showTextAxis() {
        return this.showTextAxis;
    }

    public void setShowTextAxis(boolean showTextAxis) {
        this.showTextAxis = showTextAxis;
    }

    private void calculateAudioLength() {
        if (this.mSamples != null && this.mSampleRate != 0 && this.mChannels != 0) {
            this.mAudioLength = AudioUtils.calculateAudioLength(this.mSamples.length, this.mSampleRate, this.mChannels);
        }
    }

    private void onSamplesChanged() {
        if (this.mMode == 1) {
            float[] waveformPoints;
            if (this.mHistoricalData == null) {
                this.mHistoricalData = new LinkedList();
            }
            LinkedList<float[]> temp = new LinkedList(this.mHistoricalData);
            if (temp.size() == 6) {
                waveformPoints = (float[]) temp.removeFirst();
            } else {
                waveformPoints = new float[(this.width * 4)];
            }
            drawRecordingWaveform(this.mSamples, waveformPoints);
            temp.addLast(waveformPoints);
            this.mHistoricalData = temp;
            postInvalidate();
        } else if (this.mMode == 2) {
            this.mMarkerPosition = -1;
            this.xStep = ((float) this.width) / (((float) this.mAudioLength) * 1.0f);
            Log.d("XStep", String.valueOf(this.xStep));
            createPlaybackWaveform();
        }
    }

    void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {
        float lastX = -1.0f;
        float lastY = -1.0f;
        int pointIndex = 0;
        for (int x = 0; x < this.width; x++) {
            float y = this.centerY - ((((float) buffer[(int) (((((float) x) * 1.0f) / ((float) this.width)) * ((float) buffer.length))]) / 32767.0f) * this.centerY);
            if (lastX != -1.0f) {
                int pointIndex2 = pointIndex + 1;
                waveformPoints[pointIndex] = lastX;
                pointIndex = pointIndex2 + 1;
                waveformPoints[pointIndex2] = lastY;
                pointIndex2 = pointIndex + 1;
                waveformPoints[pointIndex] = (float) x;
                pointIndex = pointIndex2 + 1;
                waveformPoints[pointIndex2] = y;
            }
            lastX = (float) x;
            lastY = y;
        }
    }

    Path drawPlaybackWaveform(int width, int height, short[] buffer) {
        int x;
        Path waveformPath = new Path();
        float centerY = ((float) height) / 2.0f;
        short[][] extremes = SamplingUtils.getExtremes(buffer, width);
        waveformPath.moveTo(0.0f, centerY);
        for (x = 0; x < width; x++) {
            waveformPath.lineTo((float) x, centerY - ((((float) extremes[x][0]) / 32767.0f) * centerY));
        }
        for (x = width - 1; x >= 0; x--) {
            waveformPath.lineTo((float) x, centerY - ((((float) extremes[x][1]) / 32767.0f) * centerY));
        }
        waveformPath.close();
        return waveformPath;
    }

    private void createPlaybackWaveform() {
        if (this.width > 0 && this.height > 0 && this.mSamples != null) {
            Canvas cacheCanvas;
            if (VERSION.SDK_INT < 23 || !isHardwareAccelerated()) {
                this.mCachedWaveformBitmap = Bitmap.createBitmap(this.width, this.height, Config.ARGB_8888);
                cacheCanvas = new Canvas(this.mCachedWaveformBitmap);
            } else {
                this.mCachedWaveform = new Picture();
                cacheCanvas = this.mCachedWaveform.beginRecording(this.width, this.height);
            }
            Path mWaveform = drawPlaybackWaveform(this.width, this.height, this.mSamples);
            cacheCanvas.drawPath(mWaveform, this.mFillPaint);
            cacheCanvas.drawPath(mWaveform, this.mStrokePaint);
            drawAxis(cacheCanvas, this.width);
            if (this.mCachedWaveform != null) {
                this.mCachedWaveform.endRecording();
            }
        }
    }

    private void drawAxis(Canvas canvas, int width) {
        if (this.showTextAxis) {
            int seconds = this.mAudioLength / 1000;
            float xStep = ((float) width) / (((float) this.mAudioLength) / 1000.0f);
            float textHeight = this.mTextPaint.getTextSize();
            int secondStep = Math.max(((int) ((((float) seconds) * this.mTextPaint.measureText("10.00")) * 2.0f)) / width, 1);
            for (float i = 0.0f; i <= ((float) seconds); i += (float) secondStep) {
                canvas.drawText(String.format("%.2f", new Object[]{Float.valueOf(i)}), i * xStep, textHeight, this.mTextPaint);
            }
        }
    }
}
