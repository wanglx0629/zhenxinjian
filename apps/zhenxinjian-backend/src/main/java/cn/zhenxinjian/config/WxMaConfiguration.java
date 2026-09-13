package cn.zhenxinjian.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 * appid/secret 存于 application-dev.yml（不入库）；未配置时跳过 Bean 注册，登录接口返回 40102
 * WxMaDefaultConfigImpl 为内存 token 实现，一期单实例部署约束见 ADR 0005
 * （docs/knowledge/arch/adr/0005-微信Token内存态与单实例部署约束.md），
 * 多实例扩容前须切换 WxMaRedisConfigImpl / WxMaRedissonConfigImpl 类 Redis 集中存储实现
 * 作者: wanglx
 */
@Configuration
@EnableConfigurationProperties(WxMaConfiguration.WxMaProperties.class)
public class WxMaConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "zhenxinjian.wechat", name = "appid")
    public WxMaService wxMaService(WxMaProperties properties) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(properties.getAppid());
        config.setSecret(properties.getSecret());
        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }

    @Data
    @ConfigurationProperties(prefix = "zhenxinjian.wechat")
    public static class WxMaProperties {
        /** 小程序 AppID */
        private String appid;
        /** 小程序 AppSecret */
        private String secret;
        /** 三餐提醒订阅消息模板ID（一次性订阅；未配置时推送任务空转） */
        private String remindTemplateId;
        /** 订阅消息跳转小程序版本：developer开发版/trial体验版/formal正式版（默认正式版） */
        private String remindMiniprogramState = "formal";
    }
}
