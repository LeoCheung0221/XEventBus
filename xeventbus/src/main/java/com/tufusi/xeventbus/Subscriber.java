package com.tufusi.xeventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by LeoCheung on 2020/11/18.
 *
 * @description 方法订阅注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscriber {

    /**
     * 线程模式
     *
     * @return 线程类型
     */
    ThreadMode threadMode() default ThreadMode.POSTING;
}
