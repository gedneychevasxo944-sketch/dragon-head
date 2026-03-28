# 通用代码规范

## 日志规范

### 使用方式

使用 Lombok `@Slf4j` 注解，日志前缀统一格式：`[ClassName] message`

```java
@Slf4j
@Service
public class XxxService {

    public void doSomething() {
        log.info("[XxxService] Starting operation...");
        log.debug("[XxxService] Detail info: {}", detail);
        log.warn("[XxxService] Warning: {}", warning);
        log.error("[XxxService] Error: {}", error);
    }
}
```

### 日志级别规范

| 级别 | 用途 |
|------|------|
| `Debug` | 详细流程追踪 |
| `Info` | 关键状态变更 |
| `Warn` | 警告信息 |
| `Error` | 错误信息，**必须附带异常对象** |

## 注释规范

### 类头部 Javadoc

```java
/**
 * 类职责说明
 *
 * @author authorName
 * @version 1.0
 */
```

### 方法 Javadoc

```java
/**
 * 方法功能说明
 *
 * @param paramName 参数说明
 * @return 返回值说明
 * @throws IllegalArgumentException 异常条件说明
 */
public void doSomething(String paramName) {
    // ...
}
```

### 代码内注释

- 代码块内用中文注释说明复杂逻辑
- 简单操作不写多余注释

## 异常处理

### 业务异常

```java
// 正确 —— 使用具体业务异常类
public class SkillNotFoundException extends RuntimeException {
    public SkillNotFoundException(String message) {
        super(message);
    }
}
```

### 禁止事项

```java
// 错误 —— 空 catch 块
catch (Exception e) {}

// 错误 —— 对外接口暴露 RuntimeException 裸类型
public void publicMethod() {
    throw new RuntimeException("error");  // 应使用业务异常
}
```

### 全局异常处理

使用 `@RestControllerAdvice` 统一处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, e.getMessage()));
    }
}
```

## 包结构

### 新功能包放置规则

```
org.dragon.{module}/
├── config/           # 配置类（ConfigurationProperties）
├── controller/       # REST API 控制器
├── service/          # 业务服务
├── store/           # 存储层接口
│   ├── MemoryXxxStore.java
│   └── MySqlXxxStore.java
├── entity/          # 数据实体（JPA/Ebean 注解）
├── dto/             # 数据传输对象
│   ├── XxxRequest.java
│   └── XxxResponse.java
├── exception/       # 业务异常
└── enums/           # 枚举类
```

### Entity 放置规范

**所有 Store 实现使用的 Entity 必须统一放在 `org.dragon.datasource.entity` 包下**

```
org.dragon.datasource.entity/
├── ConfigEntity.java
├── ObserverActionLogEntity.java
└── XxxEntity.java
```

## API 设计规范

### REST 路径规范

- 使用复数名词作为资源路径
- 使用 `@RequestMapping("/api/xxx")` 作为基础路径
- 使用 `@PreAuthorize("isAuthenticated()")` 进行身份验证

| 操作 | HTTP 方法 | 示例 |
|------|----------|------|
| 查询列表 | `@GetMapping` | `GET /api/skills` |
| 查询单个 | `@GetMapping("/{id}")` | `GET /api/skills/{id}` |
| 创建 | `@PostMapping` | `POST /api/skills` |
| 更新 | `@PutMapping("/{id}")` | `PUT /api/skills/{id}` |
| 删除 | `@DeleteMapping("/{id}")` | `DELETE /api/skills/{id}` |
| 状态变更 | `@PostMapping("/{id}/action")` | `POST /api/skills/{id}/disable` |

### API 响应格式

统一使用 `ApiResponse<T>` 包装响应：

```java
@Data
@Builder
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }
}
```

### HTTP 状态码规范

| 状态码 | HTTP 状态 | 用途 |
|--------|----------|------|
| 200 | OK | 成功 |
| 201 | CREATED | 创建成功 |
| 204 | NO_CONTENT | 删除成功 |
| 400 | BAD_REQUEST | 参数错误 |
| 403 | FORBIDDEN | 权限不足 |
| 500 | INTERNAL_SERVER_ERROR | 服务器内部错误 |
