package org.dragon.workspace.chat;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import org.dragon.datasource.entity.ChatMessageEntity;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySqlChatMessageStore 单元测试
 * 使用真实 MySQL 数据源进行测试
 */
class MySqlChatMessageStoreTest {

    private static Database database;
    private MySqlChatMessageStore messageStore;

    private String testWorkspaceId;
    private String testSenderId;
    private String testReceiverId;
    private String testSessionId;

    @BeforeAll
    static void initDatabase() throws Exception {
        // 创建 MySQL DataSource
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/adeptify_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true");
        config.setUsername("root");
        config.setPassword("123456");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        DataSource dataSource = new HikariDataSource(config);

        // 运行 Flyway 迁移
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        // 创建 Ebean Database
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDataSource(dataSource);
        dbConfig.addPackage("org.dragon.datasource.entity");
        database = DatabaseFactory.create(dbConfig);
    }

    @BeforeEach
    void setUp() {
        messageStore = new MySqlChatMessageStore(database);

        testWorkspaceId = "workspace-" + UUID.randomUUID();
        testSenderId = "sender-" + UUID.randomUUID();
        testReceiverId = "receiver-" + UUID.randomUUID();
        testSessionId = "session-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (database != null) {
            database.find(ChatMessageEntity.class)
                    .where()
                    .eq("workspaceId", testWorkspaceId)
                    .delete();
        }
    }

    @Test
    void testSaveAndFindById() {
        ChatMessage message = createTestMessage();
        messageStore.save(message);

        ChatMessage found = messageStore.findById(message.getId());
        assertNotNull(found);
        assertEquals(message.getId(), found.getId());
        assertEquals(message.getContent(), found.getContent());
        assertEquals(message.getWorkspaceId(), found.getWorkspaceId());
        assertEquals(message.getSenderId(), found.getSenderId());
        assertEquals(message.getReceiverId(), found.getReceiverId());
    }

    @Test
    void testFindByWorkspaceId() {
        for (int i = 0; i < 3; i++) {
            messageStore.save(createTestMessage());
        }

        List<ChatMessage> messages = messageStore.findByWorkspaceId(testWorkspaceId, 10);
        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().allMatch(m -> m.getWorkspaceId().equals(testWorkspaceId)));
    }

    @Test
    void testFindByWorkspaceIdWithLimit() {
        for (int i = 0; i < 5; i++) {
            messageStore.save(createTestMessage());
        }

        List<ChatMessage> messages = messageStore.findByWorkspaceId(testWorkspaceId, 3);
        assertEquals(3, messages.size());
    }

    @Test
    void testFindByWorkspaceIdAndTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message1 = createTestMessage();
        message1.setTimestamp(now.minusMinutes(10));
        messageStore.save(message1);

        ChatMessage message2 = createTestMessage();
        message2.setTimestamp(now.plusMinutes(10));
        messageStore.save(message2);

        List<ChatMessage> messages = messageStore.findByWorkspaceIdAndTimeRange(
                testWorkspaceId, now.minusMinutes(30), now.plusMinutes(30));

        assertEquals(2, messages.size());
    }

    @Test
    void testFindByWorkspaceIdAndReceiverId() {
        ChatMessage message = createTestMessage();
        message.setReceiverId(testReceiverId);
        messageStore.save(message);

        List<ChatMessage> messages = messageStore.findByWorkspaceIdAndReceiverId(
                testWorkspaceId, testReceiverId, 10);

        assertEquals(1, messages.size());
        assertEquals(testReceiverId, messages.get(0).getReceiverId());
    }

    @Test
    void testFindByCharacterId() {
        ChatMessage message1 = createTestMessage();
        message1.setSenderId(testSenderId);
        messageStore.save(message1);

        ChatMessage message2 = createTestMessage();
        message2.setSenderId(testReceiverId);
        message2.setReceiverId(testSenderId);
        messageStore.save(message2);

        List<ChatMessage> sentMessages = messageStore.findByCharacterId(testSenderId, 10);
        assertTrue(sentMessages.size() >= 2);
    }

    @Test
    void testFindBySessionId() {
        ChatMessage message = createTestMessage();
        message.setSessionId(testSessionId);
        messageStore.save(message);

        List<ChatMessage> messages = messageStore.findBySessionId(testSessionId);
        assertEquals(1, messages.size());
        assertEquals(testSessionId, messages.get(0).getSessionId());
    }

    @Test
    void testMarkAsRead() {
        ChatMessage message = createTestMessage();
        message.setRead(false);
        messageStore.save(message);

        assertFalse(messageStore.findById(message.getId()).isRead());

        messageStore.markAsRead(message.getId());

        assertTrue(messageStore.findById(message.getId()).isRead());
    }

    @Test
    void testDelete() {
        ChatMessage message = createTestMessage();
        messageStore.save(message);

        assertNotNull(messageStore.findById(message.getId()));

        messageStore.delete(message.getId());

        assertNull(messageStore.findById(message.getId()));
    }

    @Test
    void testDeleteByWorkspaceId() {
        for (int i = 0; i < 3; i++) {
            messageStore.save(createTestMessage());
        }

        assertFalse(messageStore.findByWorkspaceId(testWorkspaceId, 10).isEmpty());

        messageStore.deleteByWorkspaceId(testWorkspaceId);

        assertTrue(messageStore.findByWorkspaceId(testWorkspaceId, 10).isEmpty());
    }

    @Test
    void testSaveWithMetadata() {
        ChatMessage message = createTestMessage();
        message.setMetadata(Map.of("key1", "value1", "key2", 123L));
        messageStore.save(message);

        ChatMessage found = messageStore.findById(message.getId());
        assertNotNull(found.getMetadata());
        assertEquals("value1", found.getMetadata().get("key1"));
        assertEquals(123L, found.getMetadata().get("key2"));
    }

    @Test
    void testMessageType() {
        ChatMessage textMessage = createTestMessage();
        textMessage.setMessageType(ChatMessage.MessageType.TEXT);
        messageStore.save(textMessage);

        ChatMessage taskMessage = createTestMessage();
        taskMessage.setMessageType(ChatMessage.MessageType.TASK);
        messageStore.save(taskMessage);

        assertEquals(ChatMessage.MessageType.TEXT,
                messageStore.findById(textMessage.getId()).getMessageType());
        assertEquals(ChatMessage.MessageType.TASK,
                messageStore.findById(taskMessage.getId()).getMessageType());
    }

    private ChatMessage createTestMessage() {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(testWorkspaceId)
                .senderId(testSenderId)
                .receiverId(testReceiverId)
                .content("Test message content: " + UUID.randomUUID())
                .messageType(ChatMessage.MessageType.TEXT)
                .sessionId(testSessionId)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();
    }
}