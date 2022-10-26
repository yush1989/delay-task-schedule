package com.yush.delaytaskschedule.annotation;

import java.lang.annotation.*;

/**
 * @description DelayTaskHandler,延迟任务处理方法必须加上此注解才能被系统加载
 * @author yush
 * @date 2022/10/25
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DelayTaskHandler {

    /**
     * 回调方法的名字，这个需与新增延迟任务时添加的回调方法名一致
     */
    String value();
}
