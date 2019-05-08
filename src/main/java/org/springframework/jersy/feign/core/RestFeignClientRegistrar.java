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

    private static final String SCAN_BASE_PACKAGES_ATTR = "scanBasePackages";

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        ClassPathBeanDefinitionScanner beanDefinitionScanner = getClassPathBeanDefinitionScanner(registry);

        Set<String> scanPackageNames = getScanPackages(importingClassMetadata);

        for (String scanPackageName : scanPackageNames) {

            Set<BeanDefinition> candidateComponents = beanDefinitionScanner.findCandidateComponents(scanPackageName);

            for (BeanDefinition candidateComponent : candidateComponents) {

                if (candidateComponent instanceof AnnotatedBeanDefinition) {

                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(), "@RestFeignClient can only be specified on an interface");
                    Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(RestFeignClient.class.getName());

                    // do register
                    registerRestFeignClients(annotationAttributes, annotationMetadata, registry);

                }


            }

        }

    }

    /**
     * get {@link ClassPathBeanDefinitionScanner} to scan {@link RestFeignClient} bean definition
     *
     * @param registry bean definition registry
     * @return ClassPathBeanDefinitionScanner
     */
    private ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        // don't scan @Component
        ClassPathBeanDefinitionScanner beanDefinitionScanner = new ClassPathBeanDefinitionScanner(registry, false, environment) {

            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    isCandidate = true;
                }
                return isCandidate;
            }
        };
        beanDefinitionScanner.setResourceLoader(resourceLoader);
        // scan @RestFeignClient
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RestFeignClient.class);
        beanDefinitionScanner.addIncludeFilter(annotationTypeFilter);
        return beanDefinitionScanner;
    }

    private void registerRestFeignClients(Map<String, Object> annotationAttributes, AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

        String beanName = getName(annotationAttributes, annotationMetadata);
        String url = (String) annotationAttributes.get("url");
        Boolean isSecure = (Boolean) annotationAttributes.get("isSecure");
        Boolean singleton = (Boolean) annotationAttributes.get("singleton");
        Logger.Level level = (Logger.Level) annotationAttributes.get("level");

        Class<? super RequestInterceptor>[] interceptors = (Class<? super RequestInterceptor>[]) annotationAttributes.get("interceptors");
        for (Class<?> clazz : interceptors) {
            if (!RequestInterceptor.class.isAssignableFrom(clazz)) {
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

    /**
     * get rest feign client name
     *
     * @param attributes
     * @param annotationMetadata
     * @return
     */
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

    /**
     * get packages to scan By {@link ClassPathBeanDefinitionScanner}
     *
     * @param importingClassMetadata
     * @return scan packages
     */
    private Set<String> getScanPackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableRestFeignClients.class.getCanonicalName());
        Class<?>[] clientClasses = (Class<?>[]) attributes.get("clients");
        Set<String> scanPackageNames = new HashSet<>();
        // use scanBasePackages
        if (clientClasses == null || clientClasses.length == 0) {
            for (String pkg : (String[]) attributes.get(SCAN_BASE_PACKAGES_ATTR)) {
                if (StringUtils.hasText(pkg)) {
                    scanPackageNames.add(pkg);
                }
            }
            if (scanPackageNames.isEmpty()) {
                // if not assign scanBasePackages , use EnableRestFeignClients location package instead
                scanPackageNames.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
            }

        } else {
            // use clients
            Stream.of(clientClasses).forEach(clientClass -> {
                scanPackageNames.add(ClassUtils.getPackageName(clientClass));
            });
        }
        return scanPackageNames;
    }


}
