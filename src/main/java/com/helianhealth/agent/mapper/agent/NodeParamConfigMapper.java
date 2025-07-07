package com.helianhealth.agent.mapper.agent;

import com.helianhealth.agent.enums.ProcessType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface NodeParamConfigMapper extends Mapper<NodeParamConfigDO>{

    List<NodeParamConfigDO> selectPreProcessConfigByNodeId(Integer nodeId);

    List<NodeParamConfigDO> selectPostProcessConfigByNodeId(Integer nodeId);

    List<NodeParamConfigDO> selectAllParamConfig(@Param("nodeId") Integer nodeId,
                                                 @Param("processType") String processType);

    /**
     * 插入节点参数配置（不包含自增主键）
     * @param config 节点参数配置对象
     * @return 插入记录数
     */
    int insertNodeParamConfig(NodeParamConfigDO config);
}
