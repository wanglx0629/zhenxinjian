package cn.zhenxinjian.task;

import cn.zhenxinjian.common.enums.FoodImageSourceEnum;
import cn.zhenxinjian.domain.po.FoodImage;
import cn.zhenxinjian.mapper.FoodImageMapper;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 食物图片预热初始化器（启动时幂等上传 classpath food-images/ 200 张内置食物图至对象存储并落库）
 * 作者: wanglx
 *
 * 口径: object key 固定 food/{code}.jpg（幂等重传同 Key 覆盖）；url 冗余存 StorageService 返回值
 * 幂等: 按 food_code 补缺（已有活跃图片跳过）；MinIO/OSS 均未启用时整轮跳过（仅告警，不阻断启动）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodImageInitializer implements ApplicationRunner {

    /** 图片资源目录（classpath） */
    private static final String IMAGE_DIR = "food-images/";

    /** 食物编号真源（与 FoodLibraryInitializer 同源，取 id 清单） */
    private static final String FOODS_RESOURCE = "foods_200.json";

    /** 预热操作者标识（审计列） */
    private static final String CREATE_BY_PREHEAT = "food-image-preheat";

    private final FoodImageMapper foodImageMapper;

    private final StorageService storageService;

    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<String> codes = loadFoodCodes();
        if (codes.isEmpty()) {
            log.warn("[FoodImage] foods_200.json 无编号清单，跳过预热");
            return;
        }
        Set<String> existing = foodImageMapper.selectList(
                        Wrappers.<FoodImage>lambdaQuery()
                                .select(FoodImage::getFoodCode)
                                .eq(FoodImage::getStatus, 1))
                .stream()
                .map(FoodImage::getFoodCode)
                .collect(Collectors.toSet());
        if (existing.size() >= codes.size()) {
            log.info("[FoodImage] 食物图片已就绪（{}张），跳过预热", existing.size());
            return;
        }
        List<String> missing = new ArrayList<>(codes);
        missing.removeAll(existing);
        int uploaded = 0;
        int skipped = 0;
        int failed = 0;
        for (String code : missing) {
            String resource = IMAGE_DIR + code + ".jpg";
            try (InputStream in = new ClassPathResource(resource).getInputStream()) {
                byte[] data = in.readAllBytes();
                var vo = storageService.upload("food/" + code + ".jpg", data, "image/jpeg");
                FoodImage image = new FoodImage();
                image.setFoodCode(code);
                image.setObjectKey(vo.getObjectKey());
                image.setUrl(vo.getUrl());
                image.setSource(FoodImageSourceEnum.PREHEAT.getCode());
                image.setCreateBy(CREATE_BY_PREHEAT);
                try {
                    foodImageMapper.insert(image);
                    uploaded++;
                } catch (DuplicateKeyException e) {
                    // 多实例并发预热：同码已落库，跳过（对象同 Key 幂等覆盖，无脏数据）
                    skipped++;
                }
            } catch (Exception e) {
                // 单条失败不阻断启动，下次启动补缺
                failed++;
                log.warn("[FoodImage] 预热失败（下次启动补缺）: code={}, msg={}", code, e.getMessage());
            }
        }
        log.info("[FoodImage] 预热完成：待补{}张，上传{}张，并发冲突跳过{}张，失败{}张", missing.size(), uploaded, skipped, failed);
    }

    /** 读 foods_200.json 的 id 清单（F001–F200） */
    private List<String> loadFoodCodes() {
        try (InputStream in = new ClassPathResource(FOODS_RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            List<String> codes = new ArrayList<>();
            for (JsonNode food : root.path("foods")) {
                String id = food.path("id").asText("");
                if (!id.isEmpty()) {
                    codes.add(id);
                }
            }
            return codes;
        } catch (Exception e) {
            log.warn("[FoodImage] 读取食物编号清单失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
