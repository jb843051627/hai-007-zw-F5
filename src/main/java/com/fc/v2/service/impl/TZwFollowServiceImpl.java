package com.fc.v2.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TZwFollowMapper;
import com.fc.v2.mapper.auto.TZwFollowTouchMapper;
import com.fc.v2.mapper.auto.TZwWaterUserMapper;
import com.fc.v2.model.auto.TZwFollow;
import com.fc.v2.model.auto.TZwFollowTouch;
import com.fc.v2.model.auto.TZwWaterUser;
import com.fc.v2.service.ITZwFollowService;
import com.fc.v2.service.zw.FollowCycle;

/**
 * 供水户盯办 Service业务层处理。
 *
 * <p><b>系统自开自结</b>：盯办单由异常上报同事务自开（在异常单服务里），这里只有"系统按周期开笔"和
 * "经办人填结果"两类动作——页面没有开单口、闭口，也没有手开一笔流水的口。
 *
 * <p><b>只追加、不倒改</b>：每笔盯办一行 {@link TZwFollowTouch}，落名（status=1）后没有任何写口回改；
 * 谁也不许拿着后一笔倒改前一笔。每笔都带上一次盯办时刻（prev_touch_at），盯七八次、隔几个月也能顺序翻。
 *
 * <p><b>联系人每笔现取</b>：上报反映后卡住多半是联系人取错/联系不上，所以每开一笔都按点位从名录
 * 重新取现行联系人，名录改对后下一笔自然换对人；头表留开单快照，流水里看得见"上一笔找错了谁"。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Service
public class TZwFollowServiceImpl implements ITZwFollowService {

    private static final String TS_FMT = "yyyy-MM-dd HH:mm:ss";

    @javax.annotation.Resource
    private TZwFollowMapper followMapper;
    @javax.annotation.Resource
    private TZwFollowTouchMapper followTouchMapper;
    @javax.annotation.Resource
    private TZwWaterUserMapper waterUserMapper;

    @Override
    public List<TZwFollow> selectFollowList(Wrapper<TZwFollow> wrapper) {
        return followMapper.selectList(wrapper);
    }

    @Override
    public TZwFollow selectFollowById(Long id) {
        return followMapper.selectById(id);
    }

    @Override
    public List<TZwFollowTouch> listTouches(Long followId) {
        return followTouchMapper.selectList(new QueryWrapper<TZwFollowTouch>()
                .eq("follow_id", followId).orderByAsc("seq"));
    }

    /**
     * 经办人填结果：只准填该单当前那笔待办；结果三选一、情况必填、实际盯办时刻必填且不晚于现在。
     * 填妥同事务把头上一次盯办时刻记为实际盯办时刻、下次该盯时刻顺排一个周期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TZwFollowTouch fillTouch(Long touchId, String result, String detail, String actualAtText,
                                    String operator, List<String> rejectMsg) {
        TZwFollowTouch touch = followTouchMapper.selectById(touchId);
        if (touch == null) {
            return reject(rejectMsg, "盯办笔不存在");
        }
        TZwFollow follow = followMapper.selectById(touch.getFollowId());
        if (follow == null || int1(follow.getDelFlag())
                || follow.getStatus() == null || follow.getStatus().intValue() != FollowCycle.FOLLOW_OPEN) {
            return reject(rejectMsg, "盯办单已结案，不能再填");
        }
        if (touch.getStatus() != null && touch.getStatus().intValue() != FollowCycle.TOUCH_PENDING) {
            // 已办笔只追加不回改
            return reject(rejectMsg, "该笔已填过，落名即只读，不能改");
        }
        if (!FollowCycle.validResult(result)) {
            return reject(rejectMsg, "请选择盯办结果：已联系上 / 联系不上 / 联系人有误");
        }
        if (isEmpty(detail)) {
            return reject(rejectMsg, "请写明本次盯办情况（当天做了什么）");
        }
        Date actualAt = parseTs(actualAtText);
        if (actualAt == null) {
            return reject(rejectMsg, "实际盯办时刻格式应为 yyyy-MM-dd HH:mm:ss");
        }
        Date now = new Date();
        if (actualAt.after(now)) {
            return reject(rejectMsg, "实际盯办时刻不能晚于现在");
        }

        Date prevTouchAt = touch.getPrevTouchAt();
        int rows = followTouchMapper.update(null, new UpdateWrapper<TZwFollowTouch>()
                .set("result", result)
                .set("detail", clamp(detail, 500))
                .set("actual_touch_at", actualAt)
                .set("fill_by", operator)
                .set("fill_time", now)
                .set("status", FollowCycle.TOUCH_DONE)
                .eq("id", touchId)
                .eq("status", FollowCycle.TOUCH_PENDING));
        if (rows == 0) {
            return reject(rejectMsg, "该笔已被另一次提交抢先填定，请刷新后查看");
        }

        // 头上一次盯办/下次该盯顺排：联系不上也照排，不许因为没联系上就拖着不开下一笔
        Date nextDue = FollowCycle.nextDueAfter(actualAt,
                follow.getPeriodDays() == null ? FollowCycle.DEFAULT_PERIOD_DAYS : follow.getPeriodDays());
        followMapper.update(null, new UpdateWrapper<TZwFollow>()
                .set("last_touch_at", actualAt)
                .set("next_due_at", nextDue)
                .eq("id", follow.getId())
                .eq("status", FollowCycle.FOLLOW_OPEN));
        touch.setResult(result);
        touch.setDetail(clamp(detail, 500));
        touch.setActualTouchAt(actualAt);
        touch.setFillBy(operator);
        touch.setFillTime(now);
        touch.setStatus(FollowCycle.TOUCH_DONE);
        return touch;
    }

    /**
     * 跑一轮：在盯单里凡"到了下次该盯时刻、且当前没有待办笔（上一笔已盯完）"的，系统开下一笔。
     * 上一笔仍待办（漏办）的不开新笔——新笔会把漏办盖掉，留着红点追。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int runCycle(Date at) {
        Date now = at == null ? new Date() : at;
        List<TZwFollow> open = followMapper.selectList(new QueryWrapper<TZwFollow>()
                .eq("status", FollowCycle.FOLLOW_OPEN).eq("del_flag", 0));
        int opened = 0;
        List<String> skipped = new ArrayList<String>();
        for (TZwFollow f : open) {
            if (!FollowCycle.due(f.getNextDueAt(), now)) {
                continue;
            }
            Integer pending = followTouchMapper.selectCount(new QueryWrapper<TZwFollowTouch>()
                    .eq("follow_id", f.getId()).eq("status", FollowCycle.TOUCH_PENDING));
            if (pending != null && pending.intValue() > 0) {
                // 上一笔到点还没盯：漏办留账，不开新的盖它
                skipped.add(f.getFollowNo());
                continue;
            }
            // 联系人按名录现取（名录已改对就换对人）；名录撤了就沿用上一笔联系人并在情况里可见
            TZwWaterUser user = waterUserMapper.selectOne(new QueryWrapper<TZwWaterUser>()
                    .eq("site_no", f.getSiteNo()).eq("status", 0).eq("del_flag", 0)
                    .orderByAsc("id").last("limit 1"));
            if (user != null) {
                followMapper.update(null, new UpdateWrapper<TZwFollow>()
                        .set("contact_name", user.getContactName())
                        .set("contact_phone", user.getContactPhone())
                        .eq("id", f.getId()).eq("status", FollowCycle.FOLLOW_OPEN));
                f.setContactName(user.getContactName());
                f.setContactPhone(user.getContactPhone());
            }
            int period = f.getPeriodDays() == null ? FollowCycle.DEFAULT_PERIOD_DAYS : f.getPeriodDays();
            Date touchDue = plusDays(now, period);
            openNextTouch(f, user, now, touchDue);
            // 开出后，下一笔的该开时刻顺排到本笔时限；未盯完则待办计数挡着，不会提前开
            followMapper.update(null, new UpdateWrapper<TZwFollow>()
                    .set("next_due_at", touchDue)
                    .eq("id", f.getId()).eq("status", FollowCycle.FOLLOW_OPEN));
            opened++;
        }
        return opened;
    }

    /** 系统开下一笔待办：plan_at=现在，due_at=现在+一个周期（本笔要在此时限前盯完），prev=头上一次实际盯办时刻 */
    private void openNextTouch(TZwFollow f, TZwWaterUser user, Date now, Date dueAt) {
        Integer seq = followTouchMapper.selectCount(new QueryWrapper<TZwFollowTouch>()
                .eq("follow_id", f.getId()));
        TZwFollowTouch touch = new TZwFollowTouch();
        touch.setFollowId(f.getId());
        touch.setSeq((seq == null ? 0 : seq.intValue()) + 1);
        touch.setPlanAt(now);
        touch.setDueAt(dueAt);
        touch.setContactName(user != null ? user.getContactName() : f.getContactName());
        touch.setContactPhone(user != null ? user.getContactPhone() : f.getContactPhone());
        touch.setPrevTouchAt(f.getLastTouchAt());
        touch.setStatus(FollowCycle.TOUCH_PENDING);
        followTouchMapper.insert(touch);
    }

    private static Date plusDays(Date base, int days) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(base);
        c.add(java.util.Calendar.DAY_OF_MONTH, days);
        return c.getTime();
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

    private static boolean int1(Integer v) {
        return v != null && v.intValue() == 1;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static TZwFollowTouch reject(List<String> msg, String text) {
        if (msg != null) {
            msg.clear();
            msg.add(text);
        }
        return null;
    }
}
