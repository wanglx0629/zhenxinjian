package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.cache.CacheValueGuard;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.config.ZhenxinjianProperties;
import cn.zhenxinjian.domain.dto.ProjectConfigCreateDTO;
import cn.zhenxinjian.domain.dto.ProjectConfigUpdateDTO;
import cn.zhenxinjian.domain.po.ProjectConfig;
import cn.zhenxinjian.domain.vo.ProjectConfigVO;
import cn.zhenxinjian.mapper.ProjectConfigMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目配置服务单元测试（加密往返/key 重复/停用读取 null/SECRET 脱敏不改值）
 * 作者: wanglx
 */
class ConfigServiceImplTest {

    private static final String AES_KEY = "local-dev-config-aes-key-zxj-012";

    private ProjectConfigMapper mapper;
    private ConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProjectConfig.class);

        mapper = mock(ProjectConfigMapper.class);
        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getConfig().setAesKey(AES_KEY);
        service = new ConfigServiceImpl(mapper, properties, new CacheValueGuard(properties));
        service.setSelf(service);
    }

    /** 场景：SECRET 配置加密落库带 enc: 前缀，读取解密回原文 */
    @Test
    void create_secret_encryptsAndRoundTrips() {
        ProjectConfigCreateDTO dto = new ProjectConfigCreateDTO();
        dto.setConfigKey("ocr.api-key");
        dto.setValueType(5);
        dto.setConfigValue("my-secret-key-123");
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(ProjectConfig.class))).thenReturn(1);

        service.create(dto);

        ArgumentCaptor<ProjectConfig> captor = ArgumentCaptor.forClass(ProjectConfig.class);
        verify(mapper).insert(captor.capture());
        ProjectConfig saved = captor.getValue();
        assertTrue(saved.getConfigValue().startsWith("enc:"),
                "SECRET 值必须以 enc: 前缀密文落库");

        when(mapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(saved);
        assertEquals("my-secret-key-123", service.getValue("ocr.api-key"));
    }

    /** 场景：SECRET 密钥未配置 → 保存拒绝 */
    @Test
    void create_secret_missingAesKey_throws() {
        ZhenxinjianProperties properties = new ZhenxinjianProperties();
        properties.getConfig().setAesKey("");
        ConfigServiceImpl noKeyService = new ConfigServiceImpl(mapper, properties,
                new CacheValueGuard(properties));
        noKeyService.setSelf(noKeyService);

        ProjectConfigCreateDTO dto = new ProjectConfigCreateDTO();
        dto.setConfigKey("ocr.api-key");
        dto.setValueType(5);
        dto.setConfigValue("my-secret-key-123");

        BusinessException ex = assertThrows(BusinessException.class, () -> noKeyService.create(dto));
        assertEquals(ExceptionConstant.CONFIG_SECRET_KEY_MISSING, ex.getMessage());
    }

    /** 场景：key 格式非法 → 拒收 */
    @Test
    void create_invalidKeyPattern_throws() {
        ProjectConfigCreateDTO dto = new ProjectConfigCreateDTO();
        dto.setConfigKey("Bad_Key");
        dto.setValueType(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ExceptionConstant.CONFIG_KEY_INVALID, ex.getMessage());
    }

    /** 场景：key 活跃唯一 → 重复创建拒绝 */
    @Test
    void create_duplicateKey_throws() {
        ProjectConfigCreateDTO dto = new ProjectConfigCreateDTO();
        dto.setConfigKey("sensitive.filter.enabled");
        dto.setValueType(3);
        dto.setConfigValue("true");
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ExceptionConstant.CONFIG_KEY_DUPLICATE, ex.getMessage());
    }

    /** 场景：SECRET 更新传 ****** → 保留原密文不改值 */
    @Test
    void update_secretMaskKeepsOriginalValue() {
        ProjectConfig existing = config(1L, "ocr.api-key", "enc:AAECAwQFBgcICQ==", 5, 1);
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(ProjectConfig.class))).thenReturn(1);

        ProjectConfigUpdateDTO dto = new ProjectConfigUpdateDTO();
        dto.setConfigValue("******");

        service.update(1L, dto);

        ArgumentCaptor<ProjectConfig> captor = ArgumentCaptor.forClass(ProjectConfig.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("enc:AAECAwQFBgcICQ==", captor.getValue().getConfigValue());
    }

    /** 场景：SECRET 更新传新值 → 重新加密；读取解密回新值 */
    @Test
    void update_secretNewValue_reencrypts() {
        ProjectConfig existing = config(1L, "ocr.api-key", "enc:AAECAwQFBgcICQ==", 5, 1);
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(ProjectConfig.class))).thenReturn(1);

        ProjectConfigUpdateDTO dto = new ProjectConfigUpdateDTO();
        dto.setConfigValue("new-secret-456");
        ProjectConfigVO vo = service.update(1L, dto);

        assertEquals("******", vo.getConfigValue(), "下发必须脱敏");
        ArgumentCaptor<ProjectConfig> captor = ArgumentCaptor.forClass(ProjectConfig.class);
        verify(mapper).updateById(captor.capture());
        String cipher = captor.getValue().getConfigValue();
        assertTrue(cipher.startsWith("enc:") && !cipher.equals("enc:AAECAwQFBgcICQ=="),
                "必须按新值重新加密");

        when(mapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(captor.getValue());
        assertEquals("new-secret-456", service.getValue("ocr.api-key"));
    }

    /** 场景：配置停用（status=0 查询过滤）→ 读取返回 null */
    @Test
    void getValue_disabled_returnsNull() {
        when(mapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null);
        assertNull(service.getValue("sensitive.filter.enabled"));
    }

    /** 场景：密文损坏/密钥不匹配 → 解密失败抛业务异常 */
    @Test
    void getValue_corruptCipher_throws() {
        ProjectConfig existing = config(1L, "ocr.api-key", "enc:!!not-base64!!", 5, 1);
        when(mapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getValue("ocr.api-key"));
        assertEquals(ExceptionConstant.CONFIG_SECRET_DECRYPT_FAIL, ex.getMessage());
    }

    /** 场景：非 SECRET 类型原样读写；分页下发不脱敏 */
    @Test
    void page_plainValue_notMasked() {
        ProjectConfig row = config(2L, "sensitive.filter.enabled", "true", 3, 1);
        Page<ProjectConfig> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(row));
        page.setTotal(1);
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var result = service.page(null, 1, 10);

        assertEquals(1, result.getTotal());
        assertNotNull(result.getRecords().get(0));
        assertEquals("true", result.getRecords().get(0).getConfigValue());
    }

    /** 构造配置行 */
    private ProjectConfig config(Long id, String key, String value, int valueType, int status) {
        ProjectConfig cfg = new ProjectConfig();
        cfg.setId(id);
        cfg.setConfigKey(key);
        cfg.setConfigValue(value);
        cfg.setValueType(valueType);
        cfg.setStatus(status);
        cfg.setVersion(0);
        return cfg;
    }
}
