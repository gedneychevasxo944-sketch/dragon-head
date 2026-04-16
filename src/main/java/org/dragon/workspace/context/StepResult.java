package org.dragon.workspace.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 执行结果
 *
 * <p>记录每个 Step 的输入、输出、执行状态和耗时。
 *
 * @author yijunw
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult {

    /**
     * Step 名称
     */
    private String stepName;

    /**
     * 执行输入
     */
    private Object input;

    /**
     * 执行输出
     */
    private Object output;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 是否等待响应（当输出是 demand/request 时为 true）
     */
    @Builder.Default
    private boolean waitingForResponse = false;
}
