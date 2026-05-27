package cz.ujep.activitytracker;

import android.Manifest;
import android.app.Activity;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_ACTIVITY_RECOGNITION = 10;
    private static final long SAMPLE_INTERVAL_MS = 5000L;
    private static final long STEP_PEAK_GAP_MS = 350L;

    private ActivityDatabase database;
    private MeasurementAdapter adapter;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor motionSensor;
    private Button startButton;
    private Button stopButton;
    private TextView statusText;
    private TextView liveStatsText;
    private TextView sensorStatusText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean measuring;
    private boolean usingStepCounter;
    private long activeMeasurementId = -1L;
    private long startedAt;
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
                updateLiveStats();
                handler.postDelayed(this, SAMPLE_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new ActivityDatabase(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        motionSensor = selectMotionSensor();

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        statusText = findViewById(R.id.statusText);
        liveStatsText = findViewById(R.id.liveStatsText);
        sensorStatusText = findViewById(R.id.sensorStatusText);
        ListView measurementList = findViewById(R.id.measurementList);

        adapter = new MeasurementAdapter(this);
        measurementList.setAdapter(adapter);
        measurementList.setOnItemClickListener((parent, view, position, id) -> openDetail(adapter.getItem(position)));

        startButton.setOnClickListener(view -> startMeasurement());
        stopButton.setOnClickListener(view -> stopMeasurement());

        requestActivityRecognitionIfNeeded();
        updateSensorStatus();
        refreshList();
    }

    @Override
    protected void onDestroy() {
        if (measuring) {
            storeSample();
            database.finishMeasurement(activeMeasurementId, System.currentTimeMillis());
        }
        unregisterSensors();
        handler.removeCallbacks(sampleRunnable);
        super.onDestroy();
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
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
            updateSensorStatus();
        }
    }

    private void startMeasurement() {
        if (measuring) {
            return;
        }
        startedAt = System.currentTimeMillis();
        activeMeasurementId = database.startMeasurement(startedAt);
        if (activeMeasurementId < 0L) {
            statusText.setText("Mereni se nepodarilo ulozit.");
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
        usingStepCounter = canUseStepCounter();
        measuring = true;

        registerSensors();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusText.setText("Mereni bezi. Vzorek se uklada kazdych 5 sekund.");
        updateSensorStatus();
        updateLiveStats();
        handler.post(sampleRunnable);
    }

    private void stopMeasurement() {
        if (!measuring) {
            return;
        }
        storeSample();
        long endedAt = System.currentTimeMillis();
        database.finishMeasurement(activeMeasurementId, endedAt);
        measuring = false;
        activeMeasurementId = -1L;
        unregisterSensors();
        handler.removeCallbacks(sampleRunnable);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusText.setText("Mereni bylo ukonceno a ulozeno.");
        updateLiveStats();
        updateSensorStatus();
        refreshList();
    }

    private void storeSample() {
        if (activeMeasurementId < 0L) {
            return;
        }
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        int deltaSteps = Math.max(0, totalSteps - lastStoredSteps);
        lastStoredSteps = totalSteps;
        double averageIntensity = intensityEvents == 0 ? lastIntensity : intensitySum / intensityEvents;
        database.insertSample(activeMeasurementId, System.currentTimeMillis(), deltaSteps, averageIntensity);
        intensitySum = 0.0;
        intensityEvents = 0;
    }

    private void updateLiveStats() {
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        long duration = measuring ? System.currentTimeMillis() - startedAt : 0L;
        String intensity = ActivityStatsCalculator.formatIntensity(lastIntensity);
        liveStatsText.setText(String.format(
                Locale.US,
                "Doba: %s | kroky: %d | intenzita: %s",
                ActivityStatsCalculator.formatDuration(duration),
                totalSteps,
                intensity
        ));
    }

    private void refreshList() {
        adapter.setRecords(database.listMeasurements());
    }

    private void openDetail(ActivityRecord record) {
        Intent intent = new Intent(this, MeasurementDetailActivity.class);
        intent.putExtra("measurementId", record.id);
        startActivity(intent);
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

    private void requestActivityRecognitionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && stepSensor != null
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY_RECOGNITION);
        }
    }

    private void updateSensorStatus() {
        if (measuring) {
            sensorStatusText.setText(usingStepCounter
                    ? "Pouziva se krokomer a pohybovy senzor."
                    : "Pouziva se odhad kroku z pohyboveho senzoru.");
            return;
        }
        if (canUseStepCounter()) {
            sensorStatusText.setText("K dispozici je krokomer, intenzita se meri z pohybu.");
        } else if (motionSensor != null) {
            sensorStatusText.setText("Krokomer neni dostupny, aplikace odhaduje kroky z pohybu.");
        } else {
            sensorStatusText.setText("Zarizeni nema dostupny pohybovy senzor.");
        }
    }
}
