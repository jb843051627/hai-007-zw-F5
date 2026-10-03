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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 复测环节填写台账对象 t_zw_retest_step（只追加）。
 * 每次提交一笔 attempt_no 递增；与上一环核对不符(match_flag=0)原样留痕、不开下一环；
 * 有效行(status=1)每环节至多一笔，落名后对其他人只读。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_retest_step")
@ApiModel(value = "TZwRetestStep", description = "复测环节填写台账")
public class TZwRetestStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("order_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @TableField("step_no")
    private Integer stepNo;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("fill_item")
    private String fillItem;

    @TableField("fill_qty")
    private BigDecimal fillQty;

    @TableField("fill_test_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date fillTestAt;

    @TableField("prev_item")
    private String prevItem;

    @TableField("prev_qty")
    private BigDecimal prevQty;

    @TableField("prev_test_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date prevTestAt;

    @TableField("prev_result")
    private Integer prevResult;

    /** 本环节结论 1通过/同意 2否决；环节0 无结论 */
    @TableField("step_result")
    private Integer stepResult;

    @TableField("opinion")
    private String opinion;

    @TableField("match_flag")
    private Integer matchFlag;

    /** 0核对不符留痕(未生效) 1有效落名 */
    @TableField("status")
    private Integer status;

    @TableField("fill_by")
    private String fillBy;

    @TableField("fill_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date fillTime;

    @TableField("del_flag")
    private Integer delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getStepNo() { return stepNo; }
    public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
    public Integer getAttemptNo() { return attemptNo; }
    public void setAttemptNo(Integer attemptNo) { this.attemptNo = attemptNo; }
    public String getFillItem() { return fillItem; }
    public void setFillItem(String fillItem) { this.fillItem = fillItem; }
    public BigDecimal getFillQty() { return fillQty; }
    public void setFillQty(BigDecimal fillQty) { this.fillQty = fillQty; }
    public Date getFillTestAt() { return fillTestAt; }
    public void setFillTestAt(Date fillTestAt) { this.fillTestAt = fillTestAt; }
    public String getPrevItem() { return prevItem; }
    public void setPrevItem(String prevItem) { this.prevItem = prevItem; }
    public BigDecimal getPrevQty() { return prevQty; }
    public void setPrevQty(BigDecimal prevQty) { this.prevQty = prevQty; }
    public Date getPrevTestAt() { return prevTestAt; }
    public void setPrevTestAt(Date prevTestAt) { this.prevTestAt = prevTestAt; }
    public Integer getPrevResult() { return prevResult; }
    public void setPrevResult(Integer prevResult) { this.prevResult = prevResult; }
    public Integer getStepResult() { return stepResult; }
    public void setStepResult(Integer stepResult) { this.stepResult = stepResult; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public Integer getMatchFlag() { return matchFlag; }
    public void setMatchFlag(Integer matchFlag) { this.matchFlag = matchFlag; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getFillBy() { return fillBy; }
    public void setFillBy(String fillBy) { this.fillBy = fillBy; }
    public Date getFillTime() { return fillTime; }
    public void setFillTime(Date fillTime) { this.fillTime = fillTime; }
    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
}
