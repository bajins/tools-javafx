package com.bajins.tools.toolsjavafx.utils;

import io.avaje.inject.BeanScope;

/**
 * 应用上下文类，用于管理依赖注入（DI）容器的初始化、Bean 获取和关闭。
 * <p>
 * 该类提供了初始化 DI 容器、获取 Bean 实例以及关闭容器的方法。
 * 它使用 Avaje Inject 库来管理 Bean 生命周期。
 * </p>
 * @author bajins
 */
public class AppContext {

    private static BeanScope scope;

    /**
     * 初始化方法：在程序启动时调用
     */
    public static void init() {
        if (scope == null) {
            scope = BeanScope.builder().build();
        }
    }

    /**
     * 获取 Bean 的通用方法
     *
     * @param type Bean 类型
     * @param <T>  Bean 类型参数
     * @return Bean 实例
     */
    public static <T> T get(Class<T> type) {
        if (scope == null) {
            throw new IllegalStateException("DI container not initialized! Call DI.init() first.");
        }
        return scope.get(type);
    }

    /**
     * 获取 BeanScope 对象
     *
     * @return BeanScope 实例
     */
    public static BeanScope getScope() {
        return scope;
    }

    /**
     * 关闭容器（程序退出时调用）
     */
    public static void close() {
        if (scope != null) {
            scope.close();
        }
    }
}
