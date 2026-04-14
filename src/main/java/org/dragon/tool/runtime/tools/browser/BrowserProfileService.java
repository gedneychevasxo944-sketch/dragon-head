package org.dragon.tool.runtime.tools.browser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.store.ConfigStore;
import org.dragon.tool.runtime.tools.browser.BrowserConfig.BrowserProfileConfig;
import org.dragon.tool.runtime.tools.browser.BrowserConfig.PortRange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for managing browser profiles: CRUD operations and per-profile
 * PlaywrightSession lifecycle.
 * <p>
 * Profile configurations are persisted via {@link ConfigStore} under the
 * namespace {@code browser}. The key structure is:
 * <ul>
 *   <li>{@code browser.defaultProfile} — default profile name</li>
 *   <li>{@code browser.controlPort}    — control server port</li>
 *   <li>{@code browser.profiles}       — JSON map of profile configs</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class BrowserProfileService {

    private static final Pattern HEX_COLOR_RE = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** ConfigStore key namespace for browser settings. */
    private static final String NS = "browser";
    private static final String KEY_DEFAULT_PROFILE = "browser.defaultProfile";
    private static final String KEY_CONTROL_PORT = "browser.controlPort";
    private static final String KEY_PROFILES = "browser.profiles";

    private static final TypeReference<Map<String, BrowserProfileConfig>> PROFILES_TYPE =
            new TypeReference<>() {};

    private final ConfigStore configStore;
    private final Map<String, PlaywrightSession> sessions = new ConcurrentHashMap<>();

    public BrowserProfileService(ConfigStore configStore) {
        this.configStore = configStore;
    }

    // =========================================================================
    // Data classes
    // =========================================================================

    /** Snapshot of a profile's current status. */
    @Data
    public static class ProfileStatus {
        private final String name;
        private final Integer cdpPort;
        private final String cdpUrl;
        private final String color;
        private final boolean running;
        private final boolean isDefault;
    }

    /** Result of a successful profile creation. */
    @Data
    public static class CreateProfileResult {
        private final boolean ok;
        private final String profile;
        private final Integer cdpPort;
        private final String cdpUrl;
        private final String color;
    }

    /** Result of a successful profile deletion. */
    @Data
    public static class DeleteProfileResult {
        private final boolean ok;
        private final String profile;
        private final boolean stopped;
    }

    // =========================================================================
    // Profile CRUD
    // =========================================================================

    /**
     * List all configured profiles with their running status.
     *
     * @return list of {@link ProfileStatus}
     */
    public List<ProfileStatus> listProfiles() {
        String defaultProfile = loadDefaultProfile();
        Map<String, BrowserProfileConfig> profiles = loadProfiles();

        List<ProfileStatus> result = new ArrayList<>();

        // Always include the default profile even if not explicitly configured
        if (!profiles.containsKey(defaultProfile)) {
            PlaywrightSession session = sessions.get(defaultProfile);
            result.add(new ProfileStatus(
                    defaultProfile, null, null,
                    BrowserProfiles.PROFILE_COLORS.get(0),
                    session != null && session.isRunning(),
                    true));
        }

        for (Map.Entry<String, BrowserProfileConfig> entry : profiles.entrySet()) {
            String name = entry.getKey();
            BrowserProfileConfig cfg = entry.getValue();
            PlaywrightSession session = sessions.get(name);
            result.add(new ProfileStatus(
                    name,
                    cfg.getCdpPort(),
                    cfg.getCdpUrl(),
                    cfg.getColor() != null ? cfg.getColor() : BrowserProfiles.PROFILE_COLORS.get(0),
                    session != null && session.isRunning(),
                    name.equals(defaultProfile)));
        }

        return result;
    }

    /**
     * Create a new browser profile and persist to config store.
     *
     * @param nameRaw raw profile name (will be trimmed)
     * @param color   optional hex accent color
     * @param cdpUrl  optional explicit CDP URL; if absent, a port is auto-allocated
     * @return creation result
     */
    public CreateProfileResult createProfile(String nameRaw, String color, String cdpUrl) {
        String name = nameRaw.trim();

        if (!BrowserProfiles.isValidProfileName(name)) {
            throw new IllegalArgumentException(
                    "invalid profile name: use lowercase letters, numbers, and hyphens only");
        }

        Map<String, BrowserProfileConfig> profiles = loadProfiles();

        if (profiles.containsKey(name)) {
            throw new IllegalArgumentException("profile \"" + name + "\" already exists");
        }

        // Allocate color
        Set<String> usedColors = BrowserProfiles.getUsedColors(profiles);
        String profileColor = (color != null && HEX_COLOR_RE.matcher(color).matches())
                ? color
                : BrowserProfiles.allocateColor(usedColors);

        // Build profile config
        BrowserProfileConfig profileConfig = new BrowserProfileConfig();
        profileConfig.setColor(profileColor);

        Integer allocatedPort = null;
        String resolvedCdpUrl = null;

        if (cdpUrl != null && !cdpUrl.isBlank()) {
            profileConfig.setCdpUrl(cdpUrl.trim());
            resolvedCdpUrl = cdpUrl.trim();
        } else {
            Set<Integer> usedPorts = BrowserProfiles.getUsedPorts(profiles);
            int controlPort = loadControlPort();
            int rangeStart = controlPort + 1;
            int rangeEnd = rangeStart + BrowserConstants.DEFAULT_CDP_PORT_RANGE_SIZE - 1;
            PortRange range = new PortRange(rangeStart, rangeEnd);
            Integer cdpPort = BrowserProfiles.allocateCdpPort(usedPorts, range);
            if (cdpPort == null) {
                throw new IllegalStateException("no available CDP ports in range");
            }
            profileConfig.setCdpPort(cdpPort);
            allocatedPort = cdpPort;
        }

        // Persist to config store
        Map<String, BrowserProfileConfig> newProfiles = new LinkedHashMap<>(profiles);
        newProfiles.put(name, profileConfig);
        saveProfiles(newProfiles);

        log.info("[BrowserProfileService] Created browser profile: {} (port={}, cdpUrl={}, color={})",
                name, allocatedPort, resolvedCdpUrl, profileColor);

        return new CreateProfileResult(true, name, allocatedPort, resolvedCdpUrl, profileColor);
    }

    /**
     * Delete a browser profile. Stops its browser session if running.
     *
     * @param nameRaw raw profile name
     * @return deletion result
     */
    public DeleteProfileResult deleteProfile(String nameRaw) {
        String name = nameRaw.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("profile name is required");
        }
        if (!BrowserProfiles.isValidProfileName(name)) {
            throw new IllegalArgumentException("invalid profile name");
        }

        Map<String, BrowserProfileConfig> profiles = loadProfiles();
        if (!profiles.containsKey(name)) {
            throw new IllegalArgumentException("profile \"" + name + "\" not found");
        }

        String defaultProfile = loadDefaultProfile();
        if (name.equals(defaultProfile)) {
            throw new IllegalArgumentException(
                    "cannot delete the default profile \"" + name
                            + "\"; change browser.defaultProfile first");
        }

        // Stop running session if any
        boolean stopped = false;
        PlaywrightSession session = sessions.remove(name);
        if (session != null) {
            try {
                session.stop();
                stopped = true;
            } catch (Exception e) {
                log.debug("[BrowserProfileService] Error stopping session for profile {}: {}",
                        name, e.getMessage());
            }
        }

        // Remove from config store
        Map<String, BrowserProfileConfig> newProfiles = new LinkedHashMap<>(profiles);
        newProfiles.remove(name);
        saveProfiles(newProfiles);

        log.info("[BrowserProfileService] Deleted browser profile: {} (stopped={})", name, stopped);

        return new DeleteProfileResult(true, name, stopped);
    }

    // =========================================================================
    // Session management
    // =========================================================================

    /**
     * Get or create a {@link PlaywrightSession} for the given profile.
     * If {@code profileName} is null, uses the default profile.
     *
     * @param profileName profile name, or null for default
     * @return existing or newly created session
     */
    public PlaywrightSession getOrCreateSession(String profileName) {
        String name = resolveProfileName(profileName);
        return sessions.computeIfAbsent(name, k -> new PlaywrightSession());
    }

    /**
     * Stop a profile's browser session.
     *
     * @param profileName profile name, or null for default
     * @return true if a session was running and has been stopped
     */
    public boolean stopSession(String profileName) {
        String name = resolveProfileName(profileName);
        PlaywrightSession session = sessions.get(name);
        if (session != null) {
            return session.stop();
        }
        return false;
    }

    /**
     * Check if a profile has a running browser session.
     *
     * @param profileName profile name, or null for default
     * @return true if the session is running
     */
    public boolean isRunning(String profileName) {
        String name = resolveProfileName(profileName);
        PlaywrightSession session = sessions.get(name);
        return session != null && session.isRunning();
    }

    /**
     * Resolve the effective profile name (null → default profile).
     *
     * @param profileName raw profile name, may be null
     * @return resolved (non-null) profile name
     */
    public String resolveProfileName(String profileName) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName.trim();
        }
        return loadDefaultProfile();
    }

    /**
     * Stop all running browser sessions. Called on shutdown.
     */
    public void stopAll() {
        for (Map.Entry<String, PlaywrightSession> entry : sessions.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                log.debug("[BrowserProfileService] Error closing session {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        sessions.clear();
    }

    // =========================================================================
    // ConfigStore helpers
    // =========================================================================

    /**
     * Load the default profile name from config store.
     *
     * @return default profile name
     */
    private String loadDefaultProfile() {
        return (String) configStore.get(ConfigLevel.GLOBAL, null, KEY_DEFAULT_PROFILE)
                .orElse(BrowserProfiles.DEFAULT_PROFILE_NAME);
    }

    /**
     * Load the control port from config store.
     *
     * @return control port
     */
    private int loadControlPort() {
        Object raw = configStore.get(ConfigLevel.GLOBAL, null, KEY_CONTROL_PORT).orElse(null);
        if (raw instanceof Number n) {
            return n.intValue();
        }
        return BrowserConstants.DEFAULT_CONTROL_PORT;
    }

    /**
     * Load all profiles from config store, deserializing the JSON map.
     *
     * @return mutable map of profile configs
     */
    private Map<String, BrowserProfileConfig> loadProfiles() {
        Object raw = configStore.get(ConfigLevel.GLOBAL, null, KEY_PROFILES).orElse(null);
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        try {
            String json = raw instanceof String s ? s : MAPPER.writeValueAsString(raw);
            Map<String, BrowserProfileConfig> result = MAPPER.readValue(json, PROFILES_TYPE);
            return result != null ? new LinkedHashMap<>(result) : new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("[BrowserProfileService] Failed to deserialize profiles config: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Persist profiles map to config store as a JSON string.
     *
     * @param profiles profiles map to save
     */
    private void saveProfiles(Map<String, BrowserProfileConfig> profiles) {
        try {
            String json = MAPPER.writeValueAsString(profiles);
            configStore.set(ConfigLevel.GLOBAL, null, null, null, null, null, KEY_PROFILES, json);
        } catch (Exception e) {
            throw new IllegalStateException("[BrowserProfileService] Failed to persist profiles", e);
        }
    }
}
