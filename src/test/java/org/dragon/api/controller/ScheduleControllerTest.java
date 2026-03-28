package org.dragon.api.controller;

import org.dragon.schedule.core.CronService;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;
import org.dragon.schedule.store.ExecutionHistoryStore;
import org.dragon.store.StoreFactory;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ScheduleController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private CronService cronService;

    @Mock
    private StoreFactory storeFactory;

    @Mock
    private ExecutionHistoryStore executionHistoryStore;

    @InjectMocks
    private ScheduleController scheduleController;

    private CronDefinition testCron;
    private ExecutionHistory testHistory;

    @BeforeEach
    void setUp() {
        testCron = CronDefinition.builder()
                .id("cron-1")
                .name("Test Cron")
                .cronExpression("0 0 * * * ?")
                .status(CronStatus.ENABLED)
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis() + 86400000L)
                .build();

        testHistory = ExecutionHistory.builder()
                .id(1L)
                .cronId("cron-1")
                .executionId("exec-1")
                .status(ExecutionStatus.SUCCESS)
                .triggerTime(System.currentTimeMillis())
                .actualFireTime(System.currentTimeMillis())
                .completeTime(System.currentTimeMillis() + 1000)
                .build();
    }

    private void setupExecutionHistoryStoreMock() {
        when(storeFactory.get(ExecutionHistoryStore.class)).thenReturn(executionHistoryStore);
    }

    @Test
    void testCreateCron() {
        when(cronService.createCron(any(CronDefinition.class))).thenReturn("cron-1");

        ResponseEntity<String> response = scheduleController.createCron(testCron);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("cron-1", response.getBody());
    }

    @Test
    void testListCrons() {
        when(cronService.listCrons()).thenReturn(List.of(testCron));

        ResponseEntity<List<CronDefinition>> response = scheduleController.listCrons(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testListCronsByStatus() {
        when(cronService.listCronsByStatus(CronStatus.ENABLED)).thenReturn(List.of(testCron));

        ResponseEntity<List<CronDefinition>> response = scheduleController.listCrons(CronStatus.ENABLED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(cronService).listCronsByStatus(CronStatus.ENABLED);
    }

    @Test
    void testGetCron() {
        when(cronService.getCron("cron-1")).thenReturn(Optional.of(testCron));

        ResponseEntity<CronDefinition> response = scheduleController.getCron("cron-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cron-1", response.getBody().getId());
    }

    @Test
    void testGetCronNotFound() {
        when(cronService.getCron("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<CronDefinition> response = scheduleController.getCron("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdateCron() {
        doNothing().when(cronService).updateCron(any(CronDefinition.class));

        ResponseEntity<Void> response = scheduleController.updateCron("cron-1", testCron);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cronService).updateCron(any(CronDefinition.class));
    }

    @Test
    void testDeleteCron() {
        doNothing().when(cronService).deleteCron("cron-1");

        ResponseEntity<Void> response = scheduleController.deleteCron("cron-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cronService).deleteCron("cron-1");
    }

    @Test
    void testPauseCron() {
        doNothing().when(cronService).pauseCron("cron-1");

        ResponseEntity<Void> response = scheduleController.pauseCron("cron-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cronService).pauseCron("cron-1");
    }

    @Test
    void testResumeCron() {
        doNothing().when(cronService).resumeCron("cron-1");

        ResponseEntity<Void> response = scheduleController.resumeCron("cron-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cronService).resumeCron("cron-1");
    }

    @Test
    void testTriggerNow() {
        doNothing().when(cronService).triggerNow("cron-1");

        ResponseEntity<Void> response = scheduleController.triggerNow("cron-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cronService).triggerNow("cron-1");
    }

    @Test
    void testGetExecutionHistory() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findByCronId("cron-1", 20)).thenReturn(List.of(testHistory));

        ResponseEntity<List<ExecutionHistory>> response = scheduleController.getExecutionHistory("cron-1", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetExecutionHistoryDefaultLimit() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findByCronId("cron-1", 20)).thenReturn(List.of(testHistory));

        ResponseEntity<List<ExecutionHistory>> response = scheduleController.getExecutionHistory("cron-1", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionHistoryStore).findByCronId("cron-1", 20);
    }

    @Test
    void testGetRunningJobs() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findRunningJobs()).thenReturn(List.of(testHistory));

        ResponseEntity<List<ExecutionHistory>> response = scheduleController.getRunningJobs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetExecutionsByStatus() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findByStatus(ExecutionStatus.SUCCESS, 50)).thenReturn(List.of(testHistory));

        ResponseEntity<List<ExecutionHistory>> response = scheduleController.getExecutionsByStatus(ExecutionStatus.SUCCESS, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetExecution() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findByExecutionId("exec-1")).thenReturn(Optional.of(testHistory));

        ResponseEntity<ExecutionHistory> response = scheduleController.getExecution("exec-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("exec-1", response.getBody().getExecutionId());
    }

    @Test
    void testGetExecutionNotFound() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.findByExecutionId("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<ExecutionHistory> response = scheduleController.getExecution("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testCleanupHistory() {
        setupExecutionHistoryStoreMock();
        when(executionHistoryStore.deleteBefore(anyLong())).thenReturn(5);

        ResponseEntity<Integer> response = scheduleController.cleanupHistory(System.currentTimeMillis());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody());
    }
}
