package mtkdex.core.build.ssmen.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.res.ResourcesCompat;

import com.v2ray.ang.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrafficGraphView extends View {

    private static final int MAX_POINTS = 14;
    private static final float BEZIER_TENSION = 4.0f;

    private RectF drawingArea;

    private Paint inPaint;
    private Paint outPaint;
    private Paint axisPaint;
    private Paint textPaint;
    private Paint dotPaint;
    private Path reusablePath = new Path();

    private Queue<Float> inQueue;
    private Queue<Float> outQueue;

    private List<Float> previousIn;
    private List<Float> currentIn;

    private List<Float> previousOut;
    private List<Float> currentOut;

    private float minValue = 0f;
    private float maxValue = 1f;

    private float scaleX;
    private float scaleY;
    private int viewWidth;
    private int viewHeight;
    private boolean isZeroState = true;
    private boolean showPath = false;
    private boolean isFrozen = false;

    private float animationProgress = 1f;

    private int inColor;
    private int outColor;
    private float strokeWidth;
    private float graphEndGap;
    private float dp8, dp35, dp1, dp08;

    private OnAxisOffsetListener axisOffsetListener;

    public interface OnAxisOffsetListener {
        void onOffsetChanged(float offset);
    }

    public void setShowPath(boolean showPath) {
        this.showPath = showPath;
        invalidate();
    }

    private ValueAnimator currentAnimator;

    public void setFrozen(boolean frozen) {
        if (this.isFrozen == frozen) return;
        
        if (frozen) {
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }
            // Capture state BEFORE setting the flag so calculateScale/formatting still works
            updateFrozenStrings();
            calculateScale();
        }
        
        this.isFrozen = frozen;
        invalidate();
    }

    public void setFrozenStrings(String peakIn, String peakOut, String displayIn, String displayOut) {
        this.peakInStr = peakIn;
        this.peakOutStr = peakOut;
        this.displayInStr = displayIn;
        this.displayOutStr = displayOut;
        this.isFrozen = true;
        this.showPath = true;
        invalidate();
    }

    public String getPeakInStr() { return peakInStr; }
    public String getPeakOutStr() { return peakOutStr; }
    public String getDisplayInStr() { return displayInStr; }
    public String getDisplayOutStr() { return displayOutStr; }

    private void updateFrozenStrings() {
        // Ensure smoothing is applied to the final point before freezing
        if (currentIn.size() > 0) {
            float lastIn = currentIn.get(currentIn.size() - 1);
            float lastOut = currentOut.get(currentOut.size() - 1);
            smoothedIn = lastIn;
            smoothedOut = lastOut;
        }

        // 3rd and 4th labels: bit only with 1 decimal
        displayInStr = String.format(java.util.Locale.US, "%.1f bit", smoothedIn);
        displayOutStr = String.format(java.util.Locale.US, "%.1f bit", smoothedOut);

        // 1st and 2nd labels: scaled bits with 1 decimal
        float pIn = 0.0f;
        for (float v : currentIn) if (v > pIn) pIn = v;
        float pOut = 0.0f;
        for (float v : currentOut) if (v > pOut) pOut = v;

        peakInStr = formatSpeed(pIn, 1);
        peakOutStr = formatSpeed(pOut, 1);
    }

    public void setOnAxisOffsetListener(OnAxisOffsetListener listener) {
        this.axisOffsetListener = listener;
    }

    public TrafficGraphView(Context context) {
        super(context);
        initDefaults();
        initPaints();
        initData();
    }

    public TrafficGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TrafficGraphView);

        inColor = a.getColor(R.styleable.TrafficGraphView_inColor, 0xFF00E977);
        outColor = a.getColor(R.styleable.TrafficGraphView_outColor, 0xFFFF1744);
        strokeWidth = a.getDimension(R.styleable.TrafficGraphView_lineWidth, dp(1.5f));

        a.recycle();

        initPaints();
        initData();
    }

    private void initDefaults() {
        inColor = 0xFF00E977;
        outColor = 0xFFFF1744;
        strokeWidth = dp(1.5f);
    }

    private void initPaints() {

        inPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inPaint.setColor(inColor);
        inPaint.setStyle(Paint.Style.STROKE);
        inPaint.setStrokeWidth(strokeWidth);
        inPaint.setStrokeCap(Paint.Cap.ROUND);
        inPaint.setStrokeJoin(Paint.Join.ROUND);

        outPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outPaint.setColor(outColor);
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setStrokeWidth(strokeWidth);
        outPaint.setStrokeCap(Paint.Cap.ROUND);
        outPaint.setStrokeJoin(Paint.Join.ROUND);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(0xFFBDBDBD);
        axisPaint.setStrokeWidth(dp(0.8f));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF757575);
        textPaint.setTextSize(dp(8f));
        textPaint.setTextAlign(Paint.Align.LEFT);
        try {
            textPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.google_sans_flex));
        } catch (Exception e) {
            textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    private void initData() {
        dp8 = dp(8);
        dp35 = dp(35);
        dp1 = dp(1);
        dp08 = dp(0.8f);

        inQueue = new ConcurrentLinkedQueue<>();
        outQueue = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < MAX_POINTS; i++) {
            inQueue.add(0f);
            outQueue.add(0f);
        }

        previousIn = new ArrayList<>(inQueue);
        currentIn = new ArrayList<>(inQueue);

        previousOut = new ArrayList<>(outQueue);
        currentOut = new ArrayList<>(outQueue);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void updateLabelsAndOffset() {
        if (drawingArea == null) return;
        
        if (!isFrozen) {
            // Update the display strings BEFORE measuring to prevent the "lag" that causes cutting
            float lastIn = 0;
            float lastOut = 0;
            if (!currentIn.isEmpty() && !currentOut.isEmpty()) {
                lastIn = previousIn.get(currentIn.size() - 1) + ((currentIn.get(currentIn.size() - 1) - previousIn.get(currentIn.size() - 1)) * animationProgress);
                lastOut = previousOut.get(currentOut.size() - 1) + ((currentOut.get(currentOut.size() - 1) - previousOut.get(currentOut.size() - 1)) * animationProgress);
            }

            // Apply same smoothing used in drawing
            smoothedIn = smoothedIn + (lastIn - smoothedIn) * 0.15f;
            smoothedOut = smoothedOut + (lastOut - smoothedOut) * 0.15f;

            displayInStr = String.format(java.util.Locale.US, "%.1f bit", smoothedIn);
            displayOutStr = String.format(java.util.Locale.US, "%.1f bit", smoothedOut);
        }

        // Calculate dynamic offset based on the NEW strings
        float maxLabelWidth = getMaxLabelWidth();
        float dynamicOffset = Math.max(maxLabelWidth + dp8, dp35);

        if (drawingArea != null) {
            drawingArea.right = viewWidth - dynamicOffset;

            if (axisOffsetListener != null) {
                axisOffsetListener.onOffsetChanged(dynamicOffset);
            }
        }
    }

    public void addValues(float inValue, float outValue) {
        if (isFrozen) return;

        // Smoothing filter to prevent sharp jumps
        float filter = 0.5f; 
        float lastIn = currentIn.isEmpty() ? 0 : currentIn.get(currentIn.size() - 1);
        float lastOut = currentOut.isEmpty() ? 0 : currentOut.get(currentOut.size() - 1);
        
        float smoothedInVal = lastIn + (inValue - lastIn) * filter;
        float smoothedOutVal = lastOut + (outValue - lastOut) * filter;

        previousIn = new ArrayList<>(inQueue);
        previousOut = new ArrayList<>(outQueue);

        if (inQueue.size() >= MAX_POINTS) {
            inQueue.poll();
            outQueue.poll();
        }

        inQueue.add(smoothedInVal);
        outQueue.add(smoothedOutVal);

        currentIn = new ArrayList<>(inQueue);
        currentOut = new ArrayList<>(outQueue);

        calculateScale();
        startAnimation();
    }

    public void clear() {
        if (inQueue == null) return;
        inQueue.clear();
        outQueue.clear();
        for (int i = 0; i < MAX_POINTS; i++) {
            inQueue.add(0f);
            outQueue.add(0f);
        }
        previousIn = new ArrayList<>(inQueue);
        currentIn = new ArrayList<>(inQueue);
        previousOut = new ArrayList<>(outQueue);
        currentOut = new ArrayList<>(outQueue);
        animationProgress = 1f;
        
        // Reset all "Live" label data to default
        isFrozen = false;
        isZeroState = true;
        showPath = false; // HIDE the graph lines on clear (reopen/fresh launch)
        smoothedIn = 0f;
        smoothedOut = 0f;
        displayInStr = "0.0 bit";
        displayOutStr = "0.0 bit";
        peakInStr = "0 bit";
        peakOutStr = "0 bit";
        minValue = 0f;
        maxValue = 1f; 

        calculateScale();
        invalidate();
    }

    private void calculateScale() {
        if (!isFrozen) {
            float combinedMax = 0f;

            for (Float v : currentIn)
                if (v > combinedMax) combinedMax = v;

            for (Float v : currentOut)
                if (v > combinedMax) combinedMax = v;

            float actualMax = combinedMax;
            if (combinedMax <= 0f) combinedMax = 1f;

            minValue = 0f;
            maxValue = combinedMax;
            this.isZeroState = (actualMax <= 0f);

            // Update peaks immediately for label measurement
            float pIn = 0.0f;
            for (float v : currentIn) if (v > pIn) pIn = v;
            float pOut = 0.0f;
            for (float v : currentOut) if (v > pOut) pOut = v;
            peakInStr = formatSpeed(pIn, 1);
            peakOutStr = formatSpeed(pOut, 1);
        }

        updateLabelsAndOffset();

        if (drawingArea == null || drawingArea.width() <= 0) return;

        scaleX = (drawingArea.width() - graphEndGap) / (MAX_POINTS - 1);
        scaleY = (drawingArea.height() * 0.92f) / (maxValue - minValue);
    }

    private float getMaxLabelWidth() {
        if (drawingArea == null) return 0f;
        float maxWidth = 0;
        String[] labels = new String[4];
        if (isZeroState && !isFrozen) {
            labels[0] = "0 bit";
            labels[1] = "0 bit";
            labels[2] = "0.0 bit";
            labels[3] = "0.0 bit";
        } else {
            labels[0] = peakInStr;
            labels[1] = peakOutStr;
            labels[2] = displayInStr;
            labels[3] = displayOutStr;
        }

        for (String label : labels) {
            if (label == null) continue;
            float width = textPaint.measureText(label);
            if (width > maxWidth) maxWidth = width;
        }
        return maxWidth;
    }

    private void startAnimation() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(400);
        currentAnimator.setInterpolator(new LinearInterpolator());

        currentAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        currentAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.viewWidth = w;
        this.viewHeight = h;

        // GRAPH LAYOUT CONTROLS:
        float left = dp(1 );         // Start of the graph lines from the left
        float top = dp(0);          // Top margin of the graph area
        float right = w - dp(0);    // Initial gap for labels on the right
        
        // ADJUST THIS VALUE: Increase it (e.g., dp(3)) to move the graph higher up from the baseline
        float bottom = h - dp(3);    // The "Floor" or Zero-line of the graph

        this.graphEndGap = dp(2); // Unified gap between lines and vertical axis

        drawingArea = new RectF(left, top, right, bottom);
        calculateScale();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawingArea == null) return;

        // === ADJUSTMENT SECTION: Control all axis line properties here ===
        float horizontalStart = 0;      // FIXED: Axis always starts here, lines move independently
        float horizontalEnd = 0;        // Right extension of horizontal line
        float horizontalStrokeWidth = dp08; // Thickness of horizontal line

        float verticalTop = drawingArea.top;          // Aligned with graph top
        float verticalBottom = viewHeight - drawingArea.bottom;       // Aligned with graph "Floor"
        float verticalStrokeWidth = dp08;   // Thickness of vertical line
        // ===============================================================================

        // 1. Draw Horizontal Baseline (Locked near the bottom of the view)
        axisPaint.setStrokeWidth(horizontalStrokeWidth);
        float baselineY = viewHeight;
        canvas.drawLine(horizontalStart, baselineY, drawingArea.right + horizontalEnd, baselineY, axisPaint);
        
        // 2. Draw Vertical Axis Line
        axisPaint.setStrokeWidth(verticalStrokeWidth);
        canvas.drawLine(drawingArea.right, verticalTop, drawingArea.right, baselineY, axisPaint);

        // Paths - Only draw if showPath is true AND we have non-zero data
        if (showPath) {
            boolean hasData = false;
            for (Float f : currentIn) if (f > 0) { hasData = true; break; }
            if (!hasData) {
                for (Float f : currentOut) if (f > 0) { hasData = true; break; }
            }

            if (hasData || isFrozen) {
                drawPath(canvas, currentIn, previousIn, inPaint);
                drawPath(canvas, currentOut, previousOut, outPaint);
            }
        }

        // Labels - ALWAYS DRAWN IF NOT HIDDEN BY OUTSIDE LOGIC
        drawLabels(canvas);
    }

    private void drawPath(Canvas canvas,
                          List<Float> current,
                          List<Float> previous,
                          Paint paint) {

        if (current.size() < 2) return;

        reusablePath.reset();

        if (isFrozen) {
            reusablePath.moveTo(drawingArea.left, drawingArea.bottom);
            reusablePath.lineTo(drawingArea.right - graphEndGap, drawingArea.bottom);
        } else {
            // Pre-calculate animated points to ensure smooth waves
            List<PointF> points = new ArrayList<>();
            for (int i = 0; i < current.size(); i++) {
                float val = previous.get(i) + ((current.get(i) - previous.get(i)) * animationProgress);
                float x = drawingArea.left + (scaleX * i);
                float y = drawingArea.bottom - ((val - minValue) * scaleY);
                points.add(new PointF(x, y));
            }

            reusablePath.moveTo(points.get(0).x, points.get(0).y);

            for (int i = 0; i < points.size() - 1; i++) {
                PointF p1 = points.get(i);
                PointF p2 = points.get(i + 1);

                // If both points are at the bottom (zero), draw a perfectly straight line
                if (Math.abs(p1.y - drawingArea.bottom) < 0.5f && Math.abs(p2.y - drawingArea.bottom) < 0.5f) {
                    reusablePath.lineTo(p2.x, p2.y);
                } else {
                    PointF p0 = (i > 0) ? points.get(i - 1) : p1;
                    PointF p3 = (i < points.size() - 2) ? points.get(i + 2) : p2;

                    float tension = BEZIER_TENSION;
                    float cp1x = p1.x + (p2.x - p0.x) / tension;
                    float cp1y = p1.y + (p2.y - p0.y) / tension;
                    
                    float cp2x = p2.x - (p3.x - p1.x) / tension;
                    float cp2y = p2.y - (p3.y - p1.y) / tension;

                    // Constrain control points to drawing area
                    cp1y = Math.max(drawingArea.top, Math.min(cp1y, drawingArea.bottom));
                    cp2y = Math.max(drawingArea.top, Math.min(cp2y, drawingArea.bottom));

                    reusablePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y);
                }
            }
        }

        canvas.drawPath(reusablePath, paint);
    }

    private void drawEndDots(Canvas canvas) {
        // Dot drawing removed as per request
    }

    private float smoothedIn = 0f;
    private float smoothedOut = 0f;
    private long lastLabelUpdateTime = 0;
    private String displayInStr = "0.0 bit";
    private String displayOutStr = "0.0 bit";
    private String peakInStr = "0 bit";
    private String peakOutStr = "0 bit";

    private void drawLabels(Canvas canvas) {
        if (currentIn.isEmpty() || currentOut.isEmpty()) return;

        // Strings are now updated in updateLabelsAndOffset() to ensure correct measurement
        String[] labels = new String[4];
        if (isZeroState && !isFrozen) {
            labels[0] = "0 bit";
            labels[1] = "0 bit";
            labels[2] = "0.0 bit";
            labels[3] = "0.0 bit";
        } else {
            labels[0] = peakInStr;
            labels[1] = peakOutStr;
            labels[2] = displayInStr;
            labels[3] = displayOutStr;
        }

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeightOffset = (fm.ascent + fm.descent) / 2;

        for (int i = 0; i < 4; i++) {
            // LABEL SPACING CONTROLS:
            // 0.25f controls the vertical distance between the 4 labels
            // 0.10f controls the starting height (Top padding)
            float percent = (i * 0.25f) + 0.10f;
            
            // Labels use viewHeight so they don't move when graph 'bottom' changes
            float y = viewHeight * percent; 
            float drawY = y - textHeightOffset;

            canvas.drawText(
                    labels[i],
                    drawingArea.right + dp1, // Horizontal gap between line and text
                    drawY,
                    textPaint
            );
        }
    }

    private String formatSpeed(float value, int decimalCount) {
        if (value <= 0) {
            return "0 bit";
        }
        String format = "%." + decimalCount + "f %s";
        if (value >= 1_000_000_000_000f) {
            float val = value / 1_000_000_000_000f;
            return String.format(java.util.Locale.US, format, val, "Tbit");
        }
        if (value >= 1_000_000_000f) {
            float val = value / 1_000_000_000f;
            return String.format(java.util.Locale.US, format, val, "Gbit");
        }
        if (value >= 1_000_000f) {
            float val = value / 1_000_000f;
            return String.format(java.util.Locale.US, format, val, "Mbit");
        }
        if (value >= 1_000f) {
            float val = value / 1_000f;
            return String.format(java.util.Locale.US, format, val, "kbit");
        }
        return String.format(java.util.Locale.US, format, value, "bit");
    }
}
