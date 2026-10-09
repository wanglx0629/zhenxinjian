package cn.zhenxinjian.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FoodCategoryEnum.of 空态语义单元测试（IMPL-空值-001）
 * 作者: wanglx
 */
class FoodCategoryEnumTest {

    @Test
    void of_knownCode_returnsPresent() {
        Optional<FoodCategoryEnum> result = FoodCategoryEnum.of("01");
        assertTrue(result.isPresent());
        assertEquals(FoodCategoryEnum.STARCH, result.get());
    }

    @Test
    void of_unknownCode_returnsEmpty() {
        assertTrue(FoodCategoryEnum.of("99").isEmpty());
    }

    @Test
    void of_nullCode_returnsEmpty() {
        assertTrue(FoodCategoryEnum.of(null).isEmpty());
    }
}