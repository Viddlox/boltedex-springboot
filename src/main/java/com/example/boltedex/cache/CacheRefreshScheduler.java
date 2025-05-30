package com.example.boltedex.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.boltedex.pokemon.Pokemon;
import com.example.boltedex.pokemon.PokemonAPIClientImplementation;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class CacheRefreshScheduler {

	private static final Logger logger = LoggerFactory.getLogger(CacheRefreshScheduler.class);

	private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";
	private static final String POKEMON_NAMES_ZSET_KEY = "pokemon:names:sorted";
	private static final String POKEMON_DETAIL_CACHE_PREFIX = "pokemon:detail:";
	private static final int CACHE_TTL_HOURS = 24;

	private final AtomicBoolean preloadCompleted = new AtomicBoolean(false);

	@Value("${cache.preload.on-startup:true}")
	private boolean preloadOnStartup;

	@Autowired
	private RedisTemplate<String, String> stringRedisTemplate;

	@Autowired
	private RedisTemplate<String, Pokemon> pokemonRedisTemplate;

	@Autowired
	private PokemonAPIClientImplementation pokemonAPIClient;

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Preload Pokemon names into Redis ZSET on application startup and at 3am daily
	 */
	@Scheduled(cron = "0 0 3 * * *")
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
			stringRedisTemplate.expire(POKEMON_NAMES_ZSET_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);

			logger.info("Successfully preloaded {} Pokemon names into cache", count);

		} catch (Exception e) {
			logger.error("Failed to preload Pokemon cache", e);
		}
	}

	/**
	 * Preload Pokemon details for improved getPokemons API performance
	 */
	@Scheduled(cron = "0 2 3 * * *")
	public void preloadPokemonDetails() {

		logger.info("Starting Pokemon details preload...");

		try {
			// Get all Pokemon names from the sorted set
			Set<String> pokemonNames = stringRedisTemplate.opsForZSet()
					.range(POKEMON_NAMES_ZSET_KEY, 0, -1);

			if (pokemonNames == null || pokemonNames.isEmpty()) {
				logger.warn("No Pokemon names found in cache, skipping details preload");
				return;
			}
			logger.info("Found {} Pokemon names in cache, checking for cached details...", pokemonNames.size());

			int preloaded = 0;
			int skipped = 0;
			int failed = 0;

			for (String name : pokemonNames) {
				String cacheKey = POKEMON_DETAIL_CACHE_PREFIX + name;

				// Skip if already cached and fresh
				if (pokemonRedisTemplate.opsForValue().get(cacheKey) != null) {
					skipped++;
					continue;
				}

				try {
					String url = POKEAPI_BASE_URL + "/pokemon/" + name;
					JsonNode pokemonData = restTemplate.getForObject(url, JsonNode.class);

					if (pokemonData != null) {
						Pokemon pokemon = pokemonAPIClient.mapToPokemon(pokemonData);
						if (pokemon != null) {
							pokemonRedisTemplate.opsForValue().set(cacheKey, pokemon, CACHE_TTL_HOURS, TimeUnit.HOURS);
							preloaded++;
						} else {
							failed++;
						}
					} else {
						failed++;
					}
					if ((preloaded + failed) % 50 == 0) {
						logger.info("Progress: {} preloaded, {} skipped, {} failed", preloaded, skipped, failed);
					}
				} catch (Exception e) {
					logger.warn("Failed to preload Pokemon: {}", name, e);
					failed++;
				}
			}
			logger.info("Pokemon details preload completed. Preloaded: {}, Skipped: {}, Failed: {}",
					preloaded, skipped, failed);

		} catch (Exception e) {
			logger.error("Failed to preload Pokemon details", e);
		}
	}

	/**
	 * Run cache preload on application startup with Redis availability check
	 */
	@Scheduled(fixedDelay = 2000)
	@Async("taskExecutor")
	public void scheduleStartupPreload() {
		if (!preloadOnStartup || preloadCompleted.get()) {
			return;
		}

		if (!isRedisAvailable()) {
			logger.warn("Redis is not available, polling preload...");
			return;
		}

		try {
			logger.info("Redis available, running initial cache preload...");
			preloadPokemonCache();

			Long cacheSize = stringRedisTemplate.opsForZSet().size(POKEMON_NAMES_ZSET_KEY);
			if (cacheSize != null && cacheSize > 0) {
				preloadPokemonDetails();
			}
			preloadCompleted.set(true);
			logger.info("Startup cache preload completed");
		} catch (Exception e) {
			logger.error("Failed to preload cache on startup", e);
		}
	}

	/**
	 * Check if Redis is available and operational
	 */
	private boolean isRedisAvailable() {
		try {
			stringRedisTemplate.opsForValue().set("redis:health:check", "ok", 1, TimeUnit.SECONDS);
			String result = stringRedisTemplate.opsForValue().get("redis:health:check");
			return "ok".equals(result);
		} catch (Exception e) {
			logger.warn("Redis health check failed: {}", e.getMessage());
			return false;
		}
	}
}