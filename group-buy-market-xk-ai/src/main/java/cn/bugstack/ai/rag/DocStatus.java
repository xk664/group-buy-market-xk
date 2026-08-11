package cn.bugstack.ai.rag;

/**
 * 知识文档状态：
 * ACTIVE   正常参与检索
 * DRAFT    草稿（暂不上线）
 * CONFLICT 检测到冲突（被拦截，不参与检索）
 * DISABLED 已下线/文件删除
 */
public enum DocStatus {
    ACTIVE, DRAFT, CONFLICT, DISABLED
}