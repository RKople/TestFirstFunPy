package fr.shabbattv;

import org.junit.Test;
import static org.junit.Assert.*;
import static fr.shabbattv.SchedulePolicy.Decision.*;

public class SchedulePolicyTest {
    @Test public void neverStartsOneMillisecondEarly() { assertEquals(WAIT, SchedulePolicy.decide(999L, 1000L)); }
    @Test public void startsAtExactDeadline() { assertEquals(START, SchedulePolicy.decide(1000L, 1000L)); }
    @Test public void startsAfterNormalTickDelay() { assertEquals(START, SchedulePolicy.decide(1500L, 1000L)); }
    @Test public void permitsMaximumSmallDelay() { assertEquals(START, SchedulePolicy.decide(61000L, 1000L)); }
    @Test public void ignoresOlderMissedSession() { assertEquals(MISSED, SchedulePolicy.decide(61001L, 1000L)); }
    @Test public void doesNotReplayHoursLate() { assertEquals(MISSED, SchedulePolicy.decide(21_601_000L, 1000L)); }
    @Test public void rejectsMissingDeadline() { assertEquals(MISSED, SchedulePolicy.decide(1000L, 0L)); }
    @Test public void rejectsNegativeDeadline() { assertEquals(MISSED, SchedulePolicy.decide(1000L, -1L)); }
    @Test public void remainsWaitingAfterBackwardClockChange() { assertEquals(WAIT, SchedulePolicy.decide(1000L, 3_601_000L)); }
    @Test public void countdownWindowIsTenMinutes() { assertEquals(600_000L, SchedulePolicy.COUNTDOWN_MS); }
    @Test public void sameTimeAlwaysConflicts() { assertTrue(SchedulePolicy.overlaps(1000, 0, 1000, 0)); }
    @Test public void adjacentFilmsDoNotOverlap() { assertFalse(SchedulePolicy.overlaps(1000, 500, 1500, 500)); }
    @Test public void partialOverlapIsRejected() { assertTrue(SchedulePolicy.overlaps(1000, 1000, 1500, 1000)); }
    @Test public void containedFilmIsRejected() { assertTrue(SchedulePolicy.overlaps(1000, 5000, 1500, 100)); }
    @Test public void overlapIsSymmetric() { assertTrue(SchedulePolicy.overlaps(1500, 100, 1000, 5000)); }
    @Test public void unknownDurationIsNotInvented() { assertFalse(SchedulePolicy.overlaps(1000, 0, 1500, 100)); }
    @Test public void supportsCrossMidnightScheduling() {
        long late = 86_340_000L, tomorrow = 86_460_000L;
        assertEquals(WAIT, SchedulePolicy.decide(late, tomorrow));
        assertEquals(START, SchedulePolicy.decide(tomorrow, tomorrow));
        assertTrue(SchedulePolicy.overlaps(late, 300_000L, tomorrow, 60_000L));
    }
    @Test public void endTimeDoesNotOverflow() { assertTrue(SchedulePolicy.overlaps(Long.MAX_VALUE - 100, 1000, Long.MAX_VALUE - 10, 5)); }
}
