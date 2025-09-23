package com.helianhealth.agent.model.dto;

import com.helianhealth.agent.enums.OperationType;
import com.helianhealth.agent.enums.ParamType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 参数节点DTO（用于组装树形结构，参数的本质就是一个多叉树:N-ary tree）
 */
@Data
@Builder
public class ParamTreeNode {
    /**
     * 参数键名
     */
    private String paramKey;

    /**
     * 参数值（基本类型/子节点列表），根据paramType进行会进行转化，使得类型检查安全
     */
    private Object paramValue;

    /**
     * 节点类型（object/array/string等）
     */
    private ParamType paramType;

    /**
     * xml的属性节点
     */
    private List<ParamTreeNode> attributeNodes;

    /**
     * 子节点（用于object/array类型）
     */
    private List<ParamTreeNode> children;
}
