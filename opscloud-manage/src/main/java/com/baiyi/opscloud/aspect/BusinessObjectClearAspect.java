package com.baiyi.opscloud.aspect;

import com.baiyi.opscloud.common.exception.common.CommonRuntimeException;
import com.baiyi.opscloud.domain.annotation.BusinessObjectClear;
import com.baiyi.opscloud.domain.annotation.BusinessType;
import com.baiyi.opscloud.domain.constants.BusinessTypeEnum;
import com.baiyi.opscloud.service.business.BusinessDocumentService;
import com.baiyi.opscloud.service.business.BusinessPropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 清理业务对象属性
 *
 * @Author baiyi
 * @Date 2021/8/25 3:27 下午
 * @Version 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BusinessObjectClearAspect {

    private final BusinessPropertyService businessPropertyService;

    private final BusinessDocumentService businessDocumentService;

    @Pointcut(value = "@annotation(com.baiyi.opscloud.domain.annotation.BusinessObjectClear)")
    public void annotationPoint() {
    }

    @Around("@annotation(businessObjectClear)")
    public Object around(ProceedingJoinPoint joinPoint, BusinessObjectClear businessObjectClear) throws CommonRuntimeException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] params = methodSignature.getParameterNames();// 获取参数名称
        Object[] args = joinPoint.getArgs();// 获取参数值
        if (params != null && params.length != 0) {
            Integer businessId = Integer.valueOf(args[0].toString());
            if (businessObjectClear.value() == BusinessTypeEnum.COMMON) {
                // 通过@BusinessType 获取业务类型
                if (joinPoint.getTarget().getClass().isAnnotationPresent(BusinessType.class)) {
                    BusinessType businessType = joinPoint.getTarget().getClass().getAnnotation(BusinessType.class);
                    doClear(businessType.value().getType(), businessId);
                }
            } else {
                doClear(businessObjectClear.value().getType(), businessId);
            }
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new CommonRuntimeException(e.getMessage());
        }
    }

    private void doClear(Integer businessType, Integer businessId) {
        log.info("清除业务属性: businessType = {} , businessId = {}", businessType, businessId);
        businessPropertyService.deleteByUniqueKey(businessType, businessId);
        log.info("清除业务文档: businessType = {} , businessId = {}", businessType, businessId);
        businessDocumentService.deleteByUniqueKey(businessType, businessId);
    }

}
