package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TZwFollow;
import com.fc.v2.model.auto.TZwFollowTouch;

/**
 * 供水户盯办 Service接口。
 *
 * <p>盯办只有系统能开、也只能系统结：开单/结单挂在异常上报/归档上（见 {@link ITZwAbnCaseService}），
 * 这里负责两件事——经办人为系统已开出的待办笔填结果（落名只读），以及跑一轮按<b>另算的盯办周期</b>
 * 给到期且上一笔已盯完的单开出下一笔待办。逾期仍待办的笔不开新的，留在账上算漏办。
 *
 * @author fuce
 * @date 2026-10-03
 */
public interface ITZwFollowService {

    /** 盯办单台账查询 */
    List<TZwFollow> selectFollowList(Wrapper<TZwFollow> wrapper);

    /** 按主键查盯办单 */
    TZwFollow selectFollowById(Long id);

    /** 某盯办单全部流水（笔序升序，含逾期待办） */
    List<TZwFollowTouch> listTouches(Long followId);

    /**
     * 经办人为待办笔填盯办结果（系统开出的笔才有得填；REACHED/NOT_REACHABLE/WRONG_CONTACT 三选一，
     * 必填情况与实际盯办时刻）。只准填本单当前那笔待办，已办笔一律不回改（只追加）。
     * 填妥同事务把头上一次盯办时刻、下次该盯时刻顺排。拒绝原因写 rejectMsg。
     */
    TZwFollowTouch fillTouch(Long touchId, String result, String detail, String actualAtText,
                             String operator, List<String> rejectMsg);

    /**
     * 跑一轮（定时任务/手动同一口径）：对在盯单，凡下次该盯时刻已到（含）且当前无待办笔的，
     * 按名录现取联系人开出下一笔待办；上一笔逾期未办的不开新笔（漏办留账）。返回新开口数。
     */
    int runCycle(Date at);
}
