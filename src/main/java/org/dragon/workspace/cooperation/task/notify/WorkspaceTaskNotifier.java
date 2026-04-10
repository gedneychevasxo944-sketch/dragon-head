package org.dragon.workspace.cooperation.task.notify;

import org.dragon.task.Task;

/**
 * 任务通知器接口
 * 负责将任务状态变化通过 IM 渠道推送出去
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceTaskNotifier {

    /**
     * 通知任务开始
     */
    void notifyStarted(Task task);

    /**
     * 通知任务进度
     */
    void notifyProgress(Task task, String progress);

    /**
     * 通知任务等待用户输入
     */
    void notifyQuestion(Task task, String question);

    /**
     * 通知任务等待中
     */
    void notifyWaiting(Task task, String reason);

    /**
     * 通知任务完成
     */
    void notifyCompleted(Task task);

    /**
     * 通知任务失败
     */
    void notifyFailed(Task task, String errorMessage);
}
