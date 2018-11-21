package org.springframework.jersy.feign.core;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jersy.feign.core.annotation.EnableRestFeignClients;
import org.springframework.jersy.feign.core.annotation.RestFeignClient;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Handle {@link RestFeignClient}
 *
 * @author jiashuai.xie
 * @see EnableRestFeignClients
 * @see RestFeignClient
 */
public class RestFeignClientRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {


    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Map<String, Object> attrs = importingClassMetadata.getAnnotationAttributes(EnableRestFeignClients.class.getName());

        Class<?>[] clientClasses = (Class<?>[]) attrs.get("clients");

        // 不扫描component以及派生注解
        ClassPathBeanDefinitionScanner beanDefinitionScanner = new ClassPathBeanDefinitionScanner(registry, false, environment) {

            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                // 必须是独立的类
                if (beanDefinition.getMetadata().isIndependent()) {
                    isCandidate = true;
                }
                return isCandidate;
            }
        };
        beanDefinitionScanner.setResourceLoader(resourceLoader);

        // 只扫描RestFeignClient注解
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RestFeignClient.class);

        beanDefinitionScanner.addIncludeFilter(annotationTypeFilter);

        Set<String> scanPackageNames = new HashSet<>();

        Stream.of(clientClasses).forEach(clientClass -> {
            scanPackageNames.add(ClassUtils.getPackageName(clientClass));
        });


        for (String scanPackageName : scanPackageNames) {

            Set<BeanDefinition> candidateComponents = beanDefinitionScanner.findCandidateComponents(scanPackageName);

            for (BeanDefinition candidateComponent : candidateComponents) {

                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    // 获取元信息
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();

                    // 校验
                    Assert.isTrue(annotationMetadata.isInterface(), "@RestFeignClient can only be specified on an interface");

                    /// 获取注解信息 url =xxx
                    Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(RestFeignClient.class.getName());

                    // 注册为BeanDefinition
                    registerRestFeignClients(annotationAttributes, annotationMetadata, registry);

                }


            }

        }

    }

    private void registerRestFeignClients(Map<String, Object> annotationAttributes, AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

        String beanName = getName(annotationAttributes, annotationMetadata);

        // 获取url信息
        String url = (String) annotationAttributes.get("url");

        // 获取是否为https
        Boolean isSecure = (Boolean) annotationAttributes.get("isSecure");

        // 是否为单例
        Boolean singleton = (Boolean) annotationAttributes.get("singleton");

        Logger.Level level = (Logger.Level) annotationAttributes.get("level");

        Class<?>[] interceptors = (Class<?>[]) annotationAttributes.get("interceptors");

        for (Class<?> clazz : interceptors) {

            if (RequestInterceptor.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("not valid class ,except:" + RequestInterceptor.class + "but:" + clazz);
            }

        }


        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RestFeignClientFactoryBean.class);
        beanDefinitionBuilder.addPropertyValue("targetType", annotationMetadata.getClassName());
        beanDefinitionBuilder.addPropertyValue("beanName", beanName);
        beanDefinitionBuilder.addPropertyValue("prefixUrl", url);
        beanDefinitionBuilder.addPropertyValue("singleton", singleton);
        beanDefinitionBuilder.addPropertyValue("isSecure", isSecure);
        beanDefinitionBuilder.addPropertyValue("level", level);
        beanDefinitionBuilder.addPropertyValue("interceptors", Arrays.asList(interceptors));
        beanDefinitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinitionBuilder.getBeanDefinition(), beanName);

        BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);

    }

    private String getName(Map<String, Object> attributes, AnnotationMetadata annotationMetadata) {

        String name = (String) attributes.get("name");
        if (!StringUtils.hasText(name)) {
            name = (String) attributes.get("value");
        }
        if (!StringUtils.hasText(name)) {
            name = annotationMetadata.getClassName();
        }

        return name;

    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
