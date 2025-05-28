package com.example.boltedex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.context.annotation.Bean;

import com.example.boltedex.pokemon.Pokemon;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import java.util.List;

@Configuration
public class RedisConfig {
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
