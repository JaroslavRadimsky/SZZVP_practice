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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Celoplošná obrazovka probíhajícího měření aktivity.
 */
public class TrackingActivity extends Activity implements SensorEventListener, LocationListener {
    private static final int REQUEST_TRACKING_PERMISSIONS = 10;
    private static final int NOTIFICATION_ID = 2701;
    private static final int INACTIVITY_NOTIFICATION_ID = 2702;
    private static final String CHANNEL_ID = "active_measurement";
    private static final String INACTIVITY_CHANNEL_ID = "activity_inactivity";
    private static final long SAMPLE_INTERVAL_MS = 5000L;
    private static final long TIMER_INTERVAL_MS = 1000L;
    private static final long STEP_PEAK_GAP_MS = 350L;
    private static final long INACTIVITY_THRESHOLD_MS = 30000L;
    private static final long LOCATION_INTERVAL_MS = 2000L;
    private static final float LOCATION_MIN_DISTANCE_M = 1f;
    private static final float MAX_ACCEPTED_ACCURACY_M = 50f;
    private static final float MAX_ACCEPTED_JUMP_M = 500f;
    private static final double MOVEMENT_INTENSITY_THRESHOLD = 1.2;

    private ActivityDatabase database;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private NotificationManager notificationManager;
    private Sensor stepSensor;
    private Sensor motionSensor;
    private TextView sensorStatusText;
    private TextView clockText;
    private TextView stepsText;
    private TextView distanceText;
    private TextView intensityText;
    private TextView speedText;
    private TextView paceText;
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
    private boolean locationTrackingEnabled;
    private Location lastLocation;
    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;
    private double distanceMetersTotal;
    private double distanceMetersSinceLastSample;
    private double lastSampleSpeedKmh;
    private double lastSamplePaceSecondsPerKm;
    private long lastMovementAt;
    private boolean inactivityNotificationShown;

    /**
     * Pravidelně ukládá vzorek aktivity do databáze.
     */
    private final Runnable sampleRunnable = new Runnable() {
        /**
         * Uloží vzorek a naplánuje další spuštění po pěti sekundách.
         */
        @Override
        public void run() {
            if (measuring) {
                storeSample();
                handler.postDelayed(this, SAMPLE_INTERVAL_MS);
            }
        }
    };

    /**
     * Každou sekundu aktualizuje časovač, notifikaci a hlídání neaktivity.
     */
    private final Runnable timerRunnable = new Runnable() {
        /**
         * Obnoví živé hodnoty na obrazovce a znovu naplánuje časovač.
         */
        @Override
        public void run() {
            if (measuring) {
                updateLiveStats();
                showOrUpdateNotification();
                checkInactivityNotification();
                handler.postDelayed(this, TIMER_INTERVAL_MS);
            }
        }
    };

    /**
     * Připraví UI, senzory, GPS a po získání oprávnění spustí měření.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_tracking);

        database = new ActivityDatabase(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        motionSensor = selectMotionSensor();

        sensorStatusText = findViewById(R.id.sensorStatusText);
        clockText = findViewById(R.id.clockText);
        stepsText = findViewById(R.id.stepsText);
        distanceText = findViewById(R.id.distanceText);
        intensityText = findViewById(R.id.intensityText);
        speedText = findViewById(R.id.speedText);
        paceText = findViewById(R.id.paceText);
        sampleStatusText = findViewById(R.id.sampleStatusText);
        stopButton = findViewById(R.id.stopButton);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(view -> stopMeasurementAndFinish());

        requestTrackingPermissionsIfNeededOrStart();
    }

    /**
     * Při zavření aktivity bezpečně ukončí běžící měření.
     */
    @Override
    protected void onDestroy() {
        if (measuring) {
            finishMeasurement();
        }
        super.onDestroy();
    }

    /**
     * Při tlačítku zpět nabídne potvrzení, aby uživatel měření neukončil omylem.
     */
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

    /**
     * Zpracuje data ze step counteru nebo akcelerometru během měření.
     */
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
            int previousStepTotal = currentStepTotal;
            currentStepTotal = Math.max(0, rawSteps - stepBase);
            if (currentStepTotal > previousStepTotal) {
                markMovement();
            }
            return;
        }

        double intensity = calculateMotionIntensity(event);
        lastIntensity = intensity;
        intensitySum += intensity;
        intensityEvents++;
        if (intensity >= MOVEMENT_INTENSITY_THRESHOLD) {
            markMovement();
        }

        if (!usingStepCounter && intensity > 2.2) {
            long now = System.currentTimeMillis();
            if (now - lastPeakAt > STEP_PEAK_GAP_MS) {
                estimatedSteps++;
                lastPeakAt = now;
            }
        }
    }

    /**
     * Povinný callback senzoru; aplikace přesnost senzoru samostatně neřeší.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Po odpovědi na žádost o oprávnění pokračuje spuštěním měření.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_TRACKING_PERMISSIONS && !measuring) {
            startMeasurement();
        }
    }

    /**
     * Zpracuje novou GPS nebo síťovou polohu a přičte realistický posun.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (!measuring || !isUsableLocation(location)) {
            return;
        }

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            if (distance >= LOCATION_MIN_DISTANCE_M && distance <= MAX_ACCEPTED_JUMP_M) {
                distanceMetersTotal += distance;
                distanceMetersSinceLastSample += distance;
                markMovement();
            }
        }
        lastLocation = new Location(location);
        updateLiveStats();
    }

    /**
     * Po zapnutí poskytovatele polohy znovu zaregistruje GPS odběr.
     */
    @Override
    public void onProviderEnabled(String provider) {
        locationTrackingEnabled = canUseLocation();
        registerLocationUpdates();
        updateSensorStatus();
    }

    /**
     * Po vypnutí poskytovatele polohy aktualizuje stav GPS v UI.
     */
    @Override
    public void onProviderDisabled(String provider) {
        locationTrackingEnabled = canUseLocation();
        updateSensorStatus();
    }

    /**
     * Starší callback polohy; pro tuto aplikaci není potřeba žádná akce.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Vyžádá potřebná runtime oprávnění nebo rovnou spustí měření.
     */
    private void requestTrackingPermissionsIfNeededOrStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startMeasurement();
            return;
        }

        List<String> missingPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && stepSensor != null
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!missingPermissions.isEmpty()) {
            sensorStatusText.setText("Cekam na opravneni pro senzory a GPS.");
            requestPermissions(missingPermissions.toArray(new String[0]), REQUEST_TRACKING_PERMISSIONS);
            return;
        }
        startMeasurement();
    }

    /**
     * Založí nové měření, vynuluje živé hodnoty a spustí senzory, GPS a časovače.
     */
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
        lastLocation = null;
        currentLatitude = Double.NaN;
        currentLongitude = Double.NaN;
        distanceMetersTotal = 0.0;
        distanceMetersSinceLastSample = 0.0;
        lastSampleSpeedKmh = 0.0;
        lastSamplePaceSecondsPerKm = 0.0;
        lastMovementAt = startedAt;
        inactivityNotificationShown = false;
        usingStepCounter = canUseStepCounter();
        locationTrackingEnabled = canUseLocation();
        measuring = true;

        createNotificationChannelIfNeeded();
        registerSensors();
        registerLocationUpdates();
        stopButton.setEnabled(true);
        updateSensorStatus();
        updateLiveStats();
        showOrUpdateNotification();
        handler.postDelayed(sampleRunnable, SAMPLE_INTERVAL_MS);
        handler.postDelayed(timerRunnable, TIMER_INTERVAL_MS);
    }

    /**
     * Ukončí aktivitu z UI a zavře měřicí obrazovku.
     */
    private void stopMeasurementAndFinish() {
        if (measuring) {
            finishMeasurement();
        }
        finish();
    }

    /**
     * Uloží poslední vzorek, označí měření jako ukončené a vypne zdroje dat.
     */
    private void finishMeasurement() {
        if (activeMeasurementId >= 0L) {
            storeSample();
            database.finishMeasurement(activeMeasurementId, System.currentTimeMillis());
        }
        measuring = false;
        activeMeasurementId = -1L;
        unregisterSensors();
        unregisterLocationUpdates();
        handler.removeCallbacks(sampleRunnable);
        handler.removeCallbacks(timerRunnable);
        cancelNotification();
        cancelInactivityNotification();
    }

    /**
     * Uloží jeden pětisekundový vzorek včetně kroků, intenzity, GPS, rychlosti a tempa.
     */
    private void storeSample() {
        if (activeMeasurementId < 0L) {
            return;
        }
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        int deltaSteps = Math.max(0, totalSteps - lastStoredSteps);
        lastStoredSteps = totalSteps;
        double averageIntensity = intensityEvents == 0 ? lastIntensity : intensitySum / intensityEvents;
        double sampleDistance = distanceMetersSinceLastSample;
        distanceMetersSinceLastSample = 0.0;
        long now = System.currentTimeMillis();
        long previousSampleAt = lastSampleAt == 0L ? startedAt : lastSampleAt;
        long sampleDuration = Math.max(0L, now - previousSampleAt);
        lastSampleSpeedKmh = ActivityStatsCalculator.calculateSpeedKmh(sampleDistance, sampleDuration);
        lastSamplePaceSecondsPerKm = ActivityStatsCalculator.calculatePaceSecondsPerKm(sampleDistance, sampleDuration);
        database.insertSample(
                activeMeasurementId,
                now,
                deltaSteps,
                averageIntensity,
                currentLatitude,
                currentLongitude,
                sampleDistance,
                lastSampleSpeedKmh,
                lastSamplePaceSecondsPerKm
        );
        lastSampleAt = now;
        sampleStatusText.setText("Posledni vzorek ulozen v " + android.text.format.DateFormat.format("HH:mm:ss", now));
        intensitySum = 0.0;
        intensityEvents = 0;
    }

    /**
     * Přepíše živé hodnoty na obrazovce měření.
     */
    private void updateLiveStats() {
        int totalSteps = usingStepCounter ? currentStepTotal : estimatedSteps;
        long duration = System.currentTimeMillis() - startedAt;
        clockText.setText(ActivityStatsCalculator.formatDuration(duration));
        stepsText.setText(String.format(Locale.US, "Kroky: %d", totalSteps));
        distanceText.setText("Vzdalenost: " + ActivityStatsCalculator.formatDistance(distanceMetersTotal));
        intensityText.setText("Intenzita: " + ActivityStatsCalculator.formatIntensity(lastIntensity));
        speedText.setText("Rychlost: " + ActivityStatsCalculator.formatSpeed(lastSampleSpeedKmh));
        paceText.setText("Tempo: " + ActivityStatsCalculator.formatPace(lastSamplePaceSecondsPerKm));
        if (lastSampleAt == 0L) {
            long untilFirstSample = Math.max(0L, SAMPLE_INTERVAL_MS - duration);
            sampleStatusText.setText("Prvni vzorek za " + Math.max(1L, untilFirstSample / 1000L) + " s");
        }
    }

    /**
     * Zaregistruje posluchače krokoměru a pohybového senzoru.
     */
    private void registerSensors() {
        if (usingStepCounter) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (motionSensor != null) {
            sensorManager.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Odregistruje všechny senzory používané aktivitou.
     */
    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    /**
     * Zaregistruje GPS a síťové aktualizace polohy, pokud jsou dostupné.
     */
    private void registerLocationUpdates() {
        if (!locationTrackingEnabled) {
            return;
        }
        try {
            locationManager.removeUpdates(this);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_INTERVAL_MS,
                        LOCATION_MIN_DISTANCE_M,
                        this
                );
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        LOCATION_INTERVAL_MS,
                        LOCATION_MIN_DISTANCE_M,
                        this
                );
            }
        } catch (SecurityException ignored) {
            locationTrackingEnabled = false;
        }
    }

    /**
     * Odregistruje příjem aktualizací polohy.
     */
    private void unregisterLocationUpdates() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException ignored) {
        }
    }

    /**
     * Vybere nejlepší dostupný pohybový senzor pro intenzitu.
     */
    private Sensor selectMotionSensor() {
        Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (linear != null) {
            return linear;
        }
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Spočítá orientační intenzitu pohybu z hodnot senzoru.
     */
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

    /**
     * Ověří, zda lze použít hardwarový krokoměr.
     */
    private boolean canUseStepCounter() {
        if (stepSensor == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Ověří, zda má aplikace oprávnění a dostupný provider pro polohu.
     */
    private boolean canUseLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return locationManager != null
                && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * Zobrazí textový stav používaných senzorů a GPS na obrazovce.
     */
    private void updateSensorStatus() {
        String gpsText = locationTrackingEnabled
                ? " GPS vzdalenost je aktivni."
                : " GPS vzdalenost neni dostupna.";
        if (usingStepCounter) {
            sensorStatusText.setText("Pouziva se krokomer a pohybovy senzor." + gpsText);
        } else if (motionSensor != null) {
            sensorStatusText.setText("Pouziva se odhad kroku z pohyboveho senzoru." + gpsText);
        } else {
            sensorStatusText.setText("Zarizeni nema dostupny pohybovy senzor." + gpsText);
        }
    }

    /**
     * Filtruje nepřesné polohy, aby nevznikaly nesmyslné skoky vzdálenosti.
     */
    private boolean isUsableLocation(Location location) {
        if (location == null) {
            return false;
        }
        return !location.hasAccuracy() || location.getAccuracy() <= MAX_ACCEPTED_ACCURACY_M;
    }

    /**
     * Vytvoří notification channels pro průběžné měření a upozornění na neaktivitu.
     */
    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Probihajici aktivita",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Informace o prave merene fyzicke aktivite.");
            notificationManager.createNotificationChannel(channel);

            NotificationChannel inactivityChannel = new NotificationChannel(
                    INACTIVITY_CHANNEL_ID,
                    "Upozorneni na neaktivitu",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            inactivityChannel.setDescription("Upozorneni pri delsi neaktivite behem mereni.");
            notificationManager.createNotificationChannel(inactivityChannel);
        }
    }

    /**
     * Vytvoří nebo aktualizuje trvalou notifikaci probíhající aktivity.
     */
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
                "Cas %s | kroky %d | %s",
                ActivityStatsCalculator.formatDuration(System.currentTimeMillis() - startedAt),
                totalSteps,
                ActivityStatsCalculator.formatDistance(distanceMetersTotal)
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

    /**
     * Zkontroluje, zda uživatel není déle než 30 sekund bez pohybu.
     */
    private void checkInactivityNotification() {
        if (!measuring || inactivityNotificationShown) {
            return;
        }
        long inactiveFor = System.currentTimeMillis() - lastMovementAt;
        if (inactiveFor >= INACTIVITY_THRESHOLD_MS) {
            showInactivityNotification();
        }
    }

    /**
     * Zobrazí upozornění, že se uživatel dlouho nehýbe.
     */
    private void showInactivityNotification() {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, INACTIVITY_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_DEFAULT);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_activity_notification)
                .setContentTitle("Dlouho se nehýbeš")
                .setContentText("Nechceš aktivitu ukončit?")
                .setStyle(new Notification.BigTextStyle().bigText("Dlouho se nehýbeš. Nechceš aktivitu ukončit?"))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        inactivityNotificationShown = true;
        notificationManager.notify(INACTIVITY_NOTIFICATION_ID, notification);
    }

    /**
     * Označí, že byl detekován pohyb, a zruší případné upozornění neaktivity.
     */
    private void markMovement() {
        lastMovementAt = System.currentTimeMillis();
        if (inactivityNotificationShown) {
            cancelInactivityNotification();
        }
    }

    /**
     * Zruší trvalou notifikaci probíhající aktivity.
     */
    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Zruší upozornění na neaktivitu a dovolí jeho pozdější opětovné zobrazení.
     */
    private void cancelInactivityNotification() {
        inactivityNotificationShown = false;
        notificationManager.cancel(INACTIVITY_NOTIFICATION_ID);
    }
}
