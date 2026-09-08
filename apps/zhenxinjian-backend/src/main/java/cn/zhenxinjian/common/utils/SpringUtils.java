package cn.zhenxinjian.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring 上下文工具（静态获取 Bean）
 * 作者: wanglx
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.context = applicationContext;
    }

    /** 按类型获取全部 Bean */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        if (context == null) {
            return Map.of();
        }
        return context.getBeansOfType(type);
    }
}
