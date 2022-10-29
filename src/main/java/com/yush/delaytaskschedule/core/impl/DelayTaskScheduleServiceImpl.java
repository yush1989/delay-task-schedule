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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
public class DelayTaskScheduleServiceImpl implements DelayTaskScheduleService, ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext applicationContext;

    /**
     * 用来保存回调方法，key->回调方法名,value->回调方法
     */
    private ConcurrentHashMap<String, DelayTaskMethod> delayTaskBeanMap = new ConcurrentHashMap<>(16);

    /**
     * 用来保存redis中的任务key，防止重复使用set存放
     */
    private Set<String> redisKeySet = new CopyOnWriteArraySet<>();

    private RedisUtils redisUtils;

    private ThreadPoolTaskExecutor executor;

    public DelayTaskScheduleServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
        //初始化线程池
        initExecutor();
    }

    /**
     * 实例化并初始化线程池
     */
    private void initExecutor(){
        if(Objects.isNull(executor)){
            executor = new ThreadPoolTaskExecutor();
        }
        executor.initialize();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(100);
        executor.setKeepAliveSeconds(100);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("delay-task-schedule-thread-");
        //默认使用丢弃旧任务的拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @Override
    public void handleDelayTask() {
        //使用redis锁,防止重复使用改任务
        String key = CommonConstant.KEY_LOCK;
        log.info("开始处理延迟任务，handleDelayTask");
        while (true) {
            if(redisUtils.setNx(key,key,CommonConstant.EXPIRE)){
                try {
                    redisKeySet.parallelStream().forEach(delayKey -> {
                        //将任务放入线程池中运行
                        CompletableFuture.runAsync(() -> {handleTask(delayKey);},executor);
                        //30秒以内续期
                        if (redisUtils.getExpire(key) <= 30) {
                            redisUtils.expire(key, CommonConstant.EXPIRE);
                        }
                    });
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
        initKeySet();
    }

    /**
     * 处理单个key任务
     * @param key
     */
    private void handleTask(String key){
        while(true){
            String[] keyArr = StringUtils.split(key,"_");
            //循环拿取redis zset中的数据,每次取10条数据处理，并按照时间消费数据
            Set<ZSetOperations.TypedTuple<Object>> result = redisUtils.zRankWithScore(key, 0, 9);
            result.stream().forEach(set -> {
                long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                //如果已经过了时限则需将订单取消
                if (new Double(set.getScore()).longValue() <= timestamp) {
                    //删除对应value
                    redisUtils.zRemove(key, set.getValue());
                    DelayTaskMethod method = delayTaskBeanMap.get(keyArr[1]);
                    if(Objects.isNull(method)){
                        log.error("找不到回调方法-{}",keyArr[1]);
                    }else{
                        try {
                            log.info("开始执行延迟任务方法，method->{}",method);
                            method.execute(set.getValue());
                        } catch (Exception e) {
                            log.error("处理延迟任务失败,系统将在30秒后再次执行，msg->{}",e.getMessage(),e);
                            redisUtils.zAdd(key,set.getValue(),
                                    new Double(LocalDateTime.now().plusSeconds(30)
                                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
                        }
                    }
                }
            });
        }
    }

    /**
     * redis中的任务key缓存
     */
    private void initKeySet(){
        ConcurrentHashMap.KeySetView<String,DelayTaskMethod> keySetView = delayTaskBeanMap.keySet();
        if(!CollectionUtils.isEmpty(keySetView)){
            redisKeySet.addAll(keySetView.stream().map(key -> CommonConstant.KEY_PREFIX.concat(key)).collect(Collectors.toSet()));
        }
    }
}
