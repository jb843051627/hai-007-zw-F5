package com.fc.v2.service.zw;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 复测环节链的全站一套口径（纯领域、无 Spring、无库——可直接单测）。
 *
 * <p>三环固定次序：0化验室检测 → 1水质科复核 → 2主管审定。
 * 环节0 填检测项目/数值/时间（空表起步，绝不预填）；环节1 必须照录环节0 三项；
 * 环节2 必须照录环节1 的复核结论。对不上判核对不符（留痕但不开下一环），
 * 结论为否决则整条链终止。系统按环节开通权限：每一环一个独立授权码。
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class RetestChain {

    private RetestChain() {
    }

    public static final int STEP_LAB = 0;
    public static final int STEP_WATER = 1;
    public static final int STEP_CHIEF = 2;
    public static final int STEP_DONE = 3;

    /** 复测单情形 */
    public static final int ORDER_RUNNING = 0;
    public static final int ORDER_PASSED = 1;
    public static final int ORDER_VETOED = 2;

    /** 复核结论 */
    public static final int RESULT_PASS = 1;
    public static final int RESULT_VETO = 2;

    /** 环节台账本笔效力 */
    public static final int ROW_MISMATCH = 0;
    public static final int ROW_EFFECTIVE = 1;

    /** 各环节的授权码（与 t_sys_permission.perms 逐字一致），系统按环节只开当前一环 */
    public static final String[] STEP_PERMS = {
            "zw:zwRetest:step0", "zw:zwRetest:step1", "zw:zwRetest:step2"};

    public static String permOfStep(int step) {
        return step >= 0 && step < STEP_PERMS.length ? STEP_PERMS[step] : null;
    }

    public static String stepName(int step) {
        switch (step) {
            case STEP_LAB:
                return "化验室检测";
            case STEP_WATER:
                return "水质科复核";
            case STEP_CHIEF:
                return "主管审定";
            case STEP_DONE:
                return "已走完全程";
            default:
                return "环节" + step;
        }
    }

    public static boolean validResult(Integer result) {
        return result != null
                && (result.intValue() == RESULT_PASS || result.intValue() == RESULT_VETO);
    }

    /**
     * 环节1 照录核对：检测项目逐字相同、数值同值（compareTo=0，不受小数位影响）、检测时间同一时刻。
     * 三项全等才算对得上；任一空着也算对不上（不预填、不许蒙混）。
     */
    public static boolean labMatches(String item, BigDecimal qty, Date testAt,
                                    String prevItem, BigDecimal prevQty, Date prevTestAt) {
        if (item == null || prevItem == null || qty == null || prevQty == null
                || testAt == null || prevTestAt == null) {
            return false;
        }
        return item.trim().equals(prevItem.trim())
                && qty.compareTo(prevQty) == 0
                && testAt.getTime() == prevTestAt.getTime();
    }

    /** 环节2 照录核对：上一环复核结论逐值相同 */
    public static boolean resultMatches(Integer prevResult, Integer enteredPrevResult) {
        return prevResult != null && enteredPrevResult != null
                && prevResult.intValue() == enteredPrevResult.intValue();
    }

    public static String resultText(Integer result) {
        if (result == null) {
            return "";
        }
        return result.intValue() == RESULT_PASS ? "通过" : "否决";
    }

    public static String orderStatusText(Integer st) {
        if (st == null) {
            return "";
        }
        switch (st.intValue()) {
            case ORDER_RUNNING:
                return "在办";
            case ORDER_PASSED:
                return "全程通过";
            case ORDER_VETOED:
                return "已否决终止";
            default:
                return String.valueOf(st);
        }
    }
}
