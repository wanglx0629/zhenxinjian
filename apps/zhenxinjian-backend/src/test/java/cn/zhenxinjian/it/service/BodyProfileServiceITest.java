package cn.zhenxinjian.it.service;

import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.it.TestConfig;
import cn.zhenxinjian.mapper.UserBodyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 身体档案集成测试 — 验证 BodyProfileService 的 DB 交互
 * 作者: wanglx
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@org.springframework.context.annotation.Import(TestConfig.class)
class BodyProfileServiceITest {

    @Autowired
    private UserBodyMapper userBodyMapper;

    /** 场景：insert → selectById → 返回完整档案 */
    @Test
    void insertAndSelect_returnsFullProfile() {
        UserBody body = new UserBody();
        body.setUserId(1L);
        body.setGender(1);
        body.setAge(30);
        body.setHeight(170.0);
        body.setWeight(75.0);
        body.setTargetWeight(65.0);
        body.setActivityLevel(2);
        body.setDeficit(300);
        body.setCfc(0.8);
        body.setMode(1);
        body.setBmr(1600);
        body.setTdee(2100);
        body.setTargetKcal(1800);
        body.setTargetCarb(220.0);
        body.setTargetProtein(110.0);
        body.setTargetFat(40.0);

        int rows = userBodyMapper.insert(body);
        assertEquals(1, rows);
        assertNotNull(body.getId());

        UserBody found = userBodyMapper.selectById(body.getId());
        assertEquals(75.0, found.getWeight(), 0.01);
        assertEquals(65.0, found.getTargetWeight(), 0.01);
        assertEquals(220.0, found.getTargetCarb(), 0.01);
    }

    /** 场景：更新体重后查询 */
    @Test
    void updateAndSelect_reflectsChange() {
        UserBody body = new UserBody();
        body.setUserId(1L);
        body.setWeight(80.0);
        userBodyMapper.insert(body);

        body.setWeight(78.5);
        userBodyMapper.updateById(body);

        assertEquals(78.5, userBodyMapper.selectById(body.getId()).getWeight(), 0.01);
    }

    /** 场景：逻辑删除 */
    @Test
    void delete_softDeletes() {
        UserBody body = new UserBody();
        body.setUserId(1L);
        userBodyMapper.insert(body);

        userBodyMapper.deleteById(body.getId());
        assertNull(userBodyMapper.selectById(body.getId()));
    }
}