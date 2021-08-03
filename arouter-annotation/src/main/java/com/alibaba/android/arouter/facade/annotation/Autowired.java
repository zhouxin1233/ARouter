package com.alibaba.android.arouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  参数自动注入的注解，对需要自动注入的成员变量标记，ARouter跳转时就可以将值注入
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/20 下午4:26
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Autowired {

    // M为要注入的参数值的自定义名称
    String name() default "";

    // 为是否进行非空检验（基本类型不检验），如果设置为true并再跳转时给自动绑定参数传入null的话会抛出异常
    boolean required() default false;

    // Description of the field
    String desc() default "";
}
