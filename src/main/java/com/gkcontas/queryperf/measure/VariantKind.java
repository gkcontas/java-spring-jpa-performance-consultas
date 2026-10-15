package com.gkcontas.queryperf.measure;

public enum VariantKind {
    /** The query plan as it comes out of the obvious code. */
    PROBLEM,
    /** A repair that answers the same question for less. */
    FIX,
    /** A repair that looks like one and is not, or is worse than doing nothing. */
    TRAP
}
