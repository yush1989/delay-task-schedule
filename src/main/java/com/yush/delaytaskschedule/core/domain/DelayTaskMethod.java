package com.yush.delaytaskschedule.core.domain;

import lombok.Data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @description DelayTaskMethod,回调方法封装
 * @author yush
 * @date 2022/10/26
 */
@Data
public class DelayTaskMethod {

    private Object target;

    private Method executeMethod;

    public DelayTaskMethod(Object target,Method executeMethod){
        this.target = target;
        this.executeMethod = executeMethod;
    }

    public Object execute(Object param) throws InvocationTargetException, IllegalAccessException {
        int parameters = executeMethod.getParameterCount();
        if(parameters > 0){
            return executeMethod.invoke(target,param);
        }
        return executeMethod.invoke(target);
    }
}
