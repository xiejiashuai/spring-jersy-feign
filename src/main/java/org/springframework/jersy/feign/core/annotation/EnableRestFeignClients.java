package org.springframework.jersy.feign.core.annotation;

import org.springframework.context.annotation.Import;
import org.springframework.jersy.feign.core.RestFeignClientRegistrar;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RestFeignClientRegistrar.class)
public @interface EnableRestFeignClients {

    /**
     * <note>Class must be present {@link RestFeignClient}</note>
     */
    Class<?>[] clients() default {};

    /**
     * scan {@link RestFeignClient} base package
     * @return
     */
    String[] scanBasePackages() default {};

}
