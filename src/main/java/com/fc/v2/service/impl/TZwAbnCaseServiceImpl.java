package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TZwAbnCaseMapper;
import com.fc.v2.mapper.auto.TZwAbnTraceMapper;
import com.fc.v2.mapper.auto.TZwFollowMapper;
import com.fc.v2.mapper.auto.TZwFollowTouchMapper;
import com.fc.v2.mapper.auto.TZwImpRowMapper;
import com.fc.v2.mapper.auto.TZwRetestOrderMapper;
import com.fc.v2.mapper.auto.TZwRetestStepMapper;
import com.fc.v2.mapper.auto.TZwWaterUserMapper;
import com.fc.v2.model.auto.TZwAbnCase;
import com.fc.v2.model.auto.TZwAbnTrace;
import com.fc.v2.model.auto.TZwFollow;
import com.fc.v2.model.auto.TZwFollowTouch;
import com.fc.v2.model.auto.TZwImpRow;
import com.fc.v2.model.auto.TZwLimRule;
import com.fc.v2.model.auto.TZwRetestOrder;
import com.fc.v2.model.auto.TZwRetestStep;
import com.fc.v2.model.auto.TZwWaterUser;
import com.fc.v2.service.ITZwAbnCaseService;
import com.fc.v2.service.ITZwLimRuleService;
import com.fc.v2.service.zw.AbnFlow;
import com.fc.v2.service.zw.FollowCycle;
import com.fc.v2.service.zw.RetestChain;
import com.fc.v2.service.zw.SamDateRule;
import com.fc.v2.service.zw.WaterGrade;

/**
 * 水质异常判定留痕 Service业务层处理。
 *
 * <p><b>判定过程全程留在库里</b>：登记台账 {@link TZwAbnCase} 记一锤定音的判定快照，
 * 过程流水 {@link TZwAbnTrace} 只追加记每一步，任何人从最后一步排回第一步都能与经办屏对上。
 *
 * <p><b>写口一律条件更新</b>：订正（0态且未订正）、上报（0→1）、归档（1→2）都带"当前情形"条件，
 * 登记/上报/归档同时被改时，先落地的占坑、后到的 0 行被拒并整笔回滚——归档那一次写下的为准，
 * 系统日志同刻翻得到是哪一次写的。台账没有删除/撤回/改名口，页面也不设任何旁路。
 *
 * <p><b>上报是系统联动的总开关</b>：一笔异常只准上报一次（uk 点位+检测日+指标 + 0→1 条件双保险），
 * 同一事务内自开复测单与供水户盯办单；复测三环逐步复录，末环通过系统归档异常、同刻自结盯办，
 * 否决则链终止、台账停在"已上报"只读态。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Service
public class TZwAbnCaseServiceImpl implements ITZwAbnCaseService {

    private static final String TS_FMT = "yyyy-MM-dd HH:mm:ss";

    @javax.annotation.Resource
    private TZwAbnCaseMapper abnCaseMapper;
    @javax.annotation.Resource
    private TZwAbnTraceMapper abnTraceMapper;
    @javax.annotation.Resource
    private TZwRetestOrderMapper retestOrderMapper;
    @javax.annotation.Resource
    private TZwRetestStepMapper retestStepMapper;
    @javax.annotation.Resource
    private TZwFollowMapper followMapper;
    @javax.annotation.Resource
    private TZwFollowTouchMapper followTouchMapper;
    @javax.annotation.Resource
    private TZwWaterUserMapper waterUserMapper;
    @javax.annotation.Resource
    private TZwImpRowMapper impRowMapper;
    @javax.annotation.Resource
    private ITZwLimRuleService limRuleService;

    // ---------------------------------------------------------------- 查询

    @Override
    public List<TZwAbnCase> selectCaseList(Wrapper<TZwAbnCase> wrapper) {
        return abnCaseMapper.selectList(wrapper);
    }

    @Override
    public TZwAbnCase selectCaseById(Long id) {
        return abnCaseMapper.selectById(id);
    }

    @Override
    public List<TZwAbnTrace> listTrace(Long caseId) {
        return abnTraceMapper.selectList(new QueryWrapper<TZwAbnTrace>()
                .eq("case_id", caseId).orderByAsc("seq"));
    }

    @Override
    public TZwImpRow sourceRowOf(TZwAbnCase ac) {
        if (ac == null || ac.getImpRowId() == null) {
            return null;
        }
        return impRowMapper.selectById(ac.getImpRowId());
    }

    @Override
    public TZwRetestOrder retestOf(Long caseId) {
        return retestOrderMapper.selectOne(new QueryWrapper<TZwRetestOrder>()
                .eq("case_id", caseId).eq("del_flag", 0));
    }

    @Override
    public List<TZwRetestStep> listSteps(Long orderId) {
        return retestStepMapper.selectList(new QueryWrapper<TZwRetestStep>()
                .eq("order_id", orderId).eq("del_flag", 0)
                .orderByAsc("step_no").orderByAsc("attempt_no"));
    }

    // ---------------------------------------------------------------- 自动登记

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TZwAbnCase registerFromRow(TZwImpRow row) {
        if (row == null || row.getId() == null) {
            return null;
        }
        // 只认正式账（整批拦在库外的异常行走的是异常确认，不在此登记）
        if (row.getStatus() == null || row.getStatus().intValue() != 1) {
            return null;
        }
        Integer grade = row.getGradeLevel();
        // 只有关注/超标/严重超标才算异常；合格、未定级不登记
        if (grade == null || grade.intValue() < WaterGrade.WATCH || grade.intValue() > WaterGrade.SEVERE) {
            return null;
        }
        if (row.getTestAt() == null || isEmpty(row.getSiteNo()) || isEmpty(row.getItemCode())) {
            return null;
        }
        Date testDate = SamDateRule.startOfDay(row.getTestAt());
        TZwAbnCase exists = abnCaseMapper.selectOne(new QueryWrapper<TZwAbnCase>()
                .eq("site_no", row.getSiteNo())
                .eq("test_date", testDate)
                .eq("item_code", row.getItemCode())
                .eq("del_flag", 0));
        if (exists != null) {
            // 同点位同日同指标已登记：幂等跳过，绝不生第二单
            return null;
        }

        // 当时生效的标准条文与界值：凭登记行写下的规则代号回查（含让位行），登记即快照
        TZwLimRule rule = isEmpty(row.getRuleCode()) ? null
                : limRuleService.ruleOf(row.getRuleCode(), row.getTestAt(), true);
        BigDecimal threshold = AbnFlow.thresholdOf(rule, grade.intValue());
        BigDecimal over = AbnFlow.overQty(row.getQty(), threshold);
        int[] advice = AbnFlow.defaultAdvice(grade.intValue());

        Integer seqOfDay = abnCaseMapper.selectCount(new QueryWrapper<TZwAbnCase>()
                .eq("test_date", testDate).eq("del_flag", 0));
        String caseNo = AbnFlow.caseNo(testDate, (seqOfDay == null ? 0 : seqOfDay.intValue()) + 1);

        Date now = new Date();
        TZwAbnCase ac = new TZwAbnCase();
        ac.setCaseNo(caseNo);
        ac.setImpRowId(row.getId());
        ac.setBatchNo(row.getBatchNo());
        ac.setRowNo(row.getRowNo());
        ac.setSiteNo(row.getSiteNo());
        ac.setItemCode(row.getItemCode());
        ac.setSampleType(row.getSampleType());
        ac.setTestAt(row.getTestAt());
        ac.setTestDate(testDate);
        ac.setQty(row.getQty());
        ac.setGradeLevel(grade);
        ac.setLimitValue(threshold);
        ac.setOverQty(over);
        ac.setRuleCode(row.getRuleCode());
        ac.setRuleName(rule == null ? null : rule.getRuleName());
        ac.setRuleEffDate(row.getRuleEffDate());
        ac.setNeedWaterStop(advice[0]);
        ac.setNeedRetest(advice[1]);
        ac.setVerdictNote(buildVerdictNote(grade, row.getQty(), threshold, over, row.getRuleCode()));
        ac.setReviseNo(0);
        ac.setStatus(AbnFlow.ST_REGISTERED);
        ac.setDelFlag(0);
        try {
            abnCaseMapper.insert(ac);
        } catch (DataIntegrityViolationException dup) {
            // 并发同点位同日同指标：uk 兜底，第二单整单不成立
            return null;
        }

        appendTrace(ac.getId(), AbnFlow.ACT_REGISTER, null, AbnFlow.ST_REGISTERED,
                "导入批次 " + row.getBatchNo() + " 第" + row.getRowNo() + "行定级为"
                        + WaterGrade.gradeText(grade.intValue()) + "，自动登记："
                        + "标准线 " + row.getRuleCode()
                        + (threshold == null ? "（该档无在设界值）" : "，对照界值 " + threshold.toPlainString())
                        + (over == null ? "" : "，超 " + over.toPlainString())
                        + "，建议" + (advice[0] == 1 ? "停水" : "不停水") + "+"
                        + (advice[1] == 1 ? "复测" : "不复测"),
                AbnFlow.SYSTEM, now);
        return ac;
    }

    // ---------------------------------------------------------------- 订正

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean correct(Long caseId, Integer gradeLevel, Integer needWaterStop, Integer needRetest,
                          String note, String operator) {
        TZwAbnCase ac = abnCaseMapper.selectById(caseId);
        if (ac == null || int1(ac.getDelFlag())) {
            return false;
        }
        // 只准在"已登记"态订正，且只准订正一次；报过、归档、订正过都闭口
        if (ac.getStatus() == null || ac.getStatus().intValue() != AbnFlow.ST_REGISTERED
                || (ac.getReviseNo() != null && ac.getReviseNo().intValue() > 0)) {
            return false;
        }
        if (gradeLevel == null || gradeLevel.intValue() < WaterGrade.WATCH
                || gradeLevel.intValue() > WaterGrade.SEVERE) {
            return false;
        }
        int stop = needWaterStop != null && needWaterStop.intValue() == 1 ? 1 : 0;
        int retest = needRetest != null && needRetest.intValue() == 1 ? 1 : 0;
        String trimmedNote = isEmpty(note) ? ac.getVerdictNote() : trim(note);

        int rows = abnCaseMapper.update(null, new UpdateWrapper<TZwAbnCase>()
                .set("grade_level", gradeLevel)
                .set("need_water_stop", stop)
                .set("need_retest", retest)
                .set("verdict_note", trimmedNote)
                .set("revise_no", 1)
                .eq("id", caseId)
                .eq("status", AbnFlow.ST_REGISTERED)
                .eq("revise_no", 0)
                .eq("del_flag", 0));
        if (rows == 0) {
            return false;
        }
        // 点位/哪天的水/指标/检测值/标准快照不进 set，想改也没有写口
        appendTrace(caseId, AbnFlow.ACT_CORRECT, AbnFlow.ST_REGISTERED, AbnFlow.ST_REGISTERED,
                "判定订正：档位 " + WaterGrade.gradeText(ac.getGradeLevel()) + "→"
                        + WaterGrade.gradeText(gradeLevel.intValue())
                        + "；停水 " + yn(ac.getNeedWaterStop()) + "→" + (stop == 1 ? "要" : "不要")
                        + "；复测 " + yn(ac.getNeedRetest()) + "→" + (retest == 1 ? "要" : "不要")
                        + (isEmpty(note) ? "" : "；说明：" + trim(note)),
                operator, new Date());
        return true;
    }

    // ---------------------------------------------------------------- 上报

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TZwAbnCase report(Long caseId, String operator) {
        return report(caseId, operator, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TZwAbnCase report(Long caseId, String operator, List<String> rejectMsg) {
        TZwAbnCase ac = abnCaseMapper.selectById(caseId);
        if (ac == null || int1(ac.getDelFlag())) {
            return reject(rejectMsg, "异常登记单不存在");
        }
        if (ac.getStatus() == null || ac.getStatus().intValue() != AbnFlow.ST_REGISTERED) {
            return reject(rejectMsg, "该单已上报或已归档，台账只读，不能重复上报");
        }
        Date now = new Date();

        // 0→1 条件更新：同一笔异常只准一次，并发第二笔 0 行被拒
        int rows = abnCaseMapper.update(null, new UpdateWrapper<TZwAbnCase>()
                .set("status", AbnFlow.ST_REPORTED)
                .set("report_by", operator)
                .set("report_time", now)
                .eq("id", caseId)
                .eq("status", AbnFlow.ST_REGISTERED)
                .eq("del_flag", 0));
        if (rows == 0) {
            return reject(rejectMsg, "该单已被另一次上报抢先落定，请刷新后查看");
        }

        TZwWaterUser user = waterUserMapper.selectOne(new QueryWrapper<TZwWaterUser>()
                .eq("site_no", ac.getSiteNo()).eq("status", 0).eq("del_flag", 0)
                .orderByAsc("id").last("limit 1"));
        if (user == null) {
            // 反映不到供水户名下：上报整笔回滚（盯办该找谁无处落），先补名录再上报
            throw new AbnRejectException("点位「" + ac.getSiteNo() + "」在供水户联系名录里查无现行户，先补名录再上报");
        }

        // 复测单系统自开（uk case_id：一单只开一条）
        TZwRetestOrder order = new TZwRetestOrder();
        order.setRetestNo(AbnFlow.retestNo(ac.getCaseNo()));
        order.setCaseId(ac.getId());
        order.setCaseNo(ac.getCaseNo());
        order.setSiteNo(ac.getSiteNo());
        order.setItemCode(ac.getItemCode());
        order.setCurrentStep(RetestChain.STEP_LAB);
        order.setStatus(RetestChain.ORDER_RUNNING);
        order.setOpenBy(AbnFlow.SYSTEM);
        order.setOpenTime(now);
        order.setDelFlag(0);
        try {
            retestOrderMapper.insert(order);
        } catch (DataIntegrityViolationException dup) {
            throw new AbnRejectException("该异常单的复测单已存在，不能重复上报");
        }

        // 盯办单系统自开（uk case_id）+ 首笔待办
        TZwFollow follow = new TZwFollow();
        follow.setFollowNo(AbnFlow.followNo(ac.getCaseNo()));
        follow.setCaseId(ac.getId());
        follow.setCaseNo(ac.getCaseNo());
        follow.setSiteNo(ac.getSiteNo());
        follow.setWaterUserName(user.getUserName());
        follow.setContactName(user.getContactName());
        follow.setContactPhone(user.getContactPhone());
        follow.setPeriodDays(FollowCycle.DEFAULT_PERIOD_DAYS);
        follow.setLastTouchAt(now);
        follow.setNextDueAt(plusDays(now, FollowCycle.DEFAULT_PERIOD_DAYS));
        follow.setStatus(FollowCycle.FOLLOW_OPEN);
        follow.setOpenTime(now);
        follow.setDelFlag(0);
        try {
            followMapper.insert(follow);
        } catch (DataIntegrityViolationException dup) {
            throw new AbnRejectException("该异常单的盯办单已存在，不能重复上报");
        }
        openTouch(follow, user, now, now);

        appendTrace(caseId, AbnFlow.ACT_REPORT, AbnFlow.ST_REGISTERED, AbnFlow.ST_REPORTED,
                "异常上报，反映到供水户「" + user.getUserName() + "」名下（联系人 "
                        + user.getContactName() + "）；上报后台账只读", operator, now);
        appendTrace(caseId, AbnFlow.ACT_RETEST_OPEN, AbnFlow.ST_REPORTED, AbnFlow.ST_REPORTED,
                "复测单 " + order.getRetestNo() + " 系统自动开立，从「"
                        + RetestChain.stepName(RetestChain.STEP_LAB) + "」环节起步",
                AbnFlow.SYSTEM, now);
        return abnCaseMapper.selectById(caseId);
    }

    // ---------------------------------------------------------------- 复测环节

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TZwRetestStep submitStep(Long orderId, int stepNo, String item, String qtyText, String testAtText,
                                    Integer enteredPrevResult, Integer stepResult, String opinion,
                                    String operator, List<String> rejectMsg) {
        TZwRetestOrder order = retestOrderMapper.selectById(orderId);
        if (order == null || int1(order.getDelFlag())) {
            return rejectStep(rejectMsg, "复测单不存在");
        }
        if (order.getStatus() != null && order.getStatus().intValue() != RetestChain.ORDER_RUNNING) {
            // 上一环否决/已走完全程：本环不开
            return rejectStep(rejectMsg, "复测单已" + RetestChain.orderStatusText(order.getStatus()) + "，本环节不再开通");
        }
        if (order.getCurrentStep() == null || order.getCurrentStep().intValue() != stepNo) {
            // 上一环没填完，这一环不给开
            return rejectStep(rejectMsg, "当前停在「" + RetestChain.stepName(order.getCurrentStep() == null
                    ? RetestChain.STEP_LAB : order.getCurrentStep().intValue())
                    + "」环节，本环节尚未开通");
        }
        if (stepNo < RetestChain.STEP_LAB || stepNo > RetestChain.STEP_CHIEF) {
            return rejectStep(rejectMsg, "环节号不合法");
        }

        int attempt = nextAttempt(orderId, stepNo);
        Date now = new Date();
        TZwRetestStep row = new TZwRetestStep();
        row.setOrderId(orderId);
        row.setStepNo(stepNo);
        row.setAttemptNo(attempt);
        row.setDelFlag(0);

        if (stepNo == RetestChain.STEP_LAB) {
            // 检测项目/数值/时间由本环节填，一样不许预填、不许空交
            if (isEmpty(item) || isEmpty(qtyText) || isEmpty(testAtText)) {
                return rejectStep(rejectMsg, "检测项目、检测数值、检测时间都要由本环节填齐，不许空着提交");
            }
            BigDecimal qty = parseQty(qtyText);
            Date testAt = parseTs(testAtText);
            if (qty == null || !WaterGrade.validInput(qty)) {
                return rejectStep(rejectMsg, "检测数值不在合理区间（0~100），请核对后再填");
            }
            if (testAt == null) {
                return rejectStep(rejectMsg, "检测时间格式应为 yyyy-MM-dd HH:mm:ss");
            }
            row.setFillItem(trim(item));
            row.setFillQty(qty);
            row.setFillTestAt(testAt);
            row.setMatchFlag(1);
            row.setStatus(RetestChain.ROW_EFFECTIVE);
            row.setFillBy(operator);
            row.setFillTime(now);
            retestStepMapper.insert(row);
            advanceOrder(order, RetestChain.STEP_WATER, now);
            return row;
        }

        if (stepNo == RetestChain.STEP_WATER) {
            // 照录环节0 三项；再给本环节复核结论
            if (isEmpty(item) || isEmpty(qtyText) || isEmpty(testAtText)) {
                return rejectStep(rejectMsg, "请把上一环的检测项目、数值、时间逐项照录后再提交");
            }
            if (!RetestChain.validResult(stepResult)) {
                return rejectStep(rejectMsg, "请给出本环节复核结论：通过 / 否决");
            }
            BigDecimal qty = parseQty(qtyText);
            Date testAt = parseTs(testAtText);
            TZwRetestStep lab = effectiveStep(orderId, RetestChain.STEP_LAB);
            boolean matches = lab != null && qty != null && testAt != null
                    && RetestChain.labMatches(trim(item), qty, testAt,
                            lab.getFillItem(), lab.getFillQty(), lab.getFillTestAt());
            row.setPrevItem(trim(item));
            row.setPrevQty(qty);
            row.setPrevTestAt(testAt);
            row.setStepResult(stepResult);
            row.setOpinion(stepResult.intValue() == RetestChain.RESULT_VETO ? requireOpinion(opinion, rejectMsg) : trim(opinion));
            if (rejectMsg != null && !rejectMsg.isEmpty()) {
                return null;
            }
            if (!matches) {
                // 对不上：原样留痕（不删不改），不开下一环，可重新提交（attempt+1）
                row.setMatchFlag(0);
                row.setStatus(RetestChain.ROW_MISMATCH);
                row.setFillBy(operator);
                row.setFillTime(now);
                retestStepMapper.insert(row);
                rejectMsg.clear();
                rejectMsg.add("照录内容与化验室检测环节对不上，本笔已留痕；请逐项核对后重新提交，本环节暂不下放");
                return row;
            }
            row.setMatchFlag(1);
            row.setStatus(RetestChain.ROW_EFFECTIVE);
            row.setFillBy(operator);
            row.setFillTime(now);
            retestStepMapper.insert(row);
            if (stepResult.intValue() == RetestChain.RESULT_VETO) {
                vetoOrder(order, operator, now);
            } else {
                advanceOrder(order, RetestChain.STEP_CHIEF, now);
            }
            return row;
        }

        // 环节2 主管审定：照录环节1 的复核结论，再给审定结论
        if (!RetestChain.validResult(enteredPrevResult)) {
            return rejectStep(rejectMsg, "请照录水质科复核结论（通过/否决）");
        }
        if (!RetestChain.validResult(stepResult)) {
            return rejectStep(rejectMsg, "请给出主管审定结论：通过 / 否决");
        }
        TZwRetestStep water = effectiveStep(orderId, RetestChain.STEP_WATER);
        boolean matches = water != null
                && RetestChain.resultMatches(water.getStepResult(), enteredPrevResult);
        row.setPrevResult(enteredPrevResult);
        row.setStepResult(stepResult);
        row.setOpinion(stepResult.intValue() == RetestChain.RESULT_VETO ? requireOpinion(opinion, rejectMsg) : trim(opinion));
        if (rejectMsg != null && !rejectMsg.isEmpty()) {
            return null;
        }
        if (!matches) {
            row.setMatchFlag(0);
            row.setStatus(RetestChain.ROW_MISMATCH);
            row.setFillBy(operator);
            row.setFillTime(now);
            retestStepMapper.insert(row);
            rejectMsg.clear();
            rejectMsg.add("照录的上一环复核结论与水质科实际结论对不上，本笔已留痕；请核对后重新提交");
            return row;
        }
        row.setMatchFlag(1);
        row.setStatus(RetestChain.ROW_EFFECTIVE);
        row.setFillBy(operator);
        row.setFillTime(now);
        retestStepMapper.insert(row);

        if (stepResult.intValue() == RetestChain.RESULT_VETO) {
            vetoOrder(order, operator, now);
        } else {
            // 末环通过：复测单走完全程 → 系统归档异常单、同刻自结盯办
            int fin = retestOrderMapper.update(null, new UpdateWrapper<TZwRetestOrder>()
                    .set("current_step", RetestChain.STEP_DONE)
                    .set("status", RetestChain.ORDER_PASSED)
                    .set("final_result", RetestChain.RESULT_PASS)
                    .set("finish_by", operator)
                    .set("finish_time", now)
                    .eq("id", orderId)
                    .eq("current_step", RetestChain.STEP_CHIEF)
                    .eq("status", RetestChain.ORDER_RUNNING)
                    .eq("del_flag", 0));
            if (fin == 0) {
                throw new AbnRejectException("复测单已被并发改动，请刷新后重提");
            }
            archiveBySystem(order.getCaseId(), operator, now);
        }
        return row;
    }

    /** 系统归档异常单（1→2，以归档这一次写下的为准）并同刻自结盯办 */
    private void archiveBySystem(Long caseId, String finisher, Date now) {
        String note = "复测全程通过，系统据末环（主管审定）结论归档结案；末环落定人 " + finisher;
        int rows = abnCaseMapper.update(null, new UpdateWrapper<TZwAbnCase>()
                .set("status", AbnFlow.ST_ARCHIVED)
                .set("archive_by", AbnFlow.SYSTEM)
                .set("archive_time", now)
                .set("archive_note", note)
                .eq("id", caseId)
                .eq("status", AbnFlow.ST_REPORTED)
                .eq("del_flag", 0));
        if (rows == 0) {
            // 归档为准：异常单不在"已上报"态（已被并发改动）整笔回滚，末环节名不留悬空
            throw new AbnRejectException("异常单当前不允许归档（可能已被并发结案），请刷新后查看");
        }
        appendTrace(caseId, AbnFlow.ACT_ARCHIVE, AbnFlow.ST_REPORTED, AbnFlow.ST_ARCHIVED,
                note, AbnFlow.SYSTEM, now);

        TZwFollow follow = followMapper.selectOne(new QueryWrapper<TZwFollow>()
                .eq("case_id", caseId).eq("del_flag", 0));
        if (follow != null && follow.getStatus() != null
                && follow.getStatus().intValue() == FollowCycle.FOLLOW_OPEN) {
            followMapper.update(null, new UpdateWrapper<TZwFollow>()
                    .set("status", FollowCycle.FOLLOW_CLOSED)
                    .set("close_time", now)
                    .set("close_note", "异常归档结案，系统同刻自结盯办")
                    .eq("id", follow.getId())
                    .eq("status", FollowCycle.FOLLOW_OPEN));
        }
    }

    // ---------------------------------------------------------------- 内部

    /** 开出一笔盯办待办（系统开）：联系人按名录现取，prev 台账头上一次实际盯办时刻 */
    private void openTouch(TZwFollow follow, TZwWaterUser user, Date planAt, Date prevTouchAt) {
        Integer seq = followTouchMapper.selectCount(new QueryWrapper<TZwFollowTouch>()
                .eq("follow_id", follow.getId()));
        TZwFollowTouch touch = new TZwFollowTouch();
        touch.setFollowId(follow.getId());
        touch.setSeq((seq == null ? 0 : seq.intValue()) + 1);
        touch.setPlanAt(planAt);
        touch.setDueAt(follow.getNextDueAt());
        touch.setContactName(user == null ? follow.getContactName() : user.getContactName());
        touch.setContactPhone(user == null ? follow.getContactPhone() : user.getContactPhone());
        touch.setPrevTouchAt(prevTouchAt);
        touch.setStatus(FollowCycle.TOUCH_PENDING);
        followTouchMapper.insert(touch);
    }

    private void advanceOrder(TZwRetestOrder order, int nextStep, Date now) {
        int rows = retestOrderMapper.update(null, new UpdateWrapper<TZwRetestOrder>()
                .set("current_step", nextStep)
                .eq("id", order.getId())
                .eq("current_step", order.getCurrentStep())
                .eq("status", RetestChain.ORDER_RUNNING)
                .eq("del_flag", 0));
        if (rows == 0) {
            throw new AbnRejectException("复测单已被并发改动，请刷新后重提");
        }
    }

    private void vetoOrder(TZwRetestOrder order, String operator, Date now) {
        int rows = retestOrderMapper.update(null, new UpdateWrapper<TZwRetestOrder>()
                .set("status", RetestChain.ORDER_VETOED)
                .set("final_result", RetestChain.RESULT_VETO)
                .set("finish_by", operator)
                .set("finish_time", now)
                .eq("id", order.getId())
                .eq("current_step", order.getCurrentStep())
                .eq("status", RetestChain.ORDER_RUNNING)
                .eq("del_flag", 0));
        if (rows == 0) {
            throw new AbnRejectException("复测单已被并发改动，请刷新后重提");
        }
        // 否决即止：异常单停在"已上报"只读态，不开下一环节、不归档（处置另案）
    }

    private int nextAttempt(Long orderId, int stepNo) {
        Integer n = retestStepMapper.selectCount(new QueryWrapper<TZwRetestStep>()
                .eq("order_id", orderId).eq("step_no", stepNo));
        return (n == null ? 0 : n.intValue()) + 1;
    }

    private TZwRetestStep effectiveStep(Long orderId, int stepNo) {
        return retestStepMapper.selectOne(new QueryWrapper<TZwRetestStep>()
                .eq("order_id", orderId)
                .eq("step_no", stepNo)
                .eq("status", RetestChain.ROW_EFFECTIVE)
                .eq("del_flag", 0)
                .orderByDesc("attempt_no").last("limit 1"));
    }

    private void appendTrace(Long caseId, String action, Integer fromSt, Integer toSt,
                             String detail, String operator, Date at) {
        Integer seq = abnTraceMapper.selectCount(new QueryWrapper<TZwAbnTrace>().eq("case_id", caseId));
        TZwAbnTrace t = new TZwAbnTrace();
        t.setCaseId(caseId);
        t.setSeq((seq == null ? 0 : seq.intValue()) + 1);
        t.setAction(action);
        t.setFromStatus(fromSt);
        t.setToStatus(toSt);
        t.setDetail(clamp(detail, 1000));
        t.setOperator(operator);
        t.setActionTime(at);
        abnTraceMapper.insert(t);
    }

    private static Date plusDays(Date base, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(base);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    private static BigDecimal parseQty(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Date parseTs(String text) {
        try {
            SimpleDateFormat f = new SimpleDateFormat(TS_FMT);
            f.setLenient(false);
            return f.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private static String requireOpinion(String opinion, List<String> rejectMsg) {
        if (isEmpty(opinion)) {
            if (rejectMsg != null) {
                rejectMsg.clear();
                rejectMsg.add("否决必须写明本环节意见");
            }
            return null;
        }
        return trim(opinion);
    }

    private static String buildVerdictNote(int grade, BigDecimal qty, BigDecimal threshold,
                                          BigDecimal over, String ruleCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("读数 ").append(qty == null ? "" : qty.toPlainString())
                .append(" 判定为").append(WaterGrade.gradeText(grade));
        if (threshold != null) {
            sb.append("，对照界值 ").append(threshold.toPlainString());
            if (over != null) {
                sb.append("，超 ").append(over.toPlainString());
            }
        }
        sb.append("（标准线 ").append(ruleCode).append("）");
        return clamp(sb.toString(), 500);
    }

    private static String yn(Integer v) {
        return v != null && v.intValue() == 1 ? "要" : "不要";
    }

    private static boolean int1(Integer v) {
        return v != null && v.intValue() == 1;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static TZwAbnCase reject(List<String> msg, String text) {
        if (msg != null) {
            msg.clear();
            msg.add(text);
        }
        return null;
    }

    private static TZwRetestStep rejectStep(List<String> msg, String text) {
        if (msg != null) {
            msg.clear();
            msg.add(text);
        }
        return null;
    }

    /** 业务拒绝（用于事务内需回滚且要把话带给页面的情形） */
    public static class AbnRejectException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        AbnRejectException(String message) {
            super(message);
        }
    }
}
