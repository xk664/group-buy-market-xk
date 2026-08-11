# 模拟知识语料（Mock Knowledge Corpus）

> 用于 AI 客服 RAG 的 Phase 1 入库验证与检索调优。
> **重要：以下内容全部为模拟数据，不代表真实业务规则，上线前必须由业务方替换为真实语料。**

## 目录结构与 category 映射

```
knowledge/
├── refund/     # category=REFUND  退款政策（条款式文档）
├── product/    # category=PRODUCT 商品信息（参数表 + 商品详情）
├── groupon/    # category=GROUPON 拼团规则（规则 + 活动说明）
└── faq/        # category=FAQ     高频问答（一问一答，不切分）
```

## Front Matter 元数据

每个文档顶部带 YAML front matter，入库管线解析后写入 chunk 的 `metadata`：

```yaml
---
category: REFUND            # REFUND | PRODUCT | GROUPON | FAQ
doc_type: POLICY            # POLICY | PARAM_TABLE | PRODUCT_DETAIL | RULE | FAQ
doc_version: "2026-07"      # 版本号，用于政策时效管理
effective_date: "2026-07-01" # 生效日期，检索时过滤 effective_date <= now
---
```

## 与演示页的对应关系

- `goodsId=9890001` / `SKU=13811216`：智能降噪蓝牙耳机（演示页商品图 sku-13811216-01~04.png）
- `activityId=100123`：智能降噪蓝牙耳机 3 人团活动

## 使用说明

1. Phase 1 按目录批量入库：解析 front matter → 结构化切分 → embedding → 写入 PGVector + tsvector；
2. Golden 测试集见 `docs/ai-eval/golden-qa.md`；
3. 真实语料到位后，按相同目录结构替换并重新入库即可，无需改代码。