package com.helianhealth.agent.utils;

import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ParamNodeUtilsTest {

    @Test
    public void testParamNodeDTO2Xml() {

        // 组装code标签
        ParamTreeNode value = ParamTreeNode.builder()
                .paramKey("value")
                .paramValue("血清")
                .paramType(ParamType.STRING)
                .build();
        List<ParamTreeNode> valueList = new ArrayList<>();
        valueList.add(value);
        ParamTreeNode displayName = ParamTreeNode.builder()
                .paramKey("displayName")
                .paramValue(null)
                .paramType(ParamType.OBJECT)
                .attributeNodes(valueList)
                .build();

        List<ParamTreeNode> displayNameList = new ArrayList<>();
        displayNameList.add(displayName);

        ParamTreeNode codeAttribute = ParamTreeNode.builder()
                .paramKey("code")
                .paramValue("xq")
                .paramType(ParamType.STRING)
                .build();
        List<ParamTreeNode> codeAttributeList = new ArrayList<>();
        codeAttributeList.add(codeAttribute);

        // code标签finished
        ParamTreeNode codeElement = ParamTreeNode.builder()
                .paramKey("code")
                .paramType(ParamType.OBJECT)
                .attributeNodes(codeAttributeList)
                .children(displayNameList)
                .build();

        // 组装id标签体
        ParamTreeNode rootAttribute = ParamTreeNode.builder()
                .paramKey("root")
                .paramValue("2.16.156.10011.1.14")
                .paramType(ParamType.STRING)
                .build();
        ParamTreeNode extensionAttribute = ParamTreeNode.builder()
                .paramKey("extension")
                .paramValue("800000218793")
                .paramType(ParamType.STRING)
                .build();
        List<ParamTreeNode> idAttributeList = new ArrayList<>();
        idAttributeList.add(rootAttribute);
        idAttributeList.add(extensionAttribute);

        // id标签finished
        ParamTreeNode idElement = ParamTreeNode.builder()
                .paramKey("id")
                .paramType(ParamType.OBJECT)
                .attributeNodes(idAttributeList)
                .build();

        ParamTreeNode classCodeAttribute = ParamTreeNode.builder()
                .paramKey("classCode")
                .paramValue("SPEC")
                .paramType(ParamType.STRING)
                .attributeNodes(idAttributeList)
                .build();
        List<ParamTreeNode> specimenSonAttributeList = new ArrayList<>();
        specimenSonAttributeList.add(classCodeAttribute);

        List<ParamTreeNode> specimenSonChildList = new ArrayList<>();
        specimenSonChildList.add(idElement);
        specimenSonChildList.add(codeElement);
        ParamTreeNode specimenSonElement = ParamTreeNode.builder()
                .paramKey("specimen")
                .paramValue(null)
                .attributeNodes(specimenSonAttributeList)
                .paramType(ParamType.OBJECT)
                .children(specimenSonChildList)
                .build();

        List<ParamTreeNode> specimenFatherChildList = new ArrayList<>();
        specimenFatherChildList.add(specimenSonElement);
        ParamTreeNode specimenFatherElement = ParamTreeNode.builder()
                .paramKey("specimen")
                .paramValue(null)
                .paramType(ParamType.OBJECT)
                .children(specimenFatherChildList)
                .build();

        String s = ParamNodeUtils.paramNodeDTO2Xml(specimenFatherElement);
        System.out.println(s);
    }
}
