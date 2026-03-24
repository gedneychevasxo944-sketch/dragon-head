package org.dragon.channel.file;

import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.GetMessageResourceReq;
import com.lark.oapi.service.im.v1.model.GetMessageResourceResp;
import org.dragon.channel.entity.NormalizedFile;
import org.dragon.channel.enums.FileSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/18 0:28
 * Update Date Time:
 */
@Component
public class FeishuImStorageAdapter implements FileStorageAdapter {

    private final Client apiClient;

    public FeishuImStorageAdapter(
            @Value("${channel.feishu.appId:cli_a93e931566785bcc}") String appId,
            @Value("${channel.feishu.appSecret:usZg6YbUgxVSMoL4ihiT5fSWT6zrDNJR}") String appSecret) {
        this.apiClient = Client.newBuilder(appId, appSecret).build();
    }

    @Override
    public FileSource getSupportedSource() {
        return FileSource.FEISHU_IM;
    }

    @Override
    public NormalizedFile download(NormalizedFile fileMeta) throws Exception {
        // 创建请求对象
        GetMessageResourceReq req = GetMessageResourceReq.newBuilder()
                .messageId(fileMeta.getMessageId())
                .fileKey(fileMeta.getFileKey())
                .type(fileMeta.getMimeType())
                .build();

        // 发起请求
        GetMessageResourceResp resp = apiClient.im().v1().messageResource().get(req);

        // 处理服务端错误
        if (!resp.success()) {
            throw new RuntimeException("Feishu file download failed: " + resp.getCode() + " " + resp.getMsg());
        }

        // 标记存储 key（实际内容由调用方通过 resp 的流获取）
        fileMeta.setStorageKey(fileMeta.getFileKey() + ":" + fileMeta.getMessageId());
        return fileMeta;
    }

    @Override
    public NormalizedFile upload(NormalizedFile file) throws Exception {
        throw new UnsupportedOperationException("不支持的保存类型");
    }

}
