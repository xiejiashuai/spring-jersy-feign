package org.springframework.jersy.feign.core;

import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.jaxrs.JAXRSContract;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.jersy.feign.core.decoder.FastJsonDecoder;
import org.springframework.jersy.feign.core.encoder.FastJsonEncoder;
import org.springframework.jersy.feign.core.logger.CustomizedLogger;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiashuai.xie
 */
@Data
@Slf4j
public class RestFeignClientFactoryBean implements FactoryBean, EnvironmentAware {

    private ConfigurableEnvironment environment;

    private Class<?> targetType;

    private String beanName;

    private String prefixUrl;

    private Boolean singleton = Boolean.TRUE;

    private Boolean isSecure;

    private Logger.Level level;

    private List<Class<? super RequestInterceptor>> interceptors;

    private List<RequestInterceptor> interceptorInstances = new ArrayList<>();


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Nullable
    @Override
    public Object getObject() throws Exception {


        String actualPrefixUrl = resolvePlaceholders();


        if (isSecure) {

            if (actualPrefixUrl.startsWith("http://")) {
                StringUtils.replace(actualPrefixUrl, "http://", "https://");
            }

            if (!actualPrefixUrl.startsWith("http://") && !actualPrefixUrl.startsWith("https://")) {
                actualPrefixUrl = "https://" + actualPrefixUrl;
            }

        } else {

            if (actualPrefixUrl.startsWith("https://")) {
                StringUtils.replace(actualPrefixUrl, "https://", "http://");
            }

            if (!actualPrefixUrl.startsWith("http://") && !actualPrefixUrl.startsWith("https://")) {
                actualPrefixUrl = "http://" + actualPrefixUrl;
            }

        }

        log.info("will go to instantiate rest feign client for:{},prefix-url:{},logger-level:{}", targetType, actualPrefixUrl, String.valueOf(level));

        Feign.Builder builder = Feign.builder()
                .encoder(new FastJsonEncoder())
                .decoder(new FastJsonDecoder())
                .contract(new JAXRSContract())
                .logger(new CustomizedLogger())
                .logLevel(level);

        if (!CollectionUtils.isEmpty(interceptors)) {

            for (Class<? super RequestInterceptor> interceptor : interceptors) {

                BeanWrapper wrapper = new BeanWrapperImpl(interceptor);

                RequestInterceptor instance = (RequestInterceptor) wrapper.getWrappedInstance();

                interceptorInstances.add(instance);

            }

            builder.requestInterceptors(interceptorInstances);

        }


        Object proxy = builder.target(targetType, actualPrefixUrl);

        log.info("success to instantiate rest feign client for:{}", targetType);

        return proxy;
    }

    @Nullable
    @Override
    public Class<?> getObjectType() {
        return targetType;
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * 解决占位符
     *
     * @return
     */
    private String resolvePlaceholders() {

        String actualPrefixUrl = environment.resolvePlaceholders(prefixUrl);

        return actualPrefixUrl;
    }


}
