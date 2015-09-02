package com.self;

/**
 * trace 属性名称定义
 * Created by shaojieyue on 8/31/15.
 */
public class ZipkinTraceConstants {
    /**The overall ID of the trace. Every span in a trace will share this ID.*/
    public static final String TRACE_ID="traceId";

    /**The ID for a particular span. This may or may not be the same as the trace id.*/
    public static final String SPAN_ID="spanId";

    /**This is an optional ID that will only be present on child spans.
     * That is the span without a parent id is considered the root of the trace.*/
    public static final String PARENT_ID="parentId";


    public static final String IS_SAMPLED="isSampled";

    /** provide the ability to create and communicate feature flags.
     * This is how we can tell downstream services that this is a “debug” request.*/
    public static final String FLAGS="Flags";
}
