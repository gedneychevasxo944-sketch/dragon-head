package org.dragon.workspace.task;

import org.dragon.task.Task;

/**
 * 任务通知器接口
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceTaskNotifier {

    /**
     * 任务开始执行通知
     */
    void notifyStarted(Task task);

    /**
     * 任务进度通知
     */
    void notifyProgress(Task task, String message);

    /**
     * 任务完成通知
     */
    void notifyCompleted(Task task);

    /**
     * 任务失败通知
     */
    void notifyFailed(Task task, String errorMessage);

    /**
     * 任务等待通知
     */
    void notifyWaiting(Task task, String reason);

    /**
     * 任务询问用户通知
     */
    void notifyQuestion(Task task, String question);
}
