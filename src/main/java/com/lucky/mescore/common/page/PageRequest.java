package com.lucky.mescore.common.page;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageRequest<T> {

    @Min(1)
    private long pageNum = 1;

    @Min(1)
    @Max(100)
    private long pageSize = 10;

    /** 分页查询返回的总记录数（由分页拦截器回写） */
    private long total = 0;

    @Valid
    private T condition;
}
