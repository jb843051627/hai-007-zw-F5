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
 * 异常判定过程流水对象 t_zw_abn_trace（只追加；登记/订正/上报/复测开单/归档各落一笔）。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_zw_abn_trace")
@ApiModel(value = "TZwAbnTrace", description = "异常判定过程流水")
public class TZwAbnTrace implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long caseId;

    @TableField("seq")
    private Integer seq;

    /** REGISTER/CORRECT/REPORT/RETEST_OPEN/ARCHIVE */
    @TableField("action")
    private String action;

    @TableField("from_status")
    private Integer fromStatus;

    @TableField("to_status")
    private Integer toStatus;

    @TableField("detail")
    private String detail;

    @TableField("operator")
    private String operator;

    @TableField("action_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actionTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getFromStatus() { return fromStatus; }
    public void setFromStatus(Integer fromStatus) { this.fromStatus = fromStatus; }
    public Integer getToStatus() { return toStatus; }
    public void setToStatus(Integer toStatus) { this.toStatus = toStatus; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Date getActionTime() { return actionTime; }
    public void setActionTime(Date actionTime) { this.actionTime = actionTime; }
}
