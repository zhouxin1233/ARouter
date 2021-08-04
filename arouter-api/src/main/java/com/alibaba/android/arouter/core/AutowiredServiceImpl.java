package com.alibaba.android.arouter.core;

import android.content.Context;
import android.util.LruCache;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.AutowiredService;
import com.alibaba.android.arouter.facade.template.ISyringe;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.android.arouter.utils.Consts.SUFFIX_AUTOWIRED;

/**
 * param inject service impl.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/28 下午6:08
 */
@Route(path = "/arouter/service/autowired")
public class AutowiredServiceImpl implements AutowiredService {
    // classCache的作用是缓存类对象。它的类型LruCache得知，会把最近最少使用到的类删除
    private LruCache<String, ISyringe> classCache;
    // 应该就是一个黑名单，这里面的类都不会被初始化。
    private List<String> blackList;

    @Override
    public void init(Context context) {
        classCache = new LruCache<>(50);
        blackList = new ArrayList<>();
    }

    /**
     * 这个服务类所要显示的功能
     * @param instance the instance who need autowired.
     */
    @Override
    public void autowire(Object instance) {
        doInject(instance, null);
    }

    /**
     * Recursive injection
     *
     * @param instance who call me.
     * @param parent   parent of me.
     */
    private void doInject(Object instance, Class<?> parent) {
        // 首先根据参数instance对象获取到对应的类名。
        Class<?> clazz = null == parent ? instance.getClass() : parent;

        // @Autowire 实例化的类其父类都是ISyringe
        ISyringe syringe = getSyringe(clazz);
        if (null != syringe) {
            syringe.inject(instance);
        }

        Class<?> superClazz = clazz.getSuperclass();
        // has parent and its not the class of framework.
        if (null != superClazz && !superClazz.getName().startsWith("android")) {
            doInject(instance, superClazz);
        }
    }

    private ISyringe getSyringe(Class<?> clazz) {
        String className = clazz.getName();

        try {
            // 判断这个类名是否在黑名单中
            if (!blackList.contains(className)) {
                // 如果不在中黑名单中就从classCache中获取。
                ISyringe syringeHelper = classCache.get(className);
                if (null == syringeHelper) {  // No cache.
                    // 如果没在classCache中，就通过反射的方式实例化instance对应类名+$$Autowired所对应的类对象。
                    syringeHelper = (ISyringe) Class.forName(clazz.getName() + SUFFIX_AUTOWIRED).getConstructor().newInstance();
                }
                // 最后把实例化的类存放到classCache中（减少反射次数，减少性能消耗）
                classCache.put(className, syringeHelper);
                return syringeHelper;
            }
        } catch (Exception e) {
            blackList.add(className);    // This instance need not autowired.
        }

        return null;
    }
}
