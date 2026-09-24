package cn.zhenxinjian.mapper;

import cn.zhenxinjian.domain.po.FoodAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食物共建审核流水 Mapper
 * 作者: wanglx
 */
@Mapper
public interface FoodAuditLogMapper extends BaseMapper<FoodAuditLog> {
}
