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
## 语料清单（v2）

| 目录 | 文档 | 说明 |
|---|---|---|
| refund/ | 退款规则.md、售后与运费.md | 退款条件/时效/金额 + 运费/换货/发票/价保/运费险 |
| product/ | 商品参数表.md、商品详情-智能降噪蓝牙耳机.md、商品详情-便携保温杯.md、商品详情-机械键盘.md、商品详情-无线充电宝.md、商品详情-防晒冰袖.md | 5 个 SKU 全覆盖 |
| groupon/ | 拼团规则.md、拼团活动说明.md | 规则 + 活动 100123/100456/100789/100321/100555 |
| faq/ | 高频问答.md | 26 条高频问答 |

- 活动对照：100123=耳机 3 人团；100456=保温杯 2 人团；100789=机械键盘 3 人团；100321=冰袖 5 人团；100555=充电宝 2 人团
## 语料清单（v3 · 区分度增强）

| 目录 | 文档 | 说明 |
|---|---|---|
| refund/ | 退款规则.md、售后与运费.md、售后服务总则.md | 新增 3000 字长文档《售后服务总则》（条款式，用于切分/重排区分度测试） |
| product/ | 商品参数表.md、6 个商品详情 | 新增 Pro 耳机（SW-Pro Max）与保温杯青春版，参数与旧款高度相似 |
| groupon/ | 拼团规则.md、拼团活动说明.md | 活动扩到 7 个（100123/100456/100789/100321/100555/100666/100999） |
| faq/ | 高频问答.md | 26 条高频问答 |

- 相似商品对照：SW-Pro（-42dB/36h/不支持无线充）vs SW-Pro Max（-45dB/45h/支持无线充）；保温杯 500ml（12h）vs 青春版 350ml（8h）
- 活动对照：100666=保温杯青春版 3 人团；100999=耳机 Pro 5 人团