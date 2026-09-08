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
    }
}
