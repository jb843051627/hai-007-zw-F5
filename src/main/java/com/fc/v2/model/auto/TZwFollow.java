package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 供水户盯办单头对象 t_zw_follow。
 * 异常上报同刻系统自开，复测全程通过、异常归档同刻系统自结；没有人工开/闭口。盯办周期另算。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_follow")
@ApiModel(value = "TZwFollow", description = "供水户盯办单")
public class TZwFollow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("follow_no")
    private String followNo;

    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long caseId;

    @TableField("case_no")
    private String caseNo;

    @TableField("site_no")
    private String siteNo;

    @TableField("water_user_name")
    private String waterUserName;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("period_days")
    private Integer periodDays;

    @TableField("last_touch_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastTouchAt;

    @TableField("next_due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date nextDueAt;

    /** 0在盯 1已结（只有系统能结） */
    @TableField("status")
    private Integer status;

    @TableField("open_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date openTime;

    @TableField("close_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date closeTime;

    @TableField("close_note")
    private String closeNote;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableField("remark")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFollowNo() { return followNo; }
    public void setFollowNo(String followNo) { this.followNo = followNo; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public String getSiteNo() { return siteNo; }
    public void setSiteNo(String siteNo) { this.siteNo = siteNo; }
    public String getWaterUserName() { return waterUserName; }
    public void setWaterUserName(String waterUserName) { this.waterUserName = waterUserName; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Integer getPeriodDays() { return periodDays; }
    public void setPeriodDays(Integer periodDays) { this.periodDays = periodDays; }
    public Date getLastTouchAt() { return lastTouchAt; }
    public void setLastTouchAt(Date lastTouchAt) { this.lastTouchAt = lastTouchAt; }
    public Date getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Date nextDueAt) { this.nextDueAt = nextDueAt; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getOpenTime() { return openTime; }
    public void setOpenTime(Date openTime) { this.openTime = openTime; }
    public Date getCloseTime() { return closeTime; }
    public void setCloseTime(Date closeTime) { this.closeTime = closeTime; }
    public String getCloseNote() { return closeNote; }
    public void setCloseNote(String closeNote) { this.closeNote = closeNote; }
    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
