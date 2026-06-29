export const defaultDepartments = [
  { name: "神经内科", description: "评估头痛、眩晕、脑血管病、认知障碍、癫痫、睡眠和周围神经相关问题。", symptoms: ["突发或反复头痛", "眩晕、走路不稳", "肢体麻木无力", "记忆下降或睡眠异常"], preparation: ["记录发作时间和持续多久", "携带头颅影像、脑电图或既往病历", "整理正在使用的降压、抗凝或镇静药物"] },
  { name: "心血管内科", description: "处理胸闷胸痛、心悸、高血压、冠心病、心律失常和心功能管理。", symptoms: ["胸痛或胸闷", "心悸、心跳不齐", "血压长期异常", "活动后气短或水肿"], preparation: ["带近期血压和心率记录", "携带心电图、心脏彩超、冠脉检查结果", "列出长期用药和不良反应"] },
  { name: "呼吸内科", description: "关注咳嗽、哮喘、慢阻肺、肺部感染、肺结节和睡眠呼吸问题。", symptoms: ["持续咳嗽或咳痰", "喘息、气短", "反复发热伴肺部阴影", "睡眠打鼾和白天嗜睡"], preparation: ["记录体温、咳痰颜色和气短程度", "携带胸片或胸部 CT", "说明吸烟史、过敏史和既往肺病史"] },
  { name: "消化内科", description: "处理腹痛、反酸、腹泻便秘、肝胆胰疾病和胃肠镜相关评估。", symptoms: ["上腹痛、反酸烧心", "腹泻或便秘", "黑便、呕血"], preparation: ["记录饮食、排便和体重变化", "携带胃肠镜、腹部超声或肝功能结果", "如需检查，提前确认是否空腹"] },
  { name: "骨科", description: "评估关节疼痛、运动损伤、脊柱问题、骨折术后和慢性疼痛。", symptoms: ["颈肩腰腿痛", "关节肿痛或活动受限", "运动损伤", "骨折或术后复查"], preparation: ["携带 X 线、CT、MRI 或手术记录", "记录疼痛部位、诱因和活动受限程度", "穿着便于暴露和活动检查的衣物"] },
  { name: "妇科", description: "处理月经异常、妇科炎症、盆腔疼痛、围绝经期管理和孕前咨询。", symptoms: ["月经异常", "下腹痛或异常分泌物", "围绝经期不适", "孕前或备孕咨询"], preparation: ["记录月经周期和末次月经", "携带妇科超声、宫颈筛查或激素检查", "说明妊娠、手术和用药情况"] },
  { name: "儿科", description: "覆盖儿童发热、咳嗽、消化问题、生长发育、过敏和疫苗咨询。", symptoms: ["儿童发热咳嗽", "腹泻呕吐", "皮疹或过敏", "生长发育问题"], preparation: ["记录体温变化、精神状态和进食情况", "携带疫苗本和既往检查", "由熟悉病情的监护人陪同"] },
  { name: "肿瘤科", description: "提供肿瘤筛查、治疗方案咨询、随访管理、症状控制和支持治疗建议。", symptoms: ["肿瘤筛查异常", "治疗方案咨询", "复查影像异常", "疼痛、乏力或营养问题"], preparation: ["携带病理报告、分期资料和治疗记录", "整理近期影像和肿瘤标志物", "列出当前治疗副作用和用药"] }
];

export const defaultDoctors = [
  { id: "fallback-1", name: "周明远", title: "主任医师", department: "神经内科", specialty: "脑血管病、头痛眩晕、认知障碍", profile: "长期参与卒中绿色通道和复杂神经系统疾病多学科会诊。", tags: ["脑血管病", "头痛", "认知障碍"] },
  { id: "fallback-2", name: "林若华", title: "主任医师", department: "心血管内科", specialty: "冠心病、高血压、心律失常", profile: "关注慢病指标趋势和长期用药安全。", tags: ["冠心病", "高血压", "心律失常"] },
  { id: "fallback-3", name: "陈启航", title: "副主任医师", department: "呼吸内科", specialty: "慢阻肺、哮喘、肺部感染", profile: "围绕咳嗽、气短、肺部影像异常梳理诊疗线索。", tags: ["咳嗽", "哮喘", "肺部感染"] },
  { id: "fallback-4", name: "沈知微", title: "主任医师", department: "消化内科", specialty: "胃肠疾病、肝胆胰疾病、内镜评估", profile: "帮助患者做好胃肠镜和复诊准备。", tags: ["腹痛", "胃肠镜", "肝胆胰"] },
  { id: "fallback-5", name: "赵闻舟", title: "副主任医师", department: "骨科", specialty: "脊柱关节、运动损伤、术后康复", profile: "结合影像、疼痛部位和功能受限情况评估骨科问题。", tags: ["关节痛", "运动损伤", "康复"] },
  { id: "fallback-6", name: "许安宁", title: "主任医师", department: "儿科", specialty: "儿童发热、过敏、生长发育", profile: "强调儿童精神状态、进食睡眠和体温趋势。", tags: ["儿童发热", "过敏", "生长发育"] }
];

export const updates = [
  { kind: "通知公告", title: "门诊预约与取消规则提醒", date: "2026-06-20", source: "门诊服务中心", summary: "预约后请按时到院，无法就诊时提前取消，释放号源给其他患者。", body: ["预约记录会显示医生、科室、就诊时间、状态和取消入口。", "普通门诊不适合处理胸痛、呼吸困难、意识改变等急症。"], tags: ["预约", "号源", "就诊提醒"] },
  { kind: "通知公告", title: "检查检验报告查看说明", date: "2026-06-12", source: "医学检验与影像中心", summary: "报告生成后将按项目、时间和状态归档，复诊时请结合医生建议解读。", body: ["报告异常不等于最终诊断，应结合症状、病历和医生判断。", "复诊前建议整理近期报告和既往检查变化。"], tags: ["检查报告", "复诊", "资料归档"] },
  { kind: "健康资讯", title: "胸痛何时需要立即就医", date: "2026-06-08", source: "心血管内科", summary: "持续胸痛、出汗、气短、晕厥或放射痛应直接急诊。", body: ["记录胸痛开始时间、性质、持续多久和是否与活动相关。", "线上分诊不能排除急性心梗、肺栓塞等严重情况。"], tags: ["胸痛", "急诊", "心血管"] },
  { kind: "健康资讯", title: "复诊前如何整理用药清单", date: "2026-05-30", source: "药学部", summary: "药名、剂量、频次、漏服情况和不良反应，是医生调整方案的重要依据。", body: ["尽量带药盒、处方记录或药品照片。", "保健品、中成药和非处方药也需要告诉医生。"], tags: ["用药", "复诊", "处方"] },
  { kind: "健康资讯", title: "儿童发热就诊前要记录什么", date: "2026-05-22", source: "儿科中心", summary: "体温趋势、精神状态、进食饮水、尿量和既往用药能帮助医生判断风险。", body: ["持续高热、精神反应差、抽搐、呼吸费力应尽快线下就医。", "监护人应说明发热开始时间、最高体温和退热药反应。"], tags: ["儿科", "发热", "就诊准备"] },
  { kind: "科研动态", title: "智能分诊质量评估项目启动", date: "2026-05-16", source: "临床质量改进办公室", summary: "项目评估推荐科室与医生最终判断的一致性，并持续优化问题设计。", body: ["智能分诊的价值在于帮助患者更完整地描述症状。", "研究结果将反哺患者端分诊问题和医生端接诊提示。"], tags: ["智能分诊", "科研", "质量改进"] }
];

export const staticSearchItems = [
  { type: "服务资料", title: "患者服务指南", description: "查看就诊前准备、智能分诊、预约挂号、到院流程和诊后资料管理。", meta: ["就诊流程", "资料准备", "复诊管理"] },
  { type: "服务资料", title: "智能分诊", description: "不确定挂哪个科室时，先描述症状获得推荐方向。", meta: ["症状整理", "推荐科室", "需登录"] },
  { type: "疾病/症状", title: "头痛", description: "可能涉及神经内科、眼科、耳鼻喉或急诊。突发剧烈头痛需立即就医。", meta: ["神经内科", "红旗症状", "影像资料"] },
  { type: "疾病/症状", title: "胸痛", description: "压榨样胸痛、出汗、气短或晕厥应立即急诊。", meta: ["心血管内科", "急诊提醒", "心电图"] },
  { type: "疾病/症状", title: "咳嗽", description: "持续咳嗽、发热、气短、咯血或胸部影像异常建议呼吸内科评估。", meta: ["呼吸内科", "胸部 CT", "用药史"] },
  { type: "疾病/症状", title: "腹痛", description: "记录腹痛部位、持续时间、进食关系和排便变化。", meta: ["消化内科", "腹部超声", "胃肠镜"] }
];

export function splitTags(value) {
  return String(value || "").split(/[、,，\s]+/).filter(Boolean).slice(0, 4);
}

export function normalizeDepartment(row) {
  const name = String(row.title || row.targetName || row.name || row.departmentName || "未命名科室");
  const matched = defaultDepartments.find((item) => name.includes(item.name) || item.name.includes(name));
  return {
    name,
    id: row.departmentId || row.id || row.targetId || "",
    description: String(row.description || row.remark || row.specialty || matched && matched.description || "提供门诊、复诊和专科评估服务。"),
    symptoms: matched && matched.symptoms || ["常见病和慢病复诊", "专科症状评估", "检查结果解读"],
    preparation: matched && matched.preparation || ["准备既往病历和检查报告", "记录主要症状的时间线", "整理长期用药和过敏史"]
  };
}

export function normalizeDoctor(row, index = 0) {
  const specialty = String(row.specialty || row.description || "常见病、慢病复诊和专科评估");
  return {
    id: String(row.doctorId || row.id || row.targetId || `doctor-${index}`),
    name: String(row.name || row.doctorName || row.title || row.targetName || "未命名医生"),
    title: String(row.doctorTitle || row.title || "门诊医生"),
    department: String(row.departmentName || row.department || "未定科室"),
    specialty,
    profile: String(row.profile || row.introduction || row.description || "提供门诊评估、复诊沟通和检查结果解读服务。"),
    tags: row.tags || splitTags(specialty)
  };
}
