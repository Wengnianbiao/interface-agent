package com.helianhealth.agent.utils;

import com.alibaba.fastjson2.JSONObject;
import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.Assert.*;

@SpringBootTest
public class JsonUtilsTest {

    @Test
    public void testToJsonString() {
        // 测试正常Map转换
        Map<String, String> map = new HashMap<>();
        map.put("name", "张三");
        map.put("age", "18");
        map.put("city", "北京");

        String result = JsonUtils.toJsonString(map);
        System.out.println(result);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"张三\""));
        assertTrue(result.contains("\"age\":\"18\""));
        assertTrue(result.contains("\"city\":\"北京\""));

        // 测试空Map
        Map<String, String> emptyMap = new HashMap<>();
        String emptyResult = JsonUtils.toJsonString(emptyMap);
        System.out.println(emptyResult);
        assertEquals("{}", emptyResult);

        // 测试null值
        Map<String, String> nullMap = null;
        String mapJsonString = JsonUtils.toJsonString(nullMap);

        String hello = JSONObject.toJSONString("hello");
        System.out.println(hello);
//        assertThrows(BaseBusinessException.class, () -> );
    }

    @Test
    public void testFromJsonStringToMap() {
        // 测试正常JSON字符串转换
        String jsonString = "{\"name\":\"张三\",\"age\":\"18\",\"city\":\"北京\"}";
        Map<String, String> result = JsonUtils.fromJsonStringToMap(jsonString);

        assertNotNull(result);
        assertEquals("张三", result.get("name"));
        assertEquals("18", result.get("age"));
        assertEquals("北京", result.get("city"));

        // 测试空JSON
        String emptyJson = "{}";
        Map<String, String> emptyResult = JsonUtils.fromJsonStringToMap(emptyJson);
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        // 测试无效JSON
        assertThrows(RuntimeException.class, () -> {
            JsonUtils.fromJsonStringToMap("invalid json");
        });

        // 测试null输入
        assertThrows(RuntimeException.class, () -> {
            JsonUtils.fromJsonStringToMap(null);
        });
    }

    @Test
    public void testToMap_FromMap() {
        // 测试从Map转换
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("name", "张三");
        originalMap.put("age", 18);
        originalMap.put("active", true);

        Map<String, Object> result = JsonUtils.toMap(originalMap);
        System.out.println(result);
        assertSame(originalMap, result); // 应该返回同一个对象引用

        assertEquals("张三", result.get("name"));
        assertEquals(18, result.get("age"));
        assertEquals(true, result.get("active"));
    }

    @Test
    public void testToMap_FromJSONObject() {
        // 测试从JSONObject转换
        Map<String, Object> jsonObject = new JSONObject();
        jsonObject.put("name", "李四");
        jsonObject.put("age", 25);
        jsonObject.put("scores", Arrays.asList("90", 85, 95));
        String jsonString = JsonUtils.toJsonString(jsonObject);
        System.out.println(jsonString);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "李四");
        map.put("age", 25);
        map.put("scores", Arrays.asList(90, 85, 95));
        String jsonString1 = JsonUtils.toJsonString(map);
        System.out.println(jsonString1);
    }
}
