package cn.zhenxinjian.it.repository;

import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserMapper 集成测试 — 验证 H2 + MyBatis-Plus 基础 CRUD
 * 作者: wanglx
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@org.springframework.context.annotation.Import(TestConfig.class)
class UserMapperITest {

    @Autowired
    private UserMapper userMapper;

    /** 场景：insert → selectById → 返回实体 */
    @Test
    void insertAndSelectById_returnsUser() {
        User user = new User();
        user.setUsername("ituser");
        user.setPassword("encrypted");
        user.setNickname("测试");
        user.setRole("USER");
        user.setStatus(1);

        int rows = userMapper.insert(user);
        assertEquals(1, rows);
        assertNotNull(user.getId());

        User found = userMapper.selectById(user.getId());
        assertNotNull(found);
        assertEquals("ituser", found.getUsername());
        assertEquals("测试", found.getNickname());
    }

    /** 场景：逻辑删除 → selectById 返回 null */
    @Test
    void deleteById_softDeletes() {
        User user = new User();
        user.setUsername("todel");
        user.setPassword("pw");
        userMapper.insert(user);

        int deleted = userMapper.deleteById(user.getId());
        assertEquals(1, deleted);

        User found = userMapper.selectById(user.getId());
        assertNull(found);
    }

    /** 场景：selectList 按条件查询 */
    @Test
    void selectList_byUsername_findsMatch() {
        User user = new User();
        user.setUsername("queryuser");
        user.setPassword("pw");
        userMapper.insert(user);

        var list = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "queryuser"));
        assertEquals(1, list.size());
        assertEquals("queryuser", list.get(0).getUsername());
    }

    /** 场景：updateById 更新字段 */
    @Test
    void updateById_updatesNickname() {
        User user = new User();
        user.setUsername("upd");
        user.setPassword("pw");
        user.setNickname("旧昵称");
        userMapper.insert(user);

        user.setNickname("新昵称");
        userMapper.updateById(user);

        User updated = userMapper.selectById(user.getId());
        assertEquals("新昵称", updated.getNickname());
    }

    /** 场景：count 统计 */
    @Test
    void selectCount_countsCorrectly() {
        User u1 = new User(); u1.setUsername("c1"); u1.setPassword("pw"); userMapper.insert(u1);
        User u2 = new User(); u2.setUsername("c2"); u2.setPassword("pw"); userMapper.insert(u2);

        long count = userMapper.selectCount(null);
        assertTrue(count >= 2);
    }
}