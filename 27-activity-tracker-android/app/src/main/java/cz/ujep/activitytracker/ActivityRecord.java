package cz.ujep.activitytracker;

public class ActivityRecord {
    public final long id;
    public final long startedAt;
    public final long endedAt;
    public final int totalSteps;
    public final double distanceMeters;
    public final double averageIntensity;
    public final int sampleCount;

    public ActivityRecord(long id, long startedAt, long endedAt, int totalSteps, double distanceMeters, double averageIntensity, int sampleCount) {
        this.id = id;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.totalSteps = totalSteps;
        this.distanceMeters = distanceMeters;
        this.averageIntensity = averageIntensity;
        this.sampleCount = sampleCount;
    }

    public boolean isFinished() {
        return endedAt > 0;
    }

    public long getDisplayEndTime() {
        return isFinished() ? endedAt : System.currentTimeMillis();
    }
}
