package cz.ujep.activitytracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class IntensityGraphView extends View {
    private final List<ActivitySample> samples = new ArrayList<>();
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IntensityGraphView(Context context) {
        super(context);
        init();
    }

    public IntensityGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IntensityGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setSamples(List<ActivitySample> newSamples) {
        samples.clear();
        samples.addAll(newSamples);
        invalidate();
    }

    private void init() {
        backgroundPaint.setColor(Color.rgb(248, 250, 249));
        axisPaint.setColor(Color.rgb(170, 178, 172));
        axisPaint.setStrokeWidth(2f);
        linePaint.setColor(Color.rgb(47, 111, 94));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(Color.rgb(75, 82, 78));
        textPaint.setTextSize(30f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 26;

        canvas.drawColor(backgroundPaint.getColor());
        canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint);
        canvas.drawLine(padding, padding, padding, height - padding, axisPaint);

        if (samples.isEmpty()) {
            canvas.drawText("Bez ulozenych vzorku", padding + 12, height / 2f, textPaint);
            return;
        }

        double maxIntensity = 1.0;
        for (ActivitySample sample : samples) {
            if (sample.intensity > maxIntensity) {
                maxIntensity = sample.intensity;
            }
        }

        if (samples.size() == 1) {
            float x = width / 2f;
            float y = yFor(samples.get(0).intensity, maxIntensity, height, padding);
            canvas.drawCircle(x, y, 7f, linePaint);
            return;
        }

        Path path = new Path();
        for (int i = 0; i < samples.size(); i++) {
            float x = padding + (width - 2f * padding) * i / (samples.size() - 1f);
            float y = yFor(samples.get(i).intensity, maxIntensity, height, padding);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);
    }

    private float yFor(double intensity, double maxIntensity, int height, int padding) {
        double ratio = Math.max(0.0, Math.min(1.0, intensity / maxIntensity));
        return (float) (height - padding - ratio * (height - 2.0 * padding));
    }
}
