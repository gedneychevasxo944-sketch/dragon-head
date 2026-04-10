package org.dragon.workspace.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plugin 执行结果
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginResult {

    private boolean success;
    private String output;
    private String error;
    private Object data;

    public static PluginResult success(String output) {
        return PluginResult.builder()
                .success(true)
                .output(output)
                .build();
    }

    public static PluginResult success(String output, Object data) {
        return PluginResult.builder()
                .success(true)
                .output(output)
                .data(data)
                .build();
    }

    public static PluginResult failure(String error) {
        return PluginResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}