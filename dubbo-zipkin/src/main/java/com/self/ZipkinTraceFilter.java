package com.self;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.github.kristofa.brave.*;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollectorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * zipkin trace 收集器
 * ContextFilter(order=-10000) ZipkinTraceFilter要排在ContextFilter之前
 * Created by shaojieyue on 8/27/15.
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER},order=-5000)
public class ZipkinTraceFilter implements Filter {
    public static final Logger logger = LoggerFactory.getLogger(ZipkinTraceFilter.class);
    
    //采集trace
    public static final String SAMPLED = "0";
    //不采集trace
    public static final String NO_SAMPLED = "1";
    public static final int PERIOD = 3;

    private volatile boolean execSampled = false;
    private ZipkinSpanCollector collector;
    private  ServerTracer serverTracer;
    private  ClientTracer clientTracer;
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private Properties config = new Properties();

    public ZipkinTraceFilter() {
        //加载资源文件
        final InputStream resourceAsStream = ZipkinTraceFilter.class.getClassLoader().getResourceAsStream("zipkin-clollector.properties");
        if (resourceAsStream != null) {
            try {
                config.load(resourceAsStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("load config [zipkin-clollector.properties] fail.");
            }
        }
        initZipkin();
    }

    private void initZipkin() {
        //定时检测收集器,防止收集器down重启
        service.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (execSampled) {//已经启动,启动
                    return;
                }
                /**
                 * 单线程处理,所以没有必要使用同步锁
                 */
                final String zipkinCollectorHost = config.getProperty("zipkin.collector.host","localhost");
                final int zipkinCollectorPort = Integer.valueOf(config.getProperty("zipkin.collector.port","1463"));
                try {
                    ZipkinSpanCollectorParams params = getZipkinSpanCollectorParams();
                    if (collector != null) {
                        try {
                            collector.close();
                        }catch (Throwable e){
                        }
                    }
                    collector = new ZipkinSpanCollector(zipkinCollectorHost, zipkinCollectorPort,params);
                    serverTracer = Brave.getServerTracer(collector, Collections.EMPTY_LIST);
                    clientTracer = Brave.getClientTracer(collector, Collections.EMPTY_LIST);
                    execSampled = true;
                    if (logger.isInfoEnabled()) {
                        logger.info("zipkin span collector start success");
                    }
                }catch (Throwable e){
                    if (logger.isInfoEnabled()) {
                        logger.info("zipkin span collector start fail. flume sink="+zipkinCollectorHost+":"+zipkinCollectorPort);
                    }
                    execSampled = false;
                }
                if (execSampled) {//启动成功则关闭定时器
                    if (logger.isInfoEnabled()) {
                        logger.info("zipkin started,shutdown the scheduled work.");
                    }
                    service.shutdown();
                }
            }

            private ZipkinSpanCollectorParams getZipkinSpanCollectorParams() {
                ZipkinSpanCollectorParams params = new ZipkinSpanCollectorParams();
                //初始化默认配置
                params.setBatchSize(20);//批量提交大小
                params.setNrOfThreads(3);//提交span线程数
                params.setQueueSize(1000);//存储span队列大小
                params.setSocketTimeout(1000);//提交超时时间
                if (config == null) {
                    return params;
                }

                //批量提交大小
                final String batchsize = config.getProperty("collector.batchsize");
                if (batchsize != null) {
                    params.setBatchSize(Integer.valueOf(batchsize));
                }

                //并行提交span的线程数
                String nrOfThreads = config.getProperty("collector.nr.threads");
                if (nrOfThreads != null) {
                    params.setNrOfThreads(Integer.valueOf(nrOfThreads));
                }

                //缓存span的队列的大小
                String queuesize = config.getProperty("collector.queuesize");
                if (queuesize != null) {
                    params.setQueueSize(Integer.valueOf(queuesize));
                }

                //socket time out
                final String socketTimeout = config.getProperty("collector.socket.timeout");
                if (socketTimeout != null) {
                    params.setSocketTimeout(Integer.valueOf(socketTimeout));
                }
                return params;
            }
        },0, PERIOD, TimeUnit.SECONDS);//立即执行
    }


    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!execSampled) {//不进行采样
            return invoker.invoke(invocation);
        }
        final String serviceName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
        //serviceName 为空不进行采样
        if (serviceName == null && "".equals(serviceName.trim())) {
            return invoker.invoke(invocation);
        }
        //注意：RpcContext是一个临时状态记录器，当接收到RPC请求，或发起RPC请求时，RpcContext的状态都会变化。
        // 比如：A调B，B再调C，则B机器上，在B调C之前，RpcContext记录的是A调B的信息，在B调C之后，RpcContext记录的是B调C的信息。
        // 所以要保持该filter的状态保证invoker.invoke(invocation);之后不会改变
        final RpcContext context = RpcContext.getContext();
        final EndpointSubmitter endPointSubmitter = Brave.getEndpointSubmitter();
        final int localPort = context.getLocalPort();
        final boolean providerSide = context.isProviderSide();
        final boolean consumerSide = context.isConsumerSide();
        final String localHost = context.getLocalHost();
        final String remoteHost = context.getRemoteHost();
        final int remotePort = context.getRemotePort();
        //请求名称
        String requestName = invoker.getInterface().getName()+"."+invocation.getMethodName();
        try {
            invokeBefore(invocation, serviceName, endPointSubmitter, localPort, providerSide, consumerSide, localHost, requestName);
        }catch (Throwable e){}
        Result result = null;
        try {
            result = invoker.invoke(invocation);
        }finally {
            try {
                invokeAfter(serviceName, endPointSubmitter, localPort, providerSide, consumerSide, localHost, remoteHost, remotePort, requestName, result,invocation);
            }catch (Throwable e){}
        }
        return result;
    }

    private void invokeAfter(String serviceName, EndpointSubmitter endPointSubmitter, int localPort, boolean providerSide, boolean consumerSide, String localHost, String remoteHost, int remotePort, String requestName, Result result, Invocation invocation) {
        //由于ServerAndClientSpanState的单例,所以每次提交前都应该重新设置EndPoint,不然就会错乱
        endPointSubmitter.submit(localHost, localPort, serviceName);
        if (result == null || result.hasException()) {
            final String key = remoteHost+ ":"+ remotePort + "//" + requestName;
            StringBuilder params = new StringBuilder();
            for (Object param : invocation.getArguments()) {//连接参数
                params.append(param).append(",");
            }
            String message = null;
            if (result == null) {//result=null 说明是超时请求
                message = "request time out";
            }else {
                message = result.getException().toString();
            }
            //含有异常时,则提交调用信息
            if (providerSide) {//server端
                //设置remote信息 server端的remote为client,client的remote为server
                serverTracer.submitBinaryAnnotation("client."+key, message);
                serverTracer.submitBinaryAnnotation("client.params",params.toString());
            }else if (consumerSide){
                //设置remote信息 server端的remote为client,client的remote为server
                clientTracer.submitBinaryAnnotation("server."+key, message);
                clientTracer.submitBinaryAnnotation("server.params",params.toString());
            }
        }

        if (providerSide) {//server端
            try {
                serverTracer.setServerSend();
            } finally {
                serverTracer.clearCurrentSpan();
            }
        } else if (consumerSide) {//client端
            clientTracer.setClientReceived();
        }else {
        }
    }

    private void invokeBefore(Invocation invocation, String serviceName, EndpointSubmitter endPointSubmitter, int localPort, boolean providerSide, boolean consumerSide, String localHost, String requestName) {
        //由于ServerAndClientSpanState的单例,所以每次提交前都应该重新设置EndPoint,不然就会错乱
        endPointSubmitter.submit(localHost, localPort, serviceName);
        if (providerSide) {//server 端
            String traceId = invocation.getAttachment(ZipkinTraceConstants.TRACE_ID);
            String spanId = invocation.getAttachment(ZipkinTraceConstants.SPAN_ID);
            String parentId = invocation.getAttachment(ZipkinTraceConstants.PARENT_ID);
            String isSampled = invocation.getAttachment(ZipkinTraceConstants.IS_SAMPLED);
            if (isSampled == null || NO_SAMPLED.equals(isSampled)) {//无需搜集
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
        }else if (consumerSide) {//client 端
            //设置调用者的方法
            final SpanId span = clientTracer.startNewSpan(requestName);
            final Map<String, String> attachments = invocation.getAttachments();
            if (span == null) {//无需搜集
                attachments.put(ZipkinTraceConstants.IS_SAMPLED, NO_SAMPLED);
            }else{//需要搜集数据
                attachments.put(ZipkinTraceConstants.TRACE_ID, IdConversion.convertToString(span.getTraceId()));
                attachments.put(ZipkinTraceConstants.SPAN_ID, IdConversion.convertToString(span.getSpanId()));
                if (span.getParentSpanId() != null) {
                    attachments.put(ZipkinTraceConstants.PARENT_ID, IdConversion.convertToString(span.getParentSpanId()));
                }
                attachments.put(ZipkinTraceConstants.IS_SAMPLED, SAMPLED);
            }
            try {
                clientTracer.setClientSent();
            }catch (Throwable e){
            }
        }else {
            serverTracer.setStateNoTracing();
        }
    }
}
