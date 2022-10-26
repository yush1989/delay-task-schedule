package com.yush.delaytaskschedule.config;

import com.yush.delaytaskschedule.core.DelayTaskScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class DelayTaskLauncher implements ApplicationRunner {

    @Autowired
    private DelayTaskScheduleService delayTaskScheduleService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("初始化执行延时任务处理bean");
        CompletableFuture.runAsync(() -> {delayTaskScheduleService.handleDelayTask();});
    }
}
