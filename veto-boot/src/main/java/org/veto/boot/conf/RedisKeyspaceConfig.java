package org.veto.boot.conf;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.veto.core.common.RedisConfigKeyConstants;
import org.veto.core.common.ServiceConfig;

/**
 * listen redis key change event, to update needed data
 */
@Component
@Slf4j
public class RedisKeyspaceConfig {

    @Resource
    private ServiceConfig serviceConfig;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        init(container);

        return container;
    }

    public void init(RedisMessageListenerContainer redisMessageListenerContainer){
        initServiceConfig(redisMessageListenerContainer);
    }

    protected void initServiceConfig(RedisMessageListenerContainer redisMessageListenerContainer){
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
            log.info("检测到键修改，键为: {}, 事件 {}", new String(message.getChannel()).split(":")[1], new String(message.getBody()));
            serviceConfig.refresh();
        }, new PatternTopic("__keyspace@*__:" + RedisConfigKeyConstants.KEY_PREFIX + "*"));
    }
}
