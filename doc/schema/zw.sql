-- zw 城市供水水质检测调度与达标管理 -- schema (hai-007)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：hai_007

CREATE TABLE IF NOT EXISTS t_zw_arch_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '水源地/水厂档案卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本次序',
  site_id int DEFAULT NULL COMMENT '所属底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底档代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡供水规模(万吨/日)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增供水规模(万吨/日)',
  content varchar(255) DEFAULT NULL COMMENT '校验串（随版本次序走）',
  grade_level int DEFAULT NULL COMMENT '供水规模折到的保障强度层级',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水源地/水厂档案卡';

CREATE TABLE IF NOT EXISTS t_zw_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '处置工单号',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底档代号（只认代号不认名字）',
  stage int DEFAULT NULL COMMENT '当前处置段 0..3（登记/初检/处置/复核封卷）',
  status int DEFAULT NULL COMMENT '工单落定 0未起 1在办 2已封存',
  content varchar(255) DEFAULT NULL COMMENT '工单记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水质异常处置工单';

-- 导入批次头：一批一次定论。整批拦截/放行、核实成或不成、归档只读都挂在这张表上；
-- 行表 t_zw_imp_row 顺着 batch_no 往下钻。归档行只追加不修改（archived=1 后任何写口拒绝）。
CREATE TABLE IF NOT EXISTS t_zw_imp_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '检测数据上传批次号（全表唯一）',
  file_name varchar(255) DEFAULT NULL COMMENT '原始 Excel 文件名',
  total_rows int DEFAULT NULL COMMENT '本批数据行数',
  blocked int DEFAULT '0' COMMENT '整批拦截 0全格放行已入库 1存在硬错整批拦在库外',
  block_msg varchar(500) DEFAULT NULL COMMENT '拦截说明（点名第几行什么错）',
  content_hash varchar(64) DEFAULT NULL COMMENT '本批原始行内容指印（同内容重导幂等放行）',
  verify_result int DEFAULT NULL COMMENT '核实结论 1核实成 2核实不成；空=尚未核实',
  verify_reason varchar(500) DEFAULT NULL COMMENT '核实不成原因（核实成时记归档说明，可空）',
  verify_by varchar(64) DEFAULT NULL COMMENT '核实人（以核实那一次写下的为准）',
  verify_time datetime DEFAULT NULL COMMENT '核实时刻（系统日志同刻可翻到是哪一次写的）',
  archived int DEFAULT '0' COMMENT '归档标识 0未归档 1已归档只读（原样锁住不能再改）',
  archive_time datetime DEFAULT NULL COMMENT '归档时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_zw_imp_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测数据导入批次头（一批一次核实）';

CREATE TABLE IF NOT EXISTS t_zw_imp_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '检测数据上传批次号',
  row_no int DEFAULT NULL COMMENT '原表行次',
  site_no varchar(64) DEFAULT NULL COMMENT '采样点位代号（随样本入账，水质核实只认代号）',
  item_code varchar(64) DEFAULT NULL COMMENT '被校验出的数据项代号（读数明细项）',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行检测读数（水位）',
  sample_type varchar(32) DEFAULT NULL COMMENT '样本类别(水源水/出厂水/管网末梢)',
  test_at datetime DEFAULT NULL COMMENT '检测发生那天（用哪条线只看这天，不看今天）',
  grade_level int DEFAULT NULL COMMENT '评价等级 1合格 2关注 3超标 4严重超标；空=未定级(无可用标准)',
  rule_code varchar(64) DEFAULT NULL COMMENT '定级所依标准线代号（服务层回值钉死，写过不再改）',
  rule_eff_date datetime DEFAULT NULL COMMENT '所依标准的生效那天（历史复核只认此编号上下文）',
  declared_rule_code varchar(64) DEFAULT NULL COMMENT 'Excel 里申报的规则代号（与检测当天现行线对不上即整批拦截）',
  raw_line varchar(1000) DEFAULT NULL COMMENT '原始行原文（按原列序逐格拼接，下钻与归档都只看这条原样）',
  valid_flag int DEFAULT '1' COMMENT '是否有效（Excel 随单申报）1有效 0无效',
  reviewed_flag int DEFAULT '0' COMMENT '是否已复核（Excel 随单申报）1已复核 0未复核',
  check_status int DEFAULT '0' COMMENT '校验结论 0待校 1格式合格 2异常(硬错：读数/点位/时间/规则对不上)',
  check_msg varchar(500) DEFAULT NULL COMMENT '校验说明（每条都要说清楚能不能过、为什么）',
  confirm_by varchar(64) DEFAULT NULL COMMENT '异常确认人（生成→确认→核实→归档逐段留痕）',
  confirm_time datetime DEFAULT NULL COMMENT '异常确认时刻',
  verify_result int DEFAULT NULL COMMENT '核实结论（随批次那一次核实逐行回写）1核实成 2核实不成',
  verify_reason varchar(500) DEFAULT NULL COMMENT '核实不成原因（随批次回写）',
  verify_by varchar(64) DEFAULT NULL COMMENT '核实人（随批次回写，以那次为准）',
  verify_time datetime DEFAULT NULL COMMENT '核实时间（随批次回写）',
  archived int DEFAULT '0' COMMENT '归档标识 0未归档 1已归档只读（随批次锁定）',
  archive_time datetime DEFAULT NULL COMMENT '归档时刻',
  status int DEFAULT NULL COMMENT '行落地情形 0未入正式账(整批拦在库外) 1已入库',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除（同号重导时旧版行置删留痕）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_zw_imp_row_batch (batch_no, row_no),
  KEY idx_zw_imp_row_site (site_no, test_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测数据导入核对行（明细台账，报表只照此生成）';

CREATE TABLE IF NOT EXISTS t_zw_lim_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '水质限值判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判定线名',
  sample_type varchar(32) DEFAULT NULL COMMENT '适用样本类别(水源水/出厂水/管网末梢)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '关注档指标上限',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '超标档指标上限',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '严重超标档指标上限',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '判定线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '判定线情形 0现行 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水质指标限值分档判定线';

CREATE TABLE IF NOT EXISTS t_zw_sam_plan (
  id bigint NOT NULL COMMENT '主键',
  plan_no varchar(64) DEFAULT NULL COMMENT '检测计划周期代号',
  site_no varchar(64) DEFAULT NULL COMMENT '点位代号（派单时带入事项条）',
  team_id bigint DEFAULT NULL COMMENT '直挂承接班组(部门主键)；空=派发时按各班组当前负载加权分派',
  period_days int DEFAULT NULL COMMENT '周期自然日数（隔多少天采一次）',
  amount decimal(12,2) DEFAULT NULL COMMENT '提前浮出待办的自然日数（全站唯一可调档）',
  first_due date DEFAULT NULL COMMENT '首个约定采样日（其后按周期顺排）',
  eff_start datetime DEFAULT NULL COMMENT '计划生效之时',
  eff_end datetime DEFAULT NULL COMMENT '计划停用之时(不含)；空=长期现行',
  content varchar(255) DEFAULT NULL COMMENT '采样事由',
  status int DEFAULT NULL COMMENT '计划情形 0现行 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sam_plan_no (plan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采样检测计划周期底档';

CREATE TABLE IF NOT EXISTS t_zw_sam_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '采样事项号（计划代号-采样日，系统一次生成）',
  plan_no varchar(64) DEFAULT NULL COMMENT '检测计划周期代号（底档带入）',
  site_no varchar(64) DEFAULT NULL COMMENT '点位代号（底档带入）',
  team_id bigint DEFAULT NULL COMMENT '承接班组(部门主键)；空=派发时按当前负载加权分派后回写',
  due_at datetime DEFAULT NULL COMMENT '约定采样那一天的终结时刻(当日23:59:59)——全站唯一口径，任何动作不得挪动',
  amount decimal(12,2) DEFAULT NULL COMMENT '提前浮出待办的自然日数（唯一可调档）',
  dispatch_at datetime DEFAULT NULL COMMENT '真正派出时刻（首次钉库，后到动作不得覆盖）',
  cancel_at datetime DEFAULT NULL COMMENT '撤单时刻',
  content varchar(255) DEFAULT NULL COMMENT '采样事由与受派班组记要',
  hang_reason varchar(255) DEFAULT NULL COMMENT '挂起原因（点位退役/班组停班/无班可派）',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2挂起 3撤单 4转作他理',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注（撤单/转理留痕追加于此）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sam_item_no (item_no),
  UNIQUE KEY uk_sam_plan_site_due (plan_no, site_no, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采样任务到期派单';

CREATE TABLE IF NOT EXISTS t_zw_source (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '水源地/水厂档案代号',
  site_name varchar(128) DEFAULT NULL COMMENT '档案名称',
  site_type varchar(32) DEFAULT NULL COMMENT '档案类别(水源地/水厂)',
  road_name varchar(128) DEFAULT NULL COMMENT '所属行政区划(省—市—区—街道)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单档供水规模一档上限(万吨/日)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(万吨/日)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(万吨/日)',
  status int DEFAULT NULL COMMENT '底档情形 0在用 1已退役',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水源地/水厂底档';

-- 旧版停供单带手填计数列（sign_mode/need_count/sign_count），与"枚数顺着签核记录点算"不相容，结构重建。
DROP TABLE IF EXISTS t_zw_stop_bill;
CREATE TABLE IF NOT EXISTS t_zw_stop_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停供/限供申请签核单号（点位代号-年-当年序）',
  site_no varchar(64) DEFAULT NULL COMMENT '水源点代号（只认代号不认名字）',
  bill_year int DEFAULT NULL COMMENT '申请所属自然年（当年序按此归年）',
  year_seq int DEFAULT NULL COMMENT '同一水源点当年第几份申请（第二份当场单独点名）',
  reason varchar(32) DEFAULT NULL COMMENT '停水事由代号 DEPLETED水源枯竭/OVER_LIMIT指标连续超标/REPAIR检修',
  alt_plan varchar(500) DEFAULT NULL COMMENT '停水后的替代供水方案',
  notice_text varchar(500) DEFAULT NULL COMMENT '用户公告文案（按事由从名录带出，不经人手抄录）',
  node_no int DEFAULT NULL COMMENT '当前所在道 0业务提 1水质核 2调度评 3主管准',
  round_no int DEFAULT '1' COMMENT '签核轮次；主管打回一轮旧签核整轮作废，补签另起一轮，两轮不摆在一起',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已终止（不同意/撤回）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stop_bill_no (bill_no),
  KEY idx_stop_site_year (site_no, bill_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停供/限供申请签核单';

-- 签核台账：只追加，任何关口动作都在此落一笔；关口过没过、落了几枚名，全由服务层顺着这张表点算。
CREATE TABLE IF NOT EXISTS t_zw_stop_sign (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属签核单',
  round_no int NOT NULL COMMENT '落印轮次（作废轮原样留痕，计数只数当前轮）',
  node_no int NOT NULL COMMENT '关口 0业务提 1水质核 2调度评 3主管准',
  sign_role varchar(32) NOT NULL COMMENT '落名口 BUSINESS经办/WATER_QUA水质核实/DISP_QTY水量调配评估/DISP_NET管网影响评估/DISP_CHIEF调度主管/APPLICANT申请人',
  signer varchar(64) NOT NULL COMMENT '落名人（各评各的，互不替签）',
  action varchar(16) NOT NULL COMMENT '落印动作 AGREE同意/DISAGREE不同意/WITHDRAW撤回/SENDBACK主管打回',
  opinion varchar(500) DEFAULT NULL COMMENT '签核意见',
  sign_time datetime NOT NULL COMMENT '落印时刻（同一分钟两枚仍是两笔）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常（只追加，不置删）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stop_sign_slot (bill_id, round_no, node_no, sign_role),
  KEY idx_stop_sign_bill (bill_id, round_no, node_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停供申请签核台账（只追加）';

-- 初始档案数据：id=0 在用、id=1 已退役
-- 供水规模上限 30/80/200 万吨/日：等于上限算高一档。
INSERT IGNORE INTO t_zw_source (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'ZS00', '青坪市城北水源地（在用）', '水源地', '青坪省—青坪市—城东区—东湖街道', 30.00, 80.00, 200.00, 0, 0, 'seed', NOW()),
       (1, 'ZS01', '青坪市城南水厂（已退役）', '水厂', '青坪省—青坪市—城南区—望湖街道', 30.00, 80.00, 200.00, 1, 0, 'seed', NOW());

-- 检测计划周期底档种子：
-- PL-ZS00-M：城北水源地月度例行（30 天一周期，提前 3 天浮出，班组不直挂→按负载加权派班）；
-- PL-ZS01-Q：城南水厂季度（直挂 2 号班组；点位已退役，开单环节直接跳过、不生成新派单；
--     若库里还有退役前落下的待派条，跑轮时仍就地转挂起——旧账可查、新账不开）。
INSERT IGNORE INTO t_zw_sam_plan (id, plan_no, site_no, team_id, period_days, amount, first_due, eff_start, eff_end, content, status, del_flag, create_by, create_time)
VALUES (10, 'PL-ZS00-M', 'ZS00', NULL, 30, 3.00, '2026-09-01', '2026-09-01 00:00:00', NULL, '城北水源地月度例行采样', 0, 0, 'seed', NOW()),
       (11, 'PL-ZS01-Q', 'ZS01', 2, 90, 3.00, '2026-09-15', '2026-09-15 00:00:00', NULL, '城南水厂季度采样（点位已退役，开单即挂起）', 0, 0, 'seed', NOW());

-- 水质限值分档判定线种子（出厂水余氯 mg/L）：
-- 100 旧线 CL-FW-2024：上年口径，上限松，严重超标一格早期入册本就留空（空=不设此档，随超标档下推），
--     管到 2026-01-01（不含），现已让位，只作回溯依据，不参与现行评价；
-- 101 新线 CL-FW-2026：今年收紧，自 2026-01-01 00:00:00 起现行——交接日全厂一套口径，旧线管到进程、新线从结局起算；
-- 102/103 水源水、管网末梢各一条，与出厂水各管各的。
-- 读数值与界值相同算高档；顺位相同并行使权时按代号序列排后者胜出（确定性，四季一致）。
INSERT IGNORE INTO t_zw_lim_rule (id, rule_code, rule_name, sample_type, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time, remark)
VALUES (100, 'CL-FW-2024', '出厂水余氯分档线(上年口径)', '出厂水', 0.30, 0.50, NULL, '2024-01-01 00:00:00', '2026-01-01 00:00:00', 10, 1, 0, 'seed', NOW(), '上年宽口径；严重超标档留空即不设此档'),
       (101, 'CL-FW-2026', '出厂水余氯分档线(今年收紧)', '出厂水', 0.20, 0.40, 0.60, '2026-01-01 00:00:00', NULL, 10, 0, 0, 'seed', NOW(), '今年收紧口径'),
       (102, 'CL-SW-2026', '水源水余氯分档线', '水源水', 0.20, 0.30, 0.50, '2026-01-01 00:00:00', NULL, 10, 0, 0, 'seed', NOW(), NULL),
       (103, 'CL-PN-2026', '管网末梢余氯分档线', '管网末梢', 0.15, 0.25, 0.40, '2026-01-01 00:00:00', NULL, 10, 0, 0, 'seed', NOW(), NULL);

-- 停水公告名录（系统字典 zw_stop_notice）：文案按停水事由从名录带出，页面只展示、不另请人抄录。
-- dict_value=停水事由代号，dict_label=名录短名，remark=完整公告模板。
INSERT IGNORE INTO t_sys_dict_data (id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (880001, 1, '水源枯竭停水公告', 'DEPLETED', 'zw_stop_notice', '', 'warning', 'N', '0', 'seed', NOW(),
        '因{site}水源枯竭，定于停水时段暂停供水，请受影响用户提前蓄水，饮用水请煮沸后使用。'),
       (880002, 2, '指标连续超标停水公告', 'OVER_LIMIT', 'zw_stop_notice', '', 'danger', 'N', '0', 'seed', NOW(),
        '因{site}水质指标连续超标，为保障用水安全，定于停水时段暂停供水，恢复时间另行通知。'),
       (880003, 3, '水厂检修停水公告', 'REPAIR', 'zw_stop_notice', '', 'info', 'N', '0', 'seed', NOW(),
        '因{site}设施检修，定于停水时段暂停供水，检修完成后即时恢复，请用户提前做好储水准备。');

-- 城北水源地近三个月水源水余氯检测行：0.62/0.65/0.71，对现行线 CL-SW-2026 连续超标，供第二道核实对照。
-- 三行各属一个批次（历史导入，格式合格、已入库），本期/上期差异按同批次口径点算。
INSERT IGNORE INTO t_zw_imp_row (id, batch_no, row_no, site_no, item_code, qty, sample_type, test_at, grade_level, rule_code, rule_eff_date, valid_flag, reviewed_flag, check_status, check_msg, status, del_flag, create_by, create_time)
VALUES (9001, 'B202607', 1, 'ZS00', 'CL', 0.62, '水源水', '2026-07-18 09:00:00', 4, 'CL-SW-2026', '2026-01-01 00:00:00', 1, 1, 1, '格式合格', 1, 0, 'seed', NOW()),
       (9002, 'B202608', 1, 'ZS00', 'CL', 0.65, '水源水', '2026-08-18 09:00:00', 4, 'CL-SW-2026', '2026-01-01 00:00:00', 1, 1, 1, '格式合格', 1, 0, 'seed', NOW()),
       (9003, 'B202609', 1, 'ZS00', 'CL', 0.71, '水源水', '2026-09-18 09:00:00', 4, 'CL-SW-2026', '2026-01-01 00:00:00', 1, 0, 1, '格式合格', 1, 0, 'seed', NOW());

-- 化验室检测数据导入：菜单与按钮权限（目录 880100，挂在根下；只读到处人人可查，写口单独授）。
INSERT IGNORE INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES (880100, '检测数据导入核实', '水质检测读数 Excel 导入、异常核实与归档报表', '/zwImpRow/view', 0, 0, 'zw:zwImpRow:view', 1, 'layui-icon layui-icon-form', 60, 0, 'seed', NOW()),
       (880101, '导入明细台账', '批次与逐行明细查询（只读，人人可查归档结果）', '/zwImpRow/list', 0, 880100, 'zw:zwImpRow:list', 2, NULL, NULL, 0, 'seed', NOW()),
       (880102, 'Excel 整批导入', '当月数据整批导入校验，硬错整批拦在库外', '/zwImpRow/import', 0, 880100, 'zw:zwImpRow:import', 2, NULL, NULL, 0, 'seed', NOW()),
       (880103, '异常确认', '把异常标成已确认，进入待核实', '/zwImpRow/confirm', 0, 880100, 'zw:zwImpRow:confirm', 2, NULL, NULL, 0, 'seed', NOW()),
       (880104, '批次核实归档', '一批一次核实，核实成归档锁行、核实不成写原因', '/zwImpRow/verify', 0, 880100, 'zw:zwImpRow:verify', 2, NULL, NULL, 0, 'seed', NOW()),
       (880105, '明细导出', '按点位/有效/复核导出 UTF-8 Excel', '/zwImpRow/export', 0, 880100, 'zw:zwImpRow:export', 2, NULL, NULL, 0, 'seed', NOW());
-- 管理员角色直接挂上全套
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES (8801001, 488243256161730560, 880100, 'seed', NOW()),
       (8801002, 488243256161730560, 880101, 'seed', NOW()),
       (8801003, 488243256161730560, 880102, 'seed', NOW()),
       (8801004, 488243256161730560, 880103, 'seed', NOW()),
       (8801005, 488243256161730560, 880104, 'seed', NOW()),
       (8801006, 488243256161730560, 880105, 'seed', NOW());
-- 只读到处人人可查：普通「用户」角色也给菜单、台账查询与导出（导出=只读取数），写口不授
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES (8801011, 488289006124007424, 880100, 'seed', NOW()),
       (8801012, 488289006124007424, 880101, 'seed', NOW()),
       (8801013, 488289006124007424, 880105, 'seed', NOW());

-- 未完结处置单演示：ZS02 名下挂着在办工单，该点的停供申请到第二道即被压住，不得往前。
INSERT IGNORE INTO t_zw_source (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (2, 'ZS02', '青坪市青西水源地（在用·有在办处置单）', '水源地', '青坪省—青坪市—城西区—青西街道', 30.00, 80.00, 200.00, 0, 0, 'seed', NOW());
INSERT IGNORE INTO t_zw_case_flow (id, biz_no, site_no, stage, status, content, last_action, del_flag, create_by, create_time)
VALUES (9101, 'CF-202609-01', 'ZS02', 1, 1, '余氯异常处置中', '初检', 0, 'seed', NOW());


-- ============================================================================
-- 水质异常判定留痕（hai-007 第二期）：
-- 判定一出来自动登记 → 上报前可订正一次 → 上报（同点位同日同指标唯一一单，
-- 报后只读、不可撤、不可改名）→ 复测单按环节开权限、逐步复录上一步 → 末环通过系统归档；
-- 上报同时系统自开供水户盯办单，盯办按另算的周期自开自结，只追加、不倒改。
-- ============================================================================

-- 供水户联系名录：异常上报反映到供水户名下、盯办该找谁，都只从这张名录现取（只读底档，页面不另请人手填）。
CREATE TABLE IF NOT EXISTS t_zw_water_user (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) NOT NULL COMMENT '所属点位代号（只认代号不认名字）',
  user_name varchar(128) DEFAULT NULL COMMENT '供水户名称',
  contact_name varchar(64) DEFAULT NULL COMMENT '联系人（盯办单开单时照此快照）',
  contact_phone varchar(32) DEFAULT NULL COMMENT '联系电话',
  status int DEFAULT '0' COMMENT '名录情形 0现行 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_zw_water_user_site (site_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供水户联系名录（盯办取人唯一出处）';

-- 异常判定登记台账：异常一判出来（导入定级为关注/超标/严重超标）系统自动落一条，页面没有新增口。
-- 判定三件套（档位/标准线代号/标准生效日）、检测值与对照界值、超量、停水复测建议登记即钉死；
-- 上报前只准订正一次判定结论（档位/建议/说明），点位/哪天的水/指标/检测值/标准快照一根不能动。
-- uk(site_no,test_date,item_code)：同点位同一天同一指标只成立一单，第二单整单不成立。
CREATE TABLE IF NOT EXISTS t_zw_abn_case (
  id bigint NOT NULL COMMENT '主键',
  case_no varchar(64) NOT NULL COMMENT '异常登记单号（系统一次生成，全表唯一）',
  imp_row_id bigint DEFAULT NULL COMMENT '原始检测记录行（t_zw_imp_row.id，点回去看当日原记录）',
  batch_no varchar(64) DEFAULT NULL COMMENT '原始检测批次号（随登记钉死）',
  row_no int DEFAULT NULL COMMENT '原始行次',
  site_no varchar(64) DEFAULT NULL COMMENT '哪个点位（代号）',
  item_code varchar(64) DEFAULT NULL COMMENT '哪个指标',
  sample_type varchar(32) DEFAULT NULL COMMENT '样本类别(水源水/出厂水/管网末梢)',
  test_at datetime DEFAULT NULL COMMENT '哪天的水（检测发生时刻）',
  test_date datetime DEFAULT NULL COMMENT '检测当日零点（业务时区，唯一键按天归堆）',
  qty decimal(12,2) DEFAULT NULL COMMENT '当天检测值',
  grade_level int DEFAULT NULL COMMENT '判定档位 2关注 3超标 4严重超标（可订正一次）',
  limit_value decimal(12,2) DEFAULT NULL COMMENT '判定时对照的那条界值（钉死）',
  over_qty decimal(12,2) DEFAULT NULL COMMENT '超了多少（检测值减对照界值，登记时算死）',
  rule_code varchar(64) DEFAULT NULL COMMENT '哪条标准线（代号，钉死）',
  rule_name varchar(128) DEFAULT NULL COMMENT '标准线名（登记时快照，让位也能照名翻）',
  rule_eff_date datetime DEFAULT NULL COMMENT '当天生效的标准是哪张（生效日，钉死）',
  need_water_stop int DEFAULT '0' COMMENT '要不要停水 0不要 1要（按档位默认，可订正一次）',
  need_retest int DEFAULT '0' COMMENT '要不要复测 0不要 1要（按档位默认，可订正一次）',
  verdict_note varchar(500) DEFAULT NULL COMMENT '判定说明（订正即在此留话）',
  revise_no int DEFAULT '0' COMMENT '订正轮次（0未订正；只准订正一次，1 后写口全闭）',
  status int DEFAULT '0' COMMENT '台账情形 0已登记(未上报,可订正) 1已上报(只读,已反映到供水户) 2已归档结案',
  report_by varchar(64) DEFAULT NULL COMMENT '上报人',
  report_time datetime DEFAULT NULL COMMENT '上报时刻（同一笔异常只准一次）',
  archive_by varchar(64) DEFAULT NULL COMMENT '归档人（系统归档记 system）',
  archive_time datetime DEFAULT NULL COMMENT '归档结案时刻',
  archive_note varchar(500) DEFAULT NULL COMMENT '归档说明',
  del_flag int DEFAULT '0' COMMENT '删除标记（台账只留不删，任何写口都不置删）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_abn_case_no (case_no),
  UNIQUE KEY uk_abn_site_day_item (site_no, test_date, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水质异常判定登记台账（自动登记、上报后只读、归档锁死）';

-- 判定全过程流水：只追加。登记/订正/上报/复测开单/归档每一步落一笔，倒查从最后一步排回第一步。
CREATE TABLE IF NOT EXISTS t_zw_abn_trace (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '所属异常登记单',
  seq int NOT NULL COMMENT '流水次序（从 1 顺排，只追加）',
  action varchar(24) NOT NULL COMMENT '动作 REGISTER自动登记/CORRECT判定订正/REPORT上报/RETEST_OPEN复测开单/ARCHIVE归档结案',
  from_status int DEFAULT NULL COMMENT '动作前台账情形',
  to_status int DEFAULT NULL COMMENT '动作后台账情形',
  detail varchar(1000) DEFAULT NULL COMMENT '本次写下了什么（订正记前后值；归档记末环结论）',
  operator varchar(64) NOT NULL COMMENT '经办人（系统自动动作记 system）',
  action_time datetime NOT NULL COMMENT '动作时刻（系统日志同刻可翻到哪一次写的）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_abn_trace_seq (case_id, seq),
  KEY idx_abn_trace_case (case_id, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常判定过程流水（只追加，倒查准据）';

-- 复测单头：上报成功后系统自动从异常单上开一条（uk 保证一单只开一条，页面无开单口、无撤单口）。
CREATE TABLE IF NOT EXISTS t_zw_retest_order (
  id bigint NOT NULL COMMENT '主键',
  retest_no varchar(64) NOT NULL COMMENT '复测单号（由异常单号带出，全表唯一）',
  case_id bigint NOT NULL COMMENT '来源异常登记单',
  case_no varchar(64) DEFAULT NULL COMMENT '来源异常单号',
  site_no varchar(64) DEFAULT NULL COMMENT '点位代号',
  item_code varchar(64) DEFAULT NULL COMMENT '复测指标（由异常单带出，仅展示）',
  current_step int DEFAULT '0' COMMENT '当前环节 0化验室检测 1水质科复核 2主管审定 3已走完全程',
  status int DEFAULT '0' COMMENT '复测情形 0在办 1全程通过 2复核/审定否决(终态)',
  open_by varchar(64) DEFAULT NULL COMMENT '开单人（恒为 system）',
  open_time datetime DEFAULT NULL COMMENT '开单时刻（上报同刻）',
  finish_by varchar(64) DEFAULT NULL COMMENT '终环落定人',
  finish_time datetime DEFAULT NULL COMMENT '终环落定时刻',
  final_result int DEFAULT NULL COMMENT '最终复核结论 1通过 2否决',
  del_flag int DEFAULT '0' COMMENT '删除标记（只留不删）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_retest_no (retest_no),
  UNIQUE KEY uk_retest_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常复测单（上报自动开、按环节链走、否决即止）';

-- 复测环节台账：每次提交追加一笔（attempt_no 递增）。环节0 填检测项目/数值/时间，空表起步绝不预填；
-- 环节1 必须照录环节0 三项，环节2 必须照录环节1 的复核结论；对不上(match_flag=0)也原样留痕、不开下一环，
-- 报表出得去、可逐笔回退；对得上且非否决才推进。有效行(status=1)每环节至多一笔，落名后对所有人只读。
CREATE TABLE IF NOT EXISTS t_zw_retest_step (
  id bigint NOT NULL COMMENT '主键',
  order_id bigint NOT NULL COMMENT '所属复测单',
  step_no int NOT NULL COMMENT '环节 0化验室检测 1水质科复核 2主管审定',
  attempt_no int NOT NULL COMMENT '本环节第几次提交（对不上留痕后可再交，次序只追加）',
  fill_item varchar(64) DEFAULT NULL COMMENT '环节0填：检测项目（不预填）',
  fill_qty decimal(12,2) DEFAULT NULL COMMENT '环节0填：检测数值（不预填）',
  fill_test_at datetime DEFAULT NULL COMMENT '环节0填：检测时间（不预填）',
  prev_item varchar(64) DEFAULT NULL COMMENT '环节1照录上一环：检测项目',
  prev_qty decimal(12,2) DEFAULT NULL COMMENT '环节1照录上一环：检测数值',
  prev_test_at datetime DEFAULT NULL COMMENT '环节1照录上一环：检测时间',
  prev_result int DEFAULT NULL COMMENT '环节2照录上一环：复核结论 1通过 2否决',
  step_result int DEFAULT NULL COMMENT '本环节结论 1通过/同意 2否决（环节0无结论留空；否决即止）',
  opinion varchar(500) DEFAULT NULL COMMENT '本环节意见（否决必填）',
  match_flag int DEFAULT '1' COMMENT '与上一环核对 1对得上 0对不上（对不上原样留痕、不开下一环）',
  status int DEFAULT '0' COMMENT '本笔效力 0核对不符留痕(未生效) 1有效落名',
  fill_by varchar(64) DEFAULT NULL COMMENT '本环节填报人（谁在本环节有权谁填）',
  fill_time datetime DEFAULT NULL COMMENT '填报时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记（只留不删）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_retest_step_attempt (order_id, step_no, attempt_no),
  KEY idx_retest_step_order (order_id, step_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复测环节填写台账（只追加、逐步复录、落名只读）';

-- 供水户盯办单头：异常上报同刻由系统自开，复测全程通过、异常归档同刻由系统自结；没有人工开/闭口。
-- 盯办周期另算（period_days），与采样周期、复测时限各管各的。
CREATE TABLE IF NOT EXISTS t_zw_follow (
  id bigint NOT NULL COMMENT '主键',
  follow_no varchar(64) NOT NULL COMMENT '盯办单号（由异常单号带出，全表唯一）',
  case_id bigint NOT NULL COMMENT '来源异常登记单',
  case_no varchar(64) DEFAULT NULL COMMENT '来源异常单号',
  site_no varchar(64) DEFAULT NULL COMMENT '点位代号',
  water_user_name varchar(128) DEFAULT NULL COMMENT '被反映到的供水户名（开单时名录快照）',
  contact_name varchar(64) DEFAULT NULL COMMENT '当前联系人（开单快照；后续每笔按名录现取，取错可在流水看见）',
  contact_phone varchar(32) DEFAULT NULL COMMENT '联系电话（快照）',
  period_days int DEFAULT '7' COMMENT '盯办周期自然日（另算，隔多少天盯一次）',
  last_touch_at datetime DEFAULT NULL COMMENT '上一次盯办落定时刻（每笔结果都带上一次的时间）',
  next_due_at datetime DEFAULT NULL COMMENT '下一次该盯的时刻（上一次盯完顺排，不靠人记）',
  status int DEFAULT '0' COMMENT '盯办情形 0在盯 1已结（只有系统能结）',
  open_time datetime DEFAULT NULL COMMENT '开单时刻（上报同刻，恒 system）',
  close_time datetime DEFAULT NULL COMMENT '结案时刻（异常归档同刻，恒 system）',
  close_note varchar(500) DEFAULT NULL COMMENT '结案说明',
  del_flag int DEFAULT '0' COMMENT '删除标记（只留不删）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow_no (follow_no),
  UNIQUE KEY uk_follow_case (case_id),
  KEY idx_follow_due (status, next_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供水户盯办单（系统自开自结、周期另算）';

-- 盯办流水：系统每到点自开一笔（谁也不能手开），经办人只能填盯办结果，落名即只读。
-- 每笔都带上一次盯办时间；盯七八次、隔几个月，也能一笔笔顺序翻出，漏办（逾期仍待办）一目了然。
-- 只追加：拿着后一笔倒改前一笔没有任何写口。
CREATE TABLE IF NOT EXISTS t_zw_follow_touch (
  id bigint NOT NULL COMMENT '主键',
  follow_id bigint NOT NULL COMMENT '所属盯办单',
  seq int NOT NULL COMMENT '第几笔盯办（从 1 顺排，只追加）',
  plan_at datetime NOT NULL COMMENT '系统开出时刻（什么时候要盯）',
  due_at datetime NOT NULL COMMENT '本笔应在此时限前盯完',
  contact_name varchar(64) DEFAULT NULL COMMENT '本笔该找谁（开出时按名录现取，不沿用旧错联系人）',
  contact_phone varchar(32) DEFAULT NULL COMMENT '本笔该找的电话（现取）',
  prev_touch_at datetime DEFAULT NULL COMMENT '上一次盯办时刻（首笔盯开单时刻）',
  result varchar(24) DEFAULT NULL COMMENT '盯办结果 REACHED已联系上/NOT_REACHABLE联系不上/WRONG_CONTACT联系人有误',
  detail varchar(500) DEFAULT NULL COMMENT '本次盯办情况（当天做了什么）',
  actual_touch_at datetime DEFAULT NULL COMMENT '实际盯办时刻（填报人填，不空过）',
  fill_by varchar(64) DEFAULT NULL COMMENT '盯办经办人',
  fill_time datetime DEFAULT NULL COMMENT '填报时刻',
  status int DEFAULT '0' COMMENT '本笔情形 0待办(系统已开未填) 1已办（含联系不上/联系人有误，也算盯过一笔）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow_touch_seq (follow_id, seq),
  KEY idx_follow_touch_status (follow_id, status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供水户盯办流水（系统开、经办填、只追加）';

-- 供水户名录种子：ZS00 在册（联系人正确）、ZS02 在册但联系人已换机（演示"联系人取错/联系不上"）。
INSERT IGNORE INTO t_zw_water_user (id, site_no, user_name, contact_name, contact_phone, status, del_flag, create_by, create_time, remark)
VALUES (20, 'ZS00', '城北供水服务所', '张保供', '13800000001', 0, 0, 'seed', NOW(), '城北水源地对应供水户'),
       (21, 'ZS02', '青西供水服务站', '李旧号（已调岗）', '13900000002', 0, 0, 'seed', NOW(), '登记联系人已调岗，实测联系人有误；现行联系人 王新号 13700000003');

-- 演示用异常①：9003 行（ZS00 2026-09-18 余氯 0.71，严重超标）已上报、已反映到供水户；
-- 复测单开到水质科复核环节，且留有一笔"照录对不上"的留痕；盯办已盯成一笔、第二笔待办（隔周期可见）。
INSERT IGNORE INTO t_zw_abn_case (id, case_no, imp_row_id, batch_no, row_no, site_no, item_code, sample_type, test_at, test_date,
  qty, grade_level, limit_value, over_qty, rule_code, rule_name, rule_eff_date, need_water_stop, need_retest, verdict_note,
  revise_no, status, report_by, report_time, del_flag, create_by, create_time)
VALUES (9201, 'YC20260918-001', 9003, 'B202609', 1, 'ZS00', 'CL', '水源水', '2026-09-18 09:00:00', '2026-09-18 00:00:00',
  0.71, 4, 0.50, 0.21, 'CL-SW-2026', '水源水余氯分档线', '2026-01-01 00:00:00', 1, 1, '读数 0.71 越过严重超标档界值 0.50，超 0.21',
  0, 1, 'seed', '2026-09-18 10:00:00', 0, 'seed', '2026-09-18 09:30:00');

INSERT IGNORE INTO t_zw_abn_trace (id, case_id, seq, action, from_status, to_status, detail, operator, action_time)
VALUES (9210, 9201, 1, 'REGISTER', NULL, 0, '导入批次 B202609 第1行定级为严重超标，自动登记：界值 0.50，超 0.21，建议停水+复测', 'system', '2026-09-18 09:30:00'),
       (9211, 9201, 2, 'REPORT', 0, 1, '异常上报，反映到供水户「城北供水服务所」名下；系统同刻开复测单与盯办单', 'seed', '2026-09-18 10:00:00'),
       (9212, 9201, 3, 'RETEST_OPEN', 1, 1, '复测单 YC20260918-001-HC 自动开立，从化验室检测环节起步', 'system', '2026-09-18 10:00:00');

INSERT IGNORE INTO t_zw_retest_order (id, retest_no, case_id, case_no, site_no, item_code, current_step, status, open_by, open_time, del_flag, create_by, create_time)
VALUES (9301, 'YC20260918-001-HC', 9201, 'YC20260918-001', 'ZS00', 'CL', 1, 0, 'system', '2026-09-18 10:00:00', 0, 'system', '2026-09-18 10:00:00');

-- 环节0 已有效落名；环节1 第1次照录数值对不上（留痕、未推进），当前仍停在环节1 等待重新提交。
INSERT IGNORE INTO t_zw_retest_step (id, order_id, step_no, attempt_no, fill_item, fill_qty, fill_test_at,
  prev_item, prev_qty, prev_test_at, prev_result, step_result, opinion, match_flag, status, fill_by, fill_time, del_flag)
VALUES (9401, 9301, 0, 1, 'CL', 0.68, '2026-09-19 09:00:00',
  NULL, NULL, NULL, NULL, NULL, '化验室复测取样', 1, 1, 'lab01', '2026-09-19 10:00:00', 0),
       (9402, 9301, 1, 1, NULL, NULL, NULL,
  'CL', 0.63, '2026-09-19 09:00:00', NULL, 1, '照录复核', 0, 0, 'water01', '2026-09-20 14:00:00', 0);

INSERT IGNORE INTO t_zw_follow (id, follow_no, case_id, case_no, site_no, water_user_name, contact_name, contact_phone,
  period_days, last_touch_at, next_due_at, status, open_time, del_flag, create_by, create_time)
VALUES (9501, 'YC20260918-001-DB', 9201, 'YC20260918-001', 'ZS00', '城北供水服务所', '张保供', '13800000001',
  7, '2026-09-25 10:30:00', '2026-10-02 10:00:00', 0, '2026-09-18 10:00:00', 0, 'system', '2026-09-18 10:00:00');

-- 第1笔已联系上（带上一次=开单时刻）；第2笔已到点开出、待办（演示漏办红点与逐笔倒查）。
INSERT IGNORE INTO t_zw_follow_touch (id, follow_id, seq, plan_at, due_at, contact_name, contact_phone, prev_touch_at,
  result, detail, actual_touch_at, fill_by, fill_time, status)
VALUES (9601, 9501, 1, '2026-09-18 10:00:00', '2026-09-25 10:00:00', '张保供', '13800000001', '2026-09-18 10:00:00',
  'REACHED', '已电话告知停水预警与复测安排，对方知悉', '2026-09-25 10:30:00', 'follow01', '2026-09-25 10:31:00', 1),
       (9602, 9501, 2, '2026-10-02 10:00:00', '2026-10-09 10:00:00', '张保供', '13800000001', '2026-09-25 10:30:00',
  NULL, NULL, NULL, NULL, NULL, 0);

-- 演示用异常②：9001 行（ZS00 2026-07-18 余氯 0.62，严重超标）已登记未上报——可订正、可上报。
INSERT IGNORE INTO t_zw_abn_case (id, case_no, imp_row_id, batch_no, row_no, site_no, item_code, sample_type, test_at, test_date,
  qty, grade_level, limit_value, over_qty, rule_code, rule_name, rule_eff_date, need_water_stop, need_retest, verdict_note,
  revise_no, status, del_flag, create_by, create_time)
VALUES (9202, 'YC20260718-001', 9001, 'B202607', 1, 'ZS00', 'CL', '水源水', '2026-07-18 09:00:00', '2026-07-18 00:00:00',
  0.62, 4, 0.50, 0.12, 'CL-SW-2026', '水源水余氯分档线', '2026-01-01 00:00:00', 1, 1, '读数 0.62 越过严重超标档界值 0.50，超 0.12',
  0, 0, 0, 'system', '2026-07-18 09:30:00');
INSERT IGNORE INTO t_zw_abn_trace (id, case_id, seq, action, from_status, to_status, detail, operator, action_time)
VALUES (9213, 9202, 1, 'REGISTER', NULL, 0, '导入批次 B202607 第1行定级为严重超标，自动登记：界值 0.50，超 0.12，建议停水+复测', 'system', '2026-07-18 09:30:00');

-- 水质异常判定留痕：菜单与按钮权限（目录 880200；只读人人可查，写口按环节单独授）。
INSERT IGNORE INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES (880200, '水质异常判定留痕', '异常判定自动登记、订正上报、复测环节、归档倒查', '/zwAbn/view', 0, 0, 'zw:zwAbn:view', 1, 'layui-icon layui-icon-alert', 61, 0, 'seed', NOW()),
       (880201, '异常登记台账', '异常判定台账查询与全过程倒查（只读人人可查）', '/zwAbn/list', 0, 880200, 'zw:zwAbn:list', 2, NULL, NULL, 0, 'seed', NOW()),
       (880202, '判定订正', '上报前把判定档位/停水复测建议订正一次（订过/报过/归档皆闭口）', '/zwAbn/correct', 0, 880200, 'zw:zwAbn:correct', 2, NULL, NULL, 0, 'seed', NOW()),
       (880203, '异常上报', '同一笔异常只准上报一次；报后台账只读并自动开复测单/盯办单', '/zwAbn/report', 0, 880200, 'zw:zwAbn:report', 2, NULL, NULL, 0, 'seed', NOW()),
       (880204, '复测-化验室检测', '复测环节0：检测项目/数值/时间由本环节填报，不预填', '/zwAbn/retestStep', 0, 880200, 'zw:zwRetest:step0', 2, NULL, NULL, 0, 'seed', NOW()),
       (880205, '复测-水质科复核', '复测环节1：照录环节0结果并给复核结论，否决即止', '/zwAbn/retestStep', 0, 880200, 'zw:zwRetest:step1', 2, NULL, NULL, 0, 'seed', NOW()),
       (880206, '复测-主管审定', '复测环节2：照录环节1结论并给审定结论，通过即归档', '/zwAbn/retestStep', 0, 880200, 'zw:zwRetest:step2', 2, NULL, NULL, 0, 'seed', NOW()),
       (880207, '异常倒查报表导出', '复测环节逐项核对与盯办流水导出（对不上/漏办照台账出）', '/zwAbn/export', 0, 880200, 'zw:zwAbn:export', 2, NULL, NULL, 0, 'seed', NOW()),
       (880208, '供水户盯办台账', '盯办单与逐笔流水查询（该找谁/盯没盯/历次结果）', '/zwFollow/view', 0, 0, 'zw:zwFollow:view', 1, 'layui-icon layui-icon-phone', 62, 0, 'seed', NOW()),
       (880209, '盯办结果填报', '系统开出的盯办笔，由经办人填结果，落名只读', '/zwFollow/fill', 0, 880208, 'zw:zwFollow:fill', 2, NULL, NULL, 0, 'seed', NOW()),
       (880210, '盯办跑一轮', '按另算周期系统开盯办笔（定时/手动同一口径）', '/zwFollow/run', 0, 880208, 'zw:zwFollow:run', 2, NULL, NULL, 0, 'seed', NOW());
-- 管理员挂全套
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES (8802001, 488243256161730560, 880200, 'seed', NOW()),
       (8802002, 488243256161730560, 880201, 'seed', NOW()),
       (8802003, 488243256161730560, 880202, 'seed', NOW()),
       (8802004, 488243256161730560, 880203, 'seed', NOW()),
       (8802005, 488243256161730560, 880204, 'seed', NOW()),
       (8802006, 488243256161730560, 880205, 'seed', NOW()),
       (8802007, 488243256161730560, 880206, 'seed', NOW()),
       (8802008, 488243256161730560, 880207, 'seed', NOW()),
       (8802009, 488243256161730560, 880208, 'seed', NOW()),
       (8802010, 488243256161730560, 880209, 'seed', NOW()),
       (8802011, 488243256161730560, 880210, 'seed', NOW());
-- 普通用户角色：只读——台账、倒查、导出，写口不授
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES (8802021, 488289006124007424, 880200, 'seed', NOW()),
       (8802022, 488289006124007424, 880201, 'seed', NOW()),
       (8802023, 488289006124007424, 880207, 'seed', NOW()),
       (8802024, 488289006124007424, 880208, 'seed', NOW());

-- 盯办定时任务种子（默认暂停 status=1；在定时任务里启用后，每天 08:07 按另算周期系统开盯办笔）。
INSERT IGNORE INTO t_sys_quartz_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (880290001, '供水户盯办开笔', 'ZW', 'zwFollowTask.runCycle()', '0 7 8 * * ?', '2', '1', 1, 'seed', NOW(), '按盯办单各自周期到点开待办笔；逾期未办不开新笔，漏办留账');

-- 演示用异常③：9002 行（ZS00 2026-08-18 余氯 0.65，严重超标）已登记未上报（连续三个月超标之中的一月）。
INSERT IGNORE INTO t_zw_abn_case (id, case_no, imp_row_id, batch_no, row_no, site_no, item_code, sample_type, test_at, test_date,
  qty, grade_level, limit_value, over_qty, rule_code, rule_name, rule_eff_date, need_water_stop, need_retest, verdict_note,
  revise_no, status, del_flag, create_by, create_time)
VALUES (9203, 'YC20260818-001', 9002, 'B202608', 1, 'ZS00', 'CL', '水源水', '2026-08-18 09:00:00', '2026-08-18 00:00:00',
  0.65, 4, 0.50, 0.15, 'CL-SW-2026', '水源水余氯分档线', '2026-01-01 00:00:00', 1, 1, '读数 0.65 越过严重超标档界值 0.50，超 0.15',
  0, 0, 0, 'system', '2026-08-18 09:30:00');
INSERT IGNORE INTO t_zw_abn_trace (id, case_id, seq, action, from_status, to_status, detail, operator, action_time)
VALUES (9214, 9203, 1, 'REGISTER', NULL, 0, '导入批次 B202608 第1行定级为严重超标，自动登记：界值 0.50，超 0.15，建议停水+复测', 'system', '2026-08-18 09:30:00');
