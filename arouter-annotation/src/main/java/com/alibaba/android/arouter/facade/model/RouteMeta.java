package com.alibaba.android.arouter.facade.model;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.enums.RouteType;

import java.util.Map;

import javax.lang.model.element.Element;

/**
 * It contains basic route information.
 * 包含了一次路由操作需要的所有信息
 */
public class RouteMeta {
    private RouteType type;         // Type of route     目标路由节点的类型，
    private Element rawType;        // Raw type of route 目标路由节点的Element , 对Element类还不了解的可以先面向搜索引擎学习一下
    private Class<?> destination;   // Destination       目标路由节点的Class 对象
    private String path;            // Path of route    目标路由节点的path，(即@Route注解中的path值)
    private String group;           // Group of route   目标路由节点的group，(即@Route注解中的group值)
    private int priority = -1;      // The smaller the number, the higher the priority 目标路由节点的priority，(即@Route注解中的priority值)
    private int extra;              // Extra data       目标路由节点的extra，(即@Route注解中的extra值)
    private Map<String, Integer> paramsType;  // Param type 目标路由节点中需要自动注入的参数的（名称-参数类型）映射关系，这里的Integer值代表着枚举类TypeKind（上文已提过）的 ordinal
    private String name;

    private Map<String, Autowired> injectConfig;  // Cache inject config. 该路由节点中需要自动注入的(成员变量名-@Autowired)的映射关系

    public RouteMeta() {
    }

    /**
     * For versions of 'compiler' less than 1.0.7, contain 1.0.7
     *
     * @param type        type
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param priority    priority
     * @param extra       extra
     * @return this
     */
    public static RouteMeta build(RouteType type, Class<?> destination, String path, String group, int priority, int extra) {
        return new RouteMeta(type, null, destination, null, path, group, null, priority, extra);
    }

    /**
     * For versions of 'compiler' greater than 1.0.7
     *
     * @param type        type
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param paramsType  paramsType
     * @param priority    priority
     * @param extra       extra
     * @return this
     */
    public static RouteMeta build(RouteType type, Class<?> destination, String path, String group, Map<String, Integer> paramsType, int priority, int extra) {
        return new RouteMeta(type, null, destination, null, path, group, paramsType, priority, extra);
    }

    /**
     * Type
     *
     * @param route       route
     * @param destination destination
     * @param type        type
     */
    public RouteMeta(Route route, Class<?> destination, RouteType type) {
        this(type, null, destination, route.name(), route.path(), route.group(), null, route.priority(), route.extras());
    }

    /**
     * Type
     *
     * @param route      route
     * @param rawType    rawType
     * @param type       type
     * @param paramsType paramsType
     */
    public RouteMeta(Route route, Element rawType, RouteType type, Map<String, Integer> paramsType) {
        this(type, rawType, null, route.name(), route.path(), route.group(), paramsType, route.priority(), route.extras());
    }

    /**
     * Type
     *
     * @param type        type
     * @param rawType     rawType
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param paramsType  paramsType
     * @param priority    priority
     * @param extra       extra
     */
    public RouteMeta(RouteType type, Element rawType, Class<?> destination, String name, String path, String group, Map<String, Integer> paramsType, int priority, int extra) {
        this.type = type;
        this.name = name;
        this.destination = destination;
        this.rawType = rawType;
        this.path = path;
        this.group = group;
        this.paramsType = paramsType;
        this.priority = priority;
        this.extra = extra;
    }

    public Map<String, Integer> getParamsType() {
        return paramsType;
    }

    public RouteMeta setParamsType(Map<String, Integer> paramsType) {
        this.paramsType = paramsType;
        return this;
    }

    public Map<String, Autowired> getInjectConfig() {
        return injectConfig;
    }

    public void setInjectConfig(Map<String, Autowired> injectConfig) {
        this.injectConfig = injectConfig;
    }

    public Element getRawType() {
        return rawType;
    }

    public RouteMeta setRawType(Element rawType) {
        this.rawType = rawType;
        return this;
    }

    public RouteType getType() {
        return type;
    }

    public RouteMeta setType(RouteType type) {
        this.type = type;
        return this;
    }

    public Class<?> getDestination() {
        return destination;
    }

    public RouteMeta setDestination(Class<?> destination) {
        this.destination = destination;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RouteMeta setPath(String path) {
        this.path = path;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public RouteMeta setGroup(String group) {
        this.group = group;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public RouteMeta setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getExtra() {
        return extra;
    }

    public RouteMeta setExtra(int extra) {
        this.extra = extra;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RouteMeta{" +
                "type=" + type +
                ", rawType=" + rawType +
                ", destination=" + destination +
                ", path='" + path + '\'' +
                ", group='" + group + '\'' +
                ", priority=" + priority +
                ", extra=" + extra +
                ", paramsType=" + paramsType +
                ", name='" + name + '\'' +
                '}';
    }
}