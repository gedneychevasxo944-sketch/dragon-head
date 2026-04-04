package org.dragon.permission.security;

import org.aopalliance.intercept.MethodInvocation;
import org.dragon.permission.service.PermissionService;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * PermissionServiceExpressionHandler 自定义 Method Security Expression Handler
 * 将 PermissionService 方法注入 SpEL，使 @PreAuthorize 可调用 canView/canEdit 等方法
 */
public class PermissionServiceExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private PermissionService permissionService;

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        PermissionMethodSecurityExpression expression =
                new PermissionMethodSecurityExpression();
        expression.setPermissionService(permissionService);
        expression.setAuthentication(authentication);
        return expression;
    }
}
