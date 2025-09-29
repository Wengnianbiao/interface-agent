package com.helianhealth.agent.controller.workflow;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.service.InterfaceWorkflowService;
import com.helianhealth.agent.utils.ResponseRenderUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流配置控制器
 */
@RestController
@RequestMapping("/v1/console/workflow")
@Slf4j
@AllArgsConstructor
public class WorkflowController {

    private final InterfaceWorkflowService interfaceRuleFlowService;

    /**
     * 获取工作流列表
     */
    @GetMapping("/all")
    public ResultData<PageList<InterfaceWorkflowDO>> getAllWorkflows(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        try {
            PageList<InterfaceWorkflowDO> allWorkflows = interfaceRuleFlowService.getAllWorkflows(pageNum, pageSize);
            return ResponseRenderUtils.render(allWorkflows);
        } catch (Exception e) {
            log.error("获取工作流失败", e);
            return ResponseRenderUtils.error("获取工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流列表不分页
     */
    @GetMapping("/all-unpaged")
    public ResultData<List<InterfaceWorkflowDO>> getAllWorkflowsWithoutPaged() {
        try {
            List<InterfaceWorkflowDO> allWorkflows = interfaceRuleFlowService.getAllWorkflowsWithoutPaged();
            return ResponseRenderUtils.render(allWorkflows);
        } catch (Exception e) {
            log.error("获取工作流失败", e);
            return ResponseRenderUtils.error("获取工作流失败: " + e.getMessage());
        }
    }

    /**
     * 创建工作流
     */
    @PostMapping("/create")
    public ResultData<Integer> createFlow(@RequestBody InterfaceWorkflowDO flow) {
        try {
            int savedFlow = interfaceRuleFlowService.save(flow);
            return ResponseRenderUtils.render(savedFlow);
        } catch (Exception e) {
            log.error("创建工作流失败", e);
            return ResponseRenderUtils.error("创建工作流失败: " + e.getMessage());
        }
    }

    /**
     * 更新工作流
     */
    @PostMapping("/update")
    public ResultData<Integer> updateFlow(@RequestBody InterfaceWorkflowDO flow) {
        try {
            int updatedFlow = interfaceRuleFlowService.update(flow);
            return ResponseRenderUtils.render(updatedFlow);
        } catch (Exception e) {
            log.error("更新工作流失败", e);
            return ResponseRenderUtils.error("更新工作流失败: " + e.getMessage());
        }
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("delete/{flowId}")
    public ResultData<String> deleteFlow(@PathVariable Integer flowId) {
        try {
            interfaceRuleFlowService.deleteByFlowId(flowId);
            return ResponseRenderUtils.render("工作流删除成功");
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            return ResponseRenderUtils.error("删除工作流失败: " + e.getMessage());
        }
    }
}
