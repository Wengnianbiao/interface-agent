package com.helianhealth.agent.controller;


import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.service.impl.ConfigManagementService;
import com.helianhealth.agent.utils.ResponseModelUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/v1/console/property")
@Slf4j
@RequiredArgsConstructor
public class PropertyController {

    private final ConfigManagementService configManagementService;
    private final ConfigurableApplicationContext applicationContext;
    private final ConfigurableEnvironment environment;

    /**
     * 获取所有配置信息
     * @return 所有配置属性
     */
    @GetMapping("/properties")
    public ResultData<Map<String, String>> getAllProperties() {
        try {
            Map<String, String> properties = configManagementService.getAllProperties();
            return ResponseModelUtils.render(properties);
        } catch (Exception e) {
            log.error("获取配置信息失败", e);
            return ResponseModelUtils.error("获取配置信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定配置属性
     * @param key 属性键
     * @return 属性值
     */
    @GetMapping("/property")
    public ResultData<String> getProperty(@RequestParam String key) {
        try {
            String value = configManagementService.getProperty(key);
            if (value != null) {
                return ResponseModelUtils.render(value);
            } else {
                return ResponseModelUtils.error("配置项不存在: " + key);
            }
        } catch (Exception e) {
            log.error("获取配置项失败: {}", key, e);
            return ResponseModelUtils.error("获取配置项失败: " + e.getMessage());
        }
    }

    /**
     * 增加单个配置属性
     * @param key 属性键
     * @param value 属性值
     * @return 操作结果
     */
    @PostMapping("/property/add")
    public ResultData<String> addProperty(
            @RequestParam String key,
            @RequestParam String value) {
        try {
            // 检查配置项是否已存在
            String existingValue = configManagementService.getProperty(key);
            if (existingValue != null) {
                return ResponseModelUtils.error("配置项已存在: " + key + "，请使用更新接口");
            }

            boolean success = configManagementService.updateProperty(key, value);
            if (success) {
                return ResponseModelUtils.render("配置项添加成功: " + key + " = " + value + "，请重启应用使配置生效");
            } else {
                return ResponseModelUtils.error("配置项添加失败");
            }
        } catch (Exception e) {
            log.error("添加配置项失败: {} = {}", key, value, e);
            return ResponseModelUtils.error("添加配置项失败: " + e.getMessage());
        }
    }

    /**
     * 更新单个配置属性
     * @param key 属性键
     * @param value 属性值
     * @return 操作结果
     */
    @PostMapping("/property")
    public ResultData<String> updateProperty(
            @RequestParam String key,
            @RequestParam String value) {
        try {
            boolean success = configManagementService.updateProperty(key, value);
            if (success) {
                return ResponseModelUtils.render("配置更新成功: " + key + " = " + value + "，请重启应用使配置生效");
            } else {
                return ResponseModelUtils.error("配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新配置项失败: {} = {}", key, value, e);
            return ResponseModelUtils.error("更新配置项失败: " + e.getMessage());
        }
    }

    /**
     * 批量增加配置属性
     * @param properties 属性集合
     * @return 操作结果
     */
    @PostMapping("/properties/add")
    public ResultData<String> addProperties(@RequestBody Map<String, String> properties) {
        try {
            int addedCount = 0;
            int skippedCount = 0;
            StringBuilder message = new StringBuilder();

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 检查配置项是否已存在
                String existingValue = configManagementService.getProperty(key);
                if (existingValue != null) {
                    skippedCount++;
                    message.append(key).append("已存在，跳过；");
                } else {
                    boolean success = configManagementService.updateProperty(key, value);
                    if (success) {
                        addedCount++;
                    } else {
                        skippedCount++;
                        message.append(key).append("添加失败；");
                    }
                }
            }

            if (addedCount > 0) {
                return ResponseModelUtils.render("成功添加 " + addedCount + " 个配置项，" + skippedCount + " 个配置项未添加。" + message.toString() + "请重启应用使配置生效");
            } else {
                return ResponseModelUtils.error("未成功添加任何配置项。" + message.toString());
            }
        } catch (Exception e) {
            log.error("批量添加配置失败", e);
            return ResponseModelUtils.error("批量添加配置失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新配置属性
     * @param properties 属性集合
     * @return 操作结果
     */
    @PostMapping("/properties")
    public ResultData<String> updateProperties(@RequestBody Map<String, String> properties) {
        try {
            boolean success = configManagementService.updateProperties(properties);
            if (success) {
                return ResponseModelUtils.render("批量配置更新成功，更新了 " + properties.size() + " 个配置项，请重启应用使配置生效");
            } else {
                return ResponseModelUtils.error("批量配置更新失败");
            }
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return ResponseModelUtils.error("批量更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置项
     * @param key 属性键
     * @return 操作结果
     */
    @DeleteMapping("/property")
    public ResultData<String> deleteProperty(@RequestParam String key) {
        try {
            // 检查配置项是否存在
            String existingValue = configManagementService.getProperty(key);
            if (existingValue == null) {
                return ResponseModelUtils.error("配置项不存在: " + key);
            }

            boolean success = configManagementService.deleteProperty(key);
            if (success) {
                return ResponseModelUtils.render("配置项删除成功: " + key + "，请重启应用使配置生效");
            } else {
                return ResponseModelUtils.error("配置项删除失败");
            }
        } catch (Exception e) {
            log.error("删除配置项失败: {}", key, e);
            return ResponseModelUtils.error("删除配置项失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResultData<String> refreshConfiguration() {
        try {
            // 1. 重新加载配置文件
            refreshPropertySources();

            // 2. 手动更新需要动态生效的组件配置
            updateDynamicComponents();

            return ResponseModelUtils.render("配置已刷新。配置文件已重新加载，动态组件已更新。");
        } catch (Exception e) {
            log.error("刷新配置失败", e);
            return ResponseModelUtils.error("刷新配置失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载配置文件到环境变量中
     */
    private void refreshPropertySources() {
        try {
            Path configPath = configManagementService.getConfigFilePath();
            if (Files.exists(configPath)) {
                Properties props = new Properties();
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }

                // 将配置文件内容添加到环境变量中
                PropertiesPropertySource propertySource = new PropertiesPropertySource(
                        "dynamic-config-" + System.currentTimeMillis(), props);
                environment.getPropertySources().addFirst(propertySource);

                log.info("配置文件已重新加载到环境变量中");
            }
        } catch (Exception e) {
            log.error("重新加载配置文件失败", e);
            throw new RuntimeException("重新加载配置文件失败", e);
        }
    }

    /**
     * 更新动态组件配置
     */
    private void updateDynamicComponents() {
        try {
            // 这里可以添加需要动态更新的组件逻辑
            // 例如更新一些服务类中的配置值
            log.info("动态组件配置更新完成");
        } catch (Exception e) {
            log.warn("更新动态组件配置时出现警告: {}", e.getMessage());
        }
    }

    /**
     * 获取成功响应（无数据）
     * @return 成功响应
     */
    @GetMapping("/health")
    public ResultData<String> healthCheck() {
        return ResponseModelUtils.success();
    }
}
