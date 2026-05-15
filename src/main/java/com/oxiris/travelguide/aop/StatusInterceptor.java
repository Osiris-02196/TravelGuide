package com.oxiris.travelguide.aop;

import com.oxiris.travelguide.annotation.StatusCheck;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.BusinessException;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.enums.UserStatusEnum;
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
public class StatusInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行用户状态拦截
     */
    @Around("@annotation(statusCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, StatusCheck statusCheck) throws Throwable {
        // 获取允许的状态集合
        String[] allowedStatus = statusCheck.allowedStatus();
        Set<String> allowedStatusSet = Arrays.stream(allowedStatus).collect(Collectors.toSet());

        // 获取当前登录用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);

        // 用户状态（转成 String 与注解的 String[] 比较）
        String userStatusValue = String.valueOf(loginUser.getUserStatus());

        // 如果允许状态为空，则放行
        if (allowedStatusSet.isEmpty()) {
            return joinPoint.proceed();
        }

        // 如果用户状态不在允许范围内，则拒绝
        if (!allowedStatusSet.contains(userStatusValue)) {
            UserStatusEnum currentState = UserStatusEnum.getEnumByValue(Integer.valueOf(userStatusValue));
            if (currentState == UserStatusEnum.MUTED) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁言，无法执行此操作");
            } else if (currentState == UserStatusEnum.BANNED) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁，无法执行此操作");
            } else {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前用户状态不允许执行此操作");
            }
        }

        // 状态校验通过，放行
        return joinPoint.proceed();
    }
}
