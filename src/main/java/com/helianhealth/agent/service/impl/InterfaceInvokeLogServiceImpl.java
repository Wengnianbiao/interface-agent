package com.helianhealth.agent.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.mapper.agent.InterfaceInvokeLogMapper;
import com.helianhealth.agent.model.domain.InterfaceInvokeLogDO;
import com.helianhealth.agent.service.InterfaceInvokeLogService;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterfaceInvokeLogServiceImpl implements InterfaceInvokeLogService {

    private final InterfaceInvokeLogMapper interfaceInvokeLogMapper;

    @Override
    public void saveInvokeLog(Integer nodeId,
                              String businessData,
                              String paramBeforeInvoke,
                              String remoteInvokeResponse,
                              String paramAfterInvoke,
                              String invokeTime) {
        try {
            InterfaceInvokeLogDO logDO = InterfaceInvokeLogDO.builder()
                    .nodeId(nodeId)
                    .businessData(businessData)
                    .paramBeforeInvoke(paramBeforeInvoke)
                    .remoteInvokeResponse(remoteInvokeResponse)
                    .paramAfterInvoke(paramAfterInvoke)
                    .invokeTime(invokeTime)
                    .createTime(new Date())
                    .build();

            interfaceInvokeLogMapper.insertLog(logDO);
        } catch (Exception e) {
            log.error("保存接口调用日志失败", e);
        }
    }

    @Override
    public PageList<InterfaceInvokeLogDO> findAllInvokeLogs(Integer nodeId, int pageNum, int pageSize) {
        try {
            PageHelper.startPage(pageNum, pageSize);
            // 将查询结果转换为 PageInfo 对象
            List<InterfaceInvokeLogDO> invokeLogs = interfaceInvokeLogMapper.selectAllInvokeLogs(nodeId);
            PageInfo<InterfaceInvokeLogDO> pageInfo = new PageInfo<>(invokeLogs);

            // 构建 PageList 对象
            PageList<InterfaceInvokeLogDO> pageList = new PageList<>();
            pageList.setRows(invokeLogs);
            pageList.setTotal(pageInfo.getTotal());
            pageList.setPageNum(pageInfo.getPageNum());
            pageList.setPageSize(pageInfo.getPageSize());
            return pageList;
        } catch (Exception e) {
            log.error("获取接口调用日志列表失败", e);
            throw new RuntimeException("获取接口调用日志列表失败: " + e.getMessage());
        }
    }
}
