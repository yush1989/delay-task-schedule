package com.yush.delaytaskschedule.core.impl;

import com.yush.delaytaskschedule.constant.CommonConstant;
import com.yush.delaytaskschedule.core.DelayTaskService;
import com.yush.delaytaskschedule.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Slf4j
public class DelayTaskServiceImpl implements DelayTaskService {

    private RedisUtils redisUtils;

    public DelayTaskServiceImpl(RedisUtils redisUtils){
        this.redisUtils = redisUtils;
    }

    /**
     * 传入执行时间
     * @param callBackMethod
     * @param value
     * @param executeTime
     * @return
     */
    @Override
    public Boolean addTask(String callBackMethod, Object value, LocalDateTime executeTime) {
        Assert.hasLength(callBackMethod,"延迟任务回调方法名不能为空，请检查方法名是否正确");
        Assert.isNull(value,"延迟任务主键值不能为空，请检查value");
        Assert.hasLength(value.toString(),"延迟任务主键值不能为空，请检查value");
        Assert.isNull(executeTime,"任务执行时间不能为空，请检查时间是否正确");
        Assert.isTrue(executeTime.isAfter(LocalDateTime.now()),"任务执行时间不能小于当前时间，请检查设置的任务执行时间是否正确");
        String key = CommonConstant.KEY_PREFIX.concat(callBackMethod);
        redisUtils.zAdd(key,value,executeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return true;
    }

    /**
     * 终止延迟任务
     * @param callBackMethod
     * @param value
     * @return
     */
    @Override
    public Boolean removeTask(String callBackMethod, Object value) {
        Assert.hasLength(callBackMethod,"延迟任务回调方法名不能为空，请检查方法名是否正确");
        Assert.isNull(value,"延迟任务主键值不能为空，请检查value");
        Assert.hasLength(value.toString(),"延迟任务主键值不能为空，请检查value");
        String key = CommonConstant.KEY_PREFIX.concat(callBackMethod);
        redisUtils.zRemove(key,value);
        return true;
    }

    /**
     * 传入延迟时间和时间单位
     * @param callBackMethod
     * @param value
     * @param delayCount
     * @param timeUnit
     * @return
     */
    @Override
    public Boolean addTask(String callBackMethod, Object value, Long delayCount, ChronoUnit timeUnit){
        Assert.isTrue(delayCount > 0,"延迟时间不能为负数");
        return addTask(callBackMethod,value,LocalDateTime.now().plus(delayCount,timeUnit));
    }
}
