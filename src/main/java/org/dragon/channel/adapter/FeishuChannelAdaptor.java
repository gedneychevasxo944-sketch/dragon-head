package org.dragon.channel.adapter;

import com.lark.oapi.Client;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.MentionEvent;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.ReplyMessageReq;
import com.lark.oapi.service.im.v1.model.ReplyMessageReqBody;
import com.lark.oapi.service.im.v1.model.ReplyMessageResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.enums.ActionType;
import org.dragon.channel.parser.FeishuParser;
import org.dragon.gateway.Gateway;
import org.dragon.util.GsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * FeishuChannelAdaptor 飞书渠道适配器
 *
 * <p>支持两种凭证来源，运行时按以下优先级加载：
 * <ol>
 *   <li>通过 {@link #configure(ChannelConfig)} 动态注入（后台管理界面配置）</li>
 *   <li>Spring 配置文件 {@code channel.feishu.*}（向后兼容，默认值）</li>
 * </ol>
 *
 * <p>动态配置后需调用 {@link #restart()} 或 {@link #startListening(Gateway)} 使配置生效。
 *
 * @author zhz
 * @version 2.0
 */
@Component
@Slf4j
public class FeishuChannelAdaptor implements ChannelAdapter {
    // ===== 默认凭证（来自 application.yml，向后兼容）=====
    @Value("${channel.feishu.appId:}")
    private String defaultAppId;
    @Value("${channel.feishu.appSecret:}")
    private String defaultAppSecret;
    @Value("${channel.feishu.whitelist:}")
    private List<String> defaultWhitelist;
    @Value("${channel.feishu.wakeWord:}")
    private String defaultWakeWord;
    @Value("${channel.feishu.robotOpenId:}")
    private String defaultRobotOpenId;

    // ===== 运行时凭证（后台管理界面动态注入，优先于默认值）=====
    private String runtimeAppId;
    private String runtimeAppSecret;
    private List<String> runtimeWhitelist;
    private String runtimeWakeWord;
    private String runtimeRobotOpenId;

    private com.lark.oapi.ws.Client wsClient;
    private Client apiClient;
    private Gateway gateway;

    @Autowired
    private FeishuParser feishuParser;

    // ==================== ChannelAdapter 接口实现 ====================

    @Override
    public String getChannelName() {
        return "Feishu";
    }

    @Override
    public void startListening(Gateway gateway) {
        this.gateway = gateway;

        String appId = resolveAppId();
        String appSecret = resolveAppSecret();

        if (StringUtils.isAnyBlank(appId, appSecret)) {
            log.warn("[Feishu] appId 或 appSecret 未配置，跳过启动。请通过后台管理界面配置渠道凭证。");
            return;
        }

        // 初始化发消息的 API Client
        this.apiClient = Client.newBuilder(appId, appSecret).build();

        // 配置长连接事件分发器
        EventDispatcher eventDispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        processFeishuMessage(event);
                    }
                })
                .build();

        // 启动长连接客户端
        this.wsClient = new com.lark.oapi.ws.Client.Builder(appId, appSecret)
                .eventHandler(eventDispatcher)
                .build();
        this.wsClient.start();
        log.info("[Feishu] 长连接已建立，监听飞书消息中... appId={}", appId);
    }

    /**
     * 通过后台管理界面动态注入 ChannelConfig 凭证
     * 将覆盖 application.yml 中的默认凭证
     *
     * @param config 渠道配置（从 ChannelConfigStore 加载）
     */
    @Override
    public void configure(ChannelConfig config) {
        if (config == null || config.getCredentials() == null) {
            return;
        }
        Map<String, Object> cred = config.getCredentials();

        this.runtimeAppId = getString(cred, "appId");
        this.runtimeAppSecret = getString(cred, "appSecret");
        this.runtimeRobotOpenId = getString(cred, "robotOpenId");
        this.runtimeWakeWord = getString(cred, "wakeWord");

        Object whitelistObj = cred.get("whitelist");
        if (whitelistObj instanceof List) {
            //noinspection unchecked
            this.runtimeWhitelist = (List<String>) whitelistObj;
        } else if (whitelistObj instanceof String) {
            String raw = (String) whitelistObj;
            this.runtimeWhitelist = StringUtils.isNotBlank(raw)
                    ? Arrays.asList(raw.split(","))
                    : Collections.emptyList();
        }
        log.info("[Feishu] 动态凭证已更新: appId={}, configId={}", runtimeAppId, config.getId());
    }

    // ==================== 发消息 ====================

    @Override
    public CompletableFuture<Void> sendMessage(ActionMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (message.getActionType() == ActionType.SEND) {
                    CreateMessageReq createMessageReq = feishuParser.parseOutboundCreateMsg(message);
                    CreateMessageResp createMessageResp = apiClient.im().message().create(createMessageReq);
                    if (!createMessageResp.success()) {
                        throw new RuntimeException("飞书发送消息失败: " + createMessageResp.getMsg());
                    }
                } else if (message.getActionType() == ActionType.REPLY) {
                    ReplyMessageReq replyMessageReq = feishuParser.parseOutboundReplyMsg(message);
                    ReplyMessageResp replyMessageResp = apiClient.im().message().reply(replyMessageReq);
                    if (!replyMessageResp.success()) {
                        throw new RuntimeException("飞书发送回复失败: " + replyMessageResp.getMsg());
                    }
                }
                log.info("[Feishu] 消息成功推送给用户");
            } catch (Exception e) {
                log.error("[Feishu] 异步发送异常: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            log.info("[Feishu] 停止长连接");
            // wsClient.stop(); // SDK 提供停止方法时取消注释
        }
    }

    @Override
    public boolean isHealthy() {
        return wsClient != null;
    }

    @Override
    public void restart() throws Exception {
        stop();
        if (gateway != null) {
            startListening(gateway);
        }
    }

    // ==================== 私有：消息处理 ====================

    private void processFeishuMessage(P2MessageReceiveV1 event) {
        try {
            EventMessage message = event.getEvent().getMessage();
            String chatType = message.getChatType();
            String openId = event.getEvent().getSender().getSenderId().getOpenId();
            String content = message.getContent();

            // 1. 私信白名单拦截
            if (StringUtils.equals("p2p", chatType)) {
                List<String> whitelist = resolveWhitelist();
                if (CollectionUtils.isNotEmpty(whitelist) && !whitelist.contains(openId)) {
                    log.warn("[Feishu] 拦截非白名单私信, openId={}", openId);
                    sendRejectReply(event);
                    return;
                }
            }

            // 2. 群聊唤醒词/@ 过滤
            if (StringUtils.equals("group", chatType)) {
                boolean isMentionMe = isMentioned(message);
                String wakeWord = resolveWakeWord();
                boolean hasWakeWord = StringUtils.isNotEmpty(wakeWord) && content.contains(wakeWord);

                if (!isMentionMe && !hasWakeWord) {
                    log.info("[Feishu] 群聊消息未触发唤醒条件，忽略。messageId={}", message.getMessageId());
                    return;
                }
            }

            log.info("[Feishu] 接收原始消息: {}", GsonUtils.toJson(event));
            NormalizedMessage normalizedMessage = feishuParser.parseInbound(event, getChannelName());
            gateway.dispatch(normalizedMessage);

        } catch (Exception e) {
            log.error("[Feishu] 解析消息失败: {}", e.getMessage());
        }
    }

    private void sendRejectReply(P2MessageReceiveV1 event) {
        try {
            String content = String.format("您的openId: %s 不在白名单中，请联系管理员配置",
                    event.getEvent().getSender().getSenderId().getOpenId());
            String textContent = String.format("{\"text\":\"%s\"}", content);
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(event.getEvent().getMessage().getMessageId())
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(textContent)
                            .msgType("text")
                            .build())
                    .build();
            ReplyMessageResp resp = apiClient.im().message().reply(req);
            if (!resp.success()) {
                log.error("[Feishu] 发送拦截提示失败: {}", resp.getMsg());
            }
        } catch (Exception e) {
            log.error("[Feishu] 发送拦截提示异常", e);
        }
    }

    private boolean isMentioned(EventMessage message) {
        MentionEvent[] mentions = message.getMentions();
        if (mentions != null && mentions.length > 0) {
            String robotOpenId = resolveRobotOpenId();
            return Arrays.stream(mentions)
                    .anyMatch(m -> StringUtils.equals(m.getId().getOpenId(), robotOpenId));
        }
        return false;
    }

    // ==================== 私有：凭证解析（优先运行时 > 默认值）====================

    private String resolveAppId() {
        return StringUtils.isNotBlank(runtimeAppId) ? runtimeAppId : defaultAppId;
    }

    private String resolveAppSecret() {
        return StringUtils.isNotBlank(runtimeAppSecret) ? runtimeAppSecret : defaultAppSecret;
    }

    private String resolveRobotOpenId() {
        return StringUtils.isNotBlank(runtimeRobotOpenId) ? runtimeRobotOpenId : defaultRobotOpenId;
    }

    private String resolveWakeWord() {
        return StringUtils.isNotBlank(runtimeWakeWord) ? runtimeWakeWord : defaultWakeWord;
    }

    private List<String> resolveWhitelist() {
        return (runtimeWhitelist != null) ? runtimeWhitelist : defaultWhitelist;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

}
