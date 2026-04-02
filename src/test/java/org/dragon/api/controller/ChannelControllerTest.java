//package org.dragon.api.controller;
//
//import org.dragon.channel.ChannelManager;
//import org.dragon.channel.entity.ChannelBinding;
//import org.dragon.channel.entity.ChannelConfig;
//import org.dragon.channel.service.ChannelBindingService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * ChannelController 单元测试
// */
//@ExtendWith(MockitoExtension.class)
//class ChannelControllerTest {
//
//    @Mock
//    private ChannelBindingService channelBindingService;
//
//    @Mock
//    private ChannelManager channelManager;
//
//    @InjectMocks
//    private ChannelController channelController;
//
//    private ChannelConfig testConfig;
//    private ChannelBinding testBinding;
//
//    @BeforeEach
//    void setUp() {
//        testConfig = ChannelConfig.builder()
//                .id("config-1")
//                .channelType("Feishu")
//                .name("Test Config")
//                .enabled(true)
//                .credentials(Map.of("appId", "cli_xxx", "appSecret", "secret_xxx"))
//                .build();
//
//        testBinding = ChannelBinding.builder()
//                .id("binding-1")
//                .channelName("Feishu")
//                .chatId("chat_123")
//                .chatType("group")
//                .workspaceId("workspace-1")
//                .description("Test binding")
//                .enabled(true)
//                .build();
//    }
//
//    @Test
//    void testCreateChannelConfig() {
//        when(channelBindingService.createChannelConfig(any(ChannelConfig.class))).thenReturn(testConfig);
//
//        ResponseEntity<ChannelConfig> response = channelController.createChannelConfig(testConfig);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("config-1", response.getBody().getId());
//        verify(channelBindingService).createChannelConfig(any(ChannelConfig.class));
//    }
//
//    @Test
//    void testListChannelConfigs() {
//        when(channelBindingService.listChannelConfigs(null)).thenReturn(List.of(testConfig));
//
//        ResponseEntity<List<ChannelConfig>> response = channelController.listChannelConfigs(null);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(channelBindingService).listChannelConfigs(null);
//    }
//
//    @Test
//    void testListChannelConfigsWithChannelType() {
//        when(channelBindingService.listChannelConfigs("Feishu")).thenReturn(List.of(testConfig));
//
//        ResponseEntity<List<ChannelConfig>> response = channelController.listChannelConfigs("Feishu");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(channelBindingService).listChannelConfigs("Feishu");
//    }
//
//    @Test
//    void testGetChannelConfig() {
//        when(channelBindingService.getChannelConfig("config-1")).thenReturn(Optional.of(testConfig));
//
//        ResponseEntity<ChannelConfig> response = channelController.getChannelConfig("config-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("config-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetChannelConfigNotFound() {
//        when(channelBindingService.getChannelConfig("nonexistent")).thenReturn(Optional.empty());
//
//        ResponseEntity<ChannelConfig> response = channelController.getChannelConfig("nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testUpdateChannelConfig() {
//        when(channelBindingService.updateChannelConfig(any(ChannelConfig.class))).thenReturn(testConfig);
//
//        ResponseEntity<ChannelConfig> response = channelController.updateChannelConfig("config-1", testConfig);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(channelBindingService).updateChannelConfig(any(ChannelConfig.class));
//    }
//
//    @Test
//    void testDeleteChannelConfig() {
//        doNothing().when(channelBindingService).deleteChannelConfig("config-1");
//
//        ResponseEntity<Void> response = channelController.deleteChannelConfig("config-1");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        verify(channelBindingService).deleteChannelConfig("config-1");
//    }
//
//    @Test
//    void testReloadChannelConfig() {
//        when(channelBindingService.getChannelConfig("config-1")).thenReturn(Optional.of(testConfig));
//        doNothing().when(channelManager).reloadChannelConfig(any(ChannelConfig.class));
//
//        ResponseEntity<Void> response = channelController.reloadChannelConfig("config-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(channelManager).reloadChannelConfig(any(ChannelConfig.class));
//    }
//
//    @Test
//    void testReloadChannelConfigNotFound() {
//        when(channelBindingService.getChannelConfig("nonexistent")).thenReturn(Optional.empty());
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            channelController.reloadChannelConfig("nonexistent");
//        });
//    }
//
//    @Test
//    void testCreateBinding() {
//        when(channelBindingService.createBinding(anyString(), anyString(), anyString(), anyString(), anyString()))
//                .thenReturn(testBinding);
//
//        ChannelController.CreateBindingRequest request = new ChannelController.CreateBindingRequest();
//        request.setChannelName("Feishu");
//        request.setChatId("chat_123");
//        request.setChatType("group");
//        request.setWorkspaceId("workspace-1");
//        request.setDescription("Test binding");
//
//        ResponseEntity<ChannelBinding> response = channelController.createBinding(request);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertEquals("binding-1", response.getBody().getId());
//    }
//
//    @Test
//    void testListBindings() {
//        when(channelBindingService.listAllBindings()).thenReturn(List.of(testBinding));
//
//        ResponseEntity<List<ChannelBinding>> response = channelController.listBindings(null, null);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testListBindingsByWorkspace() {
//        when(channelBindingService.listBindingsByWorkspace("workspace-1")).thenReturn(List.of(testBinding));
//
//        ResponseEntity<List<ChannelBinding>> response = channelController.listBindings("workspace-1", null);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(channelBindingService).listBindingsByWorkspace("workspace-1");
//    }
//
//    @Test
//    void testListBindingsByChannel() {
//        when(channelBindingService.listBindingsByChannel("Feishu")).thenReturn(List.of(testBinding));
//
//        ResponseEntity<List<ChannelBinding>> response = channelController.listBindings(null, "Feishu");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(channelBindingService).listBindingsByChannel("Feishu");
//    }
//
//    @Test
//    void testGetBinding() {
//        when(channelBindingService.getBinding("Feishu", "chat_123")).thenReturn(Optional.of(testBinding));
//
//        ResponseEntity<ChannelBinding> response = channelController.getBinding("Feishu", "chat_123");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("binding-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetBindingNotFound() {
//        when(channelBindingService.getBinding("Feishu", "nonexistent")).thenReturn(Optional.empty());
//
//        ResponseEntity<ChannelBinding> response = channelController.getBinding("Feishu", "nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testUpdateBindingWorkspace() {
//        when(channelBindingService.updateBinding("Feishu", "chat_123", "workspace-2")).thenReturn(testBinding);
//
//        ResponseEntity<ChannelBinding> response = channelController.updateBindingWorkspace(
//                "Feishu", "chat_123", Map.of("workspaceId", "workspace-2"));
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testUpdateBindingWorkspaceEmptyWorkspaceId() {
//        ResponseEntity<ChannelBinding> response = channelController.updateBindingWorkspace(
//                "Feishu", "chat_123", Map.of("workspaceId", ""));
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    @Test
//    void testSetBindingEnabled() {
//        doNothing().when(channelBindingService).setBindingEnabled("Feishu", "chat_123", true);
//
//        ResponseEntity<Void> response = channelController.setBindingEnabled(
//                "Feishu", "chat_123", Map.of("enabled", true));
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(channelBindingService).setBindingEnabled("Feishu", "chat_123", true);
//    }
//
//    @Test
//    void testSetBindingEnabledNull() {
//        Map<String, Boolean> body = new java.util.HashMap<>();
//        body.put("enabled", null);
//        ResponseEntity<Void> response = channelController.setBindingEnabled("Feishu", "chat_123", body);
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    @Test
//    void testDeleteBinding() {
//        doNothing().when(channelBindingService).deleteBinding("Feishu", "chat_123");
//
//        ResponseEntity<Void> response = channelController.deleteBinding("Feishu", "chat_123");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        verify(channelBindingService).deleteBinding("Feishu", "chat_123");
//    }
//
//    @Test
//    void testResolveWorkspace() {
//        when(channelBindingService.resolveWorkspaceId("Feishu", "chat_123"))
//                .thenReturn(Optional.of("workspace-1"));
//
//        ResponseEntity<Map<String, String>> response = channelController.resolveWorkspace("Feishu", "chat_123");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("true", response.getBody().get("routed"));
//        assertEquals("workspace-1", response.getBody().get("workspaceId"));
//    }
//
//    @Test
//    void testResolveWorkspaceNotRouted() {
//        when(channelBindingService.resolveWorkspaceId("Feishu", "nonexistent"))
//                .thenReturn(Optional.empty());
//
//        ResponseEntity<Map<String, String>> response = channelController.resolveWorkspace("Feishu", "nonexistent");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("false", response.getBody().get("routed"));
//    }
//}
