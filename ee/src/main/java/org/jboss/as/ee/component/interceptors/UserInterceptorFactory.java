package org.jboss.as.ee.component.interceptors;

import org.jboss.as.ee.component.TimerInvocationMarker;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor factory that handles user interceptors, and switches between timer normal invocations
 *
 * @author Stuart Douglas
 */
public class UserInterceptorFactory implements InterceptorFactory {

    private final InterceptorFactory aroundInvoke;
    private final InterceptorFactory aroundTimeout;

    public UserInterceptorFactory(final InterceptorFactory aroundInvoke, final InterceptorFactory aroundTimeout) {
        this.aroundInvoke = aroundInvoke;
        this.aroundTimeout = aroundTimeout;
    }


    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final Interceptor aroundInvoke = this.aroundInvoke.create(context);
        final Interceptor aroundTimeout = this.aroundTimeout.create(context);
        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                TimerInvocationMarker marker = context.getPrivateData(TimerInvocationMarker.class);
                if (marker == null) {
                    return aroundInvoke.processInvocation(context);
                } else {
                    return aroundTimeout.processInvocation(context);
                }
            }
        };

    }
}
