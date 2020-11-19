package com.tufusi.xeventbus;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by LeoCheung on 2020/11/18.
 *
 * @description 总线处理类
 */
public class XEventBus {

    //=======================================================
    // 当一个新任务被提交到池中，
    // 如果当前运行线程数小于核心线程数，即使当前有空闲线程，也会新建一个线程来处理新提交的任务；
    // 如果当前运行线程数大于核心线程数，并小于最大线程数，只有当等待队列已满的情况下才会新建线程。
    // 所以最好的情况下是预估当前业务环境下，有极大可能出现运行线程数大于core，并且小于max，是最合理。
    /**
     * 核心线程数
     */
    int corePoolSize = 3;
    /**
     * 最大线程数
     */
    int maximumPoolSize = 6;
    //=======================================================

    /**
     * 超过 corePoolSize 线程数量的线程最大空闲时间
     */
    long keepAliveTime = 2;

    /**
     * 单位：秒
     */
    TimeUnit unit = TimeUnit.SECONDS;

    /**
     * 创建工作队列，用于存放提交的等待执行任务
     * 任何阻塞队列（BlockingQueue）都可以用来转移或保存提交的任务，线程池大小和阻塞队列相互约束线程池：
     * <p>
     * 1、如果运行线程数小于corePoolSize，提交新任务时就会新建一个线程来运行；
     * 2、如果运行线程数大于或等于corePoolSize，新提交的任务就会入列等待；如果队列已满，并且运行线程数小于maximumPoolSize，也将会新建一个线程来运行；
     * 3、如果线程数大于maximumPoolSize，新提交的任务将会根据拒绝策略来处理。
     */
    BlockingQueue<Runnable> workingQueue = new ArrayBlockingQueue<Runnable>(2);

    /**
     * 线程池策略
     * 当线程池已经关闭，或者达到饱和（最大线程和队列均已满）状态时，新提交的任务将会被拒绝。总共有四种拒绝策略
     * 1. AbortPolicy：默认策略，在需要拒绝任务时抛出RejectedExecutionException；
     * 2. CallerRunsPolicy：直接在 execute 方法的调用线程中运行被拒绝的任务，如果线程池已经关闭，任务将被丢弃；
     * 3. DiscardPolicy：直接丢弃任务；
     * 4. DiscardOldestPolicy：丢弃队列中等待时间最长的任务，并执行当前提交的任务，如果线程池已经关闭，任务将被丢弃。
     */
    ThreadPoolExecutor.AbortPolicy policy = null;

    ThreadPoolExecutor threadPoolExecutor = null;

    /**
     * 缓存类，用于存储不同的订阅者对应的订阅方法集合的Map
     * key -> 订阅者
     * value -> 订阅方法集合
     */
    private HashMap<Object, List<SubscriberMethod>> cacheMap;

    private Handler handler;

    /**
     * 私有化构造函数，初始化相关工具辅助类
     * 采用HashMap存储类对应的已订阅方法集合
     * 使用线程池服务技术
     */
    private XEventBus() {
        this.cacheMap = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());

        policy = new ThreadPoolExecutor.AbortPolicy();
        // 创建线程池
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workingQueue,
                policy);
    }

    private static XEventBus instance = new XEventBus();

    public static XEventBus getDefault() {
        return instance;
    }

    /**
     * 将对象注入总线
     *
     * @param subscriber 被观察对象 - 订阅者
     */
    public void register(Object subscriber) {
        // 从缓存中查询该类是否已存在，如果存在，取出该类下所有已订阅的方法类集合
        List<SubscriberMethod> subscriberMethods = cacheMap.get(subscriber);
        // 如果不存在，则进行注册
        if (subscriberMethods == null) {
            // 找出已注解过的方法并存取
            subscriberMethods = findSubscriberMethods(subscriber);
            cacheMap.put(subscriber, subscriberMethods);
        }
    }

    /**
     * 递归向上查询自身及父类方法，排除掉系统类
     *
     * @param subscriber 订阅者
     * @return 已订阅方法的集合
     */
    private List<SubscriberMethod> findSubscriberMethods(Object subscriber) {
        List<SubscriberMethod> subscriberList = new ArrayList<>();
        Class<?> aClass = subscriber.getClass();
        // 遍历 subscriber -> BaseActivity ------> Activity
        while (aClass != null) {
            String className = aClass.getName();
            // 如果是系统类，则continue
            if (className.startsWith("java.") ||
                    className.startsWith("javax.") ||
                    className.startsWith("android.") ||
                    className.startsWith("androidx.")) {
                break;
            }

            // 取出当前类所有的定义方法
            Method[] declaredMethods = aClass.getDeclaredMethods();
            // 遍历所有方法，查找是否被Subscriber注解过
            for (Method method : declaredMethods) {
                Subscriber annotation = method.getAnnotation(Subscriber.class);
                if (annotation == null) {
                    continue;
                }

                // 检测注解的方法参数合法情况
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new RuntimeException("XEventBus只能接收一个参数");
                }

                // 符合规范的订阅方法，添加到list
                ThreadMode threadMode = annotation.threadMode();
                SubscriberMethod subscriberMethod = new SubscriberMethod(method, threadMode, parameterTypes[0]);
                subscriberList.add(subscriberMethod);
            }

            // 查完当前，继续查父类
            aClass = aClass.getSuperclass();
        }
        return subscriberList;
    }

    /**
     * 解绑注入对象
     *
     * @param subscriber 被观察对象 - 订阅者
     */
    public void unregister(Object subscriber) {
        List<SubscriberMethod> subscriberMethods = cacheMap.get(subscriber);
        if (subscriberMethods != null) {
            cacheMap.remove(subscriber);
        }
    }

    /**
     * 发送事件
     *
     * @param obj 事件对象
     */
    public void post(final Object obj) {
        // 遍历map缓存，在所有的订阅者中找到符合接收的订阅方法
        Set<Object> objectSet = cacheMap.keySet();
        Iterator<Object> iterator = objectSet.iterator();
        while (iterator.hasNext()) {
            // 拿到注册的订阅对象
            final Object next = iterator.next();
            // 获取类中所有添加注解的方法
            List<SubscriberMethod> methodList = cacheMap.get(next);
            if (methodList != null && methodList.size() > 0) {
                for (final SubscriberMethod subscriberMethod : methodList) {
                    // 找出命中的订阅方法
                    if (subscriberMethod.getParamType().isAssignableFrom(obj.getClass())) {
                        // 线程类型分发
                        switch (subscriberMethod.getThreadMode()) {
                            // 订阅方法在主线程中
                            case MAIN:
                                // 如果发送事件在主线程中执行
                                if (Looper.myLooper() == Looper.getMainLooper()) {
                                    invoke(subscriberMethod, next, obj);
                                } else {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            invoke(subscriberMethod, next, obj);
                                        }
                                    });
                                }
                                break;
                            // 订阅方法在子线程中
                            case ASYNC:
                                // 如果发送事件在主线程中执行
                                if (Looper.myLooper() == Looper.getMainLooper()) {
                                    // post在主线程执行
                                    threadPoolExecutor.submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            invoke(subscriberMethod, next, obj);
                                        }
                                    });
                                } else {
                                    // post 同样是在子线程
                                    invoke(subscriberMethod, next, obj);
                                }
                                break;
                            case POSTING:
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 方法委托执行
     *
     * @param subscriberMethod 订阅方法对象
     * @param next             当前订阅者
     * @param obj              发送事件对象
     */
    private void invoke(SubscriberMethod subscriberMethod, Object next, Object obj) {
        Method method = subscriberMethod.getMethod();
        try {
            method.invoke(next, obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}