package org.veto.core.authorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods or classes that do not require authentication.
 * When a request handler method or its containing class is annotated with @UnAuthorize,
 * the AuthorizeInterceptor will not check for user login status.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnAuthorize {
}
