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
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 水质异常判定登记台账对象 t_zw_abn_case。
 *
 * <p>异常一判出来系统自动登记；判定三件套（档位/标准线/生效日）、检测值、对照界值、超量登记即钉死。
 * 上报前只准订正一次判定结论；上报后只读不可撤不可改名；归档结案锁死。
 * uk(site_no,test_date,item_code) 保证同点位同一天同一指标只成立一单。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_abn_case")
@ApiModel(value = "TZwAbnCase", description = "水质异常判定登记台账")
public class TZwAbnCase implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("case_no")
    private String caseNo;

    @TableField("imp_row_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long impRowId;

    @TableField("batch_no")
    private String batchNo;

    @TableField("row_no")
    private Integer rowNo;

    @TableField("site_no")
    private String siteNo;

    @TableField("item_code")
    private String itemCode;

    @TableField("sample_type")
    private String sampleType;

    @TableField("test_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date testAt;

    @TableField("test_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date testDate;

    @TableField("qty")
    private BigDecimal qty;

    @TableField("grade_level")
    private Integer gradeLevel;

    @TableField("limit_value")
    private BigDecimal limitValue;

    @TableField("over_qty")
    private BigDecimal overQty;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_eff_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ruleEffDate;

    @TableField("need_water_stop")
    private Integer needWaterStop;

    @TableField("need_retest")
    private Integer needRetest;

    @TableField("verdict_note")
    private String verdictNote;

    @TableField("revise_no")
    private Integer reviseNo;

    @TableField("status")
    @ApiModelProperty(value = "0已登记可订正 1已上报只读 2已归档结案")
    private Integer status;

    @TableField("report_by")
    private String reportBy;

    @TableField("report_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reportTime;

    @TableField("archive_by")
    private String archiveBy;

    @TableField("archive_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date archiveTime;

    @TableField("archive_note")
    private String archiveNote;

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
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public Long getImpRowId() { return impRowId; }
    public void setImpRowId(Long impRowId) { this.impRowId = impRowId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getSiteNo() { return siteNo; }
    public void setSiteNo(String siteNo) { this.siteNo = siteNo; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getSampleType() { return sampleType; }
    public void setSampleType(String sampleType) { this.sampleType = sampleType; }
    public Date getTestAt() { return testAt; }
    public void setTestAt(Date testAt) { this.testAt = testAt; }
    public Date getTestDate() { return testDate; }
    public void setTestDate(Date testDate) { this.testDate = testDate; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public Integer getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
    public BigDecimal getLimitValue() { return limitValue; }
    public void setLimitValue(BigDecimal limitValue) { this.limitValue = limitValue; }
    public BigDecimal getOverQty() { return overQty; }
    public void setOverQty(BigDecimal overQty) { this.overQty = overQty; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Date getRuleEffDate() { return ruleEffDate; }
    public void setRuleEffDate(Date ruleEffDate) { this.ruleEffDate = ruleEffDate; }
    public Integer getNeedWaterStop() { return needWaterStop; }
    public void setNeedWaterStop(Integer needWaterStop) { this.needWaterStop = needWaterStop; }
    public Integer getNeedRetest() { return needRetest; }
    public void setNeedRetest(Integer needRetest) { this.needRetest = needRetest; }
    public String getVerdictNote() { return verdictNote; }
    public void setVerdictNote(String verdictNote) { this.verdictNote = verdictNote; }
    public Integer getReviseNo() { return reviseNo; }
    public void setReviseNo(Integer reviseNo) { this.reviseNo = reviseNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getReportBy() { return reportBy; }
    public void setReportBy(String reportBy) { this.reportBy = reportBy; }
    public Date getReportTime() { return reportTime; }
    public void setReportTime(Date reportTime) { this.reportTime = reportTime; }
    public String getArchiveBy() { return archiveBy; }
    public void setArchiveBy(String archiveBy) { this.archiveBy = archiveBy; }
    public Date getArchiveTime() { return archiveTime; }
    public void setArchiveTime(Date archiveTime) { this.archiveTime = archiveTime; }
    public String getArchiveNote() { return archiveNote; }
    public void setArchiveNote(String archiveNote) { this.archiveNote = archiveNote; }
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
