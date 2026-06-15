# 鎶€鏈柟妗堟枃妗?

## 1. 鎺ㄨ崘鎶€鏈爤

| 灞傜骇 | 鎶€鏈?|
|---|---|
| 鍓嶇妗嗘灦 | Vue 3銆乀ypeScript銆乂ite |
| UI 缁勪欢 | Element Plus |
| 璺敱涓庣姸鎬?| Vue Router銆丳inia |
| HTTP 瀹㈡埛绔?| Axios |
| 鍚庣妗嗘灦 | Spring Boot 3銆丼pring MVC銆丼pring Cloud Gateway銆丼pring Cloud OpenFeign銆丼pring Security |
| 鏁版嵁璁块棶 | Spring Data JPA锛屾垨 SQL-Toy |
| 鍙傛暟鏍￠獙 | Spring Validation |
| 鏁版嵁搴?| MySQL 8锛孭ostgreSQL 鍙€?|
| 璁よ瘉 | Spring Security銆丣WT銆丷BAC |
| API 鏂囨。 | Knife4j / Swagger3 |
| 鏈嶅姟娌荤悊 | Nacos 鎴?Eureka銆丯acos Config 鎴?Spring Cloud Config銆丷esilience4j 鎴?Sentinel |
| AI 闆嗘垚 | 鐙珛 `ai-service`銆丠TTP API銆佸ぇ璇█妯″瀷鏈嶅姟銆丼pring AI銆丆hatClient銆佺粨鏋勫寲杈撳嚭銆丷AG 鍜?Function Calling 鍙綔涓烘帹鑽愬疄鐜?|
| 瀹炴椂閫氫俊 | WebSocket銆丼SE |
| 閮ㄧ讲 | Nginx銆丣DK 17銆丮aven銆丏ocker 鍙€?|

## 2. 鍓嶇鏋舵瀯

```text
frontend/
  src/
    api/                 鎺ュ彛灏佽
    assets/              闈欐€佽祫婧?
    components/          閫氱敤缁勪欢
    router/              璺敱閰嶇疆
    stores/              Pinia 鐘舵€佹ā鍧?
    types/               TypeScript 绫诲瀷
    utils/
      request.ts         Axios 瀹炰緥鍜屾嫤鎴櫒
    views/
      auth/              鐧诲綍娉ㄥ唽
      patient/           鎮ｈ€呯椤甸潰
      doctor/            鍖荤敓绔〉闈?
      admin/             绠＄悊绔〉闈€丄I 鎺掔彮銆丄I 鍒嗚瘖鍙?
```

### 2.1 鍓嶇鍏抽敭璁捐

- 浣跨敤 `request.ts` 缁熶竴灏佽 Axios銆?
- 璇锋眰鎷︽埅鍣ㄨ嚜鍔ㄨ鍙?Token 骞惰缃?`Authorization: Bearer <token>`銆?
- 鍝嶅簲鎷︽埅鍣ㄧ粺涓€澶勭悊 `401`銆乣403`銆佷笟鍔￠敊璇拰 AI 閿欒銆?
- Pinia 鎷嗗垎涓?`userStore`銆乣registrationStore`銆乣doctorStore` 绛夋ā鍧椼€?
- 璺敱瀹堝崼鏍规嵁鐧诲綍鐘舵€佸拰瑙掕壊鎺у埗璁块棶锛岀鐞嗙璺敱鍙厑璁?ADMIN 瑙掕壊璁块棶銆?
- AI 璇锋眰缁熶竴灞曠ず Loading锛屽け璐ュ悗淇濈暀鎵嬪姩鎿嶄綔鍏ュ彛銆?

## 3. 鍚庣绾井鏈嶅姟鏋舵瀯

```text
backend/
  common-lib/              Result銆侀敊璇爜銆侀€氱敤 DTO銆佸伐鍏风被
  gateway-service/         缁熶竴鍏ュ彛銆佽矾鐢便€丆ORS銆侀檺娴併€侀壌鏉冨墠缃?
  auth-service/            鐧诲綍銆丣WT銆佽鑹层€乀oken 鍒锋柊
  patient-service/         鎮ｈ€呮敞鍐岃祫鏂欍€佹偅鑰呬釜浜轰腑蹇?
  doctor-service/          鍖荤敓銆佺瀹ゃ€佹帓鐝€佸彿婧?
  registration-service/    鍒涘缓鎸傚彿銆佸彇娑堟寕鍙枫€佹寕鍙疯褰?
  triage-service/          鍒嗚瘖涓氬姟缂栨帓銆佸垎璇婅褰曘€佹帹鑽愮粨鏋?
  medical-record-service/  鐥呭巻鐢熸垚缂栨帓銆佺梾鍘嗕繚瀛樸€佺梾鍘嗘煡璇?
  prescription-service/    澶勬柟寮€鍏枫€佸鏂规槑缁嗐€佸鏂瑰鏍歌褰?
  ai-service/              鏅鸿兘鍒嗚瘖銆佺梾鍘嗙敓鎴愩€佸鏂瑰鏍搞€丳rompt銆佹ā鍨嬮€傞厤
  notification-service/    WebSocket 杩炴帴銆佸疄鏃堕€氱煡銆侀€氱煡鍘嗗彶
  admin-service/           绠＄悊绔仛鍚堝叆鍙ｃ€佺郴缁熼厤缃€佸悗鍙版搷浣滅紪鎺?
```

### 3.1 鍚庣鍏抽敭璁捐

- Controller 鍙鐞嗚姹傛帴鏀躲€佸弬鏁版牎楠屽拰鍝嶅簲灏佽銆?
- 鍓嶇鍙闂?`gateway-service`锛屼笉鐩存帴璁块棶涓氬姟鏈嶅姟鎴?`ai-service`銆?
- 鍚勪笟鍔℃湇鍔℃寜棰嗗煙鍒掑垎锛屽垎鍒礋璐ｈ嚜宸辩殑 Controller銆丼ervice銆丷epository 鍜屾暟鎹簱 schema銆?
- 鏈嶅姟涔嬮棿閫氳繃 OpenFeign 璋冪敤锛屼笉鍏佽璺ㄦ湇鍔＄洿鎺ヨ闂暟鎹簱銆?
- 姣忎釜 Feign Client 蹇呴』璁剧疆瓒呮椂銆侀敊璇В鐮併€佺啍鏂拰闄嶇骇銆?
- `ai-service` 鍙礋璐ｆā鍨嬭皟鐢ㄣ€丳rompt 娓叉煋銆佺粨鏋勫寲杈撳嚭鏍￠獙鍜?AI 闄嶇骇锛屼笉淇濆瓨鎸傚彿銆佺梾鍘嗐€佸鏂圭瓑涓氬姟涓绘暟鎹€?
- `notification-service` 璐熻矗 WebSocket 杩炴帴鍜岄珮椋庨櫓澶勬柟瀹炴椂鍛婅銆?
- `admin-service` 鍙綔涓虹鐞嗙鑱氬悎鍜屽悗鍙版搷浣滅紪鎺掑叆鍙ｏ紱绉戝銆佸尰鐢熴€佽嵂鍝併€丳rompt 绛変笟鍔′富鏁版嵁浠嶅啓鍏ュ悇鑷綊灞炴湇鍔°€侫I 鎺掔彮鐢?`admin-service` 缂栨帓 `ai-service` 鍜?`doctor-service`锛孉I 鍒嗚瘖鍙扮敱 `admin-service` 缂栨帓 `triage-service`銆乣doctor-service` 鍜?`ai-service`銆?
- SSE 鎴?WebSocket 鐢ㄤ簬鐥呭巻鐢熸垚娴佸紡杈撳嚭锛屽彲鐢?`medical-record-service` 閫忎紶 `ai-service` 鐨勬祦寮忕粨鏋溿€?
- 浣跨敤 `@Transactional` 淇濊瘉鍗曚釜鏈嶅姟鍐呴儴鍐欐搷浣滀竴鑷存€э紱璺ㄦ湇鍔℃祦绋嬩娇鐢ㄧ姸鎬佹祦杞€佽ˉ鍋挎垨骞傜瓑鎺ュ彛澶勭悊銆?
- 浣跨敤 `@RestControllerAdvice` 缁熶竴澶勭悊寮傚父銆?

## 4. 鏁版嵁搴撹璁℃€濊矾

- 鏁欏鐜鍙互鍏辩敤涓€涓?MySQL 瀹炰緥锛屼絾蹇呴』鎸夋湇鍔″垝鍒嗙嫭绔嬫暟鎹簱鎴?schema銆?
- `patient-service` 鍙淮鎶ゆ偅鑰呰祫鏂欙紝`doctor-service` 鍙淮鎶ゅ尰鐢熴€佺瀹ゅ拰鎺掔彮銆?
- `registration-service` 缁存姢鎸傚彿璁板綍锛屼笉鐩存帴 join 鎮ｈ€呰〃鎴栧尰鐢熻〃锛岄渶瑕佹暟鎹椂閫氳繃鏈嶅姟鎺ュ彛鏌ヨ銆?
- `medical-record-service` 缁存姢鐥呭巻璁板綍锛宍prescription-service` 缁存姢澶勬柟鍜屽鏂瑰鏍歌褰曘€?
- `ai-service` 缁存姢 AI 璋冪敤鏃ュ織銆丳rompt 妯℃澘鍜屾ā鍨嬮厤缃紝涓嶄繚瀛樻寮忕梾鍘嗘垨澶勬柟銆?
- 绂佹璺?schema 鐩存帴 join锛岃法鏈嶅姟鑱氬悎鐢辨帴鍙ｇ紪鎺掓垨鏌ヨ妯″瀷瀹屾垚銆?

## 5. 绗笁鏂规湇鍔℃垨鎺ュ彛

| 鏈嶅姟 | 鐢ㄩ€?| MVP 绛栫暐 |
|---|---|---|
| Nacos 鎴?Eureka | 鏈嶅姟娉ㄥ唽鍙戠幇 | 蹇呴€?|
| Spring Cloud Gateway | 缁熶竴 API 鍏ュ彛鍜岃矾鐢?| 蹇呴€?|
| OpenFeign | 鏈嶅姟闂村悓姝ヨ皟鐢?| 蹇呴€?|
| Resilience4j 鎴?Sentinel | 鐔旀柇銆侀檺娴併€侀檷绾?| 鎺ㄨ崘 |
| `ai-service` | 鍒嗚瘖銆佺梾鍘嗙敓鎴愩€佸鏂瑰鏍哥粺涓€鍏ュ彛 | 蹇呴€?|
| 澶ц瑷€妯″瀷 API | 鍒嗚瘖銆佺梾鍘嗙敓鎴愩€佸鏂瑰鏍?| 鍙厛浣跨敤 Mock 瀹炵幇 |
| Spring AI / ChatClient | 妯″瀷閫傞厤銆佺粨鏋勫寲杈撳嚭銆佸伐鍏疯皟鐢?| 鎺ㄨ崘 |
| 鐭ヨ瘑鍥捐氨鏈嶅姟 | 鑽搧鐩镐簰浣滅敤銆佺柧鐥呯瀹ゆ槧灏?| 鍙€?|
| Knife4j | API 鏂囨。鍜岃仈璋?| 蹇呴€?|
| Nginx | 鍓嶇閮ㄧ讲鍜屽弽鍚戜唬鐞?| 鐢熶骇鐜浣跨敤 |

## 6. 绯荤粺妯″潡鍒掑垎

| 妯″潡 | 鍚庣鏈嶅姟 | 鍓嶇椤甸潰 |
|---|---|---|
| 缃戝叧 | `gateway-service` | 鎵€鏈夐〉闈㈢粺涓€鍏ュ彛 |
| 璁よ瘉 | `auth-service` | 鐧诲綍椤点€佹敞鍐岄〉 |
| 鎮ｈ€?| `patient-service` | 鎮ｈ€呴椤点€佷釜浜轰腑蹇?|
| 鍖荤敓 | `doctor-service` | 鍖荤敓鍒楄〃銆佸尰鐢熷伐浣滃彴 |
| 鎸傚彿 | `registration-service` | 鍦ㄧ嚎鎸傚彿銆佹垜鐨勬寕鍙?|
| 鏅鸿兘鍒嗚瘖 | `triage-service` 璋冪敤 `ai-service` 鍜?`doctor-service` | 鏅鸿兘鍒嗚瘖椤?|
| 鐥呭巻 | `medical-record-service` 璋冪敤 `ai-service` | 闂瘖鐥呭巻椤点€佺梾鍘嗚鎯?|
| 澶勬柟 | `prescription-service` 璋冪敤 `ai-service` 鍜?`notification-service` | 澶勬柟寮€鍏烽〉銆佸鏂硅鎯?|
| 閫氱煡 | `notification-service` | 鍖荤敓绔€氱煡鎶藉眽銆佸疄鏃跺憡璀?|
| 绠＄悊 | `admin-service` 缂栨帓鍚勫綊灞炴湇鍔?| 绠＄悊绔熀纭€鏁版嵁缁存姢銆丄I 鎺掔彮绠＄悊銆丄I 鍒嗚瘖鍙?|

## 7. 鍏抽敭鎶€鏈毦鐐逛笌瑙ｅ喅鏂规

| 闅剧偣 | 瑙ｅ喅鏂规 |
|---|---|
| AI 杩斿洖涓嶇ǔ瀹?| 瑕佹眰 JSON 杈撳嚭锛屽悗绔牎楠屽瓧娈碉紝涓嶅悎娉曞垯杩斿洖闄嶇骇鎻愮ず |
| AI 璇锋眰鑰楁椂 | 鍓嶇 Loading锛屽悗绔缃秴鏃讹紝蹇呰鏃跺紓姝ュ鐞?|
| AI 鏈嶅姟涓嶅彲鐢?| 浣跨敤 Mock 鎴栨墜鍔ㄦ祦绋嬪厹搴?|
| 寰湇鍔¤皟鐢ㄥけ璐?| OpenFeign 璁剧疆瓒呮椂銆侀敊璇В鐮佸拰闄嶇骇锛孯esilience4j/Sentinel 杩涜鐔旀柇闄愭祦 |
| 楂橀闄╁憡璀﹀疄鏃舵€?| 浣跨敤 WebSocket 鎺ㄩ€佺粰鍖荤敓绔紝鏂嚎鍚庡墠绔噸杩?|
| 鐥呭巻鐢熸垚绛夊緟鏃堕棿闀?| 浣跨敤 SSE 鎴?WebSocket 娴佸紡杈撳嚭锛屽墠绔€愬瓧灞曠ず |
| 澶嶆潅鍓嶇娴佺▼鐘舵€佹贩涔?| 浣跨敤 Pinia 妯″潡鍖?Store 鍜屾祦绋嬬姸鎬佹灇涓?|
| Prompt 闅剧淮鎶?| 浣跨敤鍙厤缃?Prompt 妯℃澘锛屾寜浠诲姟绫诲瀷鍜岀瀹ら€夋嫨 |
| 绠＄悊绔寖鍥村け鎺?| 灏?P0 闄愬畾涓哄熀纭€鏁版嵁缁存姢銆丄I 鎺掔彮寤鸿 + 浜哄伐纭鍙戝竷銆丄I 鍒嗚瘖鍙版煡鐪?鏀规淳 |
| 鏉冮檺闅旂 | JWT + 瑙掕壊鏍￠獙 + 鏁版嵁褰掑睘鏍￠獙 |
| 鍓嶅悗绔法鍩?| Vite 浠ｇ悊 + 鍚庣 CORS 閰嶇疆 |
| 閲嶅鎸傚彿 | 鏁版嵁搴撳敮涓€绾︽潫鎴栦笟鍔℃牎楠?|
| 澶勬柟瀹夊叏 | AI 浠呰緟鍔╋紝鍖荤敓纭鍚庢墠鑳戒繚瀛?|
| 婕旂ず绋冲畾鎬?| 鍑嗗鍥哄畾婕旂ず鏁版嵁鍜?Mock AI 杩斿洖 |
## RabbitMQ 与 KingbaseES 搜索落地说明

- KingbaseES 是唯一事实库，业务写入以各领域服务的 JPA Repository 为准。
- RabbitMQ 使用 Topic Exchange `scb.domain`，承载处方风险通知等异步事件。
- 业务服务先写 `outbox_event`，再由定时发布器投递 RabbitMQ，避免主事务成功但消息丢失。
- 知识库、药品、Prompt 模板搜索由 `admin-service` 直接查询 KingbaseES 表，无需额外搜索中间件。
