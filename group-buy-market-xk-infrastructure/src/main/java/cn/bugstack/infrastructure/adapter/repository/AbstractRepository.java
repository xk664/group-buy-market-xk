package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.api.IDCCService;
import cn.bugstack.infrastructure.dcc.DCCService;
import cn.bugstack.infrastructure.redis.IRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.function.Supplier;

public abstract  class AbstractRepository {
    private final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);
    @Resource
    private DCCService dccService;
    @Resource
    private IRedisService redisService;
    protected <T> T getFromCacheOrDb(String key, Supplier<T> daFallback ) {
        //查询缓存
        if(dccService.isCacheOpenSwitch()){
            T value = redisService.getValue(key);
            if(value != null){
                return value;
            }
            value = daFallback.get();
            redisService.setValue(key, value);
            return value;
            //降级
        }else {
            logger.warn("缓存降级 {}", key);
            return daFallback.get();
        }
    }
}
