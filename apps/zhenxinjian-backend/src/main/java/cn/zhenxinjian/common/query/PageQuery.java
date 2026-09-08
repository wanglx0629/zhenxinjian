package cn.zhenxinjian.common.query;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基类（各业务 Query 继承本类）
 * 作者: wanglx
 */
@Data
@Schema(description = "分页查询基类")
public class PageQuery implements Serializable {

    @Min(value = 1, message = ExceptionConstant.PAGE_MIN)
    @Schema(description = "页码，从1开始", example = "1")
    private Long page = CommonConstant.DEFAULT_PAGE;

    @Min(value = 1, message = ExceptionConstant.SIZE_MIN)
    @Max(value = CommonConstant.MAX_PAGE_SIZE, message = ExceptionConstant.SIZE_MAX)
    @Schema(description = "每页条数，最大100", example = "10")
    private Long size = CommonConstant.DEFAULT_SIZE;

    /**
     * 安全页码（空值回落默认值）
     */
    public long safePage() {
        return page == null || page < 1 ? CommonConstant.DEFAULT_PAGE : page;
    }

    /**
     * 安全每页条数（空值回落默认值，并限制上限）
     */
    public long safeSize() {
        if (size == null || size < 1) {
            return CommonConstant.DEFAULT_SIZE;
        }
        return Math.min(size, CommonConstant.MAX_PAGE_SIZE);
    }

    /**
     * 转为 MyBatis-Plus 分页对象
     */
    public <T> Page<T> toPage() {
        return new Page<>(safePage(), safeSize());
    }
}
