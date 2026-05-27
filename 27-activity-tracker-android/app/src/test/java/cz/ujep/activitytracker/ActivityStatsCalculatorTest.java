package cz.ujep.activitytracker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ActivityStatsCalculatorTest {
    @Test
    public void summarizesSamples() {
        List<ActivitySample> samples = Arrays.asList(
                new ActivitySample(1L, 10L, 1000L, 3, 1.0),
                new ActivitySample(2L, 10L, 2000L, 4, 2.0),
                new ActivitySample(3L, 10L, 3000L, 5, 3.0)
        );

        ActivityStats stats = ActivityStatsCalculator.summarize(1000L, 11000L, samples);

        assertEquals(10000L, stats.durationMillis);
        assertEquals(12, stats.totalSteps);
        assertEquals(2.0, stats.averageIntensity, 0.001);
        assertEquals(3, stats.sampleCount);
    }

    @Test
    public void formatsDurationAsClock() {
        assertEquals("01:02:03", ActivityStatsCalculator.formatDuration(3723000L));
    }

    @Test
    public void labelsIntensity() {
        assertEquals("Klidova", ActivityStatsCalculator.intensityLabel(0.2));
        assertEquals("Lehka", ActivityStatsCalculator.intensityLabel(1.2));
        assertEquals("Stredni", ActivityStatsCalculator.intensityLabel(2.4));
        assertEquals("Vysoka", ActivityStatsCalculator.intensityLabel(5.0));
    }
}
