package com.helianhealth.agent.service;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.controller.workflow.reponse.NodeParamConfigExportResponse;
import com.helianhealth.agent.enums.ProcessType;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NodeParamConfigService {

    PageList<NodeParamConfigDO> findAllNodeParamConfigs(Integer nodeId, ProcessType processType, int pageNum, int pageSize);

    List<NodeParamConfigDO> findAllNodeParamConfigsUnpaged(Integer nodeId, ProcessType processType);

    NodeParamConfigDO update(NodeParamConfigDO config);

    NodeParamConfigDO save(NodeParamConfigDO config);

    void deleteById(Integer configId);

    void importNodeParamConfig(Integer nodeId, MultipartFile file);
}
