package org.dragon.api.controller;

import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.hiring.WorkspaceHiringService;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MemberController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private WorkspaceMemberManagementService memberManagementService;

    @Mock
    private WorkspaceHiringService workspaceHiringService;

    @InjectMocks
    private MemberController memberController;

    private WorkspaceMember testMember;
    private CharacterDuty testDuty;

    @BeforeEach
    void setUp() {
        testMember = WorkspaceMember.builder()
                .id("member-1")
                .workspaceId("workspace-1")
                .characterId("char-1")
                .role("MEMBER")
                .build();

        testDuty = CharacterDuty.builder()
                .id("workspace-1_char-1")
                .workspaceId("workspace-1")
                .characterId("char-1")
                .dutyDescription("Test duty description")
                .build();
    }

    @Test
    void testListMembers() {
        when(memberManagementService.listMembers("workspace-1")).thenReturn(List.of(testMember));

        ResponseEntity<List<WorkspaceMember>> response = memberController.listMembers("workspace-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(memberManagementService).listMembers("workspace-1");
    }

    @Test
    void testGetMember() {
        when(memberManagementService.getMember("workspace-1", "char-1")).thenReturn(Optional.of(testMember));

        ResponseEntity<WorkspaceMember> response = memberController.getMember("workspace-1", "char-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("member-1", response.getBody().getId());
    }

    @Test
    void testGetMemberNotFound() {
        when(memberManagementService.getMember("workspace-1", "nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<WorkspaceMember> response = memberController.getMember("workspace-1", "nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdateRole() {
        doNothing().when(memberManagementService).updateMemberRole("workspace-1", "char-1", "ADMIN");

        MemberController.RoleRequest request = new MemberController.RoleRequest();
        request.setRole("ADMIN");

        ResponseEntity<Void> response = memberController.updateRole("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(memberManagementService).updateMemberRole("workspace-1", "char-1", "ADMIN");
    }

    @Test
    void testUpdateTags() {
        doNothing().when(memberManagementService).updateMemberTags("workspace-1", "char-1", List.of("tag1", "tag2"));

        ResponseEntity<Void> response = memberController.updateTags("workspace-1", "char-1", List.of("tag1", "tag2"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(memberManagementService).updateMemberTags("workspace-1", "char-1", List.of("tag1", "tag2"));
    }

    @Test
    void testUpdateWeight() {
        doNothing().when(memberManagementService).updateMemberWeight("workspace-1", "char-1", 0.8);

        MemberController.WeightRequest request = new MemberController.WeightRequest();
        request.setWeight(0.8);

        ResponseEntity<Void> response = memberController.updateWeight("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(memberManagementService).updateMemberWeight("workspace-1", "char-1", 0.8);
    }

    @Test
    void testUpdatePriority() {
        doNothing().when(memberManagementService).updateMemberPriority("workspace-1", "char-1", 5);

        MemberController.PriorityRequest request = new MemberController.PriorityRequest();
        request.setPriority(5);

        ResponseEntity<Void> response = memberController.updatePriority("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(memberManagementService).updateMemberPriority("workspace-1", "char-1", 5);
    }

    @Test
    void testUpdateReputation() {
        doNothing().when(memberManagementService).updateMemberReputation("workspace-1", "char-1", 10);

        MemberController.ReputationRequest request = new MemberController.ReputationRequest();
        request.setChange(10);

        ResponseEntity<Void> response = memberController.updateReputation("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(memberManagementService).updateMemberReputation("workspace-1", "char-1", 10);
    }

    @Test
    void testHire() {
        doNothing().when(workspaceHiringService).hire("workspace-1", "char-1", HireMode.AUTO);

        MemberController.HireRequest request = new MemberController.HireRequest();
        request.setCharacterId("char-1");
        request.setMode(HireMode.AUTO);

        ResponseEntity<Void> response = memberController.hire("workspace-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workspaceHiringService).hire("workspace-1", "char-1", HireMode.AUTO);
    }

    @Test
    void testFire() {
        doNothing().when(workspaceHiringService).fire("workspace-1", "char-1", HireMode.AUTO);

        MemberController.FireRequest request = new MemberController.FireRequest();
        request.setMode(HireMode.AUTO);

        ResponseEntity<Void> response = memberController.fire("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workspaceHiringService).fire("workspace-1", "char-1", HireMode.AUTO);
    }

    @Test
    void testRemoveMember() {
        doNothing().when(memberManagementService).removeMember("workspace-1", "char-1");

        ResponseEntity<Void> response = memberController.removeMember("workspace-1", "char-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(memberManagementService).removeMember("workspace-1", "char-1");
    }

    @Test
    void testGetDuty() {
        when(workspaceHiringService.getCharacterDuty("workspace-1", "char-1")).thenReturn(Optional.of(testDuty));

        ResponseEntity<CharacterDuty> response = memberController.getDuty("workspace-1", "char-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test duty description", response.getBody().getDutyDescription());
    }

    @Test
    void testGetDutyNotFound() {
        when(workspaceHiringService.getCharacterDuty("workspace-1", "nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<CharacterDuty> response = memberController.getDuty("workspace-1", "nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testSetDuty() {
        doNothing().when(workspaceHiringService).setCharacterDuty("workspace-1", "char-1", "New duty description");

        MemberController.DutyRequest request = new MemberController.DutyRequest();
        request.setDescription("New duty description");

        ResponseEntity<Void> response = memberController.setDuty("workspace-1", "char-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workspaceHiringService).setCharacterDuty("workspace-1", "char-1", "New duty description");
    }
}
