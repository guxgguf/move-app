package abak.tr.com.boxedverticalseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.dooo.android.R;

/** Local drop-in replacement for com.github.alpbak:BoxedVerticalSeekBar (not available on JitPack anymore). */
public class BoxedVertical extends View {

    public interface OnValuesChangeListener {
        void onPointsChanged(BoxedVertical boxedPoints, int points);
        void onStartTrackingTouch(BoxedVertical boxedPoints);
        void onStopTrackingTouch(BoxedVertical boxedPoints);
    }

    private int min = 0;
    private int max = 100;
    private int value = 0;
    private boolean touchDisabled = false;
    private float cornerRadius;
    private int backgroundColor = 0x4DFFFFFF;
    private int progressColor = 0xFFFFFFFF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private OnValuesChangeListener listener;

    public BoxedVertical(Context context) { this(context, null); }
    public BoxedVertical(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public BoxedVertical(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cornerRadius = 10 * getResources().getDisplayMetrics().density;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoxedVertical, defStyleAttr, 0);
            try {
                backgroundColor = a.getColor(R.styleable.BoxedVertical_bvBackgroundColor, backgroundColor);
                progressColor = a.getColor(R.styleable.BoxedVertical_bvProgressColor, progressColor);
                cornerRadius = a.getDimension(R.styleable.BoxedVertical_bvCornerRadius, cornerRadius);
                min = a.getInt(R.styleable.BoxedVertical_bvMin, min);
                max = a.getInt(R.styleable.BoxedVertical_bvMax, max);
                value = a.getInt(R.styleable.BoxedVertical_bvDefaultValue, value);
                touchDisabled = a.getBoolean(R.styleable.BoxedVertical_bvTouchDisabled, touchDisabled);
            } finally {
                a.recycle();
            }
        }
        value = clamp(value);
    }

    private int clamp(int v) { return Math.max(min, Math.min(max, v)); }

    public int getValue() { return value; }
    public int getMax() { return max; }
    public int getMin() { return min; }

    public void setValue(int v) {
        value = clamp(v);
        invalidate();
    }

    public void setMax(int m) {
        max = Math.max(m, min + 1);
        value = clamp(value);
        invalidate();
    }

    public void setMin(int m) {
        min = Math.min(m, max - 1);
        value = clamp(value);
        invalidate();
    }

    public void setOnBoxedPointsChangeListener(OnValuesChangeListener l) { listener = l; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = Math.min(cornerRadius, Math.min(w, h) / 2f);
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        canvas.drawRect(rect, paint);
        float frac = (max == min) ? 0f : (value - min) / (float) (max - min);
        paint.setColor(progressColor);
        canvas.drawRect(0, h - h * frac, w, h, paint);
        canvas.restoreToCount(save);
    }

    private void updateFromY(float y) {
        float h = getHeight();
        if (h <= 0) return;
        float frac = 1f - Math.max(0f, Math.min(1f, y / h));
        int nv = clamp(Math.round(min + frac * (max - min)));
        if (nv != value) {
            value = nv;
            invalidate();
            if (listener != null) listener.onPointsChanged(this, value);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                if (listener != null) listener.onStartTrackingTouch(this);
                if (!touchDisabled) updateFromY(e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromY(e.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (listener != null) listener.onStopTrackingTouch(this);
                return true;
        }
        return super.onTouchEvent(e);
    }
}
