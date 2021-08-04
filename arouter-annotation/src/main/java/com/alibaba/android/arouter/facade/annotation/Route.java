package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a page can be route by router.
 * 路由节点的注解，我们可以看到这里的官方描述里没有更新，@Route注解不仅可以标记Activity
 * , Fragment还可以标记 Service (这里说的Service是实现了IProvider的类，并不是安卓的四大组件那个Service)
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/15 下午9:29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Route {

    /**
     * path 路径，这个值必须是唯一的,框架中就是通过这个值获取到对应的路由操作类。 Path of route
     */
    String path();

    /**
     * group 分组，不指定时默认按照包名进行分组 Used to merger routes, the group name MUST BE USE THE COMMON WORDS !!!
     */
    String group() default "";

    /**
     * 作用于文档中，实际开发过程中没什么作用 Name of route, used to generate javadoc.
     */
    String name() default "";

    /**
     * extras 额外信息，一个int值，相当于32个0 1标记位，可以自定义一些标记规则为路由节点提供一些信息,
     * 可以理解为一个属性配置字段，通过设置不同的值来标记跳转页面需要做的特殊逻辑。
     */
    int extras() default Integer.MIN_VALUE;

    /**
     * The priority of route.
     */
    int priority() default -1;
}
