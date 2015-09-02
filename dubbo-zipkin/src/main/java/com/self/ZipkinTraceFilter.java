package com.self;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.github.kristofa.brave.*;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollectorParams;

import java.util.Collections;
import java.util.Map;

/**
 *
 * ContextFilter(order=-10000) ZipkinTraceFilter要排在ContextFilter之前
 * Created by shaojieyue on 8/27/15.
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER},order=-5000)
public class ZipkinTraceFilter implements Filter {
    //采集trace
    public static final String SAMPLED = "0";
    //不采集trace
    public static final String NO_SAMPLED = "1";
    private ZipkinSpanCollector collector;
    private final ServerTracer serverTracer;
    private final ClientTracer clientTracer;


    public ZipkinTraceFilter() {
        ZipkinSpanCollectorParams params = new ZipkinSpanCollectorParams();
        params.setBatchSize(5);
        params.setFailOnSetup(true);
        params.setNrOfThreads(3);
        params.setQueueSize(1000);
        params.setSocketTimeout(2000);
        collector = new ZipkinSpanCollector("10.10.20.105",9410,params);
        serverTracer = Brave.getServerTracer(collector, Collections.EMPTY_LIST);
        clientTracer = Brave.getClientTracer(collector, Collections.EMPTY_LIST);
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        final String serviceName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);

        //注意：RpcContext是一个临时状态记录器，当接收到RPC请求，或发起RPC请求时，RpcContext的状态都会变化。
        // 比如：A调B，B再调C，则B机器上，在B调C之前，RpcContext记录的是A调B的信息，在B调C之后，RpcContext记录的是B调C的信息。
        // 所以要保持该filter的状态保证invoker.invoke(invocation);之后不会改变
        final RpcContext context = RpcContext.getContext();
        final EndPointSubmitter endPointSubmitter = Brave.getEndPointSubmitter();
        final int localPort = context.getLocalPort();
        final boolean providerSide = context.isProviderSide();
        final boolean consumerSide = context.isConsumerSide();
        final String localHost = context.getLocalHost();
        final String remoteHost = context.getRemoteHost();
        final int remotePort = context.getRemotePort();
        //请求名称
        String requestName = invoker.getInterface().getName()+"."+invocation.getMethodName();

        invokeBefore(invocation, serviceName, endPointSubmitter, localPort, providerSide, consumerSide, localHost, requestName);

        Result result = null;

        try {
            result = invoker.invoke(invocation);
        }catch (RpcException e){
            if (providerSide) {
                serverTracer.submitBinaryAnnotation(remoteHost+remotePort+"//"+requestName,e.getCode());
            }else if (consumerSide) {//调用端
                clientTracer.submitBinaryAnnotation(remoteHost+remotePort+"//"+requestName,e.getCode());
            }
        }finally {
            invokeAfter(serviceName, endPointSubmitter, localPort, providerSide, consumerSide, localHost, remoteHost, remotePort, requestName, result);
        }
        return result;
    }

    private void invokeAfter(String serviceName, EndPointSubmitter endPointSubmitter, int localPort, boolean providerSide, boolean consumerSide, String localHost, String remoteHost, int remotePort, String requestName, Result result) {
        //由于ServerAndClientSpanState的单例,所以每次提交前都应该重新设置EndPoint,不然就会错乱
        endPointSubmitter.submit(localHost, localPort, serviceName);
        if (providerSide) {
            try {
                serverTracer.setServerSend();
            } finally {
                serverTracer.clearCurrentSpan();
            }
        } else if (consumerSide) {//调用端
            if (result!=null && result.hasException()) {
                clientTracer.submitBinaryAnnotation(remoteHost+remotePort+"//"+requestName,result.getException().toString());
            }
            clientTracer.setClientReceived();
        }else {
        }
    }

    private void invokeBefore(Invocation invocation, String serviceName, EndPointSubmitter endPointSubmitter, int localPort, boolean providerSide, boolean consumerSide, String localHost, String requestName) {
        //由于ServerAndClientSpanState的单例,所以每次提交前都应该重新设置EndPoint,不然就会错乱
        endPointSubmitter.submit(localHost, localPort, serviceName);
        if (providerSide) {
            String traceId = invocation.getAttachment(ZipkinTraceConstants.TRACE_ID);
            String spanId = invocation.getAttachment(ZipkinTraceConstants.SPAN_ID);
            String parentId = invocation.getAttachment(ZipkinTraceConstants.PARENT_ID);
            String isSampled = invocation.getAttachment(ZipkinTraceConstants.IS_SAMPLED);
            if (isSampled == null || SAMPLED.equals(isSampled)) {//无需搜集
                serverTracer.setStateNoTracing();
            } else {
                if (spanId == null) {
                    serverTracer.setStateUnknown(requestName);
                } else {
                    serverTracer.setStateCurrentTrace(IdConversion.convertToLong(traceId),
                            IdConversion.convertToLong(spanId),
                            parentId == null ? null : IdConversion.convertToLong(parentId), requestName);
                }
                serverTracer.setServerReceived();
            }
        }else if (consumerSide) {//调用端
            final SpanId span = clientTracer.startNewSpan(requestName);
            final Map<String, String> attachments = invocation.getAttachments();
            if (span == null) {//无需搜集
                attachments.put(ZipkinTraceConstants.IS_SAMPLED, SAMPLED);
            }else{//需要搜集数据
                attachments.put(ZipkinTraceConstants.TRACE_ID, IdConversion.convertToString(span.getTraceId()));
                attachments.put(ZipkinTraceConstants.SPAN_ID, IdConversion.convertToString(span.getSpanId()));
                if (span.getParentSpanId() != null) {
                    attachments.put(ZipkinTraceConstants.PARENT_ID, IdConversion.convertToString(span.getParentSpanId()));
                }
                attachments.put(ZipkinTraceConstants.IS_SAMPLED, NO_SAMPLED);
            }
            try {
                clientTracer.setClientSent();
            }catch (Throwable e){
            }
        }else {
            serverTracer.setStateNoTracing();
        }
    }


//    public static void main(String[] args) throws InterruptedException {
//        ZipkinTraceFilter filter = new ZipkinTraceFilter();
//        Brave.getEndPointSubmitter().submit("192.168.5.52", (short) 9090, "com");
//        for (int i=0;i<2;i++){
//            final SpanId spanId = filter.clientTracer.startNewSpan("com.test");
//            filter.clientTracer.setClientSent();
//            Thread.sleep(1000);
//
//            if(spanId.getTraceId()>0){
//                filter.serverTracer.setStateCurrentTrace(spanId.getTraceId(), spanId.getSpanId(), spanId.getParentSpanId(), "com.test.service");
//            }else {
//                filter.serverTracer.setStateUnknown("com.test.service");
//            }
//            filter.serverTracer.setServerReceived();
//            Thread.sleep(2000);
//            try {
//                filter.serverTracer.setServerSend();
//            } finally {
//                filter.serverTracer.clearCurrentSpan();
//            }
//            filter.clientTracer.submitBinaryAnnotation("code",200);
//            filter.clientTracer.setClientReceived();
//            System.out.println("--->i"+i);
//        }
//
//
////        Random random = new Random();
////        Span span = new Span();
////        long rootSpanId=random.nextLong();
////        span.setId(rootSpanId);
////        span.setName("com");
////        span.setTrace_id(rootSpanId);
////        System.out.println(System.currentTimeMillis());
////        final Endpoint endpoint = new Endpoint(IpUtil.ip2int("192.168.5.52"), (short) 9090, "com");
////        final Annotation CLIENT_SEND = new Annotation(currentTimeMicroseconds(), zipkinCoreConstants.CLIENT_SEND);
////        CLIENT_SEND.setHost(endpoint);
////        span.addToAnnotations(CLIENT_SEND);
////        Thread.sleep(1000);
////        final Annotation SERVER_RECV = new Annotation(currentTimeMicroseconds(), zipkinCoreConstants.SERVER_RECV);
////        SERVER_RECV.setHost(endpoint);
////        span.addToAnnotations(SERVER_RECV);
////        Thread.sleep(1000);
////        final Annotation SERVER_SEND = new Annotation(currentTimeMicroseconds(), zipkinCoreConstants.SERVER_SEND);
////        SERVER_SEND.setHost(endpoint);
////        span.addToAnnotations(SERVER_SEND);
////        Thread.sleep(2000);
////        final Annotation CLIENT_RECV = new Annotation(currentTimeMicroseconds(), zipkinCoreConstants.CLIENT_RECV);
////        CLIENT_SEND.setHost(endpoint);
////        span.addToAnnotations(CLIENT_RECV);
////        ZipkinSpanCollector collector = new ZipkinSpanCollector("10.10.20.105",9410);
////        collector.collect(span);
////        final Span span1 = span.deepCopy();
////        span1.setParent_id(span.getId());
////        span1.setId(random.nextInt());
////        span1.setName("span1");
////        collector.collect(span1);
////        System.out.println("----------");
//    }
//    public static long currentTimeMicroseconds() {
//        return System.currentTimeMillis()*1000 ;
//    }
}
