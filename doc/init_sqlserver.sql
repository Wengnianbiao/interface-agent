-- 接口规则工作流表
CREATE TABLE interface_workflow
(
    -- 工作流ID，自增主键，不允许为空、不可更新
    flow_id INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    -- 工作流名称，描述工作流的名称信息，允许为空
    flow_name VARCHAR(100) NULL,
    -- 接口URI，上游(如Jarvis)调用接口的路由地址，与工作流一对一映射，允许为空
    interface_uri VARCHAR(150) NULL,
    -- 接口入参参数类型,JSON、XML
    content_type VARCHAR(20) NULL,
    -- 接口入参元数据信息
    content_meta_info NVARCHAR(255) NULL,
    -- 工作流首节点，存储起始节点ID列表（逗号分隔的Long类型字符串），允许为空
    first_flow_nodes VARCHAR(50) NULL,
    -- 状态标识，1-启用，0-禁用，允许为空
    status INT NOT NULL DEFAULT 1
);

-- 工作流节点表
CREATE TABLE interface_workflow_node
(
    -- 节点ID，自增主键，不允许为空、不可更新
    node_id INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    -- 节点名称，描述工作流的节点名称信息，允许为空
    node_name VARCHAR(100) NULL,
    -- 工作流ID，这里假设长度根据实际情况设置，允许为空
    flow_id INT NULL,
    -- 参数预处理表达式
    param_filter_expr VARCHAR(255) NULL,
    -- 节点类型，存储枚举对应的字符串值，长度按需调整，不允许为空
    node_type VARCHAR(50) NOT NULL,
    -- api实例元数据，存储JSON格式字符串，用NVARCHAR(MAX)存储较多内容，允许为空
    meta_info NVARCHAR(MAX) NULL,
    -- 调度表达式
    schedule_expr VARCHAR(255) NULL,
    -- 调度参数来源
    schedule_param_source_type VARCHAR(25) NULL
);

-- 工作流节点参数配置表
CREATE TABLE node_param_config
(
    -- 主键ID，自增，不允许为空和更新
    config_id INT IDENTITY(1, 1) NOT NULL PRIMARY KEY,

    -- 所属工作流节点ID（关联到interface_flow_node表的node_id）
    node_id INT NULL,

    -- 上游参数键名
    source_param_key VARCHAR(100) NULL,

    -- 节点类型（object、array、string等，对应ParamType枚举）
    source_param_type VARCHAR(50) NULL,

    -- 三方接口需要的参数类型（对应ParamType枚举）
    target_param_type VARCHAR(50) NULL,

    -- 三方接口需要的参数名称
    target_param_key VARCHAR(100) NULL,

    -- 父节点ID（顶级节点为NULL）
    parent_id INT NULL,

    -- 排序号（控制参数顺序）
    sort INT NULL DEFAULT 0,

    -- 映射数据源
    mapping_source VARCHAR(50) NOT NULL DEFAULT 'RESPONSE',

    -- 映射规则类型（对应MappingType枚举）
    mapping_type VARCHAR(50) NULL,

    -- 映射规则内容（固定值、表达式等）
    mapping_rule NVARCHAR(MAX) NULL,

    -- 参数描述
    param_desc VARCHAR(100) NULL,

    -- 参数处理类型：前置PRE_PROCESS 或 后置POST_PROCESS
    process_type VARCHAR(20) NULL
);

CREATE TABLE interface_invoke_log (
    -- 主键ID，自增，不允许为空和更新
    log_id bigint IDENTITY(1,1)  NOT NULL PRIMARY KEY,
    -- 节点id
    node_id int NOT NULL,
    -- 业务数据
    business_data nvarchar(max),
    -- 请求参数
    param_before_invoke nvarchar(max),
    -- 远程调用结果
    remote_invoke_response nvarchar(max),
    -- 响应参数
    param_after_invoke nvarchar(max),
    -- 调用时间
    invoke_time VARCHAR(20),
    -- 创建时间
    create_time datetime NOT NULL DEFAULT GETDATE()
);

