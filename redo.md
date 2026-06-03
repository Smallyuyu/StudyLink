# StudyLink Refactoring Review Guide

本文件給 LLM / Agent 用來檢查 StudyLink 是否有需要重構的地方。重構目標是讓程式碼更清楚、更安全、更容易維護，但必須維持既有整體邏輯、操作流程、URL、頁面內容與使用者體驗不變。

## 核心原則

1. 不改功能語意：使用者能做的事情、操作順序、輸入輸出結果、錯誤處理語意都要維持一致。
2. 不改頁面行為：Thymeleaf template 的頁面結構、表單欄位、連結、按鈕、路由與展示文字，除非是修正明確 bug，否則不要變動。
3. 只做架構與可維護性改善：把責任放到正確層級、降低重複、讓資料流更清楚、讓例外與權限檢查更一致。
4. 不為了「看起來更漂亮」而重構：只有當改動能降低風險、提升可讀性、消除漏洞、減少重複或改善測試性時才做。
5. 小步重構：每次只處理一組相關問題，避免一次大改 controller、service、entity、template。
6. 重構後必須可驗證：至少執行 `mvn test`，並說明哪些行為被保證沒有改變。

## 專案架構基準

StudyLink 是 Spring Boot 3 + Thymeleaf + Spring Security + Spring Data JPA 專案。主要分層如下：

```text
Browser / Thymeleaf
  -> Controller
  -> Service
  -> Repository
  -> Entity / Database
```

套件邊界：

```text
user/      帳號、註冊、登入後個人資料
course/    課程、選課、同學探索
group/     讀書會、成員、加入申請、角色與狀態
matching/  推薦配對規則與結果
chat/      群組聊天室與 WebSocket 訊息
common/    共用例外、目前使用者、全域處理
config/    Spring Security、WebSocket、Demo data
```

LLM 檢查時應以這個架構為準，避免把商業邏輯散落到 template、controller 或 repository。

## 絕對不要任意改動

- 不要改 URL path，例如 `/courses`、`/groups`、`/matches`、`/dashboard`、`/ws`。
- 不要改 template 檔名、表單欄位 name、model attribute 名稱，除非同步確認所有 controller 與測試。
- 不要改資料庫關聯語意，例如 User-Course、StudyGroup-Membership、JoinRequest、ChatMessage 的關係。
- 不要改 matching 分數規則，除非需求明確要求。
- 不要改 demo 帳號、demo flow 或 README 描述中的使用流程。
- 不要用重構名義刪除測試。
- 不要把安全檢查從 service 移到只靠前端或只靠 template 隱藏按鈕。

## 可以重構的方向

### Controller

檢查 controller 是否只負責：

- 接收 request / form。
- 呼叫 service。
- 放入 model attribute。
- 回傳 view name 或 redirect。

需要重構的訊號：

- Controller 內有複雜條件判斷、權限判斷、資料查詢組合或 entity 狀態修改。
- 多個 controller 重複取得目前使用者、重複處理 NotFound 或 AccessDenied。
- Controller 直接操作 repository。
- Controller 方法過長，讀不出 request -> service -> view 的流程。

建議做法：

- 將商業規則移到對應 service。
- 將共用的目前使用者查詢保留在 `CurrentUser` 或 common helper。
- 將重複的 redirect / flash message 規則整理成私有方法，但不要過度抽象。

### Service

檢查 service 是否集中處理商業邏輯：

- 註冊、個人資料更新、選課、建立讀書會、加入申請、核准、推薦、聊天室發訊息。
- 權限檢查，例如只有 owner 能核准加入申請、只有 group member 能看聊天室。
- 狀態轉換，例如 JoinRequest pending -> accepted / rejected，StudyGroup open / full / closed。

需要重構的訊號：

- 同一段規則在多個 service 或 controller 重複。
- 權限檢查有漏掉或只在頁面上隱藏操作。
- 方法名稱看不出業務意圖。
- 一個 service 方法同時做太多事，難以測試。
- transaction 邊界不清楚，狀態修改沒有一致地在 service 完成。

建議做法：

- 用清楚的方法名稱表達使用者動作，例如 `requestToJoinGroup`、`approveJoinRequest`。
- 把可重用的驗證拆成 service 內私有方法，例如 `requireOwner`、`requireMember`。
- 對會修改資料的方法加上合理 transaction 邊界。
- 保持 service API 面向業務行為，而不是暴露零散 entity 操作。

### Repository

檢查 repository 是否只負責資料存取：

- Repository 不應包含商業判斷。
- Query method 名稱應清楚且避免重複。
- 常見查詢可用 Spring Data method 或簡單 JPQL，不要把大量資料撈出後在 controller 過濾。

需要重構的訊號：

- Repository 被 controller 直接呼叫。
- 多處重複查詢相同資料。
- 查詢名稱過長或語意不明。
- 可能造成 N+1 query 的列表頁資料存取。

### Entity / Form / DTO

檢查資料模型責任是否清楚：

- Entity 代表資料庫狀態與基本關聯。
- Form 代表使用者輸入，應使用 validation annotation。
- DTO / view model 代表輸出給頁面的整理結果。

需要重構的訊號：

- 直接把 entity 當成表單接收所有欄位。
- Template 需要呼叫太多巢狀 entity 關係才能顯示資料。
- Entity 裡出現大量頁面顯示邏輯。
- Validation 分散在 controller if 判斷，沒有放在 form object。

建議做法：

- 保持 `RegistrationForm`、`ProfileForm`、`StudyGroupForm`、`JoinRequestForm`、`CourseForm`、`ChatMessageForm` 的輸入責任。
- 將頁面需要的複合資料整理成小型 view DTO，而不是讓 template 自己推導。
- Entity 可保留簡單狀態輔助方法，但不要承擔跨 aggregate 的流程。

### Thymeleaf Templates

檢查 template 是否只負責呈現：

- 不應在 template 中塞入複雜業務判斷。
- 不應靠 template 隱藏按鈕取代後端權限檢查。
- 重複 layout、navigation、empty state 可整理，但要保持畫面不變。

需要重構的訊號：

- 同一段 HTML 在多個頁面重複且容易不一致。
- Template 有過多 `th:if` / `th:each` 巢狀邏輯。
- 頁面直接推導權限或狀態，而 service/controller 已可提供清楚 boolean。

建議做法：

- 優先維持現有 Bootstrap 與 layout。
- 若抽出 fragment，必須確認所有頁面的畫面與操作不變。
- 將複雜顯示資料先在 controller/service 整理好。

## 安全性檢查

LLM 檢查重構時必須特別確認：

- 密碼只能以 BCrypt hash 儲存，不能明文保存或輸出。
- 需要登入的操作必須由 Spring Security 或 service 權限檢查保護。
- Group owner 操作必須驗證 owner 身分。
- Group chat 只能讓成員讀取與發送。
- 使用者不能替其他使用者修改 profile、送出 join request 或送 chat message。
- 表單輸入應使用 validation，避免空值、過長文字或非法狀態。
- Thymeleaf 預設 escape 不要被不必要的 raw HTML 關閉。
- H2 console、demo data、開發設定不要誤當正式環境安全設定。

若發現安全問題，優先修補權限與資料驗證，再做可讀性重構。

## 可讀性與維護性檢查

逐檔檢查下列 code smell：

- 方法太長，包含多個不相關步驟。
- 類別同時負責 request、業務規則、資料查詢與畫面資料組裝。
- 重複的 magic string，例如 view name、redirect path、model key。
- 重複的權限檢查或狀態判斷。
- 命名過於模糊，例如 `process`、`handle`、`data`、`result`。
- Optional / null 處理不一致。
- Exception 型別不一致，導致錯誤頁或 redirect 行為難以預測。
- 測試只能覆蓋 happy path，沒有覆蓋權限、重複提交、非法狀態。

## LLM 審查流程

1. 先閱讀：
   - `README.md`
   - `docs/diagrams/architecture.md`
   - 目標功能相關的 controller、service、repository、entity、form、template、test

2. 建立不變條件：
   - 此功能目前有哪些 URL？
   - 使用者目前如何操作？
   - 頁面目前需要哪些 model attributes？
   - 目前有哪些測試描述既有行為？

3. 找出重構候選：
   - 哪些邏輯位置錯誤？
   - 哪些規則重複？
   - 哪些地方可能有權限或資料驗證漏洞？
   - 哪些命名、方法邊界或資料流讓程式碼難以理解？

4. 分級：
   - P0：安全漏洞、資料錯誤、未授權操作、會破壞核心流程。
   - P1：高風險重複邏輯、狀態轉換不一致、測試缺口明顯。
   - P2：命名、方法長度、可讀性、較低風險的整理。

5. 動手前先說明：
   - 要重構哪些檔案。
   - 哪些行為保證不變。
   - 為什麼這是架構改善，不是功能變更。

6. 實作時：
   - 優先小範圍修改。
   - 不改 template，除非重構目標與 template 重複或 bug 直接相關。
   - 不刪測試；必要時補測試。
   - 若發現需求不明，不猜測新功能。

7. 驗證：
   - 執行 `mvn test`。
   - 若有頁面或流程風險，手動檢查相關 URL。
   - 回報改動摘要、測試結果、剩餘風險。

## 建議輸出格式

LLM 完成檢查後，請用以下格式回報：

```markdown
## Refactoring Review

### Findings

- [P0/P1/P2] 檔案:行號 - 問題描述，以及為什麼這是架構或安全風險。

### Proposed Refactor

- 保持不變的行為：
- 要調整的責任邊界：
- 要新增或修改的測試：

### Changes Made

- 實際改了哪些檔案與原因。

### Verification

- `mvn test` 結果：
- 手動確認的流程：

### Remaining Risk

- 尚未處理或需要使用者決策的事項。
```

## 快速檢查清單

開始重構前，逐項確認：

- [ ] 這次改動不會改變 URL、頁面、表單欄位或使用者操作流程。
- [ ] Controller 沒有承擔商業規則。
- [ ] Service 集中處理權限、狀態轉換與資料修改。
- [ ] Repository 沒有商業邏輯。
- [ ] Form / DTO / Entity 的責任沒有混在一起。
- [ ] 權限檢查存在於後端，不只是在頁面上隱藏操作。
- [ ] 密碼、登入、owner 權限、group membership、chat access 沒有漏洞。
- [ ] 重複邏輯被移到合理位置，而不是新增過度抽象。
- [ ] 測試覆蓋主要成功流程與至少一個失敗或未授權流程。
- [ ] `mvn test` 通過。

## 一句話目標

重構 StudyLink 時，請把它當成「行為不變的架構清理」：讓每一層只做自己的事，讓權限與狀態規則集中且可測，讓未來新增功能時不需要猜測程式碼真正的意圖。
