# Golden 测试集（模拟，30 条）

> 用途：RAG 离线评估（Recall@K / MRR / NDCG / 命中率）与回归测试。
> **重要：当前为模拟数据，真实会话/语料到位后需由业务方与标注人复核扩充。**
> 命中判定：检索结果覆盖“期望文档 + 期望章节”任一即算命中（Top-5 命中率 = 命中条数 / 总条数）。

| ID   | 分类      | 问题               | 期望文档                               | 期望章节       | 参考答案要点                                   |
| ---- | ------- | ---------------- | ---------------------------------- | ---------- | ---------------------------------------- |
| G001 | REFUND  | 退款一般多久到账？        | knowledge/refund/退款规则.md           | 四、退款时效     | 审核 1-3 个工作日；微信/支付宝 1-3 个工作日，银行卡 3-5 个工作日 |
| G002 | REFUND  | 还没发货可以退款吗？       | knowledge/refund/退款规则.md           | 2.1 未发货订单  | 可申请全额退款，系统自动受理                           |
| G003 | REFUND  | 7 天无理由退货有什么要求？   | knowledge/refund/退款规则.md           | 2.2 已发货订单  | 签收次日起 7 天内，不影响二次销售                       |
| G004 | REFUND  | 退货的运费谁出？         | knowledge/refund/售后与运费.md          | 二、运费承担规则   | 质量问题平台承担；个人原因用户承担                        |
| G005 | REFUND  | 退款后优惠券会退吗？       | knowledge/refund/退款规则.md           | 五、退款金额计算   | 不退回、不折现                                  |
| G006 | REFUND  | 银行卡支付退款多久到账？     | knowledge/refund/退款规则.md           | 四、退款时效     | 审核通过后 3-5 个工作日                           |
| G007 | REFUND  | 虚拟商品能退吗？         | knowledge/refund/退款规则.md           | 2.5 不可退款场景 | 不支持 7 天无理由退货                             |
| G008 | REFUND  | 退款申请多久审核？        | knowledge/refund/退款规则.md           | 四、退款时效     | 1-3 个工作日                                 |
| G009 | REFUND  | 极速退款是什么？         | knowledge/refund/售后与运费.md          | 三、极速退款     | 未发货自动通过；已发货商家 24 小时未响应自动同意               |
| G010 | REFUND  | 换货运费谁承担？         | knowledge/refund/售后与运费.md          | 四、换货说明     | 与退货规则一致                                  |
| G011 | PRODUCT | 智能降噪蓝牙耳机续航多久？    | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 商品参数       | 单次 8 小时，总续航 36 小时                        |
| G012 | PRODUCT | 耳机支持无线充电吗？       | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 常见问题       | 不支持，仅 Type-C 有线充电                        |
| G013 | PRODUCT | 耳机降噪效果多少分贝？      | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 商品参数       | 主动降噪 -42dB                               |
| G014 | PRODUCT | 保温杯是什么材质？        | knowledge/product/商品详情-便携保温杯.md    | 商品参数       | 316 食品级不锈钢                               |
| G015 | PRODUCT | 保温杯保温多久？         | knowledge/product/商品详情-便携保温杯.md    | 商品参数       | 保温/保冷 12 小时                              |
| G016 | PRODUCT | 机械键盘支持几种连接方式？    | knowledge/product/商品详情-机械键盘.md     | 商品参数       | 三模：有线 + 2.4G + 蓝牙                        |
| G017 | PRODUCT | 机械键盘茶轴和红轴怎么选？    | knowledge/product/商品详情-机械键盘.md     | 常见问题       | 茶轴段落感适合打字；红轴线性安静适合游戏                     |
| G018 | PRODUCT | 充电宝支持快充吗？        | knowledge/product/商品参数表.md         | 商品参数表      | 22.5W 快充，Type-C                          |
| G019 | PRODUCT | 蓝牙耳机蓝牙版本是多少？     | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 商品参数       | 蓝牙 5.3，支持双设备连接                           |
| G020 | PRODUCT | 耳机防水吗？           | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 商品参数       | IPX5，防汗防泼溅                               |
| G021 | GROUPON | 拼团失败会退款吗？        | knowledge/groupon/拼团规则.md          | 四、拼团失败     | 自动为所有团员原路退款，1-3 个工作日到账                   |
| G022 | GROUPON | 拼团一般多久成团？        | knowledge/groupon/拼团规则.md          | 二、开团与参团    | 常见 24 小时，部分活动 48 小时                      |
| G023 | GROUPON | 拼团进行中可以退出吗？      | knowledge/groupon/拼团规则.md          | 五、拼团进行中    | 成团前可取消参团；成团后不可中途退出                       |
| G024 | GROUPON | 拼团价和单独购买价什么区别？   | knowledge/groupon/拼团规则.md          | 六、拼团价与单独购买 | 拼团价是成团优惠价，支付金额即拼团价                       |
| G025 | GROUPON | 一个活动限购几件？        | knowledge/groupon/拼团规则.md          | 二、开团与参团    | 同一活动同一商品一般限购 1 件                         |
| G026 | GROUPON | 拼团成功后多久发货？       | knowledge/groupon/拼团规则.md          | 三、成团条件     | 进入正常发货流程，一般 48 小时内发货                     |
| G027 | GROUPON | 活动 100123 是什么玩法？ | knowledge/groupon/拼团活动说明.md        | 活动 100123  | 3 人成团，拼团价 299 元，24 小时时限                  |
| G028 | GROUPON | 怎么查拼团进度？         | knowledge/groupon/拼团规则.md          | 五、拼团进行中    | 订单详情/拼团列表页查看剩余人数与倒计时                     |
| G029 | REFUND  | 退款后发票怎么处理？       | knowledge/refund/售后与运费.md          | 五、发票       | 对应金额发票作废或冲红                              |
| G030 | OTHER   | 我要投诉你们           | （无知识可答）                            | -          | 不检索，直接返回“人工处理中...”                       |