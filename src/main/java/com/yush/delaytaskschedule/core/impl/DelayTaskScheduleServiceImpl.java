package com.yush.delaytaskschedule.core.impl;

import com.yush.delaytaskschedule.annotation.DelayTaskHandler;
import com.yush.delaytaskschedule.constant.CommonConstant;
import com.yush.delaytaskschedule.core.DelayTaskScheduleService;
import com.yush.delaytaskschedule.core.domain.DelayTaskMethod;
import com.yush.delaytaskschedule.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DelayTaskScheduleServiceImpl implements DelayTaskScheduleService, ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext applicationContext;

    private ConcurrentHashMap<String, DelayTaskMethod> delayTaskBeanMap = new ConcurrentHashMap<>(16);

    private RedisUtils redisUtils;

    public DelayTaskScheduleServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    public void handleDelayTask() {
        //使用redis锁,防止重复使用改任务
        String key = CommonConstant.KEY_LOCK;
        log.info("开始处理延迟任务，handleDelayTask");
        while (true) {
            if(redisUtils.setNx(key,key,CommonConstant.EXPIRE)){
                try {
                    //循环拿取redis zset中的数据,每次取10条数据处理，并按照时间消费数据
                    while (true) {
                        Set<String> keys = redisUtils.keys(CommonConstant.KEY_PREFIX.concat("*"));
                        if(!CollectionUtils.isEmpty(keys)){
                            keys.parallelStream().forEach(delayKey -> {
                                String[] keyArr = StringUtils.split(delayKey,"_");
                                Set<ZSetOperations.TypedTuple<Object>> result = redisUtils.zRankWithScore(delayKey, 0, 9);
                                result.stream().forEach(set -> {
                                    long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                                    //如果已经过了时限则需将订单取消
                                    if (new Double(set.getScore()).longValue() <= timestamp) {
                                        //删除对应value
                                        redisUtils.zRemove(delayKey, set.getValue());
                                        DelayTaskMethod method = delayTaskBeanMap.get(keyArr[1]);
                                        try {
                                            log.info("开始执行延迟任务方法，method->{}",method);
                                            method.execute(set.getValue());
                                        } catch (Exception e) {
                                            log.error("处理延迟任务失败,系统将在30秒后再次执行，msg->{}",e.getMessage(),e);
                                            redisUtils.zAdd(delayKey,set.getValue(),
                                                    new Double(LocalDateTime.now().plusSeconds(30)
                                                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
                                        }
                                    }
                                });
                            });
                        }
                        //30秒以内续期
                        if (redisUtils.getExpire(key) <= 30) {
                            redisUtils.expire(key, CommonConstant.EXPIRE);
                        }
                    }
                } catch (Exception e) {
                    redisUtils.remove(key);
                    log.error("处理延时任务出错,msg->{}",e.getMessage());
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] beans = applicationContext.getBeanNamesForType(Object.class,false,true);
        Optional.ofNullable(beans).ifPresent(arr -> {
            for(String name : arr){
                Method[] methods = applicationContext.getBean(name).getClass().getDeclaredMethods();
                for(Method method : methods){
                    method.setAccessible(true);
                    DelayTaskHandler delayTaskHandler = method.getAnnotation(DelayTaskHandler.class);
                    if(Objects.nonNull(delayTaskHandler)){
                        delayTaskBeanMap.put(delayTaskHandler.value(),new DelayTaskMethod(applicationContext.getBean(name),method));
                    }
                }
            }
            log.info("延迟任务回调方法加载成功，delayTaskBeanMap->{}",delayTaskBeanMap);
        });
    }
}
