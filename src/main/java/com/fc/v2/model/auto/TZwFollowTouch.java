package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.Date;

/**
 * 供水户盯办流水对象 t_zw_follow_touch（系统开、经办填、只追加）。
 * 系统每到点自开一笔；经办人只能填结果，落名只读。每笔带上一次盯办时间。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_follow_touch")
@ApiModel(value = "TZwFollowTouch", description = "供水户盯办流水")
public class TZwFollowTouch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("follow_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followId;

    @TableField("seq")
    private Integer seq;

    @TableField("plan_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date planAt;

    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dueAt;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("prev_touch_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date prevTouchAt;

    /** REACHED/NOT_REACHABLE/WRONG_CONTACT */
    @TableField("result")
    private String result;

    @TableField("detail")
    private String detail;

    @TableField("actual_touch_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualTouchAt;

    @TableField("fill_by")
    private String fillBy;

    @TableField("fill_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date fillTime;

    /** 0待办 1已办 */
    @TableField("status")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFollowId() { return followId; }
    public void setFollowId(Long followId) { this.followId = followId; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
    public Date getPlanAt() { return planAt; }
    public void setPlanAt(Date planAt) { this.planAt = planAt; }
    public Date getDueAt() { return dueAt; }
    public void setDueAt(Date dueAt) { this.dueAt = dueAt; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Date getPrevTouchAt() { return prevTouchAt; }
    public void setPrevTouchAt(Date prevTouchAt) { this.prevTouchAt = prevTouchAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getActualTouchAt() { return actualTouchAt; }
    public void setActualTouchAt(Date actualTouchAt) { this.actualTouchAt = actualTouchAt; }
    public String getFillBy() { return fillBy; }
    public void setFillBy(String fillBy) { this.fillBy = fillBy; }
    public Date getFillTime() { return fillTime; }
    public void setFillTime(Date fillTime) { this.fillTime = fillTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
