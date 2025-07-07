package com.helianhealth.agent.mapper.agent;

import com.helianhealth.agent.model.domain.InterfaceInvokeLogDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface InterfaceInvokeLogMapper extends Mapper<InterfaceInvokeLogDO> {
    void insertLog(InterfaceInvokeLogDO logDO);

    List<InterfaceInvokeLogDO> selectAllInvokeLogs(@Param("nodeId") Integer nodeId);
}