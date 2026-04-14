package org.dragon.workspace.task.util;

import java.util.List;

import org.dragon.workspace.member.WorkspaceMember;

/**
 * 成员相关工具方法
 *
 * @author wyj
 * @version 1.0
 */
public final class MemberUtils {

    private MemberUtils() {
    }

    /**
     * 构建成员描述信息
     *
     * @param m 成员
     * @return 描述字符串，格式：角色: xxx | 层级: xxx
     */
    public static String buildMemberDescription(WorkspaceMember m) {
        StringBuilder sb = new StringBuilder();
        if (m.getRole() != null) {
            sb.append("角色: ").append(m.getRole());
        }
        if (m.getLayer() != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("层级: ").append(m.getLayer());
        }
        return sb.toString();
    }

    /**
     * 构建成员描述信息列表
     *
     * @param members 成员列表
     * @return 描述字符串列表
     */
    public static List<String> buildMemberDescriptions(List<WorkspaceMember> members) {
        return members.stream()
                .map(MemberUtils::buildMemberDescription)
                .toList();
    }
}
