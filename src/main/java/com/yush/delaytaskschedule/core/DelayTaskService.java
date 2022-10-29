package com.yush.delaytaskschedule.core;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public interface DelayTaskService {

    Boolean addTask(String callBackMethod, Object value, LocalDateTime executeTime);

    Boolean addTask(String callBackMethod, Object value, Long delayCount, ChronoUnit timeUnit);

    Boolean removeTask(String callBackMethod, Object value);
}
