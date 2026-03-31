package org.dragon.skill.filter;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.SkillMetadata;
import org.dragon.skill.model.SkillRequires;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Skill 环境过滤器。
 * 根据 Skill 的 metadata.requires 条件检查当前运行环境是否满足，
 * 不满足的 Skill 会被过滤掉，不进入 ReAct Loop。
 *
 * <p>检查顺序：
 * <ol>
 *   <li>always 标记 - 直接放行</li>
 *   <li>os 支持检查 - 当前操作系统是否在列表中</li>
 *   <li>bins 检查 - 所有必须的可执行文件是否存在</li>
 *   <li>anyBins 检查 - 至少一个可执行文件是否存在</li>
 *   <li>env 检查 - 所有必须的环境变量是否已设置</li>
 * </ol>
 *
 * @since 1.0
 */
@Slf4j
@Component
public class SkillFilter {

    /**
     * 判断某个 Skill 是否应该在当前环境下加载。
     *
     * @param entry Skill 运行时条目
     * @return true 表示应加载，false 表示跳过
     */
    public boolean shouldInclude(SkillRuntimeEntry entry) {
        SkillMetadata metadata = entry.getSkillEntry().getMetadata();
        if (metadata == null) {
            return true;
        }

        String skillName = entry.getSkillEntry().getSkill().getName();

        // 1. always 标记直接放行
        if (Boolean.TRUE.equals(metadata.getAlways())) {
            return true;
        }

        // 2. 操作系统检查
        if (!checkOs(metadata, skillName)) {
            return false;
        }

        // 3. 依赖检查（bins/anyBins/env）
        SkillRequires requires = metadata.getRequires();
        if (requires != null) {
            // bins 全量检查（AND）
            if (!checkBins(requires, skillName)) {
                return false;
            }
            // anyBins 任一检查（OR）
            if (!checkAnyBins(requires, skillName)) {
                return false;
            }
            // env 全量检查
            if (!checkEnv(requires, skillName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查操作系统是否支持。
     */
    private boolean checkOs(SkillMetadata metadata, String skillName) {
        List<String> supportedOs = metadata.getOs();
        if (supportedOs == null || supportedOs.isEmpty()) {
            return true;
        }

        String currentOs = detectOs();
        if (!supportedOs.contains(currentOs)) {
            log.info("[SkillFilter] Skip skill {}: os {} not supported, required: {}",
                    skillName, currentOs, supportedOs);
            return false;
        }
        return true;
    }

    /**
     * 检查 bins 全量要求（全部必须存在）。
     */
    private boolean checkBins(SkillRequires requires, String skillName) {
        List<String> bins = requires.getBins();
        if (bins == null || bins.isEmpty()) {
            return true;
        }

        for (String bin : bins) {
            if (!commandExists(bin)) {
                log.info("[SkillFilter] Skip skill {}: missing bin {}",
                        skillName, bin);
                return false;
            }
        }
        return true;
    }

    /**
     * 检查 anyBins 任一要求（至少一个存在）。
     */
    private boolean checkAnyBins(SkillRequires requires, String skillName) {
        List<String> anyBins = requires.getAnyBins();
        if (anyBins == null || anyBins.isEmpty()) {
            return true;
        }

        boolean anyExists = anyBins.stream().anyMatch(this::commandExists);
        if (!anyExists) {
            log.info("[SkillFilter] Skip skill {}: none of anyBins exist: {}",
                    skillName, anyBins);
            return false;
        }
        return true;
    }

    /**
     * 检查 env 全量要求（全部必须已设置）。
     */
    private boolean checkEnv(SkillRequires requires, String skillName) {
        List<String> envVars = requires.getEnv();
        if (envVars == null || envVars.isEmpty()) {
            return true;
        }

        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value == null || value.isBlank()) {
                log.warn("[SkillFilter] Skip skill {}: env {} not set",
                        skillName, envVar);
                return false;
            }
        }
        return true;
    }

    /**
     * 检测当前操作系统。
     *
     * @return darwin / linux / windows / unknown
     */
    private String detectOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return "darwin";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        if (os.contains("win")) {
            return "windows";
        }
        return "unknown";
    }

    /**
     * 检查命令是否存在。
     */
    private boolean commandExists(String bin) {
        // 方式1：通过 which 命令检查
        try {
            ProcessBuilder pb = new ProcessBuilder("which", bin);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // 方式2：检查常见路径
        String[] commonPaths = {
                "/usr/bin/" + bin,
                "/usr/local/bin/" + bin,
                "/bin/" + bin,
                "/opt/homebrew/bin/" + bin
        };
        for (String path : commonPaths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        return false;
    }
}