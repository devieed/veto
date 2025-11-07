package org.veto.admin.conf;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * 解决jpa无法注入问题
 */
@Configuration
@EnableJpaRepositories(value = "org.veto.core.rdbms.repository")
@EntityScan(basePackages = "org.veto.core.rdbms.bean")
//@EnableElasticsearchRepositories(value = "cn.imerge.dto.index.repository")
@EnableRedisRepositories(value = "org.veto.core.redis")
public class JPAConfig {
}