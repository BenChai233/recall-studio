# API 说明（v0.1）

> 约定：所有接口以 `/api` 开头，默认返回 JSON。

## 1. 基本约定

### 1.1 响应包装
- 成功响应统一包装为：
```json
{ "code": "OK", "message": "ok", "data": { } }
```
- 本文档中的“响应示例”默认展示 `data` 字段内容。
- 错误响应保持 HTTP 4xx/5xx，并返回：
```json
{ "code": "VALIDATION_ERROR", "message": "deckId is required", "data": { "field": "deckId" } }
```


- Base URL：`http://localhost:8080`
- 时间格式：ISO 8601（例如 `2026-01-22T10:00:00+08:00`）
- ID 格式：UUID
- 请求头：`Content-Type: application/json`

## 2. 通用错误响应

HTTP 4xx/5xx 返回示例：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "deckId is required",
  "data": {
    "field": "deckId"
  }
}
```

常见错误码：
- `NOT_FOUND`
- `VALIDATION_ERROR`
- `GIT_REPO_INVALID`
- `DATA_DIR_NOT_SET`
- `CONFLICT`
- `UNDO_UNSUPPORTED`
- `UNDO_FAILED`

## 3. Deck

### 3.1 列表
- `GET /api/decks?keyword=&archived=`

响应示例：
```json
[
  {
    "deckId": "6d0a8c6b-8e9a-4b9a-a98b-0a3d4c8a2f31",
    "name": "Java 基础",
    "description": "JVM/集合/并发",
    "tags": ["java", "backend"],
    "archived": false,
    "createdAt": "2026-01-22T10:00:00+08:00",
    "updatedAt": "2026-01-22T10:00:00+08:00"
  }
]
```

### 3.2 创建
- `POST /api/decks`

请求示例：
```json
{
  "name": "Java 基础",
  "description": "JVM/集合/并发",
  "tags": ["java", "backend"]
}
```

### 3.3 详情
- `GET /api/decks/{deckId}`

### 3.4 更新
- `PUT /api/decks/{deckId}`

请求示例：
```json
{
  "name": "Java 基础（更新）",
  "description": "JVM/集合/并发",
  "tags": ["java", "backend"],
  "archived": false
}
```

### 3.5 删除
- `DELETE /api/decks/{deckId}`

## 4. Item

### 4.1 列表
- `GET /api/items?deckId=&type=&tag=&archived=&dueBefore=`

响应示例：
```json
[
  {
    "itemId": "d1f2a3b4-5678-4cde-9abc-1234567890ab",
    "deckId": "6d0a8c6b-8e9a-4b9a-a98b-0a3d4c8a2f31",
    "type": "concept",
    "prompt": "解释 JVM 的类加载过程。",
    "hint": "加载/验证/准备/解析/初始化",
    "answerMarkdown": "- 加载\n- 验证\n- 准备\n- 解析\n- 初始化",
    "tags": ["jvm"],
    "difficulty": "medium",
    "archived": false,
    "srs": {
      "easiness": 2.5,
      "interval": 3,
      "due": "2026-01-25T10:00:00+08:00"
    },
    "createdAt": "2026-01-22T10:00:00+08:00",
    "updatedAt": "2026-01-22T10:00:00+08:00"
  }
]
```

### 4.2 创建
- `POST /api/items`

请求示例：
```json
{
  "deckId": "6d0a8c6b-8e9a-4b9a-a98b-0a3d4c8a2f31",
  "type": "concept",
  "prompt": "解释 JVM 的类加载过程。",
  "hint": "加载/验证/准备/解析/初始化",
  "answerMarkdown": "- 加载\n- 验证\n- 准备\n- 解析\n- 初始化",
  "tags": ["jvm"],
  "difficulty": "medium"
}
```

### 4.3 详情
- `GET /api/items/{itemId}`

### 4.4 更新
- `PUT /api/items/{itemId}`

### 4.5 归档/恢复
- `PATCH /api/items/{itemId}/archive`

请求示例：
```json
{
  "archived": true
}
```

## 5. Review / 会话

### 5.1 今日任务
- `GET /api/reviews/today`

响应示例：
```json
{
  "dueCount": 12,
  "wrongCount": 3,
  "newCount": 5,
  "totalPlanned": 20
}
```

### 5.2 创建会话
- `POST /api/reviews/sessions`

请求示例：
```json
{
  "deckId": "6d0a8c6b-8e9a-4b9a-a98b-0a3d4c8a2f31",
  "onlyWrong": false,
  "limit": 20
}
```

响应示例：
```json
{
  "sessionId": "a7b6c5d4-1234-4b6a-9cde-9876543210ff",
  "items": [
    {
      "itemId": "d1f2a3b4-5678-4cde-9abc-1234567890ab",
      "prompt": "解释 JVM 的类加载过程。",
      "type": "concept",
      "dueType": "DUE"
    }
  ]
}
```

### 5.3 提交作答
- `POST /api/reviews`

请求示例：
```json
{
  "sessionId": "a7b6c5d4-1234-4b6a-9cde-9876543210ff",
  "itemId": "d1f2a3b4-5678-4cde-9abc-1234567890ab",
  "score": 1,
  "answer": "类加载分为加载、验证、准备、解析、初始化。",
  "reasonTags": ["concept"],
  "reviewedAt": "2026-01-22T10:05:00+08:00"
}
```

响应示例：
```json
{
  "nextDue": "2026-01-24T10:05:00+08:00",
  "srs": {
    "easiness": 2.46,
    "interval": 2,
    "due": "2026-01-24T10:05:00+08:00"
  }
}
```

### 5.4 会话复盘
- `GET /api/reviews/sessions/{sessionId}/summary`

响应示例：
```json
{
  "avgScore": 1.4,
  "scoreCount": { "0": 2, "1": 6, "2": 2 },
  "wrongItems": [
    { "itemId": "xxx", "prompt": "..." }
  ],
  "nextQuestions": [
    "为什么类加载需要验证阶段？",
    "双亲委派模型的边界在哪里？"
  ]
}
```

### 5.5 ?????
- `POST /api/reviews/sessions/{sessionId}/undo`

?????
```json
{
  "itemId": "d1f2a3b4-5678-4cde-9abc-1234567890ab",
  "score": 1,
  "answer": "?????????????????????",
  "reasonTags": ["concept"]
}
```

???
- ?????????????
- ????????????????? `UNDO_UNSUPPORTED`

### 5.6 ?????
- `POST /api/reviews/items/{itemId}/undo`

?????
```json
{
  "itemId": "d1f2a3b4-5678-4cde-9abc-1234567890ab",
  "score": 1,
  "answer": "?????????????????????",
  "reasonTags": ["concept"]
}
```

???
- ?????????????
- ????????????????? `UNDO_UNSUPPORTED`

## 6. 设置

### 6.1 获取设置
- `GET /api/settings`

响应示例：
```json
{
  "dailyLimit": 50,
  "newRatio": 0.2,
  "scoreMap": { "0": "Again", "1": "Hard", "2": "Good" },
  "srsAlgo": "SM2_SIMPLE",
  "wrongReviewSchedule": [1, 3, 7]
}
```

### 6.2 更新设置
- `PUT /api/settings`

请求示例：
```json
{
  "dailyLimit": 40,
  "newRatio": 0.3,
  "wrongReviewSchedule": [1, 3, 7]
}
```

## 7. 导入 / 导出

### 7.1 导入
- `POST /api/import`

请求示例：
```json
{
  "decks": [
    { "deckId": "d1", "name": "Java 基础" }
  ],
  "items": [
    {
      "itemId": "i1",
      "deckId": "d1",
      "type": "concept",
      "prompt": "什么是 JVM？"
    }
  ]
}
```

响应示例：
```json
{
  "importedDecks": 1,
  "importedItems": 1,
  "skippedItems": 0
}
```

### 7.2 导出
- `GET /api/export?includeReviews=true|false`

响应示例：
```json
{
  "decks": [ ... ],
  "items": [ ... ],
  "reviews": [ ... ],
  "settings": { ... }
}
```

## 8. Insights / 统计

### 8.1 错因分布
- `GET /api/insights/reasons?days=30`

响应示例：
```json
{
  "wrongReviewCount": 12,
  "totalTagCount": 20,
  "stats": [
    { "tag": "概念", "count": 8 },
    { "tag": "机制", "count": 6 },
    { "tag": "边界", "count": 4 },
    { "tag": "未标记", "count": 2 }
  ]
}
```

说明：
- `days` 为可选参数，表示仅统计最近 N 天的错题记录（不传则统计全部）。

## 9. Sync / 同步状态

### 9.1 同步状态
- `GET /api/sync/status`

响应示例：
```json
{
  "hasSnapshot": true,
  "lastSyncAt": "2026-01-22T16:20:00+08:00",
  "dirty": false,
  "fileCount": 120,
  "lastModifiedAt": "2026-01-22T16:18:21+08:00"
}
```

### 9.2 记录同步快照
- `POST /api/sync/snapshot`

响应示例：
```json
{
  "hasSnapshot": true,
  "lastSyncAt": "2026-01-22T16:20:00+08:00",
  "dirty": false,
  "fileCount": 120,
  "lastModifiedAt": "2026-01-22T16:18:21+08:00"
}
```
