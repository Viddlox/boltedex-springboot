package com.example.boltedex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.context.annotation.Bean;

import com.example.boltedex.pokemon.Pokemon;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import java.util.List;

@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String hostName;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.data.redis.password:}")
	private String password;

	@Value("${spring.data.redis.username:}")
	private String username;

	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory() {
		// Guarantees fixed redis connection without autowiring
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(hostName);
		redisStandaloneConfiguration.setPort(port);
		
		// Only set password and username if they are not empty
		if (password != null && !password.trim().isEmpty()) {
			redisStandaloneConfiguration.setPassword(password);
		}
		if (username != null && !username.trim().isEmpty()) {
			redisStandaloneConfiguration.setUsername(username);
		}
		
		return new LettuceConnectionFactory(redisStandaloneConfiguration);
	}

	@Bean
	public RedisTemplate<String, Pokemon> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Pokemon> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Pokemon.class));
		return template;
	}

	@Bean
	public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	@Bean
	public RedisTemplate<String, List<Pokemon.Abilities>> abilitiesRedisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, List<Pokemon.Abilities>> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}
}
