package cn.zhenxinjian.task;

import cn.zhenxinjian.mapper.FoodImageMapper;
import cn.zhenxinjian.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FoodImageInitializer 食物图片预热初始化器单元测试
 * 覆盖：foods 编号清单为空时整轮跳过（不查库、不上传对象存储）
 * 说明："图片已就绪跳过 / 逐条上传"分支依赖 MyBatis-Plus Lambda 元数据缓存，
 *       需 Spring+MyBatis 容器，归属 IT 层而非纯 UT。
 * 作者: wanglx
 */
class FoodImageInitializerTest {

    private final FoodImageMapper foodImageMapper = mock(FoodImageMapper.class);
    private final StorageService storageService = mock(StorageService.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final FoodImageInitializer initializer =
            new FoodImageInitializer(foodImageMapper, storageService, objectMapper);

    @Test
    void runShouldSkipWhenFoodCodeListEmpty() throws Exception {
        // Arrange：真实 foods_200.json 以空 foods 数组解析
        JsonNode root = mock(JsonNode.class);
        JsonNode foods = mock(JsonNode.class);
        when(root.path("foods")).thenReturn(foods);
        when(foods.iterator()).thenReturn(List.<JsonNode>of().iterator());
        when(objectMapper.readTree(any(java.io.InputStream.class))).thenReturn(root);

        // Act
        initializer.run(new DefaultApplicationArguments());

        // Assert
        verify(foodImageMapper, never()).selectList(any());
        verify(storageService, never())
                .upload(any(String.class), any(byte[].class), any(String.class));
    }
}
