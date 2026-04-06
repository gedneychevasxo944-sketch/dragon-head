package org.dragon.memory.core.query;

/**
 * 查询处理器接口
 *
 * @param <Q> 查询类型
 * @param <R> 返回值类型
 * @author binarytom
 * @version 1.0
 */
public interface QueryHandler<Q, R> {
    R handle(Q query);
}
