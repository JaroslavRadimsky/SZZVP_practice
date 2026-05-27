package cz.ujep.activitytracker;

public class ActivitySample {
    public final long id;
    public final long measurementId;
    public final long measuredAt;
    public final int steps;
    public final double intensity;

    public ActivitySample(long id, long measurementId, long measuredAt, int steps, double intensity) {
        this.id = id;
        this.measurementId = measurementId;
        this.measuredAt = measuredAt;
        this.steps = steps;
        this.intensity = intensity;
    }
}
