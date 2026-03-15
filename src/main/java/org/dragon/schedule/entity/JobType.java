package org.dragon.schedule.entity;

/**
 * 任务类型枚举
 */
public enum JobType {
    /**
     * Spring Bean 名称
     */
    SPRING_BEAN,

    /**
     * 全限定类名
     */
    CLASS_NAME,

    /**
     * Groovy 脚本
     */
    GROOVY_SCRIPT,

    /**
     * HTTP 请求
     */
    HTTP_REQUEST
}
