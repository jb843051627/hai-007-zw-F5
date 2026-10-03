package com.fc.v2.service.zw;

import java.util.Calendar;
import java.util.Date;

/**
 * 供水户盯办周期的全站一把尺（纯领域、无 Spring、无库——可直接单测）。
 *
 * <p>盯办周期与采样周期、复测时限各管各的：这里只认盯办单上自己的 period_days。
 * 下一次该盯的时刻从上一次<b>实际盯办</b>时刻顺排（联系人没联系上也照排，不许拖），
 * 首笔从开单时刻起算。
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class FollowCycle {

    private FollowCycle() {
    }

    /** 默认盯办周期（自然日），名录/底档没另给时用它 */
    public static final int DEFAULT_PERIOD_DAYS = 7;

    /** 盯办流水结果 */
    public static final String RES_REACHED = "REACHED";
    public static final String RES_NOT_REACHABLE = "NOT_REACHABLE";
    public static final String RES_WRONG_CONTACT = "WRONG_CONTACT";

    /** 流水笔情形 */
    public static final int TOUCH_PENDING = 0;
    public static final int TOUCH_DONE = 1;

    /** 盯办单情形 */
    public static final int FOLLOW_OPEN = 0;
    public static final int FOLLOW_CLOSED = 1;

    /** 从上一次实际盯办时刻顺排一个周期；没有上一次（首笔）从开单时刻起算 */
    public static Date nextDueAfter(Date lastTouchAt, int periodDays) {
        Date base = lastTouchAt != null ? lastTouchAt : new Date();
        int days = periodDays > 0 ? periodDays : DEFAULT_PERIOD_DAYS;
        Calendar c = Calendar.getInstance();
        c.setTime(base);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    /** 到点判定：at 越过（含）该笔时限就该开下一笔 */
    public static boolean due(Date nextDueAt, Date at) {
        return nextDueAt != null && !at.before(nextDueAt);
    }

    public static boolean validResult(String result) {
        return RES_REACHED.equals(result) || RES_NOT_REACHABLE.equals(result)
                || RES_WRONG_CONTACT.equals(result);
    }

    public static String resultText(String result) {
        if (result == null) {
            return "";
        }
        switch (result) {
            case RES_REACHED:
                return "已联系上";
            case RES_NOT_REACHABLE:
                return "联系不上";
            case RES_WRONG_CONTACT:
                return "联系人有误";
            default:
                return result;
        }
    }

    public static String statusText(Integer st) {
        if (st == null) {
            return "";
        }
        return st.intValue() == FOLLOW_CLOSED ? "已结" : "在盯";
    }
}
