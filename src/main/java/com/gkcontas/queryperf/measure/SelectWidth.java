package com.gkcontas.queryperf.measure;

/**
 * How many columns a select asked for.
 *
 * <p>This is the number that settles the projection argument. "Select only what you
 * need" is advice everybody repeats and nobody checks, and an open interface projection
 * - the one with a SpEL {@code @Value} in it - quietly selects every column of the
 * entity while looking exactly like the closed one that selects two.
 */
final class SelectWidth {

    private SelectWidth() {}

    static int of(String sql) {
        String normalised = sql.trim().toLowerCase();
        if (!normalised.startsWith("select")) {
            return 0;
        }
        int from = topLevelFrom(normalised);
        if (from < 0) {
            return 0;
        }
        String list = normalised.substring("select".length(), from);
        int depth = 0;
        int columns = 1;
        for (char c : list.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                columns++;
            }
        }
        return list.isBlank() ? 0 : columns;
    }

    /** The first {@code from} that is not inside a function call or a subquery. */
    private static int topLevelFrom(String sql) {
        int depth = 0;
        for (int i = 0; i < sql.length() - 5; i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0
                    && sql.startsWith("from", i)
                    && i > 0
                    && Character.isWhitespace(sql.charAt(i - 1))
                    && Character.isWhitespace(sql.charAt(i + 4))) {
                return i;
            }
        }
        return -1;
    }
}
