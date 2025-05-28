package com.example.boltedex.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;

@Component
public class CacheRefreshScheduler {

	private static final Logger logger = LoggerFactory.getLogger(CacheRefreshScheduler.class);

	private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";
	private static final String POKEMON_NAMES_ZSET_KEY = "pokemon:names:sorted";
	private static final int CACHE_TTL_HOURS = 24;

	@Autowired
	private RedisTemplate<String, String> stringRedisTemplate;

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Preload Pokemon names into Redis ZSET on application startup every 24 hours
	 */
	@Scheduled(fixedRate = 24 * 60 * 60 * 1000)
	public void preloadPokemonCache() {
		logger.info("Starting Pokemon cache preload...");

		try {
			// Check if cache already exists and is fresh
			Long cacheSize = stringRedisTemplate.opsForZSet().size(POKEMON_NAMES_ZSET_KEY);
			Long ttl = stringRedisTemplate.getExpire(POKEMON_NAMES_ZSET_KEY);

			// Skip if cache exists and has more than 12 hours remaining
			if (cacheSize != null && cacheSize > 0 && ttl != null && ttl > 12 * 60 * 60) {
				logger.info("Pokemon cache is fresh, skipping preload. Size: {}, TTL: {} seconds",
						cacheSize, ttl);
				return;
			}

			// Fetch all Pokemon names from API
			String url = POKEAPI_BASE_URL + "/pokemon?limit=2000";
			JsonNode response = restTemplate.getForObject(url, JsonNode.class);

			if (response == null || !response.has("results")) {
				logger.error("Invalid response from PokeAPI");
				return;
			}

			JsonNode results = response.get("results");
			ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

			// Clear existing cache
			stringRedisTemplate.delete(POKEMON_NAMES_ZSET_KEY);

			// Add all Pokemon names with score 0 for lexicographical ordering
			int count = 0;
			for (JsonNode pokemon : results) {
				String name = pokemon.get("name").asText();
				zSetOps.add(POKEMON_NAMES_ZSET_KEY, name, 0);
				count++;
			}

			// Set expiration
			stringRedisTemplate.expire(POKEMON_NAMES_ZSET_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);

			logger.info("Successfully preloaded {} Pokemon names into cache", count);

		} catch (Exception e) {
			logger.error("Failed to preload Pokemon cache", e);
		}
	}

	/**
	 * Run cache preload immediately on application startup
	 */
	@Scheduled(fixedDelay = Long.MAX_VALUE)
	public void preloadOnStartup() {
		logger.info("Running initial Pokemon cache preload on startup...");
		preloadPokemonCache();
	}
}
