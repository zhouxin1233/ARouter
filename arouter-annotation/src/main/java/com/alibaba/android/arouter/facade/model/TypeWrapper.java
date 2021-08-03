package com.alibaba.android.arouter.facade.model;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Used for get type of target object.
 *  这个类是用来获得某个类的Type实例
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 17/10/26 11:56:22
 */
public class TypeWrapper<T> {
    protected final Type type;

    protected TypeWrapper() {
        Type superClass = getClass().getGenericSuperclass();

        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
