package cn.zhenxinjian.mapper;

import cn.zhenxinjian.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 作者: wanglx
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
