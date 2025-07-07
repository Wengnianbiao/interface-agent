package com.helianhealth.agent.quartz.mapper;

import com.helianhealth.agent.quartz.model.dto.WorkflowTaskDTO;
import com.helianhealth.agent.quartz.model.entity.WorkflowTaskDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface WorkflowTaskMapper extends Mapper<WorkflowTaskDO> {

    List<WorkflowTaskDTO> selectAllTask();

    @Update("update workflow_task set latest_execute_time = #{latestExecuteTime} where id = #{id}")
    void updateLatestExecuteTimeById(@Param("id") Integer id, @Param("latestExecuteTime") String latestExecuteTime);
}
