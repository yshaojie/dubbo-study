//package com.self;
//
//import com.github.kristofa.brave.ClientTracer;
//import com.github.kristofa.brave.SpanId;
//import com.twitter.zipkin.gen.Span;
//
///**
// * Created by shaojieyue on 8/31/15.
// */
//public class DubboClientTracer implements ClientTracer {
//    private ServerAndClientSpanState state;
//    public SpanId startNewSpan(String requestName) {
//        final Boolean sample = state.sample();
//        if (Boolean.FALSE.equals(sample)) {
//            state.setCurrentClientSpan(null);
//            state.setCurrentClientServiceName(null);
//            return null;
//        }
//
//        if (sample == null) {
//            // No sample indication is present.
//            for (final TraceFilter traceFilter : traceFilters) {
//                if (!traceFilter.trace(requestName)) {
//                    state.setCurrentClientSpan(null);
//                    state.setCurrentClientServiceName(null);
//                    return null;
//                }
//            }
//        }
//
//        final SpanId newSpanId = getNewSpanId();
//        final Span newSpan = new Span();
//        newSpan.setId(newSpanId.getSpanId());
//        newSpan.setTrace_id(newSpanId.getTraceId());
//        if (newSpanId.getParentSpanId() != null) {
//            newSpan.setParent_id(newSpanId.getParentSpanId());
//        }
//        newSpan.setName(requestName);
//        state.setCurrentClientSpan(newSpan);
//        return newSpanId;
//    }
//
//    public void setCurrentClientServiceName(String serviceName) {
//
//    }
//
//    public void setClientSent() {
//
//    }
//
//    public void setClientReceived() {
//
//    }
//
//    public void submitAnnotation(String annotationName, long startTime, long endTime) {
//
//    }
//
//    public void submitAnnotation(String annotationName) {
//
//    }
//
//    public void submitBinaryAnnotation(String key, String value) {
//
//    }
//
//    public void submitBinaryAnnotation(String key, int value) {
//
//    }
//}
