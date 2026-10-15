package com.gkcontas.queryperf.measure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatementRecorderTest {

    private final StatementRecorder recorder = new StatementRecorder();

    @Test
    void returnsTheStatementUnchanged() {
        assertThat(recorder.inspect("select 1")).isEqualTo("select 1");
    }

    @Test
    void recordsInOrderBetweenStartAndStop() {
        StatementRecorder.start();
        recorder.inspect("select 1");
        recorder.inspect("select 2");
        assertThat(StatementRecorder.stop()).containsExactly("select 1", "select 2");
    }

    /** Nothing is recorded outside a window, so a stray query cannot pollute a report. */
    @Test
    void recordsNothingWhenNotStarted() {
        recorder.inspect("select 1");
        assertThat(StatementRecorder.stop()).isEmpty();
    }

    @Test
    void startDiscardsWhateverWasLeftOver() {
        StatementRecorder.start();
        recorder.inspect("select 1");
        StatementRecorder.start();
        recorder.inspect("select 2");
        assertThat(StatementRecorder.stop()).containsExactly("select 2");
    }

    /** One thread's statements never reach another's report. */
    @Test
    void isIsolatedPerThread() throws Exception {
        StatementRecorder.start();
        recorder.inspect("mine");
        Thread other = new Thread(() -> {
            StatementRecorder.start();
            recorder.inspect("theirs");
            assertThat(StatementRecorder.stop()).containsExactly("theirs");
        });
        other.start();
        other.join();
        assertThat(StatementRecorder.stop()).containsExactly("mine");
    }
}
