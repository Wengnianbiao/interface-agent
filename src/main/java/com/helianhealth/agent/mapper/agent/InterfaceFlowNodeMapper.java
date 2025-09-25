package com.helianhealth.agent.mapper.agent;

import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface InterfaceFlowNodeMapper extends Mapper<InterfaceWorkflowNodeDO> {

    InterfaceWorkflowNodeDO selectByNodeId(@Param("nodeId") Integer nodeId);
    List<InterfaceWorkflowNodeDO> selectAllNodes(@Param("flowId") Integer flowId);

    /**
     * 创建工作流节点
     * @param node 工作流节点对象
     * @return 插入记录数
     */
    int createNode(InterfaceWorkflowNodeDO node);

    /**
     * 更新工作流节点
     * @param node 工作流节点对象
     */
    int updateNode(InterfaceWorkflowNodeDO node);
}
