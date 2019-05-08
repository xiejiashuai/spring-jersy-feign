package org.springframework.jersy.feign.core.annotation;


import feign.Logger;
import feign.RequestInterceptor;

import java.lang.annotation.*;

/**
 *
 * @author jiashuai.xie
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestFeignClient {

    /**
     * client name
     * will be bean name
     *
     * @return
     * @see RestFeignClient#value()
     * name value must have one
     */
    String name() default "";

    /**
     * client name
     * name value must have one
     * will be bean name
     *
     * @return
     * @see RestFeignClient#name()
     */
    String value() default "";

    /**
     * start with http://
     * must be not null
     * <note>el express can be used eg:${}</note>
     */
    String url() default "";

    /**
     * if true, will invoke https://.. default http://....
     */
    boolean isSecure() default false;

    /**
     * if true ,will return singleton bean
     *
     * @return
     */
    boolean singleton() default true;

    /**
     * the feign log level
     *
     * @return
     * @see Logger
     */
    Logger.Level level() default Logger.Level.FULL;

    /**
     * @return
     * @see RequestInterceptor
     */
    Class<?>[] interceptors() default {};

}
