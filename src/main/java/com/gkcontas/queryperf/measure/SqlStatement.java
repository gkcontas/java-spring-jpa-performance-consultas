package com.gkcontas.queryperf.measure;

/**
 * One distinct statement a variant provoked, with how many times it was sent.
 *
 * <p>An N+1 does not send a thousand different statements, it sends the same one a
 * thousand times, so collapsing by text is what makes the record readable: two lines
 * instead of 2001.
 *
 * @param sql the statement as Hibernate rendered it, parameters still as placeholders
 * @param times how often it was sent inside the unit of work
 * @param selectedColumns width of the select list, or 0 when the statement is not a
 *     select. Counted by splitting the list on top-level commas, which is exact for
 *     the SQL Hibernate generates because it never nests a comma outside parentheses
 *     in a projection.
 */
public record SqlStatement(String sql, int times, int selectedColumns) {}
