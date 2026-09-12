package cn.zhenxinjian.task;

import cn.zhenxinjian.common.utils.RedisUtils;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.GuestMigrationOrchestrator;
import cn.zhenxinjian.service.SessionEvictor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 过期游客清理任务单元测试（无 Spring 容器，Mockito 桩依赖）
 * 覆盖：过期游客命中 / 未过期跳过 / 批次边界（满批继续、不满批终止）
 * 作者: wanglx
 */
class GuestCleanupTaskTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final SessionEvictor sessionEvictor = mock(SessionEvictor.class);
    private final GuestMigrationOrchestrator orchestrator = mock(GuestMigrationOrchestrator.class);
    private final GuestCleanupBatchExecutor executor =
            new GuestCleanupBatchExecutor(userMapper, redisUtils, sessionEvictor, orchestrator);

    private static User guest(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    @Test
    void emptyBatchShouldReturnZeroWithoutWrites() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        int count = executor.purgeOneBatch(LocalDateTime.now(), 200);

        assertEquals(0, count);
        verify(userMapper, never()).deleteById(anyLong());
        verifyNoInteractions(redisUtils, sessionEvictor, orchestrator);
    }

    @Test
    void batchShouldPurgeEveryGuestWithFullCleanupChain() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(guest(1L), guest(2L)));

        int count = executor.purgeOneBatch(LocalDateTime.now(), 200);

        assertEquals(2, count);
        verify(orchestrator).purgeAll(1L);
        verify(orchestrator).purgeAll(2L);
        verify(userMapper).deleteById(1L);
        verify(userMapper).deleteById(2L);
        verify(redisUtils).removeToken(1L);
        verify(redisUtils).removeToken(2L);
        verify(sessionEvictor).evict(1L);
        verify(sessionEvictor).evict(2L);
    }

    @Test
    void noExpiredGuestShouldStopAfterFirstEmptyBatch() {
        GuestCleanupBatchExecutor mockExecutor = mock(GuestCleanupBatchExecutor.class);
        when(mockExecutor.purgeOneBatch(any(LocalDateTime.class), anyInt())).thenReturn(0);
        GuestCleanupTask task = new GuestCleanupTask(mockExecutor);

        task.purgeExpiredGuests();

        verify(mockExecutor, times(1)).purgeOneBatch(any(LocalDateTime.class), eq(200));
    }

    @Test
    void fullBatchShouldContinueAndPartialBatchShouldTerminate() {
        GuestCleanupBatchExecutor mockExecutor = mock(GuestCleanupBatchExecutor.class);
        when(mockExecutor.purgeOneBatch(any(LocalDateTime.class), anyInt()))
                .thenReturn(200, 200, 37);
        GuestCleanupTask task = new GuestCleanupTask(mockExecutor);

        task.purgeExpiredGuests();

        // 两个满批继续 + 一个不满批终止，共 3 批
        verify(mockExecutor, times(3)).purgeOneBatch(any(LocalDateTime.class), eq(200));
    }

    @Test
    void exactFullLastBatchShouldProbeOnceMoreThenStop() {
        GuestCleanupBatchExecutor mockExecutor = mock(GuestCleanupBatchExecutor.class);
        when(mockExecutor.purgeOneBatch(any(LocalDateTime.class), anyInt()))
                .thenReturn(200, 0);
        GuestCleanupTask task = new GuestCleanupTask(mockExecutor);

        task.purgeExpiredGuests();

        // 恰好好满批收尾时需再探一次空批确认无残留
        verify(mockExecutor, times(2)).purgeOneBatch(any(LocalDateTime.class), eq(200));
    }
}
