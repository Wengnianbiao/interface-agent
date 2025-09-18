package com.helianhealth.agent.mapper.hems;

import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

/**
 * 体软通用查询方法
 * agent只查体软表不做其他以外的操作
 */
public interface HemsMapper {
    /**
     * 根据就诊编号查询患者编号
     * @param visitNo 就诊编号
     * @return 患者编号
     */
    @Select("SELECT PatientCode as patientCode " +
            "FROM VocaPatient " +
            "WHERE VisitNo = #{visitNo}")
    String selectPatientCodeByVisitNo(String visitNo);
}
