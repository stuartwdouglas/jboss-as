package org.jboss.as.test.integration.ejb.mdb.cdi;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Stuart Douglas
 */

@Inherited
@InterceptorBinding
@Retention(RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface CDIMDBBinding {
}
