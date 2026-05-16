package com.app.taskmanagement.workspace.service;

import com.app.taskmanagement.workspace.dto.*;
import com.app.taskmanagement.workspace.entity.*;
import com.app.taskmanagement.workspace.exception.*;
import com.app.taskmanagement.workspace.repository.WorkspaceMemberRepository;
import com.app.taskmanagement.workspace.repository.WorkspaceRepository;
import com.app.taskmanagement.workspace.messaging.NotificationPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceImplTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private Workspace testWorkspace;
    private WorkspaceMember adminMember;
    private WorkspaceMember regularMember;

    @BeforeEach
    void setUp() {
        testWorkspace = Workspace.builder()
                .workspaceId(1).name("Engineering").description("Eng team")
                .visibility(Workspace.Visibility.PRIVATE).createdByUserId(100)
                .members(new ArrayList<>()).build();
        adminMember = WorkspaceMember.builder()
                .id(1).workspace(testWorkspace).userId(100).role(WorkspaceMember.Role.ADMIN).build();
        regularMember = WorkspaceMember.builder()
                .id(2).workspace(testWorkspace).userId(200).role(WorkspaceMember.Role.MEMBER).build();
        testWorkspace.getMembers().add(adminMember);
    }

    @AfterEach
    void tearDown() { testWorkspace = null; adminMember = null; regularMember = null; }

    @Test
    @DisplayName("createWorkspace - creates workspace and adds creator as ADMIN")
    void createWorkspace_Success() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Engineering"); req.setDescription("Eng team");
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);
        when(memberRepository.save(any(WorkspaceMember.class))).thenReturn(adminMember);

        WorkspaceResponse res = workspaceService.createWorkspace(req, 100);

        assertNotNull(res); assertEquals("Engineering", res.getName());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(memberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("getWorkspace - returns public workspace for any user")
    void getWorkspace_Public() {
        testWorkspace.setVisibility(Workspace.Visibility.PUBLIC);
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        WorkspaceResponse res = workspaceService.getWorkspace(1, 999);
        assertNotNull(res);
    }

    @Test
    @DisplayName("getWorkspace - throws ForbiddenException for private workspace non-member")
    void getWorkspace_PrivateForbidden() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.existsByWorkspaceWorkspaceIdAndUserId(1, 999)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> workspaceService.getWorkspace(1, 999));
    }

    @Test
    @DisplayName("getWorkspace - throws ResourceNotFoundException for non-existent workspace")
    void getWorkspace_NotFound() {
        when(workspaceRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> workspaceService.getWorkspace(999, 100));
    }

    @Test
    @DisplayName("updateWorkspace - updates name by admin")
    void updateWorkspace_Success() {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest();
        req.setName("Updated Eng");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(Optional.of(adminMember));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        workspaceService.updateWorkspace(1, req, 100);
        assertEquals("Updated Eng", testWorkspace.getName());
    }

    @Test
    @DisplayName("updateWorkspace - throws ForbiddenException for non-admin")
    void updateWorkspace_ForbiddenNonAdmin() {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest(); req.setName("New Name");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 200)).thenReturn(Optional.of(regularMember));
        assertThrows(ForbiddenException.class, () -> workspaceService.updateWorkspace(1, req, 200));
    }

    @Test
    @DisplayName("deleteWorkspace - deletes by creator")
    void deleteWorkspace_Success() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        workspaceService.deleteWorkspace(1, 100);
        verify(workspaceRepository).delete(testWorkspace);
    }

    @Test
    @DisplayName("deleteWorkspace - throws ForbiddenException for non-creator")
    void deleteWorkspace_ForbiddenNonCreator() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        assertThrows(ForbiddenException.class, () -> workspaceService.deleteWorkspace(1, 200));
        verify(workspaceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("addMember - adds member when admin and no duplicate")
    void addMember_Success() {
        AddMemberRequest req = new AddMemberRequest(); req.setUserId(300);
        WorkspaceMember newMember = WorkspaceMember.builder()
                .id(3).workspace(testWorkspace).userId(300).role(WorkspaceMember.Role.MEMBER).build();
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceWorkspaceIdAndUserId(1, 300)).thenReturn(false);
        when(workspaceRepository.getReferenceById(1)).thenReturn(testWorkspace);
        when(memberRepository.save(any(WorkspaceMember.class))).thenReturn(newMember);

        WorkspaceMemberResponse res = workspaceService.addMember(1, req, 100);
        assertEquals(300, res.getUserId());
        verify(notificationPublisher).notifyWorkspaceMemberAdded(eq(300), anyString(), eq(100), eq(1));
    }

    @Test
    @DisplayName("addMember - throws DuplicateResourceException for existing member")
    void addMember_Duplicate() {
        AddMemberRequest req = new AddMemberRequest(); req.setUserId(200);
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceWorkspaceIdAndUserId(1, 200)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> workspaceService.addMember(1, req, 100));
    }

    @Test
    @DisplayName("removeMember - prevents removing last admin")
    void removeMember_LastAdmin() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findAllByWorkspaceWorkspaceId(1)).thenReturn(List.of(adminMember));
        assertThrows(BadRequestException.class, () -> workspaceService.removeMember(1, 100, 100));
    }

    @Test
    @DisplayName("updateMemberRole - throws BadRequestException for invalid role")
    void updateMemberRole_InvalidRole() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByWorkspaceWorkspaceIdAndUserId(1, 200)).thenReturn(Optional.of(regularMember));
        assertThrows(BadRequestException.class,
                () -> workspaceService.updateMemberRole(1, 200, "INVALID", 100));
    }

    @Test
    @DisplayName("getMembers - returns members for workspace member")
    void getMembers_Success() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.existsByWorkspaceWorkspaceIdAndUserId(1, 100)).thenReturn(true);
        when(memberRepository.findAllByWorkspaceWorkspaceId(1)).thenReturn(List.of(adminMember));
        List<WorkspaceMemberResponse> res = workspaceService.getMembers(1, 100);
        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getMembers - throws ForbiddenException for non-member")
    void getMembers_Forbidden() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.existsByWorkspaceWorkspaceIdAndUserId(1, 999)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> workspaceService.getMembers(1, 999));
    }
}
