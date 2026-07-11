package com.huixiang.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
