package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 拦截器的注解，用来标记作为拦截器的类，这个类必须实现 IInterceptor 接口
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 14:03
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Interceptor {
    /**
     * 拦截器的优先级 The priority of interceptor, ARouter will be excute them follow the priority.
     */
    int priority();

    /**
     * The name of interceptor, may be used to generate javadoc.
     */
    String name() default "Default";
}
