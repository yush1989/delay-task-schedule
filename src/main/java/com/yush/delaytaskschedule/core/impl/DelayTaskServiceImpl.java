package com.yush.delaytaskschedule.core.impl;

import com.yush.delaytaskschedule.constant.CommonConstant;
import com.yush.delaytaskschedule.core.DelayTaskService;
import com.yush.delaytaskschedule.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class DelayTaskServiceImpl implements DelayTaskService {

    private RedisUtils redisUtils;

    public DelayTaskServiceImpl(RedisUtils redisUtils){
        this.redisUtils = redisUtils;
    }

    @Override
    public Boolean addTask(String callBackMethod, Object value, LocalDateTime executeTime) {
        String key = CommonConstant.KEY_PREFIX.concat(callBackMethod);
        redisUtils.zAdd(key,value,executeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return true;
    }

    @Override
    public Boolean removeTask(String callBackMethod, Object value) {
        String key = CommonConstant.KEY_PREFIX.concat(callBackMethod);
        redisUtils.zRemove(key,value);
        return true;
    }
}
