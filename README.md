# interface-agent

一个可配置的接口代理服务，用于处理工作流节点的创建、管理、执行以及参数配置等功能，支持多种节点类型（如 HTTP、WEBSERVICE、DATABASE、MOCK 等）的集成与调度。

## 功能特点

- **工作流节点管理**：支持节点的创建、查询、更新、删除等操作，可按工作流 ID 分页获取节点列表
- **节点参数配置**：提供节点参数的导入、导出功能，支持参数的树形结构配置与管理
- **多类型节点支持**：内置对 SOAP 等协议的支持，可扩展至 HTTP、数据库等其他类型节点
- **动态配置管理**：支持系统配置的动态更新、查询、添加、删除，无需重启应用即可部分生效
- **开放 API 接口**：提供统一的开放 API 入口，支持多种数据格式（JSON、XML 等）的输入输出

## 技术栈

- **后端框架**：Spring Boot
- **数据访问**：MyBatis（通过 Mapper 接口操作数据库）
- **前端技术**：Vue.js（静态资源打包）
- **通信协议**：HTTP、SOAP
- **数据格式**：JSON、XML
- **构建工具**：Maven

## 目录结构
src/
├── main/
│   ├── java/com/helilianhealth/agent/
│   │   ├── controller/           # 控制器层
│   │   │   ├── workflow/         # 工作流节点相关接口
│   │   │   ├── api/              # 开放API接口
│   │   │   ├── property/         # 配置管理接口
│   │   │   └── ...
│   │   ├── service/              # 服务层
│   │   │   ├── impl/             # 服务实现类
│   │   │   └── ...
│   │   ├── model/                # 数据模型
│   │   ├── domain/               # 领域对象
│   │   │   ├── dto/              # 数据传输对象
│   │   │   └── ...
│   │   ├── remote/               # 远程调用相关
│   │   ├── webService/           # SOAP协议处理
│   │   ├── config/               # 配置类
│   │   ├── utils/                # 工具类
│   │   └── ...
│   └── resources/
│       ├── static/               # 静态资源（前端包文件）
│       └── ...
└── ...
## 快速开始

### 环境要求

- JDK 1.8+
- SQLSERVER 12.4.2
- Maven 3.6+

### 部署步骤

1. **克隆代码**
   git clone https://github.com/your-username/interface-agent.git
   cd interface-agent
2. **配置数据库**

修改 `application.properties` 或 `application.yml` 中的数据库配置：
spring.datasource.url=jdbc:mysql://localhost:3306/interface_agent?useUnicode=true&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
3. **构建项目**
   mvn clean package -Dmaven.test.skip=true
4. **启动服务**
   java -jar target/interface-agent-1.0.0.jar
5. **访问服务**

- 前端页面：http://localhost:8080
- API 接口根路径：http://localhost:8080/v1/console
- 开放 API 入口：http://localhost:8080/agent-open-api/**

## 核心接口说明

### 工作流节点管理

| 接口地址                | 方法   | 描述               | 请求参数                     |
|-------------------------|--------|--------------------|------------------------------|
| /v1/console/node/all    | GET    | 分页获取节点列表   | flowId（可选）、pageNum、pageSize |
| /v1/console/node/create | POST   | 创建节点           | WorkflowNodeCreateReq 实体   |
| /v1/console/node/{nodeId} | GET  | 获取节点详情       | nodeId（路径参数）           |
| /v1/console/node/update | POST   | 更新节点           | InterfaceWorkflowNodeDO 实体 |
| /v1/console/node/{nodeId} | DELETE | 删除节点           | nodeId（路径参数）           |

### 参数配置管理

| 接口地址                          | 方法   | 描述               | 请求参数                 |
|-----------------------------------|--------|--------------------|--------------------------|
| /v1/console/node/export/{nodeId}  | GET    | 导出节点参数配置   | nodeId（路径参数）       |
| /v1/console/node/import           | POST   | 导入节点参数配置   | nodeId、MultipartFile 文件 |

### 系统配置管理

| 接口地址                          | 方法   | 描述               | 请求参数                 |
|-----------------------------------|--------|--------------------|--------------------------|
| /v1/console/property/properties   | GET    | 获取所有配置       | -                        |
| /v1/console/property/property     | GET    | 获取指定配置       | key                      |
| /v1/console/property/property     | POST   | 更新配置           | key、value               |
| /v1/console/property/refresh      | POST   | 刷新配置           | -                        |

## 扩展指南

### 新增节点类型

1. 定义节点类型枚举（如 `NodeType` 中添加新类型）
2. 实现对应的参数解析器（参考 `SoapRequestHandler`）
3. 在 `interfaceClientProxy` 中添加新类型节点的调用逻辑

### 自定义参数映射规则

1. 在 `NodeParamConfigDO` 中扩展映射相关字段
2. 在参数解析过程中（如 `buildParamTree` 方法）添加自定义映射逻辑
3. 前端页面同步支持新的映射规则配置项

## 注意事项

- 导入参数配置时，仅支持 JSON 格式文件，且需符合 `NodeParamConfigImportDTO` 结构
- 动态配置刷新后，部分配置可能需要重启应用才能生效（具体见接口返回提示）
- SOAP 协议节点需正确配置 `metaInfo` 中的信封（envelope）、头（header）、体（body）信息

## 许可证
AI生成的MD
