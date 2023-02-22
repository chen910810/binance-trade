package com.binance.quantify.config;

import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
@Log4j2
public class BALockConfig {

    @Resource
    private RedisTemplate<String, String> redisTemplate2;

    @Pointcut("@annotation(com.binance.quantify.config.annotation.BALock)")
    public void annotationPointcut() {
    }

    @Around("annotationPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("-----------根据用户和品种添加锁-------------");
        Object[] args = joinPoint.getArgs();// 获取参数值
        if(args.length > 0){
            try{
                BinanceMemberConfig binanceMemberConfig = (BinanceMemberConfig) args[0];
                String symbol = binanceMemberConfig.getSymbol();
                Integer memberId = binanceMemberConfig.getMemberId();
                String lockKey = String.format("%s:%s", symbol, memberId);
                String token = getRedisLockToken(lockKey);
                if(StringUtils.isBlank(token)){
                    log.debug("当前正在处理[{} - {}]任务", memberId, symbol);
                    return null;
                }
                Object result = joinPoint.proceed();
                releaseRedisLockToken(lockKey, token);
                return result;
            }catch (Exception e) {
                log.debug("Process Active Member Job Error : {}", e.getMessage());
            }
        }
        return null;
    }

    private String getRedisLockToken(String lockKey){
        RedisConnectionFactory connectionFactory = redisTemplate2.getConnectionFactory();
        RedisConnection connection = null;
        try{
            if (connectionFactory != null) {
                connection = connectionFactory.getConnection();
                String token = UUID.randomUUID().toString();
                Boolean result = connection.set(lockKey.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8), Expiration.from(10, TimeUnit.SECONDS), RedisStringCommands.SetOption.SET_IF_ABSENT);
                if (org.apache.commons.lang3.ObjectUtils.firstNonNull(result)){
                    return token;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (connectionFactory != null) {
                RedisConnectionUtils.releaseConnection(connection, connectionFactory);
            }
        }
        return null;
    }

    private void releaseRedisLockToken(String lockKey, String token){
        String lockResult = redisTemplate2.opsForValue().get(lockKey);
        if(lockResult != null && StringUtils.compare(lockResult, token) == 0){
//            log.debug("release lock token : [{}] is right", lockResult);
            redisTemplate2.delete(lockKey);
        }
    }

}
