# 鎺ュ彛璁捐鏂囨。 API

## 1. 閫氱敤绾﹀畾

### 1.1 鍩虹璺緞

```text
/api
```

### 1.2 璁よ瘉鏂瑰紡

鐧诲綍鎴愬姛鍚庯紝鍓嶇鍦ㄨ姹傚ご涓惡甯︼細

```http
Authorization: Bearer <jwt-token>
```

### 1.3 缁熶竴鍝嶅簲缁撴瀯

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 2. 閿欒鐮?

| code | 鍚箟 |
|---|---|
| 0 | 鎴愬姛 |
| 400 | 鍙傛暟閿欒 |
| 401 | 鏈櫥褰曟垨 Token 澶辨晥 |
| 403 | 鏃犳潈闄?|
| 404 | 璧勬簮涓嶅瓨鍦?|
| 409 | 鏁版嵁鍐茬獊 |
| 500 | 绯荤粺寮傚父 |
| 600 | AI 鏈嶅姟涓嶅彲鐢?|

## 3. 鎺ュ彛鍒楄〃

### 3.1 瀵瑰墠绔紑鏀剧殑涓氬姟鎺ュ彛

| 鎺ュ彛 | 鏂规硶 | 鏉冮檺 | 璇存槑 |
|---|---|---|---|
| `/api/patient/register` | POST | 娓稿 | 鎮ｈ€呮敞鍐?|
| `/api/patient/login` | POST | 娓稿 | 鎮ｈ€呯櫥褰?|
| `/api/doctor/login` | POST | 娓稿 | 鍖荤敓鐧诲綍 |
| `/api/patient/info` | GET | 鎮ｈ€?| 鑾峰彇鎮ｈ€呬俊鎭?|
| `/api/doctor/list` | GET | 鐧诲綍鐢ㄦ埛 | 鍖荤敓鍒楄〃 |
| `/api/doctor/detail` | GET | 鐧诲綍鐢ㄦ埛 | 鍖荤敓璇︽儏 |
| `/api/triage/consult` | POST | 鎮ｈ€?| 鏅鸿兘鍒嗚瘖 |
| `/api/registration/create` | POST | 鎮ｈ€?| 鍒涘缓鎸傚彿 |
| `/api/registration/list` | GET | 鎮ｈ€?鍖荤敓 | 鎸傚彿鍒楄〃 |
| `/api/registration/cancel` | POST | 鎮ｈ€?| 鍙栨秷鎸傚彿 |
| `/api/medical-record/generate` | POST | 鍖荤敓 | AI 鐢熸垚鐥呭巻 |
| `/api/medical-record/save` | POST | 鍖荤敓 | 淇濆瓨鐥呭巻 |
| `/api/medical-record/list` | GET | 鎮ｈ€?鍖荤敓 | 鐥呭巻鍒楄〃 |
| `/api/medical-record/detail` | GET | 鎮ｈ€?鍖荤敓 | 鐥呭巻璇︽儏 |
| `/api/prescription/check` | POST | 鍖荤敓 | AI 澶勬柟瀹℃牳 |
| `/api/prescription/create` | POST | 鍖荤敓 | 淇濆瓨澶勬柟 |
| `/api/prescription/list` | GET | 鎮ｈ€?鍖荤敓 | 澶勬柟鍒楄〃 |
| `/api/notification/list` | GET | 鍖荤敓 | 鍖荤敓閫氱煡鍒楄〃 |
| `/api/notification/read` | POST | 鍖荤敓 | 鏍囪閫氱煡宸茶 |
| `/api/prompt-template/list` | GET | 鍖荤敓/绠＄悊鍛?| Prompt 妯℃澘鍒楄〃鎴栭厤缃睍绀?|
| `/ws/notifications` | WS | 鍖荤敓 | WebSocket 瀹炴椂閫氱煡杩炴帴 |
| `/api/admin/login` | POST | 娓稿 | 绠＄悊鍛樼櫥褰?|
| `/api/admin/department/list` | GET | 绠＄悊鍛?| 绉戝鍒楄〃 |
| `/api/admin/department/save` | POST | 绠＄悊鍛?| 鏂板鎴栫紪杈戠瀹?|
| `/api/admin/doctor/list` | GET | 绠＄悊鍛?| 鍖荤敓鍒楄〃 |
| `/api/admin/doctor/save` | POST | 绠＄悊鍛?| 鏂板鎴栫紪杈戝尰鐢?|
| `/api/admin/drug/list` | GET | 绠＄悊鍛?| 鑽搧鍒楄〃 |
| `/api/admin/drug/save` | POST | 绠＄悊鍛?| 鏂板鎴栫紪杈戣嵂鍝?|
| `/api/admin/prompt-template/list` | GET | 绠＄悊鍛?| Prompt 妯℃澘鍒楄〃 |
| `/api/admin/prompt-template/save` | POST | 绠＄悊鍛?| 鏂板鎴栫紪杈?Prompt 妯℃澘 |
| `/api/admin/dict/list` | GET | 绠＄悊鍛?| 绯荤粺瀛楀吀鍒楄〃 |
| `/api/admin/dict/save` | POST | 绠＄悊鍛?| 鏂板鎴栫紪杈戠郴缁熷瓧鍏?|
| `/api/admin/schedule/generate` | POST | 绠＄悊鍛?| AI 鐢熸垚鎺掔彮寤鸿 |
| `/api/admin/schedule/publish` | POST | 绠＄悊鍛?| 浜哄伐纭鍚庡彂甯冩帓鐝拰鍙锋簮 |
| `/api/admin/schedule/list` | GET | 绠＄悊鍛?| 鎺掔彮鍜屽彿婧愬垪琛?|
| `/api/admin/schedule/suggestion/detail` | GET | 绠＄悊鍛?| AI 鎺掔彮寤鸿璇︽儏 |
| `/api/admin/triage-desk/list` | GET | 绠＄悊鍛?| AI 鍒嗚瘖鍙板垪琛?|
| `/api/admin/triage-desk/detail` | GET | 绠＄悊鍛?| AI 鍒嗚瘖鍙拌鎯?|
| `/api/admin/triage-desk/assign` | POST | 绠＄悊鍛?| 浜哄伐鏀规淳鍒嗚瘖绉戝鎴栧尰鐢?|
| `/api/admin/triage-desk/close` | POST | 绠＄悊鍛?| 鍏抽棴鍒嗚瘖鍙板鐞嗚褰?|

### 3.2 鏈嶅姟闂村唴閮?AI 鎺ュ彛

鍐呴儴鎺ュ彛鍙厑璁稿井鏈嶅姟涔嬮棿閫氳繃鍐呯綉鍜?OpenFeign 璋冪敤锛屼笉鐩存帴鏆撮湶缁欏墠绔€傚墠绔粺涓€璁块棶 `gateway-service`锛岀敱缃戝叧杞彂鍒板搴斾笟鍔℃湇鍔°€?

| 鎺ュ彛 | 鏂规硶 | 璋冪敤鏂?| 璇存槑 |
|---|---|---|---|
| `/internal/auth/verify` | POST | `gateway-service`銆佷笟鍔℃湇鍔?| Token 鏍￠獙鍜岃韩浠借В鏋?|
| `/internal/patients/{id}` | GET | 鎸傚彿銆佺梾鍘嗐€佸鏂规湇鍔?| 鎮ｈ€呭熀纭€淇℃伅 |
| `/internal/doctors/{id}` | GET | 鎸傚彿銆佸垎璇婃湇鍔?| 鍖荤敓璇︽儏銆佺瀹ゃ€佹帓鐝?|
| `/internal/registrations/{id}` | GET | 鐥呭巻銆佸鏂规湇鍔?| 鎸傚彿鍏崇郴鏍￠獙 |
| `/internal/ai/triage` | POST | `triage-service` | 鏅鸿兘鍒嗚瘖 |
| `/internal/ai/medical-record/generate` | POST | `medical-record-service` | AI 鐢熸垚鐥呭巻 |
| `/internal/ai/medical-record/generate/stream` | GET | `medical-record-service` | AI 娴佸紡鐢熸垚鐥呭巻 |
| `/internal/ai/prescription/check` | POST | `prescription-service` | AI 澶勬柟瀹℃牳 |
| `/internal/ai/schedule/generate` | POST | `admin-service` | AI 鐢熸垚鎺掔彮寤鸿 |
| `/internal/ai/prompt-template/resolve` | POST | `ai-service` 鍐呴儴 | 鎸変换鍔＄被鍨嬪拰绉戝瑙ｆ瀽 Prompt 妯℃澘 |
| `/internal/notifications` | POST | `prescription-service` | 鍒涘缓骞舵帹閫侀€氱煡 |
| `/internal/triage-records` | GET | `admin-service` | 鏌ヨ鍒嗚瘖璁板綍 |
| `/internal/triage-records/{id}/assign` | POST | `admin-service` | 鏇存柊鍒嗚瘖浜哄伐鏀规淳缁撴灉 |
| `/internal/doctors/schedules/publish` | POST | `admin-service` | 鍙戝竷鍖荤敓鎺掔彮鍜屽彿婧?|

## 4. 鎺ュ彛璇︽儏

### 4.1 鎮ｈ€呮敞鍐?

```http
POST /api/patient/register
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "name": "鐜嬪皬鏄?,
  "phone": "13800000000",
  "password": "123456",
  "gender": "鐢?,
  "age": 35,
  "allergyHistory": "鏃?,
  "pastHistory": "鏃犳槑鏄炬棦寰€鍙?
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "patientId": 1
  }
}
```

### 4.2 鎮ｈ€呯櫥褰?

```http
POST /api/patient/login
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "phone": "13800000000",
  "password": "123456"
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "role": "PATIENT",
    "name": "鐜嬪皬鏄?
  }
}
```

### 4.3 鍖荤敓鍒楄〃

```http
GET /api/doctor/list?departmentId=1
Authorization: Bearer jwt-token
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "寮犲尰鐢?,
      "departmentName": "蹇冨唴绉?,
      "title": "涓讳换鍖诲笀",
      "specialty": "鑳哥棝銆佸績鎮搞€侀珮琛€鍘?
    }
  ]
}
```

### 4.4 鏅鸿兘鍒嗚瘖

```http
POST /api/triage/consult
Authorization: Bearer jwt-token
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "chiefComplaint": "鑳哥棝浼存皵鐭紝娲诲姩鍚庡姞閲?
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "triageRecordId": 10,
    "recommendedDepartment": "蹇冨唴绉?,
    "recommendedDoctors": [
      {
        "id": 1,
        "name": "寮犲尰鐢?,
        "title": "涓讳换鍖诲笀",
        "specialty": "鑳哥棝銆佸績鎮搞€侀珮琛€鍘?
      }
    ],
    "reason": "鐥囩姸涓庡績琛€绠＄柧鐥呯浉鍏筹紝寤鸿浼樺厛蹇冨唴绉戝氨璇?
  }
}
```

涓氬姟鎺ュ彛澶勭悊閾捐矾锛?

```text
鍓嶇 -> gateway-service -> triage-service /api/triage/consult -> ai-service /internal/ai/triage
```

### 4.5 鍒涘缓鎸傚彿

```http
POST /api/registration/create
Authorization: Bearer jwt-token
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "doctorId": 1,
  "departmentId": 1,
  "appointmentTime": "2026-06-15 09:30:00",
  "triageRecordId": 10
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "registrationId": 100,
    "status": "CREATED"
  }
}
```

### 4.6 AI 鐢熸垚鐥呭巻

```http
POST /api/medical-record/generate
Authorization: Bearer jwt-token
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "registrationId": 100,
  "dialogueText": "鎮ｈ€咃細鑳哥棝浼存皵鐭袱澶┿€傚尰鐢燂細鏄惁娲诲姩鍚庡姞閲嶏紵鎮ｈ€咃細鏄€?
}
```

涓氬姟鎺ュ彛澶勭悊閾捐矾锛?

```text
鍓嶇 -> gateway-service -> medical-record-service /api/medical-record/generate -> ai-service /internal/ai/medical-record/generate
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chiefComplaint": "鑳哥棝浼存皵鐭袱澶?,
    "presentIllness": "鎮ｈ€呬袱澶╁墠鍑虹幇鑳哥棝浼存皵鐭紝娲诲姩鍚庡姞閲嶃€?,
    "pastHistory": "鏈彁渚涙槑纭棦寰€鍙?,
    "physicalExam": "寰呰ˉ鍏?,
    "diagnosis": "鑳哥棝寰呮煡",
    "treatmentAdvice": "寤鸿瀹屽杽蹇冪數鍥俱€佸績鑲岄叾绛夋鏌?
  }
}
```

### 4.6.1 AI 娴佸紡鐢熸垚鐥呭巻

```http
GET /api/medical-record/generate/stream?registrationId=100
Authorization: Bearer jwt-token
Accept: text/event-stream
```

SSE 浜嬩欢锛?

```text
event: start
data: {"taskId":"mr-100"}

event: delta
data: {"text":"涓昏瘔锛氳兏鐥涗即姘旂煭涓ゅぉ"}

event: structured
data: {"chiefComplaint":"鑳哥棝浼存皵鐭袱澶?,"diagnosis":"鑳哥棝寰呮煡"}

event: done
data: {"taskId":"mr-100"}
```

寮傚父浜嬩欢锛?

```text
event: error
data: {"message":"AI 鐢熸垚涓柇锛屽彲閲嶈瘯鎴栨墜鍔ㄥ～鍐?}
```

璇存槑锛氳鎺ュ彛 `Accept` 鍜屽搷搴旂被鍨嬪潎涓?`text/event-stream`锛屽彧杩斿洖 SSE 浜嬩欢鍗忚锛涙櫘閫?JSON 鐥呭巻鑽夌鍝嶅簲灞炰簬闈炴祦寮?`/api/medical-record/generate`銆?

### 4.7 AI 澶勬柟瀹℃牳

```http
POST /api/prescription/check
Authorization: Bearer jwt-token
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "patientId": 1,
  "drugs": [
    {
      "drugName": "闃垮徃鍖规灄",
      "dosage": "100mg",
      "frequency": "姣忔棩涓€娆?,
      "usageMethod": "鍙ｆ湇"
    }
  ]
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "riskLevel": "LOW",
    "suggestions": "娉ㄦ剰鑳冭偁閬撲笉鑹弽搴旓紝寤鸿楗悗鏈嶇敤銆?,
    "interactions": []
  }
}
```

涓氬姟鎺ュ彛澶勭悊閾捐矾锛?

```text
鍓嶇 -> gateway-service -> prescription-service /api/prescription/check -> ai-service /internal/ai/prescription/check
```

### 4.8 绠＄悊绔?AI 鎺掔彮

```http
POST /api/admin/schedule/generate
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

璇锋眰锛?

```json
{
  "departmentId": 1,
  "dateRangeStart": "2026-06-15",
  "dateRangeEnd": "2026-06-21",
  "doctorIds": [1, 2, 3],
  "slotRule": {
    "shiftTypes": ["AM", "PM"],
    "capacityPerSlot": 20
  }
}
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "suggestionId": 9001,
    "status": "GENERATED",
    "items": [
      {
        "doctorId": 1,
        "scheduleDate": "2026-06-15",
        "shiftType": "AM",
        "capacity": 20
      }
    ]
  }
}
```

鍙戝竷锛?

```http
POST /api/admin/schedule/publish
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

```json
{
  "suggestionId": 9001,
  "items": [
    {
      "doctorId": 1,
      "scheduleDate": "2026-06-15",
      "shiftType": "AM",
      "capacity": 20
    }
  ]
}
```

涓氬姟鎺ュ彛澶勭悊閾捐矾锛?

```text
鍓嶇 -> gateway-service -> admin-service /api/admin/schedule/generate -> ai-service /internal/ai/schedule/generate
鍓嶇 -> gateway-service -> admin-service /api/admin/schedule/publish -> doctor-service /internal/doctors/schedules/publish
```

### 4.9 绠＄悊绔?AI 鍒嗚瘖鍙?

鍒楄〃锛?

```http
GET /api/admin/triage-desk/list?status=SUCCESS&page=1&pageSize=10
Authorization: Bearer admin-jwt-token
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1,
    "records": [
      {
        "triageRecordId": 10,
        "patientId": 1,
        "chiefComplaint": "鑳哥棝浼存皵鐭?,
        "recommendedDepartment": "蹇冨唴绉?,
        "recommendedDoctorIds": [1],
        "reason": "鐥囩姸涓庡績琛€绠＄柧鐥呯浉鍏?,
        "status": "SUCCESS"
      }
    ]
  }
}
```

浜哄伐鏀规淳锛?

```http
POST /api/admin/triage-desk/assign
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

```json
{
  "triageRecordId": 10,
  "departmentId": 2,
  "doctorId": 8,
  "note": "绠＄悊鍛樻牴鎹棶璇婃弿杩版敼娲惧埌鍛煎惛鍐呯"
}
```

涓氬姟鎺ュ彛澶勭悊閾捐矾锛?

```text
鍓嶇 -> gateway-service -> admin-service /api/admin/triage-desk/list -> triage-service /internal/triage-records
鍓嶇 -> gateway-service -> admin-service /api/admin/triage-desk/assign -> triage-service /internal/triage-records/{id}/assign
```

### 4.10 WebSocket 瀹炴椂閫氱煡

```text
ws://localhost:8080/ws/notifications?token=<jwt-token>
```

鏈嶅姟绔帹閫佹秷鎭細

```json
{
  "type": "PRESCRIPTION_HIGH_RISK",
  "notificationId": 300,
  "doctorId": 1,
  "patientId": 1,
  "prescriptionId": 200,
  "riskLevel": "HIGH",
  "title": "楂橀闄╃敤鑽彁閱?,
  "content": "妫€娴嬪埌娼滃湪鑽墿鐩镐簰浣滅敤锛岃澶嶆牳澶勬柟銆?,
  "createdAt": "2026-06-14 16:00:00"
}
```

瑙﹀彂瑙勫垯锛?

1. 鍖荤敓璋冪敤 `/api/prescription/check`銆?
2. AI 瀹℃牳缁撴灉涓?`HIGH`銆?
3. 鍚庣淇濆瓨 `notification_message`銆?
4. 鍚庣閫氳繃 WebSocket 鎺ㄩ€佺粰澶勬柟鎵€灞炲尰鐢熴€?

### 4.11 鍖荤敓閫氱煡鍒楄〃

```http
GET /api/notification/list?readStatus=UNREAD
Authorization: Bearer jwt-token
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 300,
      "type": "PRESCRIPTION_HIGH_RISK",
      "title": "楂橀闄╃敤鑽彁閱?,
      "content": "妫€娴嬪埌娼滃湪鑽墿鐩镐簰浣滅敤锛岃澶嶆牳澶勬柟銆?,
      "riskLevel": "HIGH",
      "readStatus": "UNREAD",
      "createdAt": "2026-06-14 16:00:00"
    }
  ]
}
```

### 4.12 Prompt 妯℃澘鍒楄〃

```http
GET /api/prompt-template/list?taskType=MEDICAL_RECORD&departmentCode=CARDIOLOGY
Authorization: Bearer jwt-token
```

鍝嶅簲锛?

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "taskType": "MEDICAL_RECORD",
      "departmentCode": "CARDIOLOGY",
      "templateName": "蹇冨唴绉戠梾鍘嗙敓鎴愭ā鏉?,
      "version": "v1",
      "enabled": true
    }
  ]
}
```
## Search APIs

| API | Method | Role | Description |
|---|---|---|---|
| `/api/search/knowledge?q=&departmentCode=` | GET | 登录用户 | 从 KingbaseES 搜索知识库 |
| `/api/search/drugs?q=` | GET | 登录用户 | 从 KingbaseES 搜索药品 |
| `/api/admin/search/prompts?q=` | GET | 管理员 | 从 KingbaseES 搜索 Prompt 模板 |
