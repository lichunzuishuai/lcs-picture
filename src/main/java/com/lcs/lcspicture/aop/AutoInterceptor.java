package com.lcs.lcspicture.aop;

import com.lcs.lcspicture.annotation.AutoCheck;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.UserRoleEnum;
import com.lcs.lcspicture.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验拦截器
 */
@Aspect
@Component
public class AutoInterceptor {
    @Resource
    private UserService userService;

    //1.定义切点
    @Around("@annotation(autoCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AutoCheck autoCheck) throws Throwable {
        String mustRole = autoCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //如果不需要全权限放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //必须有权限才会通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求必须有管理员权限,但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
