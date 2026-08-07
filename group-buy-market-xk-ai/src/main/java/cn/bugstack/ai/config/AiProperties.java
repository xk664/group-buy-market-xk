package cn.bugstack.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客服 RAG 配置（前缀 ai.，对应 application-*.yml 中 ai 块）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private Datasource datasource = new Datasource();
    private Llm llm = new Llm();
    private Knowledge knowledge = new Knowledge();
    private Retrieval retrieval = new Retrieval();
    private Analyzer analyzer = new Analyzer();
    private Tool tool = new Tool();
    private Optimization optimization = new Optimization();
    private Conflict conflict = new Conflict();
    private Chunking chunking = new Chunking();
    private Es es = new Es();

    @Data
    public static class Datasource {
        private String username;
        private String password;
        private String url;
        private String driverClassName;
        private Hikari hikari = new Hikari();
    }

    @Data
    public static class Hikari {
        private String poolName = "AiHikariCP";
        private Integer maximumPoolSize = 10;
        private Integer minimumIdle = 2;
        private Long connectionTimeout = 30000L;
    }

    @Data
    public static class Llm {
        /** OpenAI 兼容 base-url，如 DashScope compatible-mode */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        /** 留空或为占位符时启用 Mock 实现 */
        private String apiKey;
        private String chatModel = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
        private Integer timeoutSeconds = 30;
    }

    @Data
    public static class Knowledge {
        /** 知识语料根目录 */
        private String rootPath = "./knowledge";
        /** 是否启用目录监听（WatchService 增量触发） */
        private Boolean watchEnabled = false;
    }

    @Data
    public static class Retrieval {
        private Integer vectorTopK = 20;
        private Integer fulltextTopK = 20;
        private Integer rrfK = 60;
        private Integer resultTopK = 5;
        private Double similarityThreshold = 0.5;
        private Integer cacheTtlSeconds = 300;
    }

    @Data
    public static class Analyzer {
        /** rule=规则分类器(Phase1) llm=LLM 查询分析(Phase2) */
        private String mode = "rule";
        /** score=按分数重排(默认) cross-encoder=云端重排 */
        private String rerankMode = "score";
        /** 云端重排模型（DashScope text-rerank） */
        private String rerankModel = "bge-reranker-v2-m3";
        /** 云端重排接口地址 */
        private String rerankBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";
    }

    @Data
    public static class Tool {
        /** 是否启用工具调用（Phase3 开启） */
        private Boolean enabled = false;
        /** 是否通过 MCP 协议调用动态数据工具（true=拉起 MCP Server 子进程） */
        private Boolean mcpEnabled = false;
    }

    @Data
    public static class Optimization {
        /** FAQ 命中直出答案，跳过 LLM 调用（成本优化） */
        private Boolean faqDirect = true;
    }

    @Data
    public static class Es {
        /** 是否启用 Elasticsearch 关键词检索（关闭时回退 PG tsvector） */
        private Boolean enabled = false;
        private String host = "http://192.168.232.128:9200";
        private String index = "ai_knowledge_chunk";
        private String username;
        private String password;
        /** 分词器：ik_max_word（需安装 IK 插件）；未装时自动回退 standard */
        private String analyzer = "ik_max_word";
        private Integer connectTimeoutMs = 3000;
    }

    @Data
    public static class Chunking {
        /** 是否启用父子 chunk（仅政策/规则类长文档生效） */
        private Boolean parentChildEnabled = true;
        /** 启用父子切分的 section 最小长度（字符） */
        private Integer parentThreshold = 400;
        /** 子块大小（字符） */
        private Integer childSize = 200;
        /** 子块重叠（字符） */
        private Integer childOverlap = 30;
    }

    @Data
    public static class Conflict {
        /** 是否启用冲突检测 */
        private Boolean enabled = true;
        /** 相似 chunk 判定阈值（cosine） */
        private Double threshold = 0.85;
        /** 每个 chunk 最多比对多少个相似邻居 */
        private Integer topK = 10;
    }

}