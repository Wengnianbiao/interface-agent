package com.helianhealth.agent.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class ConfigManagementService {

    private final ConfigurableEnvironment environment;

    public ConfigManagementService(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    private static final String CONFIG_FILE_NAME = "application.properties";

    /**
     * 获取配置文件中的所有属性
     * @return 配置属性Map
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> result = new HashMap<>();
        try {
            Path configPath = getConfigFilePath();

            if (Files.exists(configPath)) {
                Properties props = new Properties();
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }

                for (String key : props.stringPropertyNames()) {
                    result.put(key, props.getProperty(key));
                }
            }
        } catch (Exception e) {
            log.error("读取配置文件失败", e);
        }

        return result;
    }

    /**
     * 获取指定配置属性值
     * @param key 属性键
     * @return 属性值
     */
    public String getProperty(String key) {
        try {
            Path configPath = getConfigFilePath();

            if (Files.exists(configPath)) {
                Properties props = new Properties();
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
                return props.getProperty(key);
            }
        } catch (Exception e) {
            log.error("读取配置项失败: {}", key, e);
        }
        return null;
    }

    /**
     * 更新配置属性
     * @param key 属性键
     * @param value 属性值
     * @return 是否更新成功
     */
    public boolean updateProperty(String key, String value) {
        try {
            // 读取现有配置
            Properties props = new Properties();
            Path configPath = getConfigFilePath();

            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
            }

            // 更新属性
            props.setProperty(key, value);

            // 写回文件
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "Updated at " + new Date());
            }

            log.info("配置已更新: {} = {}", key, value);
            return true;
        } catch (Exception e) {
            log.error("更新配置失败: {} = {}", key, value, e);
            return false;
        }
    }

    /**
     * 删除配置属性
     * @param key 属性键
     * @return 是否删除成功
     */
    public boolean deleteProperty(String key) {
        try {
            // 读取现有配置
            Properties props = new Properties();
            Path configPath = getConfigFilePath();

            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
            }

            // 删除属性
            Object removed = props.remove(key);
            if (removed == null) {
                log.warn("尝试删除不存在的配置项: {}", key);
                return false;
            }

            // 写回文件
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "Updated at " + new Date() + ", removed key: " + key);
            }

            log.info("配置项已删除: {}", key);
            return true;
        } catch (Exception e) {
            log.error("删除配置项失败: {}", key, e);
            return false;
        }
    }

    /**
     * 批量更新配置属性
     * @param properties 属性集合
     * @return 是否更新成功
     */
    public boolean updateProperties(Map<String, String> properties) {
        try {
            // 读取现有配置
            Properties props = new Properties();
            Path configPath = getConfigFilePath();

            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
            }

            // 批量更新属性
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }

            // 写回文件
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "Batch updated at " + new Date());
            }

            log.info("批量配置更新完成");
            return true;
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return false;
        }
    }

    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */
    public Path getConfigFilePath() {
        // 检查是否指定了配置文件位置
        String configLocation = environment.getProperty("spring.config.location");
        if (configLocation != null && !configLocation.isEmpty()) {
            String path = configLocation.replace("file:", "");
            if (path.endsWith(CONFIG_FILE_NAME)) {
                return Paths.get(path);
            }
            return Paths.get(path, CONFIG_FILE_NAME);
        }

        // 尝试在classpath中查找
        try {
            Path classpathConfig = Paths.get("src", "main", "resources", CONFIG_FILE_NAME);
            if (Files.exists(classpathConfig)) {
                return classpathConfig;
            }
        } catch (Exception e) {
            log.debug("在src/main/resources中未找到配置文件");
        }

        // 默认在当前目录
        return Paths.get(CONFIG_FILE_NAME);
    }
}
