# 绯荤粺鏋舵瀯璁捐鏂囨。

## 1. 鏁翠綋鏋舵瀯鍥?

绯荤粺浠庨」鐩垵濮嬪寲璧烽噰鐢ㄧ函寰湇鍔℃灦鏋勩€傚墠绔彧璁块棶 `gateway-service`锛屾墍鏈変笟鍔¤兘鍔涚敱鐙珛棰嗗煙鏈嶅姟鎵挎媴锛岃法鏈嶅姟鏁版嵁璁块棶閫氳繃 OpenFeign 鍜屾敞鍐屽彂鐜板畬鎴愩€?

```mermaid
flowchart TB
  P[鎮ｈ€呯 Vue 3] --> GW[gateway-service]
  D[鍖荤敓绔?Vue 3] --> GW
  A[绠＄悊绔?Vue 3] --> GW
  GW --> AUTH[auth-service]
  GW --> PAT[patient-service]
  GW --> DOC[doctor-service]
  GW --> REG[registration-service]
  GW --> TRI[triage-service]
  GW --> MR[medical-record-service]
  GW --> PRE[prescription-service]
  GW --> NOTI[notification-service]
  GW --> ADM[admin-service]
  TRI --> AIS[ai-service]
  MR --> AIS
  PRE --> AIS
  REG --> DOC
  REG --> PAT
  TRI --> DOC
  PRE --> NOTI
  ADM --> DOC
  ADM --> TRI
  ADM --> AIS
  AIS --> LLM[澶ц瑷€妯″瀷 API 鎴?Mock]
  AIS --> KG[鐭ヨ瘑鍥捐氨鏈嶅姟 鍙€塢
  AUTH --> Registry[Nacos/Eureka 娉ㄥ唽鍙戠幇]
  PAT --> Registry
  DOC --> Registry
  REG --> Registry
  TRI --> Registry
  MR --> Registry
  PRE --> Registry
  AIS --> Registry
  NOTI --> Registry
  ADM --> Registry
```

## 2. 妯″潡涔嬮棿鐨勫叧绯?

| 妯″潡 | 渚濊禆妯″潡 | 璇存槑 |
|---|---|---|
| 缃戝叧鏈嶅姟 | 娉ㄥ唽涓績銆佽璇佹湇鍔?| 缁熶竴鍏ュ彛銆佽矾鐢便€佽法鍩熴€侀檺娴併€乀oken 鍓嶇疆瑙ｆ瀽 |
| 璁よ瘉鏈嶅姟 | 鎮ｈ€呫€佸尰鐢熴€佺鐞嗗憳璐﹀彿 | 鐧诲綍鍚庣敓鎴?JWT 鍜岃鑹蹭俊鎭?|
| 鎮ｈ€呮湇鍔?| 璁よ瘉鏈嶅姟 | 缁存姢鎮ｈ€呬釜浜轰俊鎭?|
| 鍖荤敓鏈嶅姟 | 绉戝銆佹帓鐝?| 鍖荤敓褰掑睘浜庣瀹わ紝缁存姢鍙锋簮 |
| 鎸傚彿鏈嶅姟 | 鎮ｈ€呮湇鍔°€佸尰鐢熸湇鍔°€佸垎璇婃湇鍔?| 鍒涘缓鎮ｈ€呬笌鍖荤敓涔嬮棿鐨勯绾﹀叧绯?|
| 鏅鸿兘鍒嗚瘖鏈嶅姟 | 鎮ｈ€呮湇鍔°€佸尰鐢熸湇鍔°€丄I 鏈嶅姟 | 鏍规嵁涓昏瘔鎺ㄨ崘绉戝鍜屽尰鐢燂紝淇濆瓨鍒嗚瘖璁板綍 |
| 鐥呭巻鏈嶅姟 | 鎸傚彿鏈嶅姟銆佹偅鑰呮湇鍔°€佸尰鐢熸湇鍔°€丄I 鏈嶅姟 | 鐢熸垚鍜屼繚瀛樼粨鏋勫寲鐥呭巻 |
| 澶勬柟鏈嶅姟 | 鐥呭巻鏈嶅姟銆佹偅鑰呮湇鍔°€丄I 鏈嶅姟銆侀€氱煡鏈嶅姟 | 寮€鏂广€丄I 瀹℃牳骞惰Е鍙戦珮椋庨櫓閫氱煡 |
| 閫氱煡鏈嶅姟 | 澶勬柟鏈嶅姟銆佽璇佹湇鍔?| 缁存姢 WebSocket 杩炴帴鍜岄€氱煡鍘嗗彶 |
| AI 鏈嶅姟 | 澶栭儴澶фā鍨嬨€佺煡璇嗗浘璋?| Prompt 娓叉煋銆佹ā鍨嬭皟鐢ㄣ€佺粨鏋勫寲杈撳嚭鏍￠獙 |
| 绠＄悊鏈嶅姟 | 鍖荤敓鏈嶅姟銆佸垎璇婃湇鍔°€丄I 鏈嶅姟銆佸悇涓氬姟褰掑睘鏈嶅姟 | 绠＄悊绔仛鍚堝叆鍙ｏ紝鍚庡彴缁存姢璇锋眰杞彂鍒板搴旀湇鍔★紝骞剁紪鎺?AI 鎺掔彮鍜?AI 鍒嗚瘖鍙?|

## 3. 鏁版嵁娴佽浆娴佺▼

1. 鍓嶇椤甸潰鎻愪氦璇锋眰銆?
2. Axios 鑷姩鎼哄甫 JWT銆?
3. `gateway-service` 瀹屾垚璺敱銆佽法鍩熴€侀檺娴佸拰 Token 鍓嶇疆瑙ｆ瀽銆?
4. 鐩爣涓氬姟鏈嶅姟鐨?Controller 鏍￠獙璇锋眰鍙傛暟銆?
5. 涓氬姟鏈嶅姟閫氳繃 OpenFeign 璋冪敤鍏朵粬鏈嶅姟鑾峰彇蹇呰鏁版嵁銆?
6. 濡傛秹鍙?AI锛岀敱 `triage-service`銆乣medical-record-service` 鎴?`prescription-service` 璋冪敤 `ai-service`銆?
7. 鍚勬湇鍔″彧鎸佷箙鍖栬嚜宸辫礋璐ｇ殑鏁版嵁銆?
8. 涓氬姟鏈嶅姟杩斿洖缁熶竴鍝嶅簲锛岀綉鍏抽€忎紶缁欏墠绔€?
9. 鍓嶇鏇存柊椤甸潰鐘舵€併€?

## 4. 鏈嶅姟璋冪敤娴佺▼

### 4.1 鏅鸿兘鍒嗚瘖璋冪敤閾?

```text
gateway-service
  -> triage-service /api/triage/consult
    -> ai-service /internal/ai/triage
      -> AI HTTP API / Mock AI
    -> doctor-service 鏌ヨ鎺ㄨ崘鍖荤敓
    -> triage-service 淇濆瓨鍒嗚瘖璁板綍
  -> 杩斿洖鎺ㄨ崘缁撴灉
```

### 4.2 鐥呭巻鐢熸垚璋冪敤閾?

```text
gateway-service
  -> medical-record-service /api/medical-record/generate
    -> registration-service 鏍￠獙鎸傚彿鍏崇郴
    -> ai-service /internal/ai/medical-record/generate
      -> LLM API 鐢熸垚缁撴瀯鍖栫梾鍘?
  -> 杩斿洖鑽夌鐥呭巻
```

淇濆瓨鏃讹細

```text
gateway-service
  -> medical-record-service /api/medical-record/save
    -> registration-service 鏍￠獙鎸傚彿鍏崇郴
    -> medical-record-service 淇濆瓨鐥呭巻
```

### 4.3 澶勬柟瀹℃牳璋冪敤閾?

```text
gateway-service
  -> prescription-service /api/prescription/check
    -> patient-service 鑾峰彇鎮ｈ€呭熀纭€淇℃伅
    -> ai-service /internal/ai/prescription/check
      -> AI API / 鐭ヨ瘑鍥捐氨鏈嶅姟
    -> prescription-service 淇濆瓨瀹℃牳璁板綍
    -> notification-service 鎺ㄩ€侀珮椋庨櫓鍛婅
  -> 杩斿洖瀹℃牳缁撴灉
```

### 4.4 绠＄悊绔?AI 鎺掔彮璋冪敤閾?

```text
gateway-service
  -> admin-service /api/admin/schedule/generate
    -> doctor-service 鏌ヨ绉戝銆佸尰鐢熷拰鐜版湁鎺掔彮
    -> ai-service /internal/ai/schedule/generate
    -> admin-service 杩斿洖鎺掔彮寤鸿
  -> admin-service /api/admin/schedule/publish
    -> doctor-service /internal/doctors/schedules/publish
    -> doctor-service 淇濆瓨鎺掔彮鍜屽彿婧?
```

### 4.5 绠＄悊绔?AI 鍒嗚瘖鍙拌皟鐢ㄩ摼

```text
gateway-service
  -> admin-service /api/admin/triage-desk/list
    -> triage-service 鏌ヨ鍒嗚瘖璁板綍
    -> doctor-service 琛ュ厖绉戝鍜屽尰鐢熶俊鎭?
  -> admin-service /api/admin/triage-desk/assign
    -> triage-service 鏇存柊浜哄伐鏀规淳缁撴灉
    -> registration-service 鍚庣画鎸夋敼娲剧粨鏋滄寕鍙?
```

## 5. 閮ㄧ讲缁撴瀯璇存槑

```mermaid
flowchart LR
  Browser[娴忚鍣╙ --> Nginx[Nginx]
  Nginx --> Static[Vue 闈欐€佽祫婧怾
  Nginx --> Gateway[gateway-service Jar]
  Gateway --> Auth[auth-service Jar]
  Gateway --> Patient[patient-service Jar]
  Gateway --> Doctor[doctor-service Jar]
  Gateway --> Registration[registration-service Jar]
  Gateway --> Triage[triage-service Jar]
  Gateway --> MedicalRecord[medical-record-service Jar]
  Gateway --> Prescription[prescription-service Jar]
  Gateway --> Notification[notification-service Jar]
  Gateway --> Admin[admin-service Jar]
  Triage --> AiService[ai-service Jar]
  MedicalRecord --> AiService
  Prescription --> AiService
  Gateway --> Registry[Nacos/Eureka]
  Services[鍚勯鍩熸湇鍔 --> MySQL[(MySQL 澶?schema)]
  AiService --> AI[澶栭儴 AI 鏈嶅姟鎴?Mock]
```

閮ㄧ讲鏃跺悇棰嗗煙鏈嶅姟鐙珛杩涚▼杩愯銆佺嫭绔嬫敞鍐屻€佺嫭绔嬫棩蹇楀拰鍋ュ悍妫€鏌ワ紱MySQL 鍙叡鐢ㄥ疄渚嬶紝浣嗗繀椤绘寜鏈嶅姟鍒掑垎 schema锛岀姝㈣法 schema 鐩存帴鏌ヨ銆?

## 6. 鎺ㄨ崘绔彛

| 鏈嶅姟 | 鏈湴绔彛 |
|---|---|
| Vue 寮€鍙戞湇鍔?| `5173` |
| gateway-service | `8080` |
| auth-service | `8101` |
| patient-service | `8102` |
| doctor-service | `8103` |
| registration-service | `8104` |
| triage-service | `8105` |
| medical-record-service | `8106` |
| prescription-service | `8107` |
| ai-service | `8108` |
| notification-service | `8109` |
| admin-service | `8110` |
| Nacos 鎴?Eureka | `8848` 鎴?`8761` |
| MySQL | `3306` |
| Knife4j 鑱氬悎鍏ュ彛 | `http://localhost:8080/doc.html` |
## RabbitMQ / KingbaseES 搜索架构补充

```mermaid
flowchart LR
  Prescription[prescription-service] --> Outbox[(outbox_event)]
  Outbox --> Rabbit[RabbitMQ scb.domain]
  Rabbit --> Notification[notification-service]
  Notification --> NotifyDB[(notification_message)]
  Notification --> WS[/ws/notifications]
  Admin[admin-service] --> KB[(KingbaseES)]
  Gateway[gateway-service] --> Admin
```

`notification-service` 消费处方风险事件后入库并推送 WebSocket。知识库、药品、Prompt 模板搜索由 `admin-service` 直接查询 KingbaseES。
