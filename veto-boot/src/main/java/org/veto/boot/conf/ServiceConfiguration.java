package org.veto.boot.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.veto.api.context.AuthInterceptor;
import org.veto.shared.SnowflakeIdGenerator;

@Configuration
public class ServiceConfiguration implements WebMvcConfigurer {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(@Value("${service.work.id}") Integer workId, @Value("${service.center.id}") Integer centerId) {
        return new SnowflakeIdGenerator(workId, centerId);
    }


    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**").excludePathPatterns("/swagger-ui.html", "/v3/api-docs/**", "/webjars/**", "/error", "/actuator/**", "/swagger-resources/**", "/swagger-ui/**", "/v2/api-docs/**");
    }

    @Bean
    public RestTemplate restTemplate(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }
}
