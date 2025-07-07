package com.helianhealth.agent.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@ApiModel
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageList<E> {

    @ApiModelProperty(value = "总数")
    private long total;

    @ApiModelProperty(value = "页码")
    private int pageNum;

    @ApiModelProperty(value = "分页条数")
    private int pageSize;

    @ApiModelProperty(value = "数据集合")
    private List<E> rows;


}
