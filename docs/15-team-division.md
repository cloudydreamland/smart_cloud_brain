# 五人小组分工清单

本项目按“端 + 能力专题”组织五人协作，实际成员姓名可在提交前替换。

| 角色 | 主要职责 | 交付物 |
|---|---|---|
| 组长/后端负责人 | 维护总体进度、接口设计、`diagnosis-service` 主业务闭环 | 后端接口、Postman 集合、答辩主线 |
| AI 服务负责人 | 维护 `ai-service`、MockAiProvider、Prompt、Spring AI 替换点 | AI 服务代码、大模型使用说明、降级策略 |
| Web 前端负责人 | 维护 Vue 3 患者端、医生端、管理端 | Web 页面、截图、前后端联调 |
| 移动端负责人 | 维护 HBuilder/uni-app 安卓患者端 | `patient-mobile`、移动端演示截图 |
| 测试与部署负责人 | 维护 Kingbase SQL、JUnit/Vitest、WebRunner、部署脚本、PPT 素材 | 测试报告、性能报告、部署说明、演示视频 |

## 协作要求

- 所有人必须提交代码或文档，commit message 使用 `type: summary`。
- 主分支保持可构建，功能分支完成后再合并。
- 每个接口改动同步更新 Postman 或接口文档。
- 每次阶段验收至少保留一份截图或录屏证据。
