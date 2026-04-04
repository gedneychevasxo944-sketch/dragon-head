//package org.dragon.api.controller;
//
//import org.dragon.observer.Observer;
//import org.dragon.observer.ObserverRegistry;
//import org.dragon.observer.ObserverService;
//import org.dragon.observer.evaluation.EvaluationEngine;
//import org.dragon.observer.evaluation.EvaluationRecord;
//import org.dragon.observer.optimization.plan.ObserverPlanningService;
//import org.dragon.observer.optimization.plan.OptimizationAction;
//import org.dragon.observer.optimization.plan.OptimizationPlan;
//import org.dragon.observer.optimization.plan.OptimizationPlanItem;
//import org.dragon.observer.optimization.plan.OptimizationPlanParser;
//import org.dragon.observer.collector.DataCollector;
//import org.dragon.workspace.commons.CommonSense;
//import org.dragon.workspace.commons.CommonSenseValidator;
//import org.dragon.workspace.commons.store.WorkspaceCommonSenseStore;
//import org.dragon.store.StoreFactory;
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
// * ObserverController 单元测试
// */
//@ExtendWith(MockitoExtension.class)
//class ObserverControllerTest {
//
//    @Mock
//    private ObserverRegistry observerRegistry;
//
//    @Mock
//    private ObserverService observerService;
//
//    @Mock
//    private ObserverPlanningService planningService;
//
//    @Mock
//    private OptimizationPlanParser planParser;
//
//    @Mock
//    private DataCollector dataCollector;
//
//    @Mock
//    private StoreFactory storeFactory;
//
//    @InjectMocks
//    private ObserverController observerController;
//
//    private Observer testObserver;
//    private EvaluationRecord testRecord;
//    private OptimizationAction testAction;
//    private OptimizationPlan testPlan;
//    private CommonSense testCommonSense;
//
//    @BeforeEach
//    void setUp() {
//        testObserver = Observer.builder()
//                .id("observer-1")
//                .name("Test Observer")
//                .workspaceId("workspace-1")
//                .status(Observer.Status.ACTIVE)
//                .build();
//
//        testRecord = EvaluationRecord.builder()
//                .id("record-1")
//                .observerId("observer-1")
//                .targetType(EvaluationRecord.TargetType.CHARACTER)
//                .targetId("char-1")
//                .overallScore(0.85)
//                .build();
//
//        testAction = OptimizationAction.builder()
//                .id("action-1")
//                .targetType(OptimizationAction.TargetType.CHARACTER)
//                .targetId("char-1")
//                .actionType(OptimizationAction.ActionType.UPDATE_MIND)
//                .status(OptimizationAction.Status.PENDING)
//                .build();
//
//        testPlan = OptimizationPlan.builder()
//                .id("plan-1")
//                .observerId("observer-1")
//                .evaluationId("record-1")
//                .status(OptimizationPlan.Status.DRAFT)
//                .build();
//
//        testCommonSense = CommonSense.builder()
//                .id("cs-1")
//                .workspaceId("workspace-1")
//                .category(CommonSense.Category.PERFORMANCE)
//                .severity(CommonSense.Severity.HIGH)
//                .enabled(true)
//                .content("Test rule")
//                .build();
//    }
//
//    private WorkspaceCommonSenseStore createMockCommonSenseStore() {
//        WorkspaceCommonSenseStore store = mock(WorkspaceCommonSenseStore.class);
//        when(storeFactory.get(WorkspaceCommonSenseStore.class)).thenReturn(store);
//        return store;
//    }
//
//    @Test
//    void testRegisterObserver() {
//        doNothing().when(observerRegistry).register(any(Observer.class));
//
//        ResponseEntity<Observer> response = observerController.registerObserver(testObserver);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertEquals("observer-1", response.getBody().getId());
//    }
//
//    @Test
//    void testListObservers() {
//        when(observerRegistry.listAll()).thenReturn(List.of(testObserver));
//
//        ResponseEntity<List<Observer>> response = observerController.listObservers(false);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testListActiveObservers() {
//        when(observerRegistry.listActive()).thenReturn(List.of(testObserver));
//
//        ResponseEntity<List<Observer>> response = observerController.listObservers(true);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(observerRegistry).listActive();
//    }
//
//    @Test
//    void testGetObserver() {
//        when(observerRegistry.get("observer-1")).thenReturn(Optional.of(testObserver));
//
//        ResponseEntity<Observer> response = observerController.getObserver("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("observer-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetObserverNotFound() {
//        when(observerRegistry.get("nonexistent")).thenReturn(Optional.empty());
//
//        ResponseEntity<Observer> response = observerController.getObserver("nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetObserverByWorkspace() {
//        when(observerRegistry.getByWorkspace("workspace-1")).thenReturn(Optional.of(testObserver));
//
//        ResponseEntity<Observer> response = observerController.getObserverByWorkspace("workspace-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("observer-1", response.getBody().getId());
//    }
//
//    @Test
//    void testUpdateObserver() {
//        doNothing().when(observerRegistry).update(any(Observer.class));
//
//        Observer updateData = Observer.builder().name("Updated").build();
//        ResponseEntity<Observer> response = observerController.updateObserver("observer-1", updateData);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(observerRegistry).update(any(Observer.class));
//    }
//
//    @Test
//    void testUnregisterObserver() {
//        doNothing().when(observerRegistry).unregister("observer-1");
//
//        ResponseEntity<Void> response = observerController.unregisterObserver("observer-1");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        verify(observerRegistry).unregister("observer-1");
//    }
//
//    @Test
//    void testActivateObserver() {
//        doNothing().when(observerRegistry).activate("observer-1");
//
//        ResponseEntity<Void> response = observerController.activateObserver("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(observerRegistry).activate("observer-1");
//    }
//
//    @Test
//    void testPauseObserver() {
//        doNothing().when(observerRegistry).pause("observer-1");
//
//        ResponseEntity<Void> response = observerController.pauseObserver("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(observerRegistry).pause("observer-1");
//    }
//
//    @Test
//    void testSetDefaultObserver() {
//        doNothing().when(observerRegistry).setDefaultObserver("observer-1");
//
//        ResponseEntity<Void> response = observerController.setDefaultObserver("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(observerRegistry).setDefaultObserver("observer-1");
//    }
//
//    @Test
//    void testGetStats() {
//        ObserverService.ObserverStats stats = new ObserverService.ObserverStats();
//        stats.setTotalEvaluations(10);
//        when(observerService.getStats("observer-1")).thenReturn(stats);
//
//        ResponseEntity<ObserverService.ObserverStats> response = observerController.getStats("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(10, response.getBody().getTotalEvaluations());
//    }
//
//    @Test
//    void testGetEvaluations() {
//        when(observerService.getEvaluationsByTarget(EvaluationRecord.TargetType.CHARACTER, "char-1"))
//                .thenReturn(List.of(testRecord));
//
//        ResponseEntity<List<EvaluationRecord>> response = observerController.getEvaluations(
//                "observer-1", EvaluationRecord.TargetType.CHARACTER, "char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testGetBelowThresholdEvaluations() {
//        when(observerService.getBelowThresholdEvaluations("observer-1")).thenReturn(List.of(testRecord));
//
//        ResponseEntity<List<EvaluationRecord>> response = observerController.getBelowThresholdEvaluations("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testGetEvaluation() {
//        when(observerService.getEvaluation("record-1")).thenReturn(testRecord);
//
//        ResponseEntity<EvaluationRecord> response = observerController.getEvaluation("observer-1", "record-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("record-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetEvaluationNotFound() {
//        when(observerService.getEvaluation("nonexistent")).thenReturn(null);
//
//        ResponseEntity<EvaluationRecord> response = observerController.getEvaluation("observer-1", "nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testTriggerEvaluation() {
//        when(observerService.evaluateTask(eq("observer-1"), any(EvaluationEngine.TaskData.class)))
//                .thenReturn(testRecord);
//
//        EvaluationEngine.TaskData taskData = new EvaluationEngine.TaskData();
//        ResponseEntity<EvaluationRecord> response = observerController.triggerEvaluation("observer-1", taskData);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testTriggerEvaluationServiceUnavailable() {
//        when(observerService.evaluateTask(eq("observer-1"), any(EvaluationEngine.TaskData.class)))
//                .thenReturn(null);
//
//        EvaluationEngine.TaskData taskData = new EvaluationEngine.TaskData();
//        ResponseEntity<EvaluationRecord> response = observerController.triggerEvaluation("observer-1", taskData);
//
//        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
//    }
//
//    @Test
//    void testTriggerPeriodicEvaluation() {
//        when(observerService.evaluatePeriodically(eq("observer-1"), any(), anyString(), anyInt()))
//                .thenReturn(testRecord);
//
//        ObserverController.PeriodicEvaluationRequest request = new ObserverController.PeriodicEvaluationRequest();
//        request.setTargetType(EvaluationRecord.TargetType.CHARACTER);
//        request.setTargetId("char-1");
//        request.setPeriodHours(24);
//
//        ResponseEntity<EvaluationRecord> response = observerController.triggerPeriodicEvaluation("observer-1", request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testGetOptimizationHistory() {
//        when(observerService.getOptimizationHistory(EvaluationRecord.TargetType.CHARACTER, "char-1"))
//                .thenReturn(List.of(testAction));
//
//        ResponseEntity<List<OptimizationAction>> response = observerController.getOptimizationHistory(
//                "observer-1", EvaluationRecord.TargetType.CHARACTER, "char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testGetOptimizationAction() {
//        when(observerService.getOptimizationAction("action-1")).thenReturn(testAction);
//
//        ResponseEntity<OptimizationAction> response = observerController.getOptimizationAction("observer-1", "action-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("action-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetOptimizationActionNotFound() {
//        when(observerService.getOptimizationAction("nonexistent")).thenReturn(null);
//
//        ResponseEntity<OptimizationAction> response = observerController.getOptimizationAction("observer-1", "nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testExecutePendingOptimizations() {
//        when(observerService.executePendingOptimizations("observer-1")).thenReturn(List.of(testAction));
//
//        ResponseEntity<List<OptimizationAction>> response = observerController.executePendingOptimizations("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testRollbackOptimization() {
//        when(observerService.rollbackOptimization("action-1")).thenReturn(testAction);
//
//        ResponseEntity<OptimizationAction> response = observerController.rollbackOptimization("observer-1", "action-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testCreateCommonSense() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        when(store.save(any(CommonSense.class))).thenReturn(testCommonSense);
//
//        ResponseEntity<CommonSense> response = observerController.createCommonSense(testCommonSense);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertEquals("cs-1", response.getBody().getId());
//    }
//
//    @Test
//    void testListCommonSenses() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        when(store.findByWorkspace("workspace-1")).thenReturn(List.of(testCommonSense));
//
//        ResponseEntity<List<CommonSense>> response = observerController.listCommonSenses(
//                "workspace-1", null, null, false);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testGetCommonSense() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        when(store.findById("cs-1")).thenReturn(Optional.of(testCommonSense));
//
//        ResponseEntity<CommonSense> response = observerController.getCommonSense("cs-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("cs-1", response.getBody().getId());
//    }
//
//    @Test
//    void testUpdateCommonSense() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        when(store.findById("cs-1")).thenReturn(Optional.of(testCommonSense));
//        when(store.save(any(CommonSense.class))).thenReturn(testCommonSense);
//
//        ResponseEntity<CommonSense> response = observerController.updateCommonSense("cs-1", testCommonSense);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testUpdateCriticalCommonSenseDisabled() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        CommonSense critical = CommonSense.builder()
//                .id("cs-1")
//                .severity(CommonSense.Severity.CRITICAL)
//                .enabled(true)
//                .build();
//        testCommonSense.setEnabled(false);
//        when(store.findById("cs-1")).thenReturn(Optional.of(critical));
//
//        ResponseEntity<CommonSense> response = observerController.updateCommonSense("cs-1", testCommonSense);
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void testDeleteCommonSense() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        when(store.findById("cs-1")).thenReturn(Optional.of(testCommonSense));
//        when(store.delete("cs-1")).thenReturn(true);
//
//        ResponseEntity<Void> response = observerController.deleteCommonSense("cs-1");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//    }
//
//    @Test
//    void testDeleteCriticalCommonSenseForbidden() {
//        WorkspaceCommonSenseStore store = createMockCommonSenseStore();
//        testCommonSense.setSeverity(CommonSense.Severity.CRITICAL);
//        when(store.findById("cs-1")).thenReturn(Optional.of(testCommonSense));
//
//        ResponseEntity<Void> response = observerController.deleteCommonSense("cs-1");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void testValidateAction() {
//        CommonSenseValidator.ValidationResult result = CommonSenseValidator.ValidationResult.success();
//        when(observerService.validateAgainstCommonSense(any(), any(), any())).thenReturn(result);
//
//        ObserverController.ValidateActionRequest request = new ObserverController.ValidateActionRequest();
//        request.setActionTargetType("CHARACTER");
//        request.setActionType("UPDATE_MIND");
//        request.setParameters(Map.of());
//
//        ResponseEntity<CommonSenseValidator.ValidationResult> response = observerController.validateAction(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testGeneratePlan() {
//        when(observerService.getEvaluation("record-1")).thenReturn(testRecord);
//        when(planningService.generatePlan("record-1")).thenReturn(testPlan);
//
//        ObserverController.GeneratePlanRequest request = new ObserverController.GeneratePlanRequest();
//        request.setEvaluationId("record-1");
//
//        ResponseEntity<OptimizationPlan> response = observerController.generatePlan("observer-1", request);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//    }
//
//    @Test
//    void testGeneratePlanWithText() {
//        when(observerService.getEvaluation("record-1")).thenReturn(testRecord);
//        when(planParser.parse(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(testPlan);
//
//        ObserverController.GeneratePlanRequest request = new ObserverController.GeneratePlanRequest();
//        request.setEvaluationId("record-1");
//        request.setPlanText("Some plan text");
//        request.setTargetType("CHARACTER");
//        request.setTargetId("char-1");
//
//        ResponseEntity<OptimizationPlan> response = observerController.generatePlan("observer-1", request);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        verify(planParser).parse(anyString(), eq("observer-1"), eq("record-1"), any(), eq("char-1"));
//    }
//
//    @Test
//    void testGeneratePlanEvaluationNotFound() {
//        when(observerService.getEvaluation("nonexistent")).thenReturn(null);
//
//        ObserverController.GeneratePlanRequest request = new ObserverController.GeneratePlanRequest();
//        request.setEvaluationId("nonexistent");
//
//        ResponseEntity<OptimizationPlan> response = observerController.generatePlan("observer-1", request);
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetPlan() {
//        when(planningService.getPlan("plan-1")).thenReturn(testPlan);
//
//        ResponseEntity<OptimizationPlan> response = observerController.getPlan("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("plan-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetPlanNotFound() {
//        when(planningService.getPlan("nonexistent")).thenReturn(null);
//
//        ResponseEntity<OptimizationPlan> response = observerController.getPlan("observer-1", "nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetPlanItems() {
//        OptimizationPlanItem item = OptimizationPlanItem.builder()
//                .id("item-1")
//                .planId("plan-1")
//                .actionType(OptimizationAction.ActionType.UPDATE_MIND)
//                .build();
//        when(planningService.getPlanItems("plan-1")).thenReturn(List.of(item));
//
//        ResponseEntity<List<OptimizationPlanItem>> response = observerController.getPlanItems("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testGetPendingApprovalPlans() {
//        when(planningService.getPendingApprovalPlans()).thenReturn(List.of(testPlan));
//
//        ResponseEntity<List<OptimizationPlan>> response = observerController.getPendingApprovalPlans("observer-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//
//    @Test
//    void testReviewPlan() {
//        ObserverPlanningService.ReviewResult result = new ObserverPlanningService.ReviewResult();
//        result.setApproved(true);
//        when(planningService.reviewPlan("plan-1", "reviewer-1")).thenReturn(result);
//
//        ObserverController.ReviewPlanRequest request = new ObserverController.ReviewPlanRequest();
//        request.setReviewer("reviewer-1");
//
//        ResponseEntity<ObserverPlanningService.ReviewResult> response = observerController.reviewPlan("observer-1", "plan-1", request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testApprovePlan() {
//        when(planningService.approvePlan("plan-1", "approver-1", "Approved")).thenReturn(testPlan);
//
//        ObserverController.ApprovePlanRequest request = new ObserverController.ApprovePlanRequest();
//        request.setApprover("approver-1");
//        request.setComment("Approved");
//
//        ResponseEntity<OptimizationPlan> response = observerController.approvePlan("observer-1", "plan-1", request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testApprovePlanBadRequest() {
//        when(planningService.approvePlan("plan-1", "approver-1", "Approved"))
//                .thenThrow(new IllegalStateException("Invalid state"));
//
//        ObserverController.ApprovePlanRequest request = new ObserverController.ApprovePlanRequest();
//        request.setApprover("approver-1");
//        request.setComment("Approved");
//
//        ResponseEntity<OptimizationPlan> response = observerController.approvePlan("observer-1", "plan-1", request);
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    @Test
//    void testRejectPlan() {
//        when(planningService.rejectPlan("plan-1", "Not approved")).thenReturn(testPlan);
//
//        ObserverController.RejectPlanRequest request = new ObserverController.RejectPlanRequest();
//        request.setReason("Not approved");
//
//        ResponseEntity<OptimizationPlan> response = observerController.rejectPlan("observer-1", "plan-1", request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testExecutePlan() {
//        when(planningService.executePlan("plan-1")).thenReturn(testPlan);
//
//        ResponseEntity<OptimizationPlan> response = observerController.executePlan("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testExecutePlanBadRequest() {
//        when(planningService.executePlan("plan-1")).thenThrow(new IllegalStateException("Invalid state"));
//
//        ResponseEntity<OptimizationPlan> response = observerController.executePlan("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    @Test
//    void testRollbackPlan() {
//        when(planningService.rollbackPlan("plan-1")).thenReturn(testPlan);
//
//        ResponseEntity<OptimizationPlan> response = observerController.rollbackPlan("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void testGetPlanSummary() {
//        when(planningService.buildPlanSummary("plan-1")).thenReturn("Plan summary text");
//
//        ResponseEntity<Map<String, Object>> response = observerController.getPlanSummary("observer-1", "plan-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Plan summary text", response.getBody().get("summary"));
//    }
//
//    @Test
//    void testGetPlansByTarget() {
//        when(planningService.getPlansByTarget(OptimizationAction.TargetType.CHARACTER, "char-1"))
//                .thenReturn(List.of(testPlan));
//
//        ResponseEntity<List<OptimizationPlan>> response = observerController.getPlansByTarget(
//                "observer-1", OptimizationAction.TargetType.CHARACTER, "char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//    }
//}
