package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TZwAbnCase;
import com.fc.v2.model.auto.TZwAbnTrace;
import com.fc.v2.model.auto.TZwImpRow;
import com.fc.v2.model.auto.TZwRetestOrder;
import com.fc.v2.model.auto.TZwRetestStep;

/**
 * 水质异常判定留痕 Service接口。
 *
 * <p>主线：<b>异常判出系统自动登记 → 上报前订正一次 → 上报（唯一一单、报后只读，
 * 同刻自开复测单与供水户盯办单）→ 复测按环节链逐步复录 → 末环通过系统归档、同刻结盯办</b>。
 * 登记台账与过程流水只追加；订正、上报、归档全部走带"当前情形"条件的原子更新，
 * 登记/上报/归档并发被改时，以归档那一次写下的为准（0 行即拒、整笔回滚）。
 *
 * @author fuce
 * @date 2026-10-03
 */
public interface ITZwAbnCaseService {

    /** 异常台账分页/条件查询 */
    List<TZwAbnCase> selectCaseList(Wrapper<TZwAbnCase> wrapper);

    /** 按主键查异常单 */
    TZwAbnCase selectCaseById(Long id);

    /** 过程流水（次序升序，倒查时从尾往头看） */
    List<TZwAbnTrace> listTrace(Long caseId);

    /** 点回去看当日原始检测记录（无则 null） */
    TZwImpRow sourceRowOf(TZwAbnCase ac);

    /** 该异常单的复测单（无则 null） */
    TZwRetestOrder retestOf(Long caseId);

    /** 复测单全部环节台账（环节、次序升序；含核对不符留痕笔） */
    List<TZwRetestStep> listSteps(Long orderId);

    /**
     * 异常一判出来系统自动登记（由检测数据导入在正式账落账后逐行调用，页面无新增口）。
     * 仅正式账（status=1）且定级为关注/超标/严重超标的行登记；同点位同日同指标已登记则幂等跳过。
     * 判定三件套、对照界值、超量当场从唯一评价出口的规则条文算死并落 REGISTER 流水。
     *
     * @return 新建的登记单；幂等跳过/不参评返回 null
     */
    TZwAbnCase registerFromRow(TZwImpRow row);

    /**
     * 上报前唯一一次订正：只准改判定档位(2/3/4)与停水/复测建议、说明；
     * 点位/哪天的水/指标/检测值/标准快照一根不能动。已订正过、已上报、已归档一律拒。
     * 订正落 CORRECT 流水，记清前后值。
     */
    boolean correct(Long caseId, Integer gradeLevel, Integer needWaterStop, Integer needRetest,
                   String note, String operator);

    /**
     * 异常上报（同一笔异常只准一次）：条件更新把台账从"已登记"翻到"已上报"，0 行即拒。
     * 同事务：落 REPORT 流水；系统自开复测单（RETEST_OPEN 流水）；反映到供水户名下、自开盯办单与首笔待办。
     */
    TZwAbnCase report(Long caseId, String operator);

    /** 与 {@link #report(Long, String)} 同，拒绝原因写进 rejectMsg 单元素出口 */
    TZwAbnCase report(Long caseId, String operator, List<String> rejectMsg);

    /**
     * 复测环节提交（系统按环节开权限：调用方须已持本环节授权码，{@code stepNo} 与授权不匹配即拒）。
     * 上一环未完成（单当前环节不等于本环）或上一环结论为否决（单已终态）不开本环。
     * <ul>
     *   <li>环节0：检测项目/数值/时间由本环节填，空值拒（不预填）；</li>
     *   <li>环节1：照录环节0 三项+复核结论，对不上原样留痕(status=0)不开下一环，否决即终止；</li>
     *   <li>环节2：照录环节1 结论+审定结论，对不上留痕，否决终止，通过则系统归档异常并结盯办。</li>
     * </ul>
     * 每次提交 attempt 递增只追加；有效落名后该环对所有人只读。拒绝原因写 rejectMsg 单元素出口。
     */
    TZwRetestStep submitStep(Long orderId, int stepNo, String item, String qtyText, String testAtText,
                             Integer enteredPrevResult, Integer stepResult, String opinion,
                             String operator, List<String> rejectMsg);
}
