package cn.bugstack.ai;

import cn.bugstack.ai.rag.KnowledgeIngestionService;
import cn.bugstack.ai.rag.eval.EvalResult;
import cn.bugstack.ai.rag.eval.EvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * AI 控制台运行器：只启动 cn.bugstack.ai 相关 Bean，不依赖 MySQL/Redis/RabbitMQ
 * 用法：
 *   ingest [语料根目录]
 *   eval   [Golden 文件路径]
 */
@SpringBootApplication(scanBasePackages = "cn.bugstack.ai")
public class AiConsoleRunner {

    public static void main(String[] args) {
        java.util.List<String> springArgs = new java.util.ArrayList<String>();
        String cmd = "help";
        String pathArg = null;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                springArgs.add(arg);
            } else if ("help".equals(cmd)) {
                cmd = arg;
            } else if (pathArg == null) {
                pathArg = arg;
            }
        }
        SpringApplication app = new SpringApplication(AiConsoleRunner.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(springArgs.toArray(new String[0]));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if ("ingest".equals(cmd)) {
                KnowledgeIngestionService.IngestResult result = ctx.getBean(KnowledgeIngestionService.class)
                        .ingest(pathArg);
                System.out.println("INGEST_RESULT " + objectMapper.writeValueAsString(result));
            } else if ("eval".equals(cmd)) {
                EvalResult result = ctx.getBean(EvaluationService.class)
                        .evaluate(pathArg);
                System.out.println("EVAL_RESULT " + objectMapper.writeValueAsString(result));
            } else {
                System.out.println("usage: ingest [path] | eval [goldenPath]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

}