package com.tufusi.xeventbus;

import java.lang.reflect.Method;

/**
 * Created by LeoCheung on 2020/11/18.
 *
 * @description 存储订阅方法对应的属性
 */
public class SubscriberMethod {

    /**
     * 注册的方法
     */
    private Method method;

    /**
     * 线程类型
     */
    private ThreadMode threadMode;

    /**
     * 参数类型
     */
    private Class<?> paramType;

    public SubscriberMethod(Method method, ThreadMode threadMode, Class<?> paramType) {
        this.method = method;
        this.threadMode = threadMode;
        this.paramType = paramType;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        this.threadMode = threadMode;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }
}