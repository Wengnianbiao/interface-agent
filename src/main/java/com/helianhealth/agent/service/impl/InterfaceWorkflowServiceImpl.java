package com.helianhealth.agent.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.mapper.agent.InterfaceWorkflowMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.service.InterfaceWorkflowService;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterfaceWorkflowServiceImpl implements InterfaceWorkflowService {

    private final InterfaceWorkflowMapper workflowMapper;

    @Override
    public Optional<InterfaceWorkflowDO> findByInterfaceUri(String apiUri) {
        return Optional.ofNullable(workflowMapper.selectByInterfaceUri(apiUri));
    }

    @Override
    public int save(InterfaceWorkflowDO workflow) {
        // 验证参数
        if (workflow == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        return workflowMapper.createWorkflow(workflow);
    }

    @Override
    public PageList<InterfaceWorkflowDO> getAllWorkflows(int pageNum, int pageSize) {
        try {
            PageHelper.startPage(pageNum, pageSize);
            // 将查询结果转换为 PageInfo 对象
            List<InterfaceWorkflowDO> workflows = workflowMapper.selectAllWorkflows();
            PageInfo<InterfaceWorkflowDO> pageInfo = new PageInfo<>(workflows);

            // 构建 PageList 对象
            PageList<InterfaceWorkflowDO> pageList = new PageList<>();
            pageList.setRows(workflows);
            pageList.setTotal(pageInfo.getTotal());
            pageList.setPageNum(pageInfo.getPageNum());
            pageList.setPageSize(pageInfo.getPageSize());
            return pageList;
        } catch (Exception e) {
            log.error("获取工作流列表失败", e);
            throw new RuntimeException("获取工作流列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<InterfaceWorkflowDO> getAllWorkflowsWithoutPaged() {
        return workflowMapper.selectAllWorkflows();
    }

    @Override
    public int update(InterfaceWorkflowDO interfaceInstanceDO) {
        return workflowMapper.updateByPrimaryKeySelective(interfaceInstanceDO);
    }

    @Override
    public void deleteByFlowId(Integer flowId) {
        try {
            // 验证参数
            if (flowId == null) {
                throw new IllegalArgumentException("参数不能为空，flowId 必须存在");
            }

            // 执行删除操作
            workflowMapper.deleteByPrimaryKey(flowId);
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            throw new RuntimeException("删除工作流失败: " + e.getMessage());
        }
    }

    @Override
    public InterfaceWorkflowDO selectByFlowId(Integer flowId) {
        return workflowMapper.selectByPrimaryKey(flowId);
    }
}
