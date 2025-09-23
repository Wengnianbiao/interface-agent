package com.helianhealth.agent.controller.log;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.model.domain.InterfaceInvokeLogDO;
import com.helianhealth.agent.service.InterfaceInvokeLogService;
import com.helianhealth.agent.utils.ResponseModelUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/console/api-log")
@Slf4j
@AllArgsConstructor
public class ApiLogController {

    private final InterfaceInvokeLogService interfaceInvokeLogService;

    /**
     * 获取所有节点参数配置
     */
    @GetMapping("/all")
    public ResultData<PageList<InterfaceInvokeLogDO>> findAllInvokeLogs(
            @RequestParam(value = "nodeId", required = false) Integer nodeId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        try {
            PageList<InterfaceInvokeLogDO> pageList = interfaceInvokeLogService.findAllInvokeLogs(nodeId, pageNum, pageSize);
            return ResponseModelUtils.render(pageList);
        } catch (Exception e) {
            log.error("获取节点参数配置列表失败", e);
            return ResponseModelUtils.error("获取节点参数配置列表失败: " + e.getMessage());
        }
    }
}
