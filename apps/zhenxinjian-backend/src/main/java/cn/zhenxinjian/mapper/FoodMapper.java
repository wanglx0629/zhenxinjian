package cn.zhenxinjian.mapper;

import cn.zhenxinjian.domain.po.Food;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食物库 Mapper
 * 作者: wanglx
 */
@Mapper
public interface FoodMapper extends BaseMapper<Food> {
}
