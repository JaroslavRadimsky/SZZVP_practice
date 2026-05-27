package cz.ujep.activitytracker;

import java.util.List;
import java.util.Locale;

/**
 * Pomocná třída pro výpočty a formátování statistik aktivity.
 */
public class ActivityStatsCalculator {
    /**
     * Zabraňuje vytvoření instance, protože třída obsahuje jen statické metody.
     */
    private ActivityStatsCalculator() {
    }

    /**
     * Spočítá souhrn kroků, vzdálenosti, průměrné intenzity a délky aktivity.
     */
    public static ActivityStats summarize(long startedAt, long endedAt, List<ActivitySample> samples) {
        int totalSteps = 0;
        double distanceMeters = 0.0;
        double intensitySum = 0.0;
        for (ActivitySample sample : samples) {
            totalSteps += sample.steps;
            distanceMeters += sample.distanceMeters;
            intensitySum += sample.intensity;
        }
        int count = samples.size();
        double averageIntensity = count == 0 ? 0.0 : intensitySum / count;
        long duration = Math.max(0L, endedAt - startedAt);
        return new ActivityStats(duration, totalSteps, distanceMeters, averageIntensity, count);
    }

    /**
     * Převede délku v milisekundách na čitelný formát HH:mm:ss.
     */
    public static String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(0L, durationMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Převede číselnou intenzitu na slovní úroveň pro UI.
     */
    public static String intensityLabel(double intensity) {
        if (intensity < 0.8) {
            return "Klidova";
        }
        if (intensity < 2.0) {
            return "Lehka";
        }
        if (intensity < 4.0) {
            return "Stredni";
        }
        return "Vysoka";
    }

    /**
     * Vrátí slovní i číselný zápis intenzity pohybu.
     */
    public static String formatIntensity(double intensity) {
        return String.format(Locale.US, "%s (%.1f)", intensityLabel(intensity), intensity);
    }

    /**
     * Formátuje vzdálenost v metrech; od 1000 m ji vypíše v kilometrech.
     */
    public static String formatDistance(double meters) {
        if (meters >= 1000.0) {
            return String.format(Locale.US, "%.2f km", meters / 1000.0);
        }
        return String.format(Locale.US, "%.0f m", Math.max(0.0, meters));
    }

    /**
     * Spočítá rychlost v km/h z uražené vzdálenosti a délky intervalu.
     */
    public static double calculateSpeedKmh(double distanceMeters, long durationMillis) {
        if (distanceMeters <= 0.0 || durationMillis <= 0L) {
            return 0.0;
        }
        double hours = durationMillis / 3600000.0;
        return (distanceMeters / 1000.0) / hours;
    }

    /**
     * Spočítá tempo v sekundách na kilometr z uražené vzdálenosti a délky intervalu.
     */
    public static double calculatePaceSecondsPerKm(double distanceMeters, long durationMillis) {
        if (distanceMeters <= 0.0 || durationMillis <= 0L) {
            return 0.0;
        }
        return (durationMillis / 1000.0) / (distanceMeters / 1000.0);
    }

    /**
     * Formátuje rychlost jako hodnotu v kilometrech za hodinu.
     */
    public static String formatSpeed(double speedKmh) {
        return String.format(Locale.US, "%.1f km/h", Math.max(0.0, speedKmh));
    }

    /**
     * Formátuje tempo jako minuty:sekundy na kilometr, případně --/km bez pohybu.
     */
    public static String formatPace(double secondsPerKm) {
        if (secondsPerKm <= 0.0) {
            return "--/km";
        }
        long roundedSeconds = Math.round(secondsPerKm);
        long minutes = roundedSeconds / 60L;
        long seconds = roundedSeconds % 60L;
        return String.format(Locale.US, "%d:%02d/km", minutes, seconds);
    }
}
