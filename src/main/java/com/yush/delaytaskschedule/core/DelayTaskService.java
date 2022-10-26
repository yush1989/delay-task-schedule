package com.yush.delaytaskschedule.core;

import java.time.LocalDateTime;

public interface DelayTaskService {

    Boolean addTask(String callBackMethod, Object value, LocalDateTime executeTime);

    Boolean removeTask(String callBackMethod, Object value);
}
