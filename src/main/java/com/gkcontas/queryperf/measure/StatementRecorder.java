package com.gkcontas.queryperf.measure;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Every statement Hibernate is about to send passes through {@link #inspect(String)}.
 *
 * <p>Hibernate's own {@code Statistics} answers <em>how many</em> statements a unit of
 * work provoked, which is the number this project reports, but it says nothing about
 * what they were. A report that only claims "11 statements instead of 2001" is a claim
 * the reader has to take on faith; recording the SQL turns it into something they can
 * read, and it is also how the projection scenario counts the columns a variant
 * actually asked the database for.
 *
 * <p>Hibernate instantiates this class itself from the class name in
 * {@code application.yml}, so it cannot be a Spring bean and cannot hold collaborators.
 * The buffer is a thread local: recording is started and stopped around a unit of work
 * on the thread that runs it, and two concurrent requests never see each other's SQL.
 */
public final class StatementRecorder implements StatementInspector {

    private static final ThreadLocal<List<String>> BUFFER = new ThreadLocal<>();

    /** Begins recording on the calling thread, discarding anything left over. */
    public static void start() {
        BUFFER.set(new ArrayList<>());
    }

    /** Ends recording and hands back what was captured, in order. */
    public static List<String> stop() {
        List<String> captured = BUFFER.get();
        BUFFER.remove();
        return captured == null ? List.of() : List.copyOf(captured);
    }

    @Override
    public String inspect(String sql) {
        List<String> buffer = BUFFER.get();
        if (buffer != null) {
            buffer.add(sql);
        }
        return sql;
    }
}
