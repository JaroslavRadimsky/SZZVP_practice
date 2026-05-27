package cz.ujep.activitytracker;

/**
 * Souhrnný záznam jednoho měření aktivity uložený v tabulce measurements.
 */
public class ActivityRecord {
    public final long id;
    public final long startedAt;
    public final long endedAt;
    public final int totalSteps;
    public final double distanceMeters;
    public final double averageIntensity;
    public final int sampleCount;

    /**
     * Vytvoří neměnný objekt se souhrnem měření načteným z databáze.
     */
    public ActivityRecord(long id, long startedAt, long endedAt, int totalSteps, double distanceMeters, double averageIntensity, int sampleCount) {
        this.id = id;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.totalSteps = totalSteps;
        this.distanceMeters = distanceMeters;
        this.averageIntensity = averageIntensity;
        this.sampleCount = sampleCount;
    }

    /**
     * Vrací true, pokud už má měření uložený čas ukončení.
     */
    public boolean isFinished() {
        return endedAt > 0;
    }

    /**
     * Vrací čas konce pro výpis; u probíhajícího měření použije aktuální čas.
     */
    public long getDisplayEndTime() {
        return isFinished() ? endedAt : System.currentTimeMillis();
    }
}
