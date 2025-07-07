package com.helianhealth.agent.model.dto;

import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.enums.ProcessType;
import lombok.Data;

import java.util.List;

@Data
public class NodeParamConfigImportDTO {
    private NodeInfo nodeInfo;
    private List<NodeParamConfigDTO> paramsConfig;

    @Data
    public static class NodeInfo {
        private String nodeName;

        private String nodeType;
    }

    @Data
    public static class NodeParamConfigDTO {

        private ProcessType processType;
        private String sourceParamKey;
        private ParamType sourceParamType;
        private String paramDesc;
        private String targetParamKey;
        private ParamType targetParamType;
        private Integer sort;
        private MappingType mappingType;
        private MappingSource mappingSource;
        private String mappingRule;

        // 子参数列表
        private List<NodeParamConfigDTO> childParam;
    }
}
