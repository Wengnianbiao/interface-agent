package com.helianhealth.agent.service;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.model.domain.InterfaceInvokeLogDO;

public interface InterfaceInvokeLogService {

    /**
     * 保存接口调用日志
     * @param nodeId 节点ID
     * @param businessData 业务数据
     * @param paramBeforeInvoke 调用前参数
     * @param remoteInvokeResponse 远程调用响应
     * @param paramAfterInvoke 调用后参数
     */
    void saveInvokeLog(Integer nodeId,
                       String businessData,
                       String paramBeforeInvoke,
                       String remoteInvokeResponse,
                       String paramAfterInvoke,
                       String invokeTime);

    PageList<InterfaceInvokeLogDO> findAllInvokeLogs(Integer nodeId, int pageNum, int pageSize);
}
