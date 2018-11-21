# Spring Jersy Feign 

## 前言	

- 本项目是基于`Feign`进行二次开发，旨在帮助使用`Jersy`框架又不好升级到`Spring Boot`的开发人员，提供和`Spring Cloud Open Feign`类似的功能

- 如有好的建议，请发邮件`707094980@qq.com`



## Quick Start

- 在`Jersy Resource`接口上声明注解

  ```java
  @RestFeignClient(name="hello-world",url="${service.url}")
  @Path("/hello")
  public interface IHelloWorldResource{
      
      @Path("/world")
      @GET
      public String helloWorld();
      
  }
  ```

  - `@RestFeignClient`注解其他重要属性说明

    - `level`是配置`Feign` `Logger.Level `日志级别，可选值有如下

      ```java
       public static enum Level {
              NONE,
              BASIC,
              HEADERS,
              FULL;
         }
      ```

    - `interceptors`是指定`Feign`的拦截器`RequestInterceptor`

      > 必须是`RequestInterceptor`的实现类

- 在`@Configuration`类中激活

  ```java
  @EnableRestFeignClients(clients={IHelloWorldResource.class})
  @Configuration
  public class RestFeinClientsConfiguration{
      
  }
  ```

  > 必须能让Spring 能够扫描到`RestFeinClientsConfiguration`类

如上配置，即可实现Rest调用