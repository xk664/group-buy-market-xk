package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.rag.KnowledgeIngestionService;
import cn.bugstack.ai.rag.conflict.ConflictService;
import cn.bugstack.ai.rag.eval.EvalResult;
import cn.bugstack.ai.rag.eval.EvaluationService;
import cn.bugstack.ai.rag.repository.ChatLogRepository;
import cn.bugstack.ai.rag.repository.ConflictRepository;
import cn.bugstack.api.response.Response;
import cn.bugstack.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * AI 管理接口：知识增量入库 / 冲突检测 / 版本下线 / 离线评估 / 未命中分析
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/ai/admin/")
public class AIAdminController {

    @Resource
    private KnowledgeIngestionService ingestionService;

    @Resource
    private EvaluationService evaluationService;

    @Resource
    private ChatLogRepository chatLogRepository;

    @Resource
    private ConflictService conflictService;

    /**
     * 全量扫描式增量入库（指纹跳过未变文件）
     * POST /api/ai/admin/ingest?path=./knowledge
     */
    @PostMapping("ingest")
    public Response<KnowledgeIngestionService.IngestResult> ingest(@RequestParam(required = false) String path) {
        try {
            KnowledgeIngestionService.IngestResult result = ingestionService.ingest(path);
            return Response.<KnowledgeIngestionService.IngestResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("AI 知识入库失败 path={}", path, e);
            return Response.<KnowledgeIngestionService.IngestResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 单文件增量入库
     * POST /api/ai/admin/ingest/file?path=knowledge/refund/退款规则.md
     */
    @PostMapping("ingest/file")
    public Response<KnowledgeIngestionService.FileProcessResult> ingestFile(@RequestParam String path) {
        try {
            KnowledgeIngestionService.FileProcessResult result = ingestionService.ingestFile(java.nio.file.Paths.get(path));
            return Response.<KnowledgeIngestionService.FileProcessResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("AI 单文件入库失败 path={}", path, e);
            return Response.<KnowledgeIngestionService.FileProcessResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 冲突检测：指定文档或全量
     * POST /api/ai/admin/conflict/detect?docId=123  或  ?all=true
     */
    @PostMapping("conflict/detect")
    public Response<Integer> detectConflict(@RequestParam(required = false) Long docId,
                                            @RequestParam(defaultValue = "false") boolean all) {
        try {
            int count = all ? conflictService.detectAll() : conflictService.detectForDoc(docId);
            return Response.<Integer>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(count)
                    .build();
        } catch (Exception e) {
            log.error("AI 冲突检测失败 docId={} all={}", docId, all, e);
            return Response.<Integer>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 冲突列表
     * GET /api/ai/admin/conflict/list?status=PENDING&limit=100
     */
    @GetMapping("conflict/list")
    public Response<List<ConflictRepository.ConflictRecord>> listConflict(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ConflictRepository.ConflictRecord> list = conflictService.list(status, limit);
            return Response.<List<ConflictRepository.ConflictRecord>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("AI 冲突列表失败", e);
            return Response.<List<ConflictRepository.ConflictRecord>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 冲突人工确认：RESOLVED（已修复）/ IGNORED（误报）
     * POST /api/ai/admin/conflict/resolve?id=1&action=RESOLVED
     */
    @PostMapping("conflict/resolve")
    public Response<Boolean> resolveConflict(@RequestParam long id, @RequestParam String action) {
        try {
            boolean ok = conflictService.resolve(id, action);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ok)
                    .build();
        } catch (Exception e) {
            log.error("AI 冲突确认失败 id={} action={}", id, action, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 离线评估（Golden 回归）
     * POST /api/ai/admin/eval?path=docs/ai-eval/golden-qa.md
     */
    @PostMapping("eval")
    public Response<EvalResult> eval(@RequestParam(required = false) String path) {
        try {
            EvalResult result = evaluationService.evaluate(path);
            return Response.<EvalResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("AI 离线评估失败 path={}", path, e);
            return Response.<EvalResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 未命中问题分析（反馈闭环）
     * POST /api/ai/admin/analysis/unanswered?limit=100
     */
    @PostMapping("analysis/unanswered")
    public Response<List<String>> unanswered(@RequestParam(defaultValue = "100") int limit) {
        try {
            List<String> questions = chatLogRepository.listNeedHumanQuestions(limit);
            return Response.<List<String>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(questions)
                    .build();
        } catch (Exception e) {
            log.error("AI 未命中分析失败 limit={}", limit, e);
            return Response.<List<String>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 下线某分类某版本的旧知识
     * POST /api/ai/admin/version/deactivate?category=REFUND&version=2026-07
     */
    @PostMapping("version/deactivate")
    public Response<Integer> deactivateVersion(@RequestParam String category, @RequestParam String version) {
        try {
            int affected = ingestionService.deactivateVersion(category, version);
            return Response.<Integer>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(affected)
                    .build();
        } catch (Exception e) {
            log.error("AI 版本下线失败 category={} version={}", category, version, e);
            return Response.<Integer>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

}