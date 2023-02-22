package com.binance.quantify.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String password;
    @Value("${spring.redis.jedis.pool.maxTotal}")
    private int maxTotal;
    @Value("${spring.redis.jedis.pool.maxWait}")
    private int maxWait;
    @Value("${spring.redis.jedis.pool.maxIdle}")
    private int maxIdle;
    @Value("${spring.redis.jedis.pool.minIdle}")
    private int minIdle;

    @Bean(name = "db1RedisTemplate")
    public RedisTemplate<String, Object> db1RedisTemplate() {
        return getRedisTemplate(db1Factory());
    }
    @Bean(name = "db2RedisTemplate")
    public RedisTemplate<String, Object> db2RedisTemplate() {
        return getRedisTemplate(db2Factory());
    }
    @Bean(name = "db3RedisTemplate")
    public RedisTemplate<String, Object> db3RedisTemplate() {
        return getRedisTemplate(db3Factory());
    }
    @Bean(name = "db5RedisTemplate")
    public RedisTemplate<String, Object> db5RedisTemplate() {
        return getRedisTemplate(db5Factory());
    }
    @Bean(name = "db6RedisTemplate")
    public RedisTemplate<String, Object> db6RedisTemplate() {
        return getRedisTemplate(db6Factory());
    }
    @Bean(name = "db10RedisTemplate")
    public RedisTemplate<String, Object> db10RedisTemplate() {
        return getRedisTemplate(db10Factory());
    }

    @Bean
    @Primary
    public LettuceConnectionFactory db1Factory(){
        return new LettuceConnectionFactory(getRedisConfig(1), getClientConfig());
    }

    @Bean
    public LettuceConnectionFactory db2Factory(){
        return new LettuceConnectionFactory(getRedisConfig(2), getClientConfig());
    }

    @Bean
    public LettuceConnectionFactory db3Factory(){
        return new LettuceConnectionFactory(getRedisConfig(3), getClientConfig());
    }

    @Bean
    public LettuceConnectionFactory db5Factory(){
        return new LettuceConnectionFactory(getRedisConfig(5), getClientConfig());
    }

    @Bean
    public LettuceConnectionFactory db6Factory(){
        return new LettuceConnectionFactory(getRedisConfig(6), getClientConfig());
    }

    @Bean
    public LettuceConnectionFactory db10Factory(){
        return new LettuceConnectionFactory(getRedisConfig(10), getClientConfig());
    }

    private RedisStandaloneConfiguration getRedisConfig(int dbNo) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);
        config.setDatabase(dbNo);
        return config;
    }

    private LettuceClientConfiguration getClientConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWaitMillis(maxWait);
        return LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).build();
    }

    public RedisTemplate<String, Object> getRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //将类名称序列化到json串中，去掉会导致得出来的的是LinkedHashMap对象，直接转换实体对象会失败
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        //设置输入时忽略JSON字符串中存在而Java对象实际没有的属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    public StringRedisTemplate getStringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }

}
