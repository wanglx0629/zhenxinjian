package cn.zhenxinjian.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.cache.CacheValueGuard;
import cn.zhenxinjian.common.constant.CacheConstant;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.enums.ConfigValueTypeEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.dto.ProjectConfigCreateDTO;
import cn.zhenxinjian.domain.dto.ProjectConfigUpdateDTO;
import cn.zhenxinjian.domain.po.ProjectConfig;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.domain.vo.ProjectConfigVO;
import cn.zhenxinjian.mapper.ProjectConfigMapper;
import cn.zhenxinjian.service.ConfigService;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 项目配置服务实现
 * 读：JetCache BOTH（本地+远程）+ cacheNullValue 防穿透，SECRET 值解密后返回；
 * 写：key 活跃唯一校验，SECRET AES 加密落库（enc: 前缀，兼容手工插入的明文值），
 *     写后经 self 代理失效该 key 缓存；admin 下发 SECRET 一律脱敏 ******
 * 作者: wanglx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    /** SECRET 值密文前缀（读取时按前缀判别明文/密文） */
    private static final String ENC_PREFIX = "enc:";

    /** SECRET 值脱敏掩码（admin 下发；更新传该值 = 保留原值） */
    private static final String SECRET_MASK = "******";

    private final ProjectConfigMapper projectConfigMapper;
    private final ZhenxinjianProperties properties;
    private final CacheValueGuard cacheValueGuard;

    /** 自代理：读/失效方法经代理调用才能生效 JetCache 注解（单测注入 this 直调） */
    private ConfigService self;

    @Autowired
    void setSelf(@Lazy ConfigService self) {
        this.self = self;
    }

    @Override
    @Cached(
            name = CacheConstant.CONFIG,
            key = "#configKey",
            cacheType = CacheType.BOTH,
            syncLocal = true,
            cacheNullValue = true,
            expire = CacheConstant.DEFAULT_REMOTE_EXPIRE_SECONDS,
            localExpire = CacheConstant.DEFAULT_LOCAL_EXPIRE_SECONDS
    )
    public String getValue(String configKey) {
        // 活跃唯一（key_active 生成列兜底），status=1 才视为有效配置
        ProjectConfig cfg = projectConfigMapper.selectOne(Wrappers.<ProjectConfig>lambdaQuery()
                .eq(ProjectConfig::getConfigKey, configKey)
                .eq(ProjectConfig::getStatus, 1), false);
        if (cfg == null) {
            return null;
        }
        String value = decryptIfNeeded(cfg.getConfigValue());
        cacheValueGuard.check(value);
        return value;
    }

    @Override
    public boolean getBoolean(String configKey, boolean defaultValue) {
        String value = self.getValue(configKey);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    @Override
    public int getInt(String configKey, int defaultValue) {
        String value = self.getValue(configKey);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> getStringList(String configKey) {
        String value = self.getValue(configKey);
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(value, String.class).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            // 值非法不阻断业务（fail-open），记 WARN 按空列表处理
            log.warn("配置 {} 不是合法 JSON 数组，按空列表处理", configKey, e);
            return List.of();
        }
    }

    @Override
    public IPage<ProjectConfigVO> page(String keyword, long page, long size) {
        IPage<ProjectConfig> result = projectConfigMapper.selectPage(new Page<>(page, size),
                Wrappers.<ProjectConfig>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ProjectConfig::getConfigKey, keyword)
                                .or()
                                .like(ProjectConfig::getRemark, keyword))
                        .orderByAsc(ProjectConfig::getId));
        return result.convert(this::toVO);
    }

    @Override
    public ProjectConfigVO create(ProjectConfigCreateDTO dto) {
        String key = dto.getConfigKey().trim();
        if (!ProjectConfigKeyConstant.KEY_PATTERN.matcher(key).matches()) {
            throw new BusinessException(CommonConstant.CONFIG_KEY_INVALID_CODE,
                    ExceptionConstant.CONFIG_KEY_INVALID);
        }
        Long exists = projectConfigMapper.selectCount(Wrappers.<ProjectConfig>lambdaQuery()
                .eq(ProjectConfig::getConfigKey, key));
        if (exists > 0) {
            throw new BusinessException(CommonConstant.CONFIG_KEY_DUPLICATE_CODE,
                    ExceptionConstant.CONFIG_KEY_DUPLICATE);
        }
        ProjectConfig cfg = new ProjectConfig();
        cfg.setConfigKey(key);
        cfg.setConfigValue(encryptIfNeeded(dto.getConfigValue(), dto.getValueType()));
        cfg.setValueType(dto.getValueType());
        cfg.setRemark(dto.getRemark());
        cfg.setStatus(1);
        cfg.setCreateBy(operator());
        projectConfigMapper.insert(cfg);
        // 防同 key 历史行（已删/停用）残留的空值缓存
        self.evictCache(key);
        return toVO(cfg);
    }

    @Override
    public ProjectConfigVO update(Long id, ProjectConfigUpdateDTO dto) {
        ProjectConfig cfg = projectConfigMapper.selectById(id);
        if (cfg == null) {
            throw new BusinessException(CommonConstant.CONFIG_NOT_FOUND_CODE,
                    ExceptionConstant.CONFIG_NOT_FOUND);
        }
        if (dto.getConfigValue() != null) {
            if (ConfigValueTypeEnum.SECRET.getCode().equals(cfg.getValueType())) {
                // 传 ****** 或空白 = 保留原值；传新值则重新加密
                String incoming = dto.getConfigValue().trim();
                if (StringUtils.hasText(incoming) && !SECRET_MASK.equals(incoming)) {
                    cfg.setConfigValue(encryptIfNeeded(incoming, cfg.getValueType()));
                }
            } else {
                cfg.setConfigValue(dto.getConfigValue());
            }
        }
        if (dto.getRemark() != null) {
            cfg.setRemark(dto.getRemark());
        }
        if (dto.getStatus() != null) {
            cfg.setStatus(dto.getStatus());
        }
        cfg.setUpdateBy(operator());
        if (projectConfigMapper.updateById(cfg) <= 0) {
            // 乐观锁冲突或行已不可操作
            throw new BusinessException(CommonConstant.CONFIG_NOT_FOUND_CODE,
                    ExceptionConstant.CONFIG_NOT_FOUND);
        }
        self.evictCache(cfg.getConfigKey());
        return toVO(cfg);
    }

    @Override
    @CacheInvalidate(name = CacheConstant.CONFIG, key = "#configKey")
    public void evictCache(String configKey) {
        // JetCache 注解驱动失效（需经 self 代理调用）
    }

    /** SECRET 值解密（带 enc: 前缀才解密，其余原样返回；解密失败抛业务异常） */
    private String decryptIfNeeded(String storedValue) {
        if (!StringUtils.hasText(storedValue) || !storedValue.startsWith(ENC_PREFIX)) {
            return storedValue;
        }
        try {
            return aes().decryptStr(storedValue.substring(ENC_PREFIX.length()));
        } catch (Exception e) {
            log.error("配置解密失败", e);
            throw new BusinessException(CommonConstant.CONFIG_SECRET_DECRYPT_FAIL_CODE,
                    ExceptionConstant.CONFIG_SECRET_DECRYPT_FAIL);
        }
    }

    /** SECRET 值加密（非 SECRET 类型或空值原样返回；密钥未配置/长度非法抛业务异常） */
    private String encryptIfNeeded(String value, Integer valueType) {
        if (!ConfigValueTypeEnum.SECRET.getCode().equals(valueType) || !StringUtils.hasText(value)) {
            return value;
        }
        return ENC_PREFIX + aes().encryptBase64(value);
    }

    /** AES 对称加密（密钥 16/24/32 字符） */
    private AES aes() {
        String key = properties.getConfig().getAesKey();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(CommonConstant.CONFIG_SECRET_KEY_MISSING_CODE,
                    ExceptionConstant.CONFIG_SECRET_KEY_MISSING);
        }
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new BusinessException(CommonConstant.CONFIG_SECRET_KEY_MISSING_CODE,
                    ExceptionConstant.CONFIG_SECRET_KEY_MISSING);
        }
        return SecureUtil.aes(bytes);
    }

    /** PO → VO（SECRET 值脱敏下发） */
    private ProjectConfigVO toVO(ProjectConfig cfg) {
        ProjectConfigVO vo = new ProjectConfigVO();
        vo.setId(cfg.getId());
        vo.setConfigKey(cfg.getConfigKey());
        vo.setValueType(cfg.getValueType());
        vo.setRemark(cfg.getRemark());
        vo.setStatus(cfg.getStatus());
        vo.setUpdateTime(cfg.getUpdateTime());
        vo.setConfigValue(ConfigValueTypeEnum.SECRET.getCode().equals(cfg.getValueType())
                ? SECRET_MASK : cfg.getConfigValue());
        return vo;
    }

    /** 操作者标识（审计列，当前管理员用户名） */
    private String operator() {
        LoginUserVO user = UserContext.get();
        return user != null && StringUtils.hasText(user.getUsername())
                ? user.getUsername() : CommonConstant.CREATE_BY_SYSTEM;
    }
}
