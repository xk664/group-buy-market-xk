# Golden 测试集（模拟，101 条）

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
| G018 | PRODUCT | 充电宝支持快充吗？         | knowledge/product/商品详情-无线充电宝.md | 商品参数       | 22.5W 快充，Type-C                          |
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
| G031 | REFUND  | 未发货订单可以部分退款吗？     | knowledge/refund/退款规则.md           | 五、退款金额计算   | 未发货订单支持按件部分退款；已发货订单需整单退货                     |
| G032 | REFUND  | 包邮商品退货会扣运费吗？       | knowledge/refund/售后与运费.md          | 二、运费承担规则   | 包邮商品退货后未达包邮门槛的，退款时扣除发货运费                   |
| G033 | REFUND  | 换货时没有库存怎么办？         | knowledge/refund/售后与运费.md          | 四、换货说明     | 可协商退款或等待补货                                |
| G034 | REFUND  | 定制商品支持无理由退货吗？      | knowledge/refund/退款规则.md           | 2.5 不可退款场景 | 虚拟/定制/生鲜不支持 7 天无理由退货                         |
| G035 | REFUND  | 客服工作时间是什么时候？       | knowledge/refund/售后与运费.md          | 六、售后联系方式   | 每日 9:00-21:00，节假日另行公告                        |
| G036 | REFUND  | 拼团进行中能修改收货地址吗？     | knowledge/refund/退款规则.md           | 2.3 拼团进行中的订单 | 暂不支持，取消后重新参团                               |
| G037 | REFUND  | 电子发票多久能开出来？         | knowledge/refund/售后与运费.md          | 五、发票       | 一般 3-5 个工作日发送至邮箱                           |
| G038 | PRODUCT | 无线充电宝容量多大？           | knowledge/product/商品详情-无线充电宝.md  | 商品参数       | 10000mAh，额定容量 5800mAh                         |
| G039 | PRODUCT | 无线充电宝支持快充吗？         | knowledge/product/商品详情-无线充电宝.md  | 商品参数       | 22.5W 双向快充，Type-C / USB-A                    |
| G040 | PRODUCT | 防晒冰袖的防晒指数是多少？      | knowledge/product/商品详情-防晒冰袖.md    | 商品参数       | UPF50+，紫外线阻隔率 ≥ 98%                          |
| G041 | PRODUCT | 防晒冰袖是什么面料？           | knowledge/product/商品详情-防晒冰袖.md    | 商品参数       | 冰丝面料（锦纶 + 氨纶）                              |
| G042 | PRODUCT | 机械键盘蓝牙能连几台设备？      | knowledge/product/商品详情-机械键盘.md     | 常见问题       | 支持 3 台设备记忆，一键切换                             |
| G043 | PRODUCT | 保温杯可以装咖啡吗？           | knowledge/product/商品详情-便携保温杯.md    | 常见问题       | 可以，建议 4 小时内饮用并清洗                           |
| G044 | PRODUCT | 蓝牙耳机单只多重？            | knowledge/product/商品详情-智能降噪蓝牙耳机.md | 商品参数       | 单耳 4.8g                                      |
| G045 | PRODUCT | 机械键盘质保多久？            | knowledge/product/商品详情-机械键盘.md     | 商品参数       | 2 年                                          |
| G046 | GROUPON | 活动 100456 是什么玩法？      | knowledge/groupon/拼团活动说明.md        | 活动 100456   | 保温杯 2 人团，拼团价 89 元，成团时限 48 小时                 |
| G047 | GROUPON | 活动 100789 每人限购几件？     | knowledge/groupon/拼团活动说明.md        | 活动 100789   | 每人限购 1 件                                     |
| G048 | GROUPON | 防晒冰袖有拼团活动吗？         | knowledge/groupon/拼团活动说明.md        | 活动 100321   | 5 人团，拼团价 19.9 元，成团时限 24 小时                   |
| G049 | GROUPON | 拼团成团人数一般有哪几种？      | knowledge/groupon/拼团规则.md          | 三、成团条件     | 常见 2 人、3 人、5 人团                             |
| G050 | GROUPON | 拼团失败退款退到哪里？         | knowledge/groupon/拼团规则.md          | 四、拼团失败     | 原路退回支付账户，一般 1-3 个工作日到账                     |
| G051 | REFUND  | 商品降价能申请价保吗？         | knowledge/refund/售后与运费.md          | 七、价格保护     | 签收 7 天内降价可退差价；拼团价/秒杀价不参与价保                 |
| G052 | REFUND  | 退货有运费险吗？             | knowledge/refund/售后与运费.md          | 八、运费险      | 部分商品赠送运费险，退货时可自动理赔                        |
| G053 | OTHER   | 我要找人工客服              | （无知识可答）                            | -          | 不检索，直接返回“人工处理中...”                       |
| G054 | OTHER   | 你们服务太差了              | （无知识可答）                            | -          | 不检索，直接返回“人工处理中...”                       |
| G055 | PRODUCT | SW-Pro Max 续航多久？         | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 总续航 45 小时，单次 10 小时                       |
| G056 | PRODUCT | 耳机 Pro 支持无线充电吗？      | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 支持，充电仓支持 Qi 无线充电                        |
| G057 | PRODUCT | 耳机 Pro 降噪多少分贝？        | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 主动降噪 -45dB                                  |
| G058 | PRODUCT | 耳机 Pro 蓝牙是第几代？        | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 蓝牙 5.4，支持三设备连接                            |
| G059 | PRODUCT | 青春版保温杯保温多久？         | knowledge/product/商品详情-便携保温杯青春版.md | 商品参数       | 保温/保冷 8 小时                                  |
| G060 | PRODUCT | 青春版保温杯容量多大？         | knowledge/product/商品详情-便携保温杯青春版.md | 商品参数       | 350ml                                        |
| G061 | PRODUCT | 青春版保温杯是什么材质？        | knowledge/product/商品详情-便携保温杯青春版.md | 商品参数       | 316L 食品级不锈钢                                 |
| G062 | PRODUCT | Pro 耳机和普通耳机有什么区别？    | knowledge/product/商品详情-智能降噪耳机Pro.md | 常见问题       | 降噪更深、续航更长、支持无线充、防水更高                    |
| G063 | REFUND  | 售后服务响应时效是多久？        | knowledge/refund/售后服务总则.md         | 第二章 售后时效与响应 | 人工客服 2 小时内首次响应                             |
| G064 | REFUND  | 争议处理时效是多久？           | knowledge/refund/售后服务总则.md         | 第二章 售后时效与响应 | 48 小时内完成复核并给出结论                           |
| G065 | REFUND  | 质量问题商品多久内可换新？       | knowledge/refund/售后服务总则.md         | 第三章 退换货细则   | 签收 30 天内免费换新，超期提供免费维修                    |
| G066 | REFUND  | 投诉最高能赔付多少？           | knowledge/refund/售后服务总则.md         | 第五章 争议与投诉处理 | 最高订单实付 30%，不超过 1000 元                     |
| G067 | REFUND  | 售后总则和退款规则冲突时以哪个为准？  | knowledge/refund/售后服务总则.md         | 第六章 附则      | 以售后服务总则为准                                 |
| G068 | REFUND  | 定制商品做错了谁负责？          | knowledge/refund/售后服务总则.md         | 第四章 特殊商品售后  | 平台承担重做或退款，重做一般 15 个工作日                  |
| G069 | GROUPON | 活动 100999 是什么玩法？       | knowledge/groupon/拼团活动说明.md        | 活动 100999   | 耳机 Pro 5 人团，拼团价 459 元，成团时限 48 小时          |
| G070 | GROUPON | 活动 100666 成团时限多久？      | knowledge/groupon/拼团活动说明.md        | 活动 100666   | 24 小时                                       |
| G071 | GROUPON | 活动 100321 每人限购几件？      | knowledge/groupon/拼团活动说明.md        | 活动 100321   | 每人限购 2 件                                    |
| G072 | GROUPON | Pro 版耳机拼团失败退多少钱？  | knowledge/groupon/拼团活动说明.md        | 活动 100999   | 按实付拼团价 459 元全额原路退款                        |
| G073 | PRODUCT | 青春版保温杯能装咖啡吗？         | knowledge/product/商品详情-便携保温杯青春版.md | 常见问题       | 可以，建议 3 小时内饮用并清洗                          |
| G074 | REFUND  | 售后审核一般要多久？           | knowledge/refund/售后服务总则.md         | 第二章 售后时效与响应 | 1-3 个工作日完成审核                                |
| G075 | GROUPON | 哪些活动是 5 人团？            | knowledge/groupon/拼团活动说明.md        | 活动 100321   | 100321 冰袖 5 人团、100999 耳机 Pro 5 人团            |
| G076 | OTHER   | 我要投诉你们平台              | （无知识可答）                            | -          | 不检索，直接返回“人工处理中...”                       |
| G077 | OTHER   | 你们是骗子                  | （无知识可答）                            | -          | 不检索，直接返回“人工处理中...”                       |
| G078 | REFUND  | 七天无理由退货对商品状态有什么要求？ | knowledge/refund/售后服务总则.md         | 3.1 七天无理由退货 | 吊牌齐全、包装完整、未洗涤未使用、防伪标识完好                |
| G079 | PRODUCT | 升级款耳机充满电能用多少小时？    | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 单次 10 小时，总续航 45 小时                         |
| G080 | PRODUCT | 高配版耳机可以无线充电吗？       | knowledge/product/商品详情-智能降噪耳机Pro.md | 商品参数       | 支持，充电仓支持 Qi 无线充电                          |
| G081 | PRODUCT | 一万毫安的充电宝能带上飞机吗？     | knowledge/product/商品详情-无线充电宝.md  | 使用说明       | 可以，10000mAh 符合民航随身携带规定，禁止托运             |
| G082 | GROUPON | 五个人才能成团的冰袖活动是哪个？    | knowledge/groupon/拼团活动说明.md        | 活动 100321   | 活动 100321：防晒冰袖 5 人团                         |
| G083 | REFUND  | 商家一直不处理退货申请会自动通过吗？  | knowledge/refund/售后与运费.md          | 三、极速退款     | 商家 24 小时未响应，系统自动同意退货申请                   |
| G084 | PRODUCT | 青春版杯子保温比标准版短吗？       | knowledge/product/商品详情-便携保温杯青春版.md | 常见问题       | 是，青春版 8 小时，标准版 12 小时                      |
| G085 | REFUND  | 总则里规定人工客服几点开始上班？     | knowledge/refund/售后服务总则.md         | 第二章 售后时效与响应 | 9:00 起计算响应时间，非工作时间顺延                     |
| G086 | REFUND  | 退货后钱多久能回到账户？        | knowledge/refund/退款规则.md           | 四、退款时效     | 微信/支付宝 1-3 个工作日，银行卡 3-5 个工作日              |
| G087 | REFUND  | 快递费谁出，商家还是我？         | knowledge/refund/售后与运费.md          | 二、运费承担规则   | 质量问题平台承担；个人原因用户承担                        |
| G088 | REFUND  | 拆了包装还能退吗？             | knowledge/refund/退款规则.md           | 2.2 已发货订单  | 需不影响二次销售，包装完好、未使用                        |
| G089 | REFUND  | 换货和退货的运费一样吗？         | knowledge/refund/售后与运费.md          | 四、换货说明     | 一致，按退货规则执行                                |
| G090 | REFUND  | 电子发票什么时候发给我？         | knowledge/refund/售后与运费.md          | 五、发票       | 一般 3-5 个工作日发送至邮箱                           |
| G091 | GROUPON | 参团之后反悔了怎么办？          | knowledge/groupon/拼团规则.md          | 七、取消与退出    | 成团前可取消参团，成团后按普通订单退款流程                    |
| G092 | REFUND  | 商家少发东西了怎么处理？         | knowledge/refund/售后与运费.md          | 二、运费承担规则   | 漏发由平台承担运费，可补发或退款                         |
| G093 | REFUND  | 收到的东西是坏的找谁？          | knowledge/refund/售后服务总则.md         | 第三章 退换货细则   | 质量问题 30 天内免费换新，超期免费维修                     |
| G094 | REFUND  | 衣服洗了还能退吗？             | knowledge/refund/退款规则.md           | 2.2 已发货订单  | 需未洗涤、不影响二次销售，否则不支持无理由退货                |
| G095 | REFUND  | 赠品不见了影响退货吗？          | knowledge/refund/售后服务总则.md         | 第三章 退换货细则   | 赠品需一并退回，缺失按市场价值在退款中扣除                   |
| G096 | REFUND  | 用户原因损坏的商品支持无理由退货吗？ | knowledge/refund/退款规则.md           | 2.5 不可退款场景 | 用户原因导致破损不支持无理由退货                         |
| G097 | GROUPON | 成团前能撤单吗？              | knowledge/groupon/拼团规则.md          | 七、取消与退出    | 可以，成团前随时取消参团，款项原路退回                     |
| G103 | GROUPON | 活动 100555 是什么玩法？       | knowledge/groupon/拼团活动说明.md        | 活动 100555   | 充电宝 2 人团，拼团价 99 元，成团时限 48 小时              |
| G104 | GROUPON | 活动 100666 是什么玩法？       | knowledge/groupon/拼团活动说明.md        | 活动 100666   | 保温杯青春版 3 人团，拼团价 59 元，成团时限 24 小时           |
| G106 | GROUPON | 活动 100789 是什么玩法？       | knowledge/groupon/拼团活动说明.md        | 活动 100789   | 机械键盘 3 人团，拼团价 259 元，成团时限 24 小时            |
| G107 | GROUPON | 活动 100321 是什么玩法？       | knowledge/groupon/拼团活动说明.md        | 活动 100321   | 冰袖 5 人团，拼团价 19.9 元，成团时限 24 小时             |
