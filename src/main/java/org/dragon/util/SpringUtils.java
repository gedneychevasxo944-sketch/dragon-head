package org.dragon.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring 容器工具类
 * 提供从 Spring 容器获取 Bean 的静态方法
 */
@Component
public class SpringUtils {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
    }

    /**
     * 根据类型获取 Bean
     *
     * @param beanClass Bean 类
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * 根据名称和类型获取 Bean
     *
     * @param name      Bean 名称
     * @param beanClass Bean 类
     * @return Bean 实例
     */
    public static <T> T getBean(String name, Class<T> beanClass) {
        return applicationContext.getBean(name, beanClass);
    }

    /**
     * 根据名称获取 Bean
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }
}
