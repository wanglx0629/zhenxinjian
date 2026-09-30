package cn.zhenxinjian.mapper;

import cn.zhenxinjian.domain.po.WeightRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录 Mapper
 * 作者: wanglx
 */
@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    /**
     * 查询每日最后一条体重（同日 id 最大者），按 record_date 升序。
     * 专供趋势曲线与平台期判定，聚合在数据库完成。
     *
     * @param userId    用户ID
     * @param startDate 起始日期（含，可空）
     * @param endDate   结束日期（含，可空）
     * @return 每日末值记录（时间序）
     */
    @Select("""
            <script>
            SELECT * FROM weight_record
            WHERE id IN (
                SELECT MAX(id) FROM weight_record
                WHERE user_id = #{userId} AND delete_flag = 0
                <if test="startDate != null">AND record_date &gt;= #{startDate}</if>
                <if test="endDate != null">AND record_date &lt;= #{endDate}</if>
                GROUP BY record_date
            )
            ORDER BY record_date ASC
            </script>
            """)
    List<WeightRecord> selectDailyLast(@Param("userId") Long userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}
