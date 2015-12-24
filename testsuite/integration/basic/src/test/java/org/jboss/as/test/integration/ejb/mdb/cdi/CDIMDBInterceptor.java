package org.jboss.as.test.integration.ejb.mdb.cdi;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Stuart Douglas
 */
@Interceptor
@CDIMDBBinding
@Priority(1)
public class CDIMDBInterceptor {

    public static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @AroundInvoke
    public Object invoke(InvocationContext invocationContext) throws Exception {
        MESSAGES.add("intercepted");
        return invocationContext.proceed();
    }
}
