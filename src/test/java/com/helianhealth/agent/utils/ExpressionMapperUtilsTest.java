package com.helianhealth.agent.utils;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class ExpressionMapperUtilsTest {

    @Test
    public void testBasicExpressionParsing() {
        // 测试基本表达式解析
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", "张三");
        businessData.put("age", 25);

        // 基本属性访问
        Object result1 = ExpressionMapperUtils.parser("#data[name]", businessData,  null);
        assertEquals("张三", result1);

        // 数值比较
        Object result2 = ExpressionMapperUtils.parser("#data[age] > 18", businessData, null);
        assertEquals(true, result2);
    }

    @Test
    public void testPaybackExpressionParsing() {
        String xml = "<HrRequest>\n" +
                "\t<Head>\n" +
                "\t\t<TradeCode>PHY102</TradeCode>\n" +
                "\t\t<TradeTime>2025-09-09 10:22:36</TradeTime>\n" +
                "\t\t<TradeNo>lw250909022236570167</TradeNo>\n" +
                "\t\t<BranchCode/>\n" +
                "\t\t<HospitalCode/>\n" +
                "\t\t<SystemCode>JCPT</SystemCode>\n" +
                "\t\t<HipSessionId>8617292</HipSessionId>\n" +
                "\t</Head>\n" +
                "\t<Body>\n" +
                "\t\t<BodyList>\n" +
                "\t\t\t<TjSerialNo>20250909002</TjSerialNo>\n" +
                "\t\t\t<operationName>超级管理员</operationName>\n" +
                "\t\t\t<operationCode>ADMIN</operationCode>\n" +
                "\t\t\t<time>2025-09-09 10:22:32</time>\n" +
                "\t\t\t<patientId>030001377</patientId>\n" +
                "\t\t\t<visitNo>20250909000401</visitNo>\n" +
                "\t\t\t<rcptNo>20250909000000000001</rcptNo>\n" +
                "\t\t\t<popFlay>1</popFlay>\n" +
                "\t\t\t<Items>\n" +
                "\t\t\t\t<itemsList>\n" +
                "\t\t\t\t\t<itemCode>10020</itemCode>\n" +
                "\t\t\t\t\t<itemName>10020</itemName>\n" +
                "\t\t\t\t\t<money>188</money>\n" +
                "\t\t\t\t\t<externalId>20250909110</externalId>\n" +
                "\t\t\t\t\t<peClinicItemCode/>\n" +
                "\t\t\t\t</itemsList>\n" +
                "                <itemsList>\n" +
                "\t\t\t\t\t<itemCode>10020</itemCode>\n" +
                "\t\t\t\t\t<itemName>10020</itemName>\n" +
                "\t\t\t\t\t<money>188</money>\n" +
                "\t\t\t\t\t<externalId>20250909110</externalId>\n" +
                "\t\t\t\t\t<peClinicItemCode/>\n" +
                "\t\t\t\t</itemsList>\n" +
                "\t\t\t</Items>\n" +
                "\t\t</BodyList>\n" +
                "\t</Body>\n" +
                "</HrRequest>";
        Map<String, Object> stringObjectMap = XmlUtils.xmlToMap(xml);
        Object parser = ExpressionMapperUtils.businessDataFilter("#data[Body][BodyList][popFlay] == '2' ? #data : #data.clear()", stringObjectMap);
        System.out.println(JSON.toJSONString(parser));
    }

    @Test
    public void testApplyExpressionParsing() {

        String json = "{\n" +
                "  \"ServiceProviderType\" : \"LIS\",\n" +
                "  \"QueryType\" : \"4\",\n" +
                "  \"AuditTime\" : \"2025-09-16 15:19:39\",\n" +
                "  \"BusinessMethod\" : \"GetItemResult\",\n" +
                "  \"BusinessCode\" : \"2508190082\",\n" +
                "  \"DepartType\" : null\n" +
                "}";
        Map<String, Object> stringObjectMap = JSON.parseObject(json);
        Object parser = ExpressionMapperUtils.businessDataFilter("#data[ServiceProviderType] == 'PACS' ? #data :  #data.clear()", stringObjectMap);


    }

    @Test
    public void testCardTypeExpressionParsing() {
        // 测试基本表达式解析
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("cardType", "1");

        // 基本属性访问
        Object result1 = ExpressionMapperUtils.parser("(#data[cardType] == null || #data[cardType] == '') ? #data[cardType] : (#data[cardType] == '2' ? '7' : (#data[cardType] == '3' ? '4' : (#data[cardType] == '4' ? '6' : (#data[cardType] == '5' ? '3' : #data[cardType]))))", businessData,  null);
        System.out.println(result1);
    }

    @Test
    public void testResponseExpressionParsing() {
        // 测试基本表达式解析
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("code", "-1");

        // 基本属性访问
        Object result1 = ExpressionMapperUtils.parser("#data['code'] == \"-1\" ? T(java.util.Arrays).asList(1L) : null", businessData, null);
        System.out.println(result1);
    }

    @Test
    public void testRootExpressionParsing() {
        // 测试基本表达式解析
        Map<String, Object> businessData = new HashMap<>();

        Map<String, Object> chagPatientSheet = new HashMap<>();
        chagPatientSheet.put("idPatientSheet", "123445");
        Map<String, Object> doctorInfo = new HashMap<>();
        doctorInfo.put("idUser", "222");
        businessData.put("chagPatientSheet", chagPatientSheet);
        businessData.put("doctorInfo", doctorInfo);

        System.out.println(JSON.toJSONString(businessData));

        // 基本属性访问
        Object result1 = ExpressionMapperUtils.parser("#source[chagPatientSheet][idPatientSheet]", businessData, businessData);
        System.out.println(result1);
    }

    @Test
    public void testArrayToObjectExpressionParsing() {
        // 测试基本表达式解析
        List<PatientInfoHis> patientInfoList = new ArrayList<>();
        PatientInfoHis patientInfo = new PatientInfoHis();
        patientInfo.setPatientId("123445");
        patientInfoList.add(patientInfo);

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("data", patientInfoList);
        businessData.put("chargeType", "CHARGED");
        System.out.println(JSON.toJSONString(businessData));

        // 基本属性访问
        String expression = "#data[data][0].patientId";
        Object result1 = ExpressionMapperUtils.parser(expression, businessData, null);
        System.out.println(result1);
//        assertEquals("张三", result1);
//
//        // 数值比较
//        Object result2 = ExpressionMapperUtils.parser("#data[age] > 18", businessData);
//        assertEquals(true, result2);
    }

    @Test
    public void testApplyFilterExpressionParsing() throws JsonProcessingException {
        // jarvis申请单入参,2个PACS项目和1个LIS项目
        // 过滤出各自的项目
        String jsonStr = "{\n" +
                "  \"data\" : {\n" +
                "    \"items\" : [ {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 0,\n" +
                "      \"departType\" : \"OTHER\",\n" +
                "      \"interfaceCode\" : \"TJ00004\",\n" +
                "      \"idSampleType\" : null,\n" +
                "      \"idDepart\" : 8,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : null,\n" +
                "      \"sampleName\" : null,\n" +
                "      \"sampleCode\" : null,\n" +
                "      \"feeItemName\" : \"眼科检查\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 1500.00,\n" +
                "      \"priceYuan\" : 15.00,\n" +
                "      \"toPayPrice\" : 1500.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 1500.00,\n" +
                "      \"originalPriceYuan\" : 15.00,\n" +
                "      \"factPrice\" : 1500.00,\n" +
                "      \"factPriceYuan\" : 15.00,\n" +
                "      \"idPatientFeeItem\" : \"158311\",\n" +
                "      \"idFeeItem\" : \"617\",\n" +
                "      \"applyId\" : \"80158311\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"眼科\",\n" +
                "      \"ddInterfaceCode1\" : \"1058\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"TJ00004\",\n" +
                "      \"dfiInterfaceCode2\" : \"\",\n" +
                "      \"dfiInterfaceCode3\" : \"\",\n" +
                "      \"dfiInterfaceCode4\" : \"\",\n" +
                "      \"dfiInterfaceCode5\" : \"\",\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    }, {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 0,\n" +
                "      \"departType\" : \"OTHER\",\n" +
                "      \"interfaceCode\" : \"TJ00002\",\n" +
                "      \"idSampleType\" : null,\n" +
                "      \"idDepart\" : 6,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : null,\n" +
                "      \"sampleName\" : null,\n" +
                "      \"sampleCode\" : null,\n" +
                "      \"feeItemName\" : \"内科检查\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 500.00,\n" +
                "      \"priceYuan\" : 5.00,\n" +
                "      \"toPayPrice\" : 500.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 500.00,\n" +
                "      \"originalPriceYuan\" : 5.00,\n" +
                "      \"factPrice\" : 500.00,\n" +
                "      \"factPriceYuan\" : 5.00,\n" +
                "      \"idPatientFeeItem\" : \"158312\",\n" +
                "      \"idFeeItem\" : \"9\",\n" +
                "      \"applyId\" : \"80158312\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"内科\",\n" +
                "      \"ddInterfaceCode1\" : \"1058\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"TJ00002\",\n" +
                "      \"dfiInterfaceCode2\" : null,\n" +
                "      \"dfiInterfaceCode3\" : null,\n" +
                "      \"dfiInterfaceCode4\" : null,\n" +
                "      \"dfiInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    }, {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 0,\n" +
                "      \"departType\" : \"LIS\",\n" +
                "      \"interfaceCode\" : \"51\",\n" +
                "      \"idSampleType\" : 138,\n" +
                "      \"idDepart\" : 19,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : \"1\",\n" +
                "      \"sampleName\" : \"血液\",\n" +
                "      \"sampleCode\" : \"1012\",\n" +
                "      \"feeItemName\" : \"血常规（五分类）\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 1360.00,\n" +
                "      \"priceYuan\" : 13.60,\n" +
                "      \"toPayPrice\" : 1360.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 1360.00,\n" +
                "      \"originalPriceYuan\" : 13.60,\n" +
                "      \"factPrice\" : 1360.00,\n" +
                "      \"factPriceYuan\" : 13.60,\n" +
                "      \"idPatientFeeItem\" : \"158313\",\n" +
                "      \"idFeeItem\" : \"1151\",\n" +
                "      \"applyId\" : \"80158313\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"检验科\",\n" +
                "      \"ddInterfaceCode1\" : \"1062\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"51\",\n" +
                "      \"dfiInterfaceCode2\" : null,\n" +
                "      \"dfiInterfaceCode3\" : null,\n" +
                "      \"dfiInterfaceCode4\" : null,\n" +
                "      \"dfiInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    }, {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 0,\n" +
                "      \"departType\" : \"PACS\",\n" +
                "      \"interfaceCode\" : \"1237\",\n" +
                "      \"idSampleType\" : null,\n" +
                "      \"idDepart\" : 67,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : null,\n" +
                "      \"sampleName\" : null,\n" +
                "      \"sampleCode\" : null,\n" +
                "      \"feeItemName\" : \"常规心电图检查\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 2000.00,\n" +
                "      \"priceYuan\" : 20.00,\n" +
                "      \"toPayPrice\" : 2000.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 2000.00,\n" +
                "      \"originalPriceYuan\" : 20.00,\n" +
                "      \"factPrice\" : 2000.00,\n" +
                "      \"factPriceYuan\" : 20.00,\n" +
                "      \"idPatientFeeItem\" : \"158314\",\n" +
                "      \"idFeeItem\" : \"744\",\n" +
                "      \"applyId\" : \"80158314\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"心电图室\",\n" +
                "      \"ddInterfaceCode1\" : \"1058\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"1237\",\n" +
                "      \"dfiInterfaceCode2\" : \"\",\n" +
                "      \"dfiInterfaceCode3\" : \"\",\n" +
                "      \"dfiInterfaceCode4\" : \"\",\n" +
                "      \"dfiInterfaceCode5\" : \"\",\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    }, {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 0,\n" +
                "      \"departType\" : \"PACS\",\n" +
                "      \"interfaceCode\" : \"1258\",\n" +
                "      \"idSampleType\" : null,\n" +
                "      \"idDepart\" : 18,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : null,\n" +
                "      \"sampleName\" : null,\n" +
                "      \"sampleCode\" : null,\n" +
                "      \"feeItemName\" : \"胸部后前位\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 3900.00,\n" +
                "      \"priceYuan\" : 39.00,\n" +
                "      \"toPayPrice\" : 3900.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 3900.00,\n" +
                "      \"originalPriceYuan\" : 39.00,\n" +
                "      \"factPrice\" : 3900.00,\n" +
                "      \"factPriceYuan\" : 39.00,\n" +
                "      \"idPatientFeeItem\" : \"158315\",\n" +
                "      \"idFeeItem\" : \"907\",\n" +
                "      \"applyId\" : \"80158315\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"放射科DR\",\n" +
                "      \"ddInterfaceCode1\" : \"1063\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"1258\",\n" +
                "      \"dfiInterfaceCode2\" : null,\n" +
                "      \"dfiInterfaceCode3\" : null,\n" +
                "      \"dfiInterfaceCode4\" : null,\n" +
                "      \"dfiInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    }, {\n" +
                "      \"hisDetailId\" : null,\n" +
                "      \"presId\" : \"595322\",\n" +
                "      \"isConsum\" : 1,\n" +
                "      \"departType\" : \"OUTSEND\",\n" +
                "      \"interfaceCode\" : \"14020090500\",\n" +
                "      \"idSampleType\" : null,\n" +
                "      \"idDepart\" : 26,\n" +
                "      \"isFeeType\" : \"0\",\n" +
                "      \"sampleType\" : null,\n" +
                "      \"sampleName\" : null,\n" +
                "      \"sampleCode\" : null,\n" +
                "      \"feeItemName\" : \"计算机图文报告费\",\n" +
                "      \"discount\" : \"1.00000\",\n" +
                "      \"price\" : 800.00,\n" +
                "      \"priceYuan\" : 8.00,\n" +
                "      \"toPayPrice\" : 800.00,\n" +
                "      \"isFeeState\" : 0,\n" +
                "      \"originalPrice\" : 800.00,\n" +
                "      \"originalPriceYuan\" : 8.00,\n" +
                "      \"factPrice\" : 800.00,\n" +
                "      \"factPriceYuan\" : 8.00,\n" +
                "      \"idPatientFeeItem\" : \"158316\",\n" +
                "      \"idFeeItem\" : \"1250\",\n" +
                "      \"applyId\" : \"80158316\",\n" +
                "      \"barCode\" : null,\n" +
                "      \"departName\" : \"材料费\",\n" +
                "      \"ddInterfaceCode1\" : \"1058\",\n" +
                "      \"ddInterfaceCode2\" : null,\n" +
                "      \"ddInterfaceCode3\" : null,\n" +
                "      \"ddInterfaceCode4\" : null,\n" +
                "      \"ddInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode1\" : \"14020090500\",\n" +
                "      \"dfiInterfaceCode2\" : null,\n" +
                "      \"dfiInterfaceCode3\" : null,\n" +
                "      \"dfiInterfaceCode4\" : null,\n" +
                "      \"dfiInterfaceCode5\" : null,\n" +
                "      \"dfiInterfaceCode6\" : null,\n" +
                "      \"dfiInterfaceCode7\" : null,\n" +
                "      \"dfiInterfaceCode8\" : null,\n" +
                "      \"dfiInterfaceCode9\" : null,\n" +
                "      \"dfiInterfaceCode10\" : null,\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"loginName\" : \"zqf0558\",\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"idRegister\" : 722\n" +
                "    } ],\n" +
                "    \"patientInfo\" : {\n" +
                "      \"sex\" : \"2\",\n" +
                "      \"vpInvoiceTitle\" : null,\n" +
                "      \"vpSocialCode\" : null,\n" +
                "      \"age\" : 54,\n" +
                "      \"marriage\" : \"3\",\n" +
                "      \"identitycard\" : \"330502197101062226\",\n" +
                "      \"cardType\" : \"1\",\n" +
                "      \"phone\" : \"18367276320\",\n" +
                "      \"birthday\" : \"1971-01-06\",\n" +
                "      \"hisOrgId\" : null,\n" +
                "      \"address\" : \"浙江省湖州市吴兴区滨湖街道大钱村唐家浒２０号\",\n" +
                "      \"patientName\" : \"蔡培勤\",\n" +
                "      \"patientCode\" : \"2504280032\",\n" +
                "      \"hisPatientId\" : \"85184\",\n" +
                "      \"visitNo\" : \"2504280032\",\n" +
                "      \"userType\" : 0,\n" +
                "      \"orgName\" : \"\",\n" +
                "      \"registrationExtend1\" : \"330502197101062226\",\n" +
                "      \"registrationExtend2\" : \"2025007226\",\n" +
                "      \"registrationExtend3\" : null,\n" +
                "      \"registertime\" : \"2025-04-28 08:00:55\",\n" +
                "      \"physicalTypes\" : \"physical:types:O\",\n" +
                "      \"physicalTypeName\" : \"入职体检\"\n" +
                "    },\n" +
                "    \"doctorInfo\" : {\n" +
                "      \"idUser\" : 722,\n" +
                "      \"userName\" : \"朱秋芳\",\n" +
                "      \"number\" : \"zqf0558\",\n" +
                "      \"departList\" : [ {\n" +
                "        \"departName\" : \"一般检查\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"内科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"外科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"眼科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"耳鼻咽喉科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"口腔科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"妇科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"冠脉积分检查\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"心电图\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"心脏彩色超声\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"彩超室\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1065\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"经颅多普勒\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"骨密度\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"放射科DR\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"检验科\",\n" +
                "        \"departType\" : \"LIS\",\n" +
                "        \"departCode\" : \"1062\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"碳13\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"四肢动脉硬化\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"电测听\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"超薄细胞检测(TCT)\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"肺功能\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"病理科\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"100204\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"材料费\",\n" +
                "        \"departType\" : \"OUTSEND\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"产科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"妇科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"肛肠外科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"口腔科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"泌尿外科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"泌尿外科门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"内镜室\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1077\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"健康管理中心\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"儿科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"多功能检查室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"内二科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"外二科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"肺功能室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"临床营养科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"检验科外检\",\n" +
                "        \"departType\" : \"OUTSEND\",\n" +
                "        \"departCode\" : \"1062\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"腹部彩超\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"人体成分分析\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"100134\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"电子阴道镜\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"纯音测听\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"放射科MRI\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"放射科CT\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"呼吸科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"门诊内镜\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"脑电图室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"100105\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"乳腺彩超\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"舒适门诊\",\n" +
                "        \"departType\" : \"OUTSEND\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"胃肠肛肠外科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"胃肠镜\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"胃镜检查\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"胃镜室药品\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"胃镜舒适门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"心理门诊\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"动脉硬化检查室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"耳鼻喉科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"碳14呼气试验\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"营养科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"心电图室\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"消化科\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : null\n" +
                "      }, {\n" +
                "        \"departName\" : \"剪切波检查室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"呼气试验室\",\n" +
                "        \"departType\" : \"OTHER\",\n" +
                "        \"departCode\" : \"1058\"\n" +
                "      }, {\n" +
                "        \"departName\" : \"放射科MG\",\n" +
                "        \"departType\" : \"PACS\",\n" +
                "        \"departCode\" : \"1063\"\n" +
                "      } ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"BusinessMethod\" : \"SyncPacsLisNewApply\",\n" +
                "  \"BusinessCode\" : \"2504280032\",\n" +
                "  \"DepartType\" : null\n" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> dataMap = (Map<String, Object>) objectMapper.readValue(
                jsonStr,
                new TypeReference<Map<String, Object>>() {}
        ).get("data");
        System.out.println(JSON.toJSONString(dataMap));
        // 1、过滤出LIS项目，是1
        String lisSpEL = "(#tempItems = #data[items]?.?[[departType] == 'LIS']) == null || #tempItems.isEmpty() ? #data.clear() : #data.put('items', #tempItems)";

        Map<String, Object> lisMap = ExpressionMapperUtils.businessDataFilter(lisSpEL, dataMap);
        assertNotNull(lisMap);
        System.out.println(JSON.toJSONString(lisMap));
        List<Map<String, Object>> lisItems = (List<Map<String, Object>>) lisMap.get("items");
        assertEquals(1, lisItems.stream()
                .filter(item -> "LIS".equals(item.get("departType")))
                .count());

        //  2、过滤出PACS项目，是2
        String pacsSpEL = "(#tempItems = #data[items]?.?[[departType] == 'PACS']) == null || #tempItems.isEmpty() ? #data.clear() : #data.put('items', #tempItems)";
        Map<String, Object> pacsMap = ExpressionMapperUtils.businessDataFilter(pacsSpEL, dataMap);
        assertNotNull(pacsMap);
        System.out.println(JSON.toJSONString(pacsMap
        ));
        List<Map<String, Object>> pacsItems = (List<Map<String, Object>>) pacsMap.get("items");
        assertEquals(2, pacsItems.stream()
                .filter(item -> "PACS".equals(item.get("departType")))
                .count());

        // 3、过滤出ECG项目，是空
        String ecgSpEL = "(#tempItems = #data[items]?.?[[departType] == 'ECG']) == null || #tempItems.isEmpty() ? #data.clear() : #data.put('items', #tempItems)";
        Map<String, Object> ecgMap = ExpressionMapperUtils.businessDataFilter(ecgSpEL, dataMap);
        assertNotNull(ecgMap);
        System.out.println(JSON.toJSONString(ecgMap
        ));
        List<Map<String, Object>> ecgItems = (List<Map<String, Object>>) ecgMap.get("items");

        assertNull(ecgItems);

        // 4、过滤同时保留PACS和LIS,应为3个
        String pacsLisPacsSpEL = "(#tempItems = #data[items]?.?[([departType] == 'PACS' || [departType] == 'LIS')]) == null || #tempItems.isEmpty() ? #data.clear() : #data.put('items', #tempItems)";
        Map<String, Object> pacsLisMap = ExpressionMapperUtils.businessDataFilter(pacsLisPacsSpEL, dataMap);
        assertNotNull(pacsLisMap);
        System.out.println(JSON.toJSONString(pacsLisMap));
        List<Map<String, Object>> pacsLisItems = (List<Map<String, Object>>) pacsLisMap.get("items");
        assertEquals(3, pacsLisItems.stream()
                .filter(item -> "PACS".equals(item.get("departType")) || "LIS".equals(item.get("departType")))
                .count());
    }

    @Test
    public void testCurrentTimeFormatting() {
        String express = "T(java.time.LocalDateTime).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM-dd HH:mm:ss'))";
        Object result = ExpressionMapperUtils.parser(express, null, null);
        // 验证结果不为null
        assertNotNull(result);

        // 验证结果为字符串类型
        assertInstanceOf(String.class, result);
        String timeStr = (String) result;

        String EXPECTED_FORMAT = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(EXPECTED_FORMAT);
        // 验证格式正确性
        try {
            LocalDateTime.parse(timeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            fail("时间格式不符合预期: " + EXPECTED_FORMAT + "，实际结果: " + timeStr + "，错误信息: " + e.getMessage());
        }
    }

}
