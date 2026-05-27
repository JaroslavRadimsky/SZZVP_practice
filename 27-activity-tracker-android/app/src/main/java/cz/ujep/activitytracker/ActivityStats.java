package cz.ujep.activitytracker;

/**
 * Výsledek výpočtu statistik nad sadou vzorků.
 */
public class ActivityStats {
    public final long durationMillis;
    public final int totalSteps;
    public final double distanceMeters;
    public final double averageIntensity;
    public final int sampleCount;

    /**
     * Uchová vypočtenou délku, kroky, vzdálenost, intenzitu a počet vzorků.
     */
    public ActivityStats(long durationMillis, int totalSteps, double distanceMeters, double averageIntensity, int sampleCount) {
        this.durationMillis = durationMillis;
        this.totalSteps = totalSteps;
        this.distanceMeters = distanceMeters;
        this.averageIntensity = averageIntensity;
        this.sampleCount = sampleCount;
    }
}
