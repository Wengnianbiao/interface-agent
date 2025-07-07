package com.helianhealth.agent.controller.request;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("jarvis调用入参")
public class JarvisRequest {

    @ApiModelProperty("业务方法")
    @JsonProperty(value = "BusinessMethod")
    private String businessMethod;

    @JsonProperty(value = "BusinessCode")
    @ApiModelProperty("体检编码")
    private String businessCode;

    @JsonProperty(value = "data")
    @ApiModelProperty("方法入参")
    private JSONObject data;

    // 报告相关
    @JsonProperty(value = "ServiceProviderType")
    @ApiModelProperty("厂商类型")
    private String serviceProviderType;

    @JsonProperty(value = "PatientCode")
    @ApiModelProperty("体检编号")
    private String patientCode;

    @JsonProperty(value = "OutFeeItemIdList")
    @ApiModelProperty("收费项外部编码集合")
    private List<String> outFeeItemIdList;

    @JsonProperty(value = "ApplyNoList")
    @ApiModelProperty("申请单集合")
    private List<String> applyNoList;

    @JsonProperty(value = "BarCodeList")
    @ApiModelProperty("条码号集合")
    private List<String> barCodeList;

    @JsonProperty(value = "AuditTime")
    @ApiModelProperty("审核时间")
    private String auditTime;

    @JsonProperty(value = "QueryType")
    @ApiModelProperty("查询以哪个参数为准")
    private Integer queryType;
}
