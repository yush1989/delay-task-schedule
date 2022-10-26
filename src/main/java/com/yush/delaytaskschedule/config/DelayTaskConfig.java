package com.yush.delaytaskschedule.config;

import com.yush.delaytaskschedule.core.DelayTaskScheduleService;
import com.yush.delaytaskschedule.core.DelayTaskService;
import com.yush.delaytaskschedule.core.impl.DelayTaskScheduleServiceImpl;
import com.yush.delaytaskschedule.core.impl.DelayTaskServiceImpl;
import com.yush.delaytaskschedule.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class DelayTaskConfig {

    @Bean
    public DelayTaskService delayTaskService(RedisUtils redisUtils){
        log.info("初始化延时任务......redisUtils -> {}",redisUtils);
        return new DelayTaskServiceImpl(redisUtils);
    }

    @Bean
    public DelayTaskScheduleService delayTaskScheduleService(RedisUtils redisUtils){
        log.info("初始化自动处理延时任务bean。。");
        return new DelayTaskScheduleServiceImpl(redisUtils);
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public DelayTaskLauncher delayTaskLauncher(){
        return new DelayTaskLauncher();
    }
}
