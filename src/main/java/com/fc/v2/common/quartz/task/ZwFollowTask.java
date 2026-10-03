package com.fc.v2.common.quartz.task;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fc.v2.service.ITZwFollowService;

/**
 * 供水户盯办定时任务：调度配置里调用目标填 {@code zwFollowTask.runCycle()}。
 * 与页面"跑一轮"同一个服务口径——按另算的盯办周期系统开笔，漏办不开新笔。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Component("zwFollowTask")
public class ZwFollowTask {

    @Autowired
    private ITZwFollowService followService;

    /** 按盯办周期开下一笔待办（无参，供 quartz invoke_target 调用） */
    public void runCycle() {
        int opened = followService.runCycle(new Date());
        System.out.println("[zwFollowTask] 本轮开出盯办待办 " + opened + " 笔，时刻 " + new Date());
    }
}
