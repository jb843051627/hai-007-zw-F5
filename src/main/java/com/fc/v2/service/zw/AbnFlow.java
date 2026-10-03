package com.fc.v2.service.zw;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fc.v2.model.auto.TZwLimRule;

/**
 * 水质异常判定留痕的全站一套口径（纯领域、无 Spring、无库——可直接单测）。
 *
 * <p>台账情形、过程动作、单号样式、档位到界值/默认建议的映射全钉在这里，
 * 服务层与页面不许各写各的简称。
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class AbnFlow {

    private AbnFlow() {
    }

    /** 台账情形 */
    public static final int ST_REGISTERED = 0;
    public static final int ST_REPORTED = 1;
    public static final int ST_ARCHIVED = 2;

    /** 过程动作（t_zw_abn_trace.action） */
    public static final String ACT_REGISTER = "REGISTER";
    public static final String ACT_CORRECT = "CORRECT";
    public static final String ACT_REPORT = "REPORT";
    public static final String ACT_RETEST_OPEN = "RETEST_OPEN";
    public static final String ACT_ARCHIVE = "ARCHIVE";

    /** 系统自动动作的经办人落名 */
    public static final String SYSTEM = "system";

    /** 单号样式：YC + 检测日(yyyyMMdd) + - + 当日三位序；复测/盯办单在异常单号后挂后缀 */
    public static String caseNo(Date testDate, int seqOfDay) {
        return "YC" + new SimpleDateFormat("yyyyMMdd").format(testDate) + "-" + String.format("%03d", seqOfDay);
    }

    public static String retestNo(String caseNo) {
        return caseNo + "-HC";
    }

    public static String followNo(String caseNo) {
        return caseNo + "-DB";
    }

    /**
     * 该档位对照的那条界值：关注→th1、超标→th2、严重超标→th3；
     * 界值留空（该档不设）时向下取最近一个在设界值，取不到返回 null（无界值则无"超量"可算）。
     */
    public static BigDecimal thresholdOf(TZwLimRule rule, int grade) {
        if (rule == null) {
            return null;
        }
        if (grade == WaterGrade.SEVERE) {
            return rule.getTh3Max() != null ? rule.getTh3Max()
                    : (rule.getTh2Max() != null ? rule.getTh2Max() : rule.getTh1Max());
        }
        if (grade == WaterGrade.OVER) {
            return rule.getTh2Max() != null ? rule.getTh2Max() : rule.getTh1Max();
        }
        if (grade == WaterGrade.WATCH) {
            return rule.getTh1Max();
        }
        return null;
    }

    /** 超量＝检测值−对照界值（界值为空算不出，回 null；登记时算死，之后标准让位也不重算） */
    public static BigDecimal overQty(BigDecimal qty, BigDecimal threshold) {
        if (qty == null || threshold == null) {
            return null;
        }
        return qty.subtract(threshold);
    }

    /** 档位默认的停水/复测建议（上报前仅此一次可人工订正）：关注只盯着，超标安排复测，严重超标停水+复测 */
    public static int[] defaultAdvice(int grade) {
        if (grade == WaterGrade.SEVERE) {
            return new int[]{1, 1};
        }
        if (grade == WaterGrade.OVER) {
            return new int[]{0, 1};
        }
        return new int[]{0, 0};
    }

    public static String statusText(Integer st) {
        if (st == null) {
            return "";
        }
        switch (st.intValue()) {
            case ST_REGISTERED:
                return "已登记·未上报";
            case ST_REPORTED:
                return "已上报·只读";
            case ST_ARCHIVED:
                return "已归档结案";
            default:
                return String.valueOf(st);
        }
    }

    public static String actionText(String action) {
        if (action == null) {
            return "";
        }
        switch (action) {
            case ACT_REGISTER:
                return "自动登记";
            case ACT_CORRECT:
                return "判定订正";
            case ACT_REPORT:
                return "异常上报";
            case ACT_RETEST_OPEN:
                return "复测开单";
            case ACT_ARCHIVE:
                return "归档结案";
            default:
                return action;
        }
    }
}
