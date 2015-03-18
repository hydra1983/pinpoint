package com.navercorp.pinpoint.plugin.tomcat.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request;

import com.navercorp.pinpoint.bootstrap.MetadataAccessor;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.MethodInfo;
import com.navercorp.pinpoint.bootstrap.interceptor.SimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.Cached;
import com.navercorp.pinpoint.bootstrap.plugin.Name;
import com.navercorp.pinpoint.plugin.tomcat.TomcatConstants;

public class RequestRecycleInterceptor implements SimpleAroundInterceptor, TomcatConstants {

    private PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private MethodInfo targetMethod;
    private MetadataAccessor asyncAccessor;

    public RequestRecycleInterceptor(TraceContext context, @Cached MethodInfo targetMethod, @Name(METADATA_ASYNC) MetadataAccessor asyncAccessor) {
        this.targetMethod = targetMethod;
        this.asyncAccessor = asyncAccessor;
    }

    @Override
    public void before(Object target, Object[] args) {
        logger.beforeInterceptor(target, target.getClass().getName(), targetMethod.getName(), "", args);

        try {
            final Request request = (Request) target;
            if (asyncAccessor.isApplicable(target)) {
                asyncAccessor.set(target, Boolean.FALSE);
            }

            if (request.getAttribute("PINPOINT_TRACE") != null) {
                Trace trace = (Trace) request.getAttribute("PINPOINT_TRACE");
                if (trace.canSampled()) {
                    trace.markAfterTime();
                    trace.traceRootBlockEnd();
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to before process. {}", t.getMessage(), t);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }
}
