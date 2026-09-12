package cn.zhenxinjian.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 游客迁移编排者单元测试（无 Spring 容器，直接构造 List 注入）
 * 作者: wanglx
 */
class GuestMigrationOrchestratorTest {

    /** 记录调用顺序的桩迁移器 */
    private static class StubMigrator implements GuestDataMigrator {
        private final int order;
        private final List<String> calls;

        StubMigrator(int order, List<String> calls) {
            this.order = order;
            this.calls = calls;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void migrate(Long guestId, Long formalId) {
            calls.add("migrate:" + order + ":" + guestId + "->" + formalId);
        }

        @Override
        public void purge(Long guestId) {
            calls.add("purge:" + order + ":" + guestId);
        }
    }

    @Test
    void migrateAllShouldInvokeEveryMigratorWithIds() {
        List<String> calls = new ArrayList<>();
        GuestMigrationOrchestrator orchestrator = new GuestMigrationOrchestrator(List.of(
                new StubMigrator(0, calls), new StubMigrator(1, calls)));

        orchestrator.migrateAll(401L, 400L);

        assertEquals(List.of("migrate:0:401->400", "migrate:1:401->400"), calls);
    }

    @Test
    void purgeAllShouldInvokeEveryMigratorWithGuestId() {
        List<String> calls = new ArrayList<>();
        GuestMigrationOrchestrator orchestrator = new GuestMigrationOrchestrator(List.of(
                new StubMigrator(0, calls), new StubMigrator(1, calls)));

        orchestrator.purgeAll(401L);

        assertEquals(List.of("purge:0:401", "purge:1:401"), calls);
    }

    @Test
    void emptyMigratorListShouldBeNoop() {
        GuestMigrationOrchestrator orchestrator = new GuestMigrationOrchestrator(List.of());
        // 不抛异常即通过（编排者对空族安全）
        orchestrator.migrateAll(1L, 2L);
        orchestrator.purgeAll(1L);
        assertTrue(true);
    }
}
