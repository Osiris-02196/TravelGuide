package com.oxiris.travelguide.aop;

import com.oxiris.travelguide.annotation.AuthCheck;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.BusinessException;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.enums.UserRoleEnum;
import com.oxiris.travelguide.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String[] mustRoles = authCheck.mustRole();
        // 如果没有指定角色，放行
        if (mustRoles == null || mustRoles.length == 0) {
            return joinPoint.proceed();
        }

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        String userRole = loginUser.getUserRole();

        // 检查用户角色是否在允许的角色列表中
        Set<String> allowedRoles = Arrays.stream(mustRoles).collect(Collectors.toSet());
        if (!allowedRoles.contains(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 权限校验通过，放行
        return joinPoint.proceed();
    }
}
