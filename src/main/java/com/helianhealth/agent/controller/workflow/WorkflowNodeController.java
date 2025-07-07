package com.helianhealth.agent.controller.workflow;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.controller.request.workflownode.WorkflowNodeCreateReq;
import com.helianhealth.agent.controller.workflow.reponse.NodeParamConfigExportResponse;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.service.InterfaceWorkflowNodeService;
import com.helianhealth.agent.service.NodeParamConfigService;
import com.helianhealth.agent.utils.ResponseModelUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 工作流配置控制器
 */
@RestController
@RequestMapping("/v1/console/node")
@Slf4j
@AllArgsConstructor
public class WorkflowNodeController {

    private final InterfaceWorkflowNodeService interfaceFlowNodeService;
    private final NodeParamConfigService nodeParamConfigService;

    @GetMapping("/all")
    public ResultData<PageList<InterfaceWorkflowNodeDO>> getAllNodes(
            @RequestParam(value = "flowId", required = false) Integer flowId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        try {
            PageList<InterfaceWorkflowNodeDO> nodes = interfaceFlowNodeService.getAllNodes(flowId, pageNum, pageSize);

            return ResponseModelUtils.render(nodes);
        } catch (Exception e) {
            log.error("获取工作流节点失败", e);
            return ResponseModelUtils.error("获取工作流节点失败: " + e.getMessage());
        }
    }

    @GetMapping("/all-unpaged")
    public ResultData<List<InterfaceWorkflowNodeDO>> getAllNodesWithoutPaged() {
        try {
            List<InterfaceWorkflowNodeDO> nodes = interfaceFlowNodeService.getAllNodesWithoutPaged();

            return ResponseModelUtils.render(nodes);
        } catch (Exception e) {
            log.error("获取工作流节点失败", e);
            return ResponseModelUtils.error("获取工作流节点失败: " + e.getMessage());
        }
    }



    /**
     * 创建工作流节点
     */
    @PostMapping("/create")
    public ResultData<Integer> createNode(@RequestBody WorkflowNodeCreateReq node) {
        try {
            int savedNode = interfaceFlowNodeService.save(node);
            return ResponseModelUtils.render(savedNode);
        } catch (Exception e) {
            log.error("创建工作流节点失败", e);
            return ResponseModelUtils.error("创建工作流节点失败: " + e.getMessage());
        }
    }

    /**
     * 根据节点ID获取工作流节点
     */
    @GetMapping("/{nodeId}")
    public ResultData<InterfaceWorkflowNodeDO> getNodeById(@PathVariable Integer nodeId) {
        try {
            InterfaceWorkflowNodeDO node = interfaceFlowNodeService.selectByNodeId(nodeId);
            if (node == null) {
                return ResponseModelUtils.error("工作流节点不存在");
            }
            return ResponseModelUtils.render(node);
        } catch (Exception e) {
            log.error("获取工作流节点失败", e);
            return ResponseModelUtils.error("获取工作流节点失败: " + e.getMessage());
        }
    }

    /**
     * 更新工作流节点
     */
    @PostMapping("/update")
    public ResultData<Integer> updateNode(@RequestBody InterfaceWorkflowNodeDO node) {
        try {
            int result = interfaceFlowNodeService.update(node);
            return ResponseModelUtils.render(result);
        } catch (Exception e) {
            log.error("更新工作流节点失败", e);
            return ResponseModelUtils.error("更新工作流节点失败: " + e.getMessage());
        }
    }

    /**
     * 删除工作流节点
     */
    @DeleteMapping("/{nodeId}")
    public ResultData<String> deleteNode(@PathVariable Integer nodeId) {
        try {
            interfaceFlowNodeService.deleteByNodeId(nodeId);
            return ResponseModelUtils.render("工作流节点删除成功");
        } catch (Exception e) {
            log.error("删除工作流节点失败", e);
            return ResponseModelUtils.error("删除工作流节点失败: " + e.getMessage());
        }
    }

    @GetMapping("/export/{nodeId}")
    public ResultData<NodeParamConfigExportResponse> exportNodeParamConfig(@PathVariable Integer nodeId) {
        try {
            NodeParamConfigExportResponse nodeParamConfigExportResponse = interfaceFlowNodeService.exportNodeParamConfig(nodeId);

            return ResponseModelUtils.render(nodeParamConfigExportResponse);
        } catch (Exception e) {
            log.error("获取工作流节点失败", e);
            return ResponseModelUtils.error("获取工作流节点失败: " + e.getMessage());
        }
    }

    /**
     * 导入节点参数配置
     */
    @PostMapping("/import")
    public ResultData<Void> importNodeParamConfig(
            @RequestParam("nodeId") Integer nodeId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (nodeId == null) {
                return ResponseModelUtils.error("nodeId不能为空");
            }

            if (file == null || file.isEmpty()) {
                return ResponseModelUtils.error("文件不能为空");
            }

            nodeParamConfigService.importNodeParamConfig(nodeId, file);
            return ResponseModelUtils.success();
        } catch (Exception e) {
            log.error("导入节点参数配置失败", e);
            return ResponseModelUtils.error("导入节点参数配置失败: " + e.getMessage());
        }
    }
}
