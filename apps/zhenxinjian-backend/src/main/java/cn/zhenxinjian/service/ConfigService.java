package cn.zhenxinjian.service;

import cn.zhenxinjian.domain.dto.ProjectConfigCreateDTO;
import cn.zhenxinjian.domain.dto.ProjectConfigUpdateDTO;
import cn.zhenxinjian.domain.vo.ProjectConfigVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 项目配置服务（project_config 读缓存 + admin 维护；SECRET 值 AES 加解密）
 * 作者: wanglx
 */
public interface ConfigService {

    /**
     * 读配置原始值（带 JetCache BOTH 缓存，含空值防穿透；SECRET 值解密后返回）
     *
     * @param configKey 配置键
     * @return 配置值；不存在/已停用/已删除返回 null
     */
    String getValue(String configKey);

    /**
     * 读布尔配置
     *
     * @param configKey    配置键
     * @param defaultValue 值缺失或非法时返回的默认值
     * @return 布尔值
     */
    boolean getBoolean(String configKey, boolean defaultValue);

    /**
     * 读整数配置
     *
     * @param configKey    配置键
     * @param defaultValue 值缺失或非法时返回的默认值
     * @return 整数值
     */
    int getInt(String configKey, int defaultValue);

    /**
     * 读 JSON 数组字符串配置（如敏感词追加/排除词）
     *
     * @param configKey 配置键
     * @return 字符串列表；值缺失/非法 JSON 返回空列表
     */
    List<String> getStringList(String configKey);

    /**
     * 分页查询配置（config_key/remark 模糊；SECRET 值脱敏下发）
     *
     * @param keyword 关键词（可空）
     * @param page    页码（1 起）
     * @param size    每页条数
     * @return 分页结果
     */
    IPage<ProjectConfigVO> page(String keyword, long page, long size);

    /**
     * 新增配置（key 活跃唯一；SECRET 值加密落库）
     *
     * @param dto 入参
     * @return 新增结果（SECRET 已脱敏）
     */
    ProjectConfigVO create(ProjectConfigCreateDTO dto);

    /**
     * 修改配置（仅 value/remark/status；key 与 valueType 不可改；SECRET 传 ****** 或空 = 保留原值）
     *
     * @param id  主键
     * @param dto 入参
     * @return 修改结果（SECRET 已脱敏）
     */
    ProjectConfigVO update(Long id, ProjectConfigUpdateDTO dto);

    /**
     * 按配置键失效缓存（写路径专用，供 self 代理触发 JetCache 注解）
     *
     * @param configKey 配置键
     */
    void evictCache(String configKey);
}
