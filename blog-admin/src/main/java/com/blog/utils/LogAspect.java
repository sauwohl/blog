package com.blog.utils;


import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Arrays;

@Aspect
@Component
public class LogAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 拦截所有 controller 包下的方法
    @Pointcut("execution(* com.blog.controller..*(..))") // ← 改成你自己 Controller 的包路径
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        Object[] args = joinPoint.getArgs();

        logger.info("➡️ 请求 {}.{} 参数: {}", className, methodName, Arrays.toString(args));

        Object result = joinPoint.proceed();

        logger.info("⬅️ 响应 {}.{} 返回值: {}", className, methodName, result);

        return result;
    }
}