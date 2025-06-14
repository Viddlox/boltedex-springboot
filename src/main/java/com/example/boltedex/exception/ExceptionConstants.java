package com.example.boltedex.exception;

public final class ExceptionConstants {
	private ExceptionConstants() {
		throw new IllegalStateException("Utility class");
	}

	// Error codes
	public static final String CACHE_ERROR = "CACHE_ERROR";
	public static final String API_ERROR = "API_ERROR";
	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
	public static final String WARNING_ERROR = "WARNING_ERROR";

	// Error status codes
	public static final int SERVICE_UNAVAILABLE = 503;
	public static final int BAD_GATEWAY = 502;
	public static final int INTERNAL_SERVER_ERROR = 500;
	public static final int WARNING_STATUS_CODE = 200;

	// Generic error messages
	public static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";
	public static final String UNEXPECTED_ERROR_MESSAGE = "Unexpected error while fetching %s";

	// Cache related messages
	public static final String CACHE_CONNECTION_FAILED_MESSAGE = "Failed to connect to cache";
	public static final String REDIS_CONNECTION_FAILED_MESSAGE = "Failed to connect to Redis";
	public static final String REDIS_POKEMON_PRELOAD_MESSAGE = "Failed to preload Pokemon";
	public static final String REDIS_POKEMON_PRELOAD_DETAILS_MESSAGE = "Failed to preload Pokemon details";
	public static final String REDIS_CONNECTION_ERROR_DETAILED = "Redis connection failed: %s";
	public static final String REDIS_PRELOAD_ERROR_DETAILED = "Failed to preload Pokemon cache: %s";
	public static final String REDIS_PRELOAD_DETAILS_ERROR_DETAILED = "Failed to preload Pokemon details: %s";

	// API related messages
	public static final String API_FETCH_FAILED_MESSAGE = "Failed to fetch Pokemon data";
	public static final String POKEMON_API_ERROR_MESSAGE = "Failed to fetch Pokemon data from API";
	public static final String POKEMON_FETCH_FAILED_MESSAGE = "Failed to fetch Pokemon: %s";
	public static final String POKEMON_API_FETCH_CACHE_ERROR_MESSAGE = "Failed to fetch Pokemon data from cache";
	public static final String POKEMEMON_API_FETCH_INSTANCE_ERROR_MESSAGE = "Failed to fetch Pokemon instance";
	public static final String POKEMON_API_FETCH_EVOLUTION_CHAIN_ERROR_MESSAGE = "Failed to fetch Pokemon evolution chain";
	public static final String POKEMON_API_FETCH_EVOLUTION_STAGE_ERROR_MESSAGE = "Failed to fetch Pokemon evolution stage";
	public static final String POKEMON_API_FETCH_LOCATION_AREA_ENCOUNTERS_ERROR_MESSAGE = "Failed to fetch Pokemon location area encounters";
	public static final String POKEMON_API_FETCH_ABILITIES_ERROR_MESSAGE = "Failed to fetch Pokemon abilities";

	// Warning messages
	public static final String WARNING_MESSAGE = "Warning: %s";
	public static final String WARNING_MESSAGE_REDIS_CONNECTION_FAILED = "Redis is not available, polling preload...";
}
