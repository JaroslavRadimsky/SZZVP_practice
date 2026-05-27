package cz.ujep.activitytracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class TrackingActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_ACTIVITY_RECOGNITION = 10;
    private static final int NOTIFICATION_ID = 2701;
    private static final String CHANNEL_ID = "active_measurement";
    private static final long SAMPLE_INTERVAL_MS = 5000L;
    private static final long TIMER_INTERVAL_MS = 1000L;
    private static final long STEP_PEAK_GAP_MS = 350L;

    private ActivityDatabase database;
    private SensorManager sensorManager;
    private NotificationManager notificationManager;
    private Sensor stepSensor;
    private Sensor motionSensor;
    private TextView sensorStatusText;
    private TextView clockText;
    private TextView stepsText;
    private TextView intensityText;
    private TextView sampleStatusText;
    private Button stopButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean measuring;
    private boolean usingStepCounter;
    private long activeMeasurementId = -1L;
    private long startedAt;
    private long lastSampleAt;
    private int stepBase = -1;
    private int currentStepTotal;
    private int estimatedSteps;
    private int lastStoredSteps;
    private double intensitySum;
    private int intensityEvents;
    private double lastIntensity;
    private long lastPeakAt;

    private final Runnable sampleRunnable = new Runnable() {
        @Override
        public void run() {
            if (measuring) {
                storeSample();
                handler.postDelayed(this, SAMPLE_INTERVAL_MS);
            }
        }
    };

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (measuring) {
                updateLiveStats();
                showOrUpdateNotification();
                handler.postDelayed(this, TIMER_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_tracking);

        database = new ActivityDatabase(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        motionSensor = selectMotionSensor();

        sensorStatusText = findViewById(R.id.sensorStatusText);
        clockText = findViewById(R.id.clockText);
        stepsText = findViewById(R.id.stepsText);
        intensityText = findViewById(R.id.intensityText);
        sampleStatusText = findViewById(R.id.sampleStatusText);
        stopButton = findViewById(R.id.stopButton);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(view -> stopMeasurementAndFinish());

        requestActivityRecognitionIfNeededOrStart();
    }

    @Override
    protected void onDestroy() {
        if (measuring) {
            finishMeasurement();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!measuring) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Mereni probiha")
                .setMessage("Chcete aktivitu ukoncit a ulozit?")
                .setPositiveButton("Ukoncit", (dialog, which) -> stopMeasurementAndFinish())
                .setNegativeButton("Pokracovat", null)
                .show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!measuring) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int rawSteps = (int) event.values[0];
            if (stepBase < 0) {
                stepBase = rawSteps;
            }
            currentStepTotal = Math.max(0, rawSteps - stepBase);
            return;
        }

        double intensity = calculateMotionIntensity(event);
        lastIntensity = intensity;
        intensitySum += intensity;
        intensityEvents++;

        if (!usingStepCounter && intensity > 2.2) {
            long now = System.currentTimeMillis();
            if (now - lastPeakAt > STEP_PEAK_GAP_MS) {
                estimatedSteps++;
                lastPeakAt = now;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION && !measuring) {
            startMeasurement();
        }
    }

    private void requestActivityRecognitionIfNeededOrStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && stepSensor != null
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            sensorStatusText.setText("Cekam na opravneni pro krokomer.");
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY_RECOGNITION);
            return;
        }
        startMeasurement();
    }

    private void startMeasurement() {
        if (measuring) {
            return;
        }
        startedAt = System.currentTimeMillis();
        activeMeasurementId = database.startMeasurement(startedAt);
        if (activeMeasurementId < 0L) {
            sensorStatusText.setText("Mereni se nepodarilo ulozit.");
            return;
        }

        stepBase = -1;
        currentStepTotal = 0;
        estimatedSteps = 0;
        lastStoredSteps = 0;
        intensitySum = 0.0;
        intensityEvents = 0;
        lastIntensity = 0.0;
        lastPeakAt = 0L;
        lastSampleAt = 0L;
        usingStepCounter = canUseStepCounter();
        measuring = true;

        createNotificationChannelIfNeeded();
        registerSensors();
        stopButton.setEnabled(true);
        updateSensorStatus();
        updateLiveStats();
        showOrUpdateNotification();
        handler.postDelayed(sampleRunnable, SAMPLE_INTERVAL_MS);
        handler.postDelayed(timerRunnable, TIMER_INTERVAL_MS);
    }

    private void stopMeasurementAndFinish() {
        if (measuring) {
            finishMeasurement();
        }
        finish();
    }

    private void finishMeasurement() {
        if (activeMeasurementId >= 0L) {
            storeSample();
            database.finishMeasurement(activeMeasurementId, System.currentTimeMillis());
        }
        measuring = false;
        activeMeasurementId = -1L;
        unregisterSensors();
        handler.removeCallbacks(sampleRunnable);
        handler.removeCallbacks(timerRunnable);
        cancelNotification();
    }

    private void storeSample() {
        if (activeMeasurementId < 0L) {
            return;
        }
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        int deltaSteps = Math.max(0, totalSteps - lastStoredSteps);
        lastStoredSteps = totalSteps;
        double averageIntensity = intensityEvents == 0 ? lastIntensity : intensitySum / intensityEvents;
        long now = System.currentTimeMillis();
        database.insertSample(activeMeasurementId, now, deltaSteps, averageIntensity);
        lastSampleAt = now;
        sampleStatusText.setText("Posledni vzorek ulozen v " + android.text.format.DateFormat.format("HH:mm:ss", now));
        intensitySum = 0.0;
        intensityEvents = 0;
    }

    private void updateLiveStats() {
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        long duration = System.currentTimeMillis() - startedAt;
        clockText.setText(ActivityStatsCalculator.formatDuration(duration));
        stepsText.setText(String.format(Locale.US, "Kroky: %d", totalSteps));
        intensityText.setText("Intenzita: " + ActivityStatsCalculator.formatIntensity(lastIntensity));
        if (lastSampleAt == 0L) {
            long untilFirstSample = Math.max(0L, SAMPLE_INTERVAL_MS - duration);
            sampleStatusText.setText("Prvni vzorek za " + Math.max(1L, untilFirstSample / 1000L) + " s");
        }
    }

    private void registerSensors() {
        if (usingStepCounter) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (motionSensor != null) {
            sensorManager.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    private Sensor selectMotionSensor() {
        Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (linear != null) {
            return linear;
        }
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private double calculateMotionIntensity(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            magnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
        }
        return Math.min(10.0, magnitude);
    }

    private boolean canUseStepCounter() {
        if (stepSensor == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updateSensorStatus() {
        if (usingStepCounter) {
            sensorStatusText.setText("Pouziva se krokomer a pohybovy senzor.");
        } else if (motionSensor != null) {
            sensorStatusText.setText("Pouziva se odhad kroku z pohyboveho senzoru.");
        } else {
            sensorStatusText.setText("Zarizeni nema dostupny pohybovy senzor.");
        }
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Probihajici aktivita",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Informace o prave merene fyzicke aktivite.");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showOrUpdateNotification() {
        if (!measuring) {
            return;
        }

        Intent intent = new Intent(this, TrackingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        String text = String.format(
                Locale.US,
                "Cas %s | kroky %d",
                ActivityStatsCalculator.formatDuration(System.currentTimeMillis() - startedAt),
                totalSteps
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_activity_notification)
                .setContentTitle("Aktivita probiha")
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setWhen(startedAt)
                .setUsesChronometer(true)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
