package cz.ujep.activitytracker;

public class ActivityStats {
    public final long durationMillis;
    public final int totalSteps;
    public final double averageIntensity;
    public final int sampleCount;

    public ActivityStats(long durationMillis, int totalSteps, double averageIntensity, int sampleCount) {
        this.durationMillis = durationMillis;
        this.totalSteps = totalSteps;
        this.averageIntensity = averageIntensity;
        this.sampleCount = sampleCount;
    }
}
