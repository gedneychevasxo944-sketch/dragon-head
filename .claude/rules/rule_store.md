# Store 存储架构规范

## 核心原则

所有实体存储（Entity Store）必须遵循以下三层架构：

1. **接口层** — 定义在 `org.dragon.{module}.store` 包下，继承 `Store` 接口
2. **实现层** — 按存储介质命名（如 `MemoryXxxStore`、`MySqlXxxStore`），标注 `@StoreTypeAnn`
3. **工厂层** — 通过 `StoreFactory` 获取实例，**禁止直接注入实现类**

## 接口定义

```java
// 位置: org.dragon.{module}.store.XxxStore.java
public interface XxxStore extends Store {
    // 业务方法定义
    void save(XxxEntity entity);
    Optional<XxxEntity> findById(String id);
}
```

## 实现类规范

每个存储介质至少提供一个实现类：

```java
// 位置: org.dragon.{module}.store.MemoryXxxStore.java
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)  // 必须标注存储类型
public class MemoryXxxStore implements XxxStore {
    // 内存实现
}

// 位置: org.dragon.{module}.store.MySqlXxxStore.java
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlXxxStore implements XxxStore {
    private final Database mysqlDb;

    public MySqlXxxStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }
    // MySQL 实现
}
```

**注意**：
- `@StoreTypeAnn` 已包含 `@Component`，无需重复标注
- MySQL 实现类**不要**加 `@Primary`，由 `StoreProperties.type` 决定使用哪个
- 构造方法参数通过 Spring 注入，不要手动 `new`

## StoreType 枚举

存储类型定义在 `org.dragon.store.StoreType`：

```java
public enum StoreType {
    MEMORY,  // 内存（开发/测试用）
    MYSQL,   // MySQL 持久化
    FILE     // 文件存储
}
```

## 使用方式

**正确做法 — 通过 StoreFactory 获取：**
```java
@Service
public class XxxService {
    private final StoreFactory storeFactory;

    public XxxService(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public void doSomething() {
        XxxStore store = storeFactory.get(XxxStore.class);  // 按配置类型获取
        store.save(entity);
    }

    public void doSomethingWithSpecificType() {
        XxxStore store = storeFactory.get(XxxStore.class, StoreType.MYSQL);  // 指定类型
        store.findById(id);
    }
}
```

**错误做法 — 直接注入实现：**
```java
// WRONG — 违反工厂模式，耦合特定实现
@Autowired
private MySqlXxxStore mySqlStore;

// WRONG — 直接实例化
private XxxStore store = new MemoryXxxStore();
```

## 新增实体存储步骤

1. 在 `org.dragon.datasource.entity` 包下创建 `XxxEntity` 实体类（JPA/Ebean 注解标注）
2. 在对应模块的 `store` 包下创建 `XxxStore` 接口，继承 `Store`
3. 创建 `MemoryXxxStore` 实现，标注 `@StoreTypeAnn(StoreType.MEMORY)`
4. 创建 `MySqlXxxStore` 实现，标注 `@StoreTypeAnn(StoreType.MYSQL)`
5. 如有需要，创建 `FileXxxStore` 实现，标注 `@StoreTypeAnn(StoreType.FILE)`
6. 如需数据库表，创建 Flyway migration SQL 文件
7. 使用 `storeFactory.get(XxxStore.class)` 获取实例

## Entity 放置规范

**所有 Store 实现使用的 Entity 必须统一放在 `org.dragon.datasource.entity` 包下**，禁止在各模块目录下分散创建 Entity 类。

```
org.dragon.datasource.entity/
├── ConfigEntity.java          # ConfigStore 对应
├── ObserverActionLogEntity.java  # ActionLogStore 对应
├── XxxEntity.java            # 其他 Store 对应
```

Entity 与 Store 接口的对应关系通过模块约定：Entity 类放在 `datasource.entity`，Store 接口放在 `{module}.store`。

## 已有示例

参考以下实现理解模式：

| 接口 | Memory 实现 | MySQL 实现 |
|------|------------|-----------|
| `ExecutionHistoryStore` | `MemoryExecutionHistoryStore` | `MySqlExecutionHistoryStore` |
| `ChatSessionStore` | — | `MySqlChatSessionStore` |
| `ConfigStore` | `MemoryConfigStore` | `MySqlConfigStore` |
| `ActionLogStore` | — | `MySqlActionLogStore` |
