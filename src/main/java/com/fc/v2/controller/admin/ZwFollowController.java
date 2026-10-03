package com.fc.v2.controller.admin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.fc.v2.model.auto.TZwFollow;
import com.fc.v2.model.auto.TZwFollowTouch;
import com.fc.v2.service.ITZwFollowService;
import com.fc.v2.service.zw.FollowCycle;
import com.fc.v2.service.zw.ImpSampleTime;
import com.fc.v2.service.zw.ReportExcel;
import com.fc.v2.util.StringUtils;
import com.fc.v2.util.file.FileUtils;
import com.github.pagehelper.PageInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 供水户盯办 Controller。
 *
 * <p>盯办单/流水都由系统自开，页面只有查询、下钻、为系统开出的待办笔填结果、手动跑一轮；
 * 没有开单/闭口/删笔口。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "供水户盯办台账")
@Controller
@RequestMapping("/zwFollow")
public class ZwFollowController extends BaseController {

    private final String prefix = "admin/zwFollow";

    @Autowired
    private ITZwFollowService followService;

    @GetMapping("/view")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "供水户盯办台账查询", action = "list")
    @GetMapping("/list")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:view")
    @ResponseBody
    public ResultTable list(TZwFollow query) {
        QueryWrapper<TZwFollow> qw = new QueryWrapper<TZwFollow>();
        qw.eq("del_flag", 0);
        qw.like(StringUtils.isNotEmpty(query.getFollowNo()), "follow_no", query.getFollowNo());
        qw.like(StringUtils.isNotEmpty(query.getSiteNo()), "site_no", query.getSiteNo());
        qw.like(StringUtils.isNotEmpty(query.getWaterUserName()), "water_user_name", query.getWaterUserName());
        qw.eq(StringUtils.isNotNull(query.getStatus()), "status", query.getStatus());
        qw.orderByAsc("status").orderByAsc("next_due_at");
        startPage();
        PageInfo<TZwFollow> page = new PageInfo<TZwFollow>(followService.selectFollowList(qw));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "供水户盯办下钻", action = "detail")
    @GetMapping("/detail")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:view")
    public String detail(@RequestParam("id") Long id, ModelMap model) {
        TZwFollow follow = followService.selectFollowById(id);
        if (follow == null) {
            return prefix + "/list";
        }
        List<TZwFollowTouch> touches = followService.listTouches(id);
        model.put("follow", follow);
        model.put("touches", touches);
        java.util.Collections.reverse(touches);
        model.put("touchesDesc", touches);
        model.put("canFill", SecurityUtils.getSubject().isPermitted("zw:zwFollow:fill"));
        return prefix + "/detail";
    }

    @Log(title = "供水户盯办结果填报", action = "fill")
    @PostMapping("/fill")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:fill")
    @ResponseBody
    public AjaxResult fill(Long touchId, String result, String detail, String actualAt) {
        List<String> msg = new ArrayList<String>();
        String operator = com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        TZwFollowTouch touch = followService.fillTouch(touchId, result, detail, actualAt, operator, msg);
        if (touch == null) {
            return AjaxResult.error(msg.isEmpty() ? "填报未生效" : msg.get(0));
        }
        return AjaxResult.success("盯办结果已记名（落名只读），下一次盯办时刻已自动顺排");
    }

    @Log(title = "供水户盯办跑一轮", action = "run")
    @PostMapping("/run")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:run")
    @ResponseBody
    public AjaxResult run() {
        int opened = followService.runCycle(new Date());
        return AjaxResult.success("本轮系统开出盯办待办 " + opened + " 笔（逾期未办的单不开新笔，漏办留账）");
    }

    @Log(title = "供水户盯办流水导出", action = "export")
    @GetMapping("/export")
    @org.apache.shiro.authz.annotation.RequiresPermissions("zw:zwFollow:view")
    public void export(@RequestParam(value = "siteNo", required = false) String siteNo,
                       @RequestParam(value = "status", required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        QueryWrapper<TZwFollow> qw = new QueryWrapper<TZwFollow>().eq("del_flag", 0);
        qw.like(StringUtils.isNotEmpty(siteNo), "site_no", siteNo);
        qw.eq(StringUtils.isNotNull(status), "status", status);
        List<TZwFollow> follows = followService.selectFollowList(qw);
        List<String> headers = java.util.Arrays.asList(
                "盯办单号", "异常单号", "点位代号", "供水户", "笔序", "计划盯办时刻", "本笔时限",
                "该找谁", "电话", "上一次盯办时刻", "盯办结果", "本次情况",
                "实际盯办时刻", "经办人", "笔情形");
        List<List<String>> lines = new ArrayList<List<String>>();
        for (TZwFollow f : follows) {
            for (TZwFollowTouch t : followService.listTouches(f.getId())) {
                List<String> line = new ArrayList<String>();
                line.add(s(f.getFollowNo()));
                line.add(s(f.getCaseNo()));
                line.add(s(f.getSiteNo()));
                line.add(s(f.getWaterUserName()));
                line.add(String.valueOf(t.getSeq()));
                line.add(ImpSampleTime.format(t.getPlanAt()));
                line.add(ImpSampleTime.format(t.getDueAt()));
                line.add(s(t.getContactName()));
                line.add(s(t.getContactPhone()));
                line.add(ImpSampleTime.format(t.getPrevTouchAt()));
                line.add(FollowCycle.resultText(t.getResult()));
                line.add(s(t.getDetail()));
                line.add(ImpSampleTime.format(t.getActualTouchAt()));
                line.add(s(t.getFillBy()));
                line.add(t.getStatus() != null && t.getStatus().intValue() == 1 ? "已办" : "待办(漏办)");
                lines.add(line);
            }
        }
        byte[] bytes = ReportExcel.write("供水户盯办流水", headers, lines);
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd");
        dayFmt.setTimeZone(ImpSampleTime.ZONE);
        String fileName = "供水户盯办流水_" + dayFmt.format(new Date()) + ".xlsx";
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private static String s(String v) {
        return v == null ? "" : v;
    }
}
