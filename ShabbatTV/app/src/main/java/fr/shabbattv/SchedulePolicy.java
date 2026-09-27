package fr.shabbattv;

/** Pure scheduling rules. No Android alarms, networking or device-power operations. */
public final class SchedulePolicy {
    public static final long COUNTDOWN_MS = 10 * 60_000L;
    public static final long MAX_LATENESS_MS = 60_000L;
    public enum Decision { WAIT, START, MISSED }
    private SchedulePolicy() {}

    public static Decision decide(long now, long when) {
        if (when <= 0L) return Decision.MISSED;
        if (when > now) return Decision.WAIT;
        return now - when > MAX_LATENESS_MS ? Decision.MISSED : Decision.START;
    }

    public static boolean overlaps(long start, long duration, long otherStart, long otherDuration) {
        if (start == otherStart) return true;
        // Unknown durations cannot be validated in advance; the UI warns about this.
        if (duration <= 0L || otherDuration <= 0L) return false;
        return start < end(otherStart, otherDuration) && otherStart < end(start, duration);
    }

    private static long end(long start, long duration) {
        return start > Long.MAX_VALUE - duration ? Long.MAX_VALUE : start + duration;
    }
}
