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
 * 异常复测单头对象 t_zw_retest_order。
 * 上报成功后系统自动从异常单开一条（uk case_id 一单一条）；按 0化验室→1水质科→2主管 环节链走，否决即止。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_retest_order")
@ApiModel(value = "TZwRetestOrder", description = "异常复测单")
public class TZwRetestOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("retest_no")
    private String retestNo;

    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long caseId;

    @TableField("case_no")
    private String caseNo;

    @TableField("site_no")
    private String siteNo;

    @TableField("item_code")
    private String itemCode;

    /** 0化验室检测 1水质科复核 2主管审定 3走完全程 */
    @TableField("current_step")
    private Integer currentStep;

    /** 0在办 1全程通过 2否决终态 */
    @TableField("status")
    private Integer status;

    @TableField("open_by")
    private String openBy;

    @TableField("open_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date openTime;

    @TableField("finish_by")
    private String finishBy;

    @TableField("finish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishTime;

    @TableField("final_result")
    private Integer finalResult;

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
    public String getRetestNo() { return retestNo; }
    public void setRetestNo(String retestNo) { this.retestNo = retestNo; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public String getSiteNo() { return siteNo; }
    public void setSiteNo(String siteNo) { this.siteNo = siteNo; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getOpenBy() { return openBy; }
    public void setOpenBy(String openBy) { this.openBy = openBy; }
    public Date getOpenTime() { return openTime; }
    public void setOpenTime(Date openTime) { this.openTime = openTime; }
    public String getFinishBy() { return finishBy; }
    public void setFinishBy(String finishBy) { this.finishBy = finishBy; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
    public Integer getFinalResult() { return finalResult; }
    public void setFinalResult(Integer finalResult) { this.finalResult = finalResult; }
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
