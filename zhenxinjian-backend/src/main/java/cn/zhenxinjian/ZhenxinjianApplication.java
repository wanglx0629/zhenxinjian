package cn.zhenxinjian;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * luote 后端启动类
 * 作者: luote (luote) - https://luote996.cn
 */
@SpringBootApplication
@MapperScan("cn.zhenxinjian.mapper")
@EnableMethodCache(basePackages = "cn.zhenxinjian")
public class ZhenxinjianApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhenxinjianApplication.class, args);
    }
}
