# RAG A/B 对照实验结果（v3.2 · 2026-08-12）

> 本轮变更：① 语料扩容 + 加难：15 篇入库（新增《售后服务总则》3000 字长政策文档、Pro 耳机/保温杯青春版相似商品、活动 100666/100999）；② Golden 评估集扩到 **101 条（参与评估 96 条）**，加入相似商品区分、条款细节、活动编号、口语化改写等困难题；③ 重排模型切换为 **gte-rerank-v2**（qwen3-rerank 额度用尽）。

## 一、实验矩阵

| 轮次 | 切分策略 | 重排方式 | 说明 |
|---|---|---|---|
| R1 | 固定大小（500/50） | score（RRF 分数兜底） | 基线 |
| R2 | 父子 chunk（阈值 50 / 子 80） | score | 仅优化切分 |
| R3 | 固定大小（500/50） | gte-rerank-v2（云端） | 仅优化重排 |
| R4 | 父子 chunk | gte-rerank-v2 | 两项同时优化 |

## 二、结果（Golden 101 条，参与评估 96 条）

| 轮次 | Top-5 命中率 / Recall@5 | 命中 | 漏题 |
|---|---|---|---|
| R1 基线 | 95.83%（92/96） | 92 | G027、G069、G106、G107 |
| R2 父子+score | 96.88%（93/96） | 93 | G003、G034、G092 |
| R3 固定+rerank | 94.79%（91/96） | 91 | G027、G069、G092、G106、G107 |
| R4 父子+rerank | 100%（96/96） | 96 | 无 |

分类明细：

| 分类 | 题数 | R1 | R2 | R3 | R4 |
|---|---|---|---|---|---|
| REFUND | 40 | 40 | 37 | 39 | 40 |
| PRODUCT | 31 | 31 | 31 | 31 | 31 |
| GROUPON | 25 | 21 | 25 | 21 | 25 |

## 三、关键结论

1. **切分策略决定召回上限。** 4 道“活动 X 是什么玩法”题（G027/G069/G106/G107）在固定切分下**候选为空**（BM25 全词 AND 对数字活动编号过严 + 向量相似度低于阈值），父子 chunk 按标题树切块后 4 道全部命中（GROUPON 21/25 → 25/25）。这 4 道题任何重排模型都救不回（正确 chunk 没进候选）。
2. **重排在父子切分上完成“最后一公里”。** R2 漏的 3 道 REFUND 题（G003/G034/G092）正确 chunk 排在 6~8 位，score 截断在 top-5 之外；gte-rerank-v2 将其全部提进 top-5，R4 达到 100%。
3. **固定切分下 rerank 无收益甚至略降。** R3 比 R1 少 1 分：G092（漏发运费）在固定切分下正确 chunk 排第 8，gte-rerank-v2 未救回（参考：qwen3-rerank 可救回 G092，其余 4 道活动题同样救不回）。说明 rerank 依赖“候选里有没有正确答案”。
4. **组合最优：父子 chunk + gte-rerank-v2 = 100%**，相比基线 +4.17pt。
5. 数据：R1/R3 固定切分 40 chunks；R2/R4 父子 chunk 265 chunks（子块 130+）。

## 四、复现命令

```powershell
# 每轮：清库 → 入库 → 评估（rerank 轮加 --ai.analyzer.rerank-model=gte-rerank-v2）
java -cp $cp target/Reset.java
java -cp $cp cn.bugstack.ai.AiConsoleRunner ingest D:/group-buy-market-xk/knowledge --ai.chunking.strategy=fixed-size --ai.chunking.parent-child-enabled=false
java -cp $cp cn.bugstack.ai.AiConsoleRunner eval D:/group-buy-market-xk/docs/ai-eval/golden-qa.md --ai.analyzer.rerank-mode=score
# 父子轮：--ai.chunking.strategy=structured --ai.chunking.parent-child-enabled=true；重排轮：--ai.analyzer.rerank-mode=cross-encoder --ai.analyzer.rerank-model=gte-rerank-v2
```

- 当前数据库状态：R4（父子 chunk + gte-rerank-v2），15 篇文档，265 chunks。
- 重排模型配置：`src/test/resources/application.yml` → `ai.analyzer.rerank-model: gte-rerank-v2`。