package com.fc.v2.controller.admin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TZwAbnCase;
import com.fc.v2.model.auto.TZwAbnTrace;
import com.fc.v2.model.auto.TZwFollow;
import com.fc.v2.model.auto.TZwFollowTouch;
import com.fc.v2.model.auto.TZwImpRow;
import com.fc.v2.model.auto.TZwLimRule;
import com.fc.v2.model.auto.TZwRetestOrder;
import com.fc.v2.model.auto.TZwRetestStep;
import com.fc.v2.service.ITZwAbnCaseService;
import com.fc.v2.service.ITZwFollowService;
import com.fc.v2.service.ITZwLimRuleService;
import com.fc.v2.service.zw.ImpSampleTime;
import com.fc.v2.service.zw.RetestChain;
import com.fc.v2.util.StringUtils;
import com.fc.v2.util.file.FileUtils;
import com.github.pagehelper.PageInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 水质异常判定留痕 Controller。
 *
 * <p>台账只读到处可查；订正/上报写口单独授权；复测三环节各一个授权码，
 * 提交时服务端再按"单当前环节 == 本环 + 持本环授权"双验，页面不设任何旁路。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "水质异常判定留痕")
@Controller
@RequestMapping("/zwAbn")
public class ZwAbnController extends BaseController {

    private final String prefix = "admin/zwAbn";

    @Autowired
    private ITZwAbnCaseService abnService;
    @Autowired
    private ITZwFollowService followService;
    @Autowired
    private ITZwLimRuleService limRuleService;

    @ApiOperation(value = "异常判定台账跳转", notes = "异常判定台账跳转")
    @GetMapping("/view")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "水质异常判定台账查询", action = "list")
    @GetMapping("/list")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:list")
    @ResponseBody
    public ResultTable list(TZwAbnCase query) {
        QueryWrapper<TZwAbnCase> qw = new QueryWrapper<TZwAbnCase>();
        qw.eq("del_flag", 0);
        qw.like(StringUtils.isNotEmpty(query.getCaseNo()), "case_no", query.getCaseNo());
        qw.like(StringUtils.isNotEmpty(query.getSiteNo()), "site_no", query.getSiteNo());
        qw.eq(StringUtils.isNotEmpty(query.getItemCode()), "item_code", query.getItemCode());
        qw.eq(StringUtils.isNotNull(query.getGradeLevel()), "grade_level", query.getGradeLevel());
        qw.eq(StringUtils.isNotNull(query.getStatus()), "status", query.getStatus());
        qw.orderByDesc("test_date").orderByAsc("site_no");
        startPage();
        PageInfo<TZwAbnCase> page = new PageInfo<TZwAbnCase>(abnService.selectCaseList(qw));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "水质异常判定倒查", action = "detail")
    @GetMapping("/detail")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:list")
    public String detail(@RequestParam("id") Long id, ModelMap model) {
        TZwAbnCase ac = abnService.selectCaseById(id);
        if (ac == null) {
            return prefix + "/list";
        }
        List<TZwAbnTrace> traces = abnService.listTrace(id);
        TZwImpRow sourceRow = abnService.sourceRowOf(ac);
        TZwLimRule rule = ac.getRuleCode() == null ? null
                : limRuleService.ruleOf(ac.getRuleCode(), ac.getTestAt(), true);
        TZwRetestOrder order = abnService.retestOf(id);
        List<TZwRetestStep> steps = order == null ? new ArrayList<TZwRetestStep>()
                : abnService.listSteps(order.getId());
        TZwFollow follow = followService.selectFollowList(new QueryWrapper<TZwFollow>()
                .eq("case_id", id).eq("del_flag", 0))
                .stream().findFirst().orElse(null);
        List<TZwFollowTouch> touches = follow == null ? new ArrayList<TZwFollowTouch>()
                : followService.listTouches(follow.getId());

        // 每个环节当前登录人是否可填：单停在该环 + 持该环授权；页面照此只开当前一环的格子
        Map<Integer, Boolean> stepEditable = new HashMap<Integer, Boolean>();
        for (int s = RetestChain.STEP_LAB; s <= RetestChain.STEP_CHIEF; s++) {
            boolean onStep = order != null && order.getCurrentStep() != null
                    && order.getCurrentStep().intValue() == s
                    && order.getStatus() != null && order.getStatus().intValue() == RetestChain.ORDER_RUNNING;
            stepEditable.put(Integer.valueOf(s),
                    Boolean.valueOf(onStep && hasPerm(RetestChain.permOfStep(s))));
        }

        model.put("ac", ac);
        model.put("traces", traces);
        java.util.Collections.reverse(traces);
        model.put("tracesDesc", traces);
        model.put("sourceRow", sourceRow);
        model.put("rule", rule);
        model.put("order", order);
        model.put("steps", steps);
        Map<Integer, List<TZwRetestStep>> stepRowsMap = new HashMap<Integer, List<TZwRetestStep>>();
        for (int s = RetestChain.STEP_LAB; s <= RetestChain.STEP_CHIEF; s++) {
            stepRowsMap.put(Integer.valueOf(s), new ArrayList<TZwRetestStep>());
        }
        for (TZwRetestStep st : steps) {
            if (st.getStepNo() != null) {
                List<TZwRetestStep> bucket = stepRowsMap.get(st.getStepNo());
                if (bucket != null) {
                    bucket.add(st);
                }
            }
        }
        model.put("stepRowsMap", stepRowsMap);
        model.put("follow", follow);
        java.util.Collections.reverse(touches);
        model.put("touchesDesc", touches);
        model.put("stepNos", java.util.Arrays.asList(
                RetestChain.STEP_LAB, RetestChain.STEP_WATER, RetestChain.STEP_CHIEF));
        model.put("stepEditable", stepEditable);
        return prefix + "/detail";
    }

    @Log(title = "水质异常判定订正", action = "correct")
    @PostMapping("/correct")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:correct")
    @ResponseBody
    public AjaxResult correct(Long id, Integer gradeLevel, Integer needWaterStop, Integer needRetest,
                              String note) {
        String operator = com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        boolean ok = abnService.correct(id, gradeLevel, needWaterStop, needRetest, note, operator);
        return ok ? AjaxResult.success("判定已订正（仅可订正一次），过程流水已留痕")
                : AjaxResult.error("订正未生效：该单已订正过、已上报或已归档，写口已闭");
    }

    @Log(title = "水质异常上报", action = "report")
    @PostMapping("/report")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:report")
    @ResponseBody
    public AjaxResult report(Long id) {
        List<String> msg = new ArrayList<String>();
        String operator = com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        TZwAbnCase ac;
        try {
            ac = abnService.report(id, operator, msg);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
        if (ac == null) {
            return AjaxResult.error(msg.isEmpty() ? "上报未生效" : msg.get(0));
        }
        return AjaxResult.success("已上报，台账转为只读；系统已自开复测单与供水户盯办单");
    }

    @Log(title = "复测环节提交", action = "retestStep")
    @PostMapping("/retestStep")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:list")
    @ResponseBody
    public AjaxResult retestStep(Long orderId, Integer stepNo, String item, String qty, String testAt,
                                 Integer enteredPrevResult, Integer stepResult, String opinion) {
        if (stepNo == null) {
            return AjaxResult.error("缺少环节号");
        }
        int s = stepNo.intValue();
        String perm = RetestChain.permOfStep(s);
        // 系统按环节开权限：不是本环该填的人，服务口也不给进（页面按钮授权之外的第二道）
        if (perm == null || !hasPerm(perm)) {
            return AjaxResult.error("本环节不由你填报，系统未开通此环节权限");
        }
        List<String> msg = new ArrayList<String>();
        String operator = com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        TZwRetestStep row;
        try {
            row = abnService.submitStep(orderId, s, item, qty, testAt, enteredPrevResult,
                    stepResult, opinion, operator, msg);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
        if (row == null) {
            return AjaxResult.error(msg.isEmpty() ? "提交未生效" : msg.get(0));
        }
        if (row.getStatus() != null && row.getStatus().intValue() == RetestChain.ROW_MISMATCH) {
            return AjaxResult.error(msg.isEmpty() ? "照录对不上，本笔已留痕" : msg.get(0));
        }
        if (s == RetestChain.STEP_CHIEF && stepResult != null
                && stepResult.intValue() == RetestChain.RESULT_PASS) {
            return AjaxResult.success("主管审定通过：复测全程通过，异常已由系统归档结案、盯办同刻自结");
        }
        if (stepResult != null && stepResult.intValue() == RetestChain.RESULT_VETO) {
            return AjaxResult.success("已记否决，复测链到此终止（异常单保持已上报只读）");
        }
        return AjaxResult.success(s == RetestChain.STEP_LAB
                ? "化验室检测已落名，本环节对其他人只读，下放水质科复核"
                : "本环节已落名，下放下一环节");
    }

    @Log(title = "复测环节核对报表导出", action = "export")
    @GetMapping("/export")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwAbn:export")
    public void export(HttpServletResponse response) throws IOException {
        List<String> headers = java.util.Arrays.asList(
                "异常单号", "点位代号", "指标", "复测单号", "环节号", "环节名", "提交次序",
                "检测项目(本环/照录)", "检测数值(本环)", "检测数值(照录)",
                "检测时间(本环)", "检测时间(照录)", "照录上一环结论", "本环节结论",
                "核对", "本笔效力", "填报人", "填报时刻", "意见");
        List<TZwAbnCase> cases = abnService.selectCaseList(new QueryWrapper<TZwAbnCase>()
                .eq("del_flag", 0).orderByDesc("test_date").orderByAsc("site_no"));
        List<List<String>> lines = new ArrayList<List<String>>();
        for (TZwAbnCase ac : cases) {
            TZwRetestOrder order = abnService.retestOf(ac.getId());
            if (order == null) {
                continue;
            }
            List<TZwRetestStep> steps = abnService.listSteps(order.getId());
            for (TZwRetestStep st : steps) {
                List<String> line = new ArrayList<String>();
                line.add(s(ac.getCaseNo()));
                line.add(s(ac.getSiteNo()));
                line.add(s(ac.getItemCode()));
                line.add(s(order.getRetestNo()));
                line.add(String.valueOf(st.getStepNo()));
                line.add(RetestChain.stepName(st.getStepNo()));
                line.add(String.valueOf(st.getAttemptNo()));
                line.add(s(st.getFillItem()) + (s(st.getPrevItem()).isEmpty() ? "" : "/" + s(st.getPrevItem())));
                line.add(st.getFillQty() == null ? "" : st.getFillQty().toPlainString());
                line.add(st.getPrevQty() == null ? "" : st.getPrevQty().toPlainString());
                line.add(ImpSampleTime.format(st.getFillTestAt()));
                line.add(ImpSampleTime.format(st.getPrevTestAt()));
                line.add(st.getPrevResult() == null ? "" : RetestChain.resultText(st.getPrevResult()));
                line.add(st.getStepResult() == null ? "" : RetestChain.resultText(st.getStepResult()));
                line.add(st.getMatchFlag() != null && st.getMatchFlag().intValue() == 1 ? "对得上" : "对不上");
                line.add(st.getStatus() != null && st.getStatus().intValue() == 1 ? "有效落名" : "留痕未生效");
                line.add(s(st.getFillBy()));
                line.add(ImpSampleTime.format(st.getFillTime()));
                line.add(s(st.getOpinion()));
                lines.add(line);
            }
        }
        byte[] bytes = com.fc.v2.service.zw.ReportExcel.write("复测环节核对", headers, lines);
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd");
        dayFmt.setTimeZone(ImpSampleTime.ZONE);
        String fileName = "复测环节核对报表_" + dayFmt.format(new Date()) + ".xlsx";
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private static boolean hasPerm(String perm) {
        return perm != null && SecurityUtils.getSubject().isPermitted(perm);
    }

    private static String s(String v) {
        return v == null ? "" : v;
    }
}
