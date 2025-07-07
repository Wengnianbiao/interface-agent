package com.helianhealth.agent.utils;

import lombok.Data;

@Data
public class PatientInfoHis {
    /**
     * 体检编号
     */
    private String patientCode;

    /**
     * 病人 ID
     */
    private String patientId;

    /**
     * 门诊号
     */
    private String clinicId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别
     */
    private String sex;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 出生日期
     */
    private String birthDate;

    /**
     * 证件类型
     */
    private String cardType;

    /**
     * 证件编号
     */
    private String cardId;

    /**
     * 家庭地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 婚姻状况
     */
    private String marital;

    /**
     * 民族
     */
    private String nation;

    /**
     * 国籍
     */
    private String country;

    /**
     * 学历
     */
    private String education;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String contactPhone;

    /**
     * 联系人地址
     */
    private String contactAddress;

    /**
     * 合约单位 ID
     */
    private String orgId;

    /**
     * 合约单位名称
     */
    private String orgName;
}

