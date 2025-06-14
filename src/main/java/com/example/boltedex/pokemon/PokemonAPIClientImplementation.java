package com.example.boltedex.pokemon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.lang.Nullable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ZSetOperations;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.boltedex.exception.APIException;
import com.example.boltedex.exception.ExceptionConstants;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

@Service
public class PokemonAPIClientImplementation implements PokemonAPIClient {

	private static final String POKEAPI_BASE_URL = "https://pokeapi.co/api/v2";
	private static final String POKEMON_NAMES_ZSET_KEY = "pokemon:names:sorted";
	private static final String POKEMON_DETAIL_CACHE_PREFIX = "pokemon:detail:";
	private static final String POKEMON_SEARCH_PREFIX = "pokemon:search:";
	private static final String POKEMON_EVOLUTION_CHAIN_CACHE_PREFIX = "pokemon:evolution:chain:";
	private static final String POKEMON_LOCATION_AREA_ENCOUNTERS_CACHE_PREFIX = "pokemon:location:encounters:";
	private static final String POKEMON_SPECIES_CACHE_PREFIX = "pokemon:species:";
	private static final String POKEMON_ABILITIES_CACHE_PREFIX = "pokemon:abilities:";
	private static final int CACHE_TTL_HOURS = 24;
	private static final int CACHE_SEARCH_TTL_HOURS = 1;

	@Autowired
	private RedisTemplate<String, Pokemon> pokemonRedisTemplate;

	@Autowired
	private RedisTemplate<String, String> stringRedisTemplate;

	@Autowired
	private RedisTemplate<String, List<Pokemon.Abilities>> abilitiesRedisTemplate;

	@Autowired
	private RestTemplate restTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public PokemonAPIClientDTO getPokemons(String cursor, int limit, @Nullable String searchQuery) {
		try {
			if (stringRedisTemplate.opsForZSet().size(POKEMON_NAMES_ZSET_KEY) == 0) {
				fetchAndCacheAllPokemonNames();
			}
			ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
			String key = (searchQuery != null && !searchQuery.trim().isEmpty())
					? POKEMON_SEARCH_PREFIX + searchQuery.toLowerCase().trim()
					: POKEMON_NAMES_ZSET_KEY;

			List<String> pokemonNames = getPaginatedPokemonNames(cursor, limit, searchQuery);
			List<Pokemon> pokemons = fetchPokemons(pokemonNames);

			String nextCursor = (pokemonNames.isEmpty()) ? null : pokemonNames.get(pokemonNames.size() - 1);
			Long size = zSetOps.size(key);
			long totalCount = size != null ? size : 0;

			return new PokemonAPIClientDTO(pokemons, nextCursor, totalCount);
		} catch (Exception error) {
			throw new APIException(
				String.format(ExceptionConstants.UNEXPECTED_ERROR_MESSAGE, "Pokemons"),
				ExceptionConstants.CACHE_ERROR,
				ExceptionConstants.SERVICE_UNAVAILABLE,
				Instant.now().toString(),
				error
			);
		}
	}
	
	public Pokemon getPokemon(String name) {
		try {
			String cacheKey = POKEMON_DETAIL_CACHE_PREFIX + name;
			Pokemon cachedPokemon = pokemonRedisTemplate.opsForValue().get(cacheKey);

			if (cachedPokemon != null) {
				return cachedPokemon;
			}

			Pokemon pokemon = fetchPokemonFromAPI(name);
			if (pokemon != null) {
				pokemonRedisTemplate.opsForValue().set(cacheKey, pokemon, CACHE_TTL_HOURS, TimeUnit.HOURS);
			}

			return pokemon;
		} catch (Exception error) {
			throw new APIException(
				String.format(ExceptionConstants.UNEXPECTED_ERROR_MESSAGE, "Pokemon"),
				ExceptionConstants.INTERNAL_ERROR,
				ExceptionConstants.INTERNAL_SERVER_ERROR,
				Instant.now().toString(),
				error
			);
		}
	}

	private void fetchAndCacheAllPokemonNames() {
		try {
			String url = POKEAPI_BASE_URL + "/pokemon?limit=2000";
			JsonNode response = restTemplate.getForObject(url, JsonNode.class);

			if (response == null) {
				throw new APIException(
					ExceptionConstants.POKEMON_API_ERROR_MESSAGE,
					ExceptionConstants.API_ERROR,
					ExceptionConstants.BAD_GATEWAY,
					Instant.now().toString()
				);
			}

			JsonNode results = response.get("results");
			if (results != null) {
				ZSetOperations<String, String> ZSetOps = stringRedisTemplate.opsForZSet();
				for (JsonNode result : results) {
					String name = result.get("name").asText();
					ZSetOps.add(POKEMON_NAMES_ZSET_KEY, name, 0);
				}
				stringRedisTemplate.expire(POKEMON_NAMES_ZSET_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);
			}
		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMON_API_FETCH_CACHE_ERROR_MESSAGE,
				ExceptionConstants.CACHE_ERROR,
				ExceptionConstants.SERVICE_UNAVAILABLE,
				Instant.now().toString(),
				error
			);
		}
	}

	private List<String> getPaginatedPokemonNames(String cursor, int limit, @Nullable String searchQuery) {
		ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			return getPaginatedSearchResults(cursor, limit, searchQuery.toLowerCase().trim());
		}

		if (cursor == null || cursor.isEmpty()) {
			Set<String> names = zSetOps.range(POKEMON_NAMES_ZSET_KEY, 0, limit - 1);
			return new ArrayList<>(names);
		} else {
			Long cursorRank = zSetOps.rank(POKEMON_NAMES_ZSET_KEY, cursor);
			if (cursorRank == null) {
				Set<String> names = zSetOps.range(POKEMON_NAMES_ZSET_KEY, 0, limit - 1);
				return new ArrayList<>(names);
			}
			long startIndex = cursorRank + 1;
			long endIndex = startIndex + limit - 1;
			Set<String> names = zSetOps.range(POKEMON_NAMES_ZSET_KEY, startIndex, endIndex);
			return new ArrayList<>(names);
		}
	}

	private List<String> getPaginatedSearchResults(String cursor, int limit, String searchQuery) {
		String searchCacheKey = POKEMON_SEARCH_PREFIX + searchQuery;
		ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

		// Check if search results are cached (ensure all elements have score 0)
		if (zSetOps.size(searchCacheKey) == 0) {
			cacheSearchResults(searchQuery, searchCacheKey);
		}

		// range() will return in lexicographical order since all scores are 0
		if (cursor == null || cursor.isEmpty()) {
			Set<String> names = zSetOps.range(searchCacheKey, 0, limit - 1);
			return new ArrayList<>(names);
		} else {
			Long cursorRank = zSetOps.rank(searchCacheKey, cursor);
			if (cursorRank == null) {
				Set<String> names = zSetOps.range(searchCacheKey, 0, limit - 1);
				return new ArrayList<>(names);
			}

			long startIndex = cursorRank + 1;
			long endIndex = startIndex + limit - 1;
			Set<String> names = zSetOps.range(searchCacheKey, startIndex, endIndex);
			return new ArrayList<>(names);
		}
	}

	private void cacheSearchResults(String searchQuery, String cacheKey) {
		ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
		Set<String> allNames = zSetOps.range(POKEMON_NAMES_ZSET_KEY, 0, -1);

		if (allNames == null) {
			allNames = new HashSet<>();
		}

		for (String name : allNames) {
			if (name.toLowerCase().contains(searchQuery)) {
				zSetOps.add(cacheKey, name, 0);
			}
		}

		stringRedisTemplate.expire(cacheKey, CACHE_SEARCH_TTL_HOURS, TimeUnit.HOURS);
	}

	private List<Pokemon> fetchPokemons(List<String> pokemonNames) {
		List<Pokemon> pokemons = new ArrayList<>();
		for (String name : pokemonNames) {
			String cacheKey = POKEMON_DETAIL_CACHE_PREFIX + name;
			Pokemon pokemon = pokemonRedisTemplate.opsForValue().get(cacheKey);

			if (pokemon == null) {
				pokemon = fetchPokemonFromAPI(name);

				if (pokemon != null) {
					pokemonRedisTemplate.opsForValue().set(cacheKey, pokemon, CACHE_TTL_HOURS, TimeUnit.HOURS);
				}
			}
			if (pokemon != null) {
				pokemons.add(pokemon);
			}
		}
		return pokemons;
	}

	private Pokemon fetchPokemonFromAPI(String name) {
		try {
			String url = POKEAPI_BASE_URL + "/pokemon/" + name;
			JsonNode pokemonData = restTemplate.getForObject(url, JsonNode.class);
			return mapToPokemon(pokemonData);
		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMEMON_API_FETCH_INSTANCE_ERROR_MESSAGE,
				ExceptionConstants.INTERNAL_ERROR,
				ExceptionConstants.INTERNAL_SERVER_ERROR,
				Instant.now().toString(),
				error
			);
		}
	}

	public Pokemon mapToPokemon(JsonNode data) {
		Pokemon pokemon = new Pokemon();

		pokemon.setId(data.get("id").asInt());
		pokemon.setName(data.get("name").asText());
		pokemon.setHeight(data.get("height").asInt());
		pokemon.setWeight(data.get("weight").asInt());

		// Map types
		List<String> types = new ArrayList<>();
		JsonNode typesArray = data.get("types");
		for (JsonNode typeNode : typesArray) {
			types.add(typeNode.get("type").get("name").asText());
		}
		pokemon.setTypes(types);

		// Map weaknesses and resistances
		Map<String, Double> weaknesses = new HashMap<>();
		Map<String, Double> resistances = new HashMap<>();
		Map<String, Double> immunities = new HashMap<>();
		for (String attackType : PokemonTypeEffectiveness.TYPES) {
			double effectiveness = PokemonTypeEffectiveness.getEffectiveness(attackType, types);
			if (effectiveness > 1.0) {
				weaknesses.put(attackType, effectiveness);
			} else if (effectiveness == 0.0) {
				immunities.put(attackType, effectiveness);
			} else if (effectiveness < 1.0) {
				resistances.put(attackType, effectiveness);
			}
		}
		pokemon.setWeaknesses(weaknesses);
		pokemon.setResistances(resistances);
		pokemon.setImmunities(immunities);

		// Map stats
		Pokemon.Stats stats = new Pokemon.Stats();
		JsonNode statsArray = data.get("stats");
		for (JsonNode statNode : statsArray) {
			String statName = statNode.get("stat").get("name").asText();
			int baseStat = statNode.get("base_stat").asInt();

			switch (statName) {
				case "hp":
					stats.setHp(baseStat);
					break;
				case "attack":
					stats.setAttack(baseStat);
					break;
				case "defense":
					stats.setDefense(baseStat);
					break;
				case "speed":
					stats.setSpeed(baseStat);
					break;
				case "special-attack":
					stats.setSpecialAttack(baseStat);
					break;
				case "special-defense":
					stats.setSpecialDefense(baseStat);
					break;
			}
		}
		pokemon.setBaseStats(stats);

		Pokemon.Sprites sprites = new Pokemon.Sprites();
		JsonNode spritesNode = data.path("sprites");

		// Try showdown .gif sprites first
		JsonNode showdownNode = spritesNode.path("other").path("showdown");

		if (!showdownNode.isMissingNode() && !showdownNode.isNull()) {
			sprites.setFrontDefault(showdownNode.path("front_default").asText(null));
			sprites.setBackDefault(showdownNode.path("back_default").asText(null));
			sprites.setFrontShiny(showdownNode.path("front_shiny").asText(null));
			sprites.setBackShiny(showdownNode.path("back_shiny").asText(null));
		}

		pokemon.setSprites(sprites);

		return pokemon;
	}

	public List<Pokemon.EvolutionStage> getPokemonEvolutionChain(String pokemonName) {
		try {
			// Step 1: Get species data to find evolution chain URL
			JsonNode speciesData = getSpeciesData(pokemonName);
			if (speciesData == null || !speciesData.has("evolution_chain") ||
					speciesData.get("evolution_chain") == null ||
					!speciesData.get("evolution_chain").has("url")) {
				return new ArrayList<>();
			}

			String evolutionChainUrl = speciesData.get("evolution_chain").get("url").asText();

			if (evolutionChainUrl.endsWith("/")) {
				evolutionChainUrl = evolutionChainUrl.substring(0, evolutionChainUrl.length() - 1);
			}

			// Now extract the last segment
			String chainId = evolutionChainUrl.substring(evolutionChainUrl.lastIndexOf('/') + 1);

			// Step 2: Get evolution chain data (cached)
			JsonNode evolutionChainData = getEvolutionChainData(chainId);
			if (evolutionChainData == null || !evolutionChainData.has("chain")) {
				return new ArrayList<>();
			}

			// Step 3: Parse evolution chain recursively
			return parseEvolutionChain(evolutionChainData.get("chain"));
		} catch (Exception error) {
			throw new APIException(
				String.format(ExceptionConstants.UNEXPECTED_ERROR_MESSAGE, "Evolution chain"),
				ExceptionConstants.INTERNAL_ERROR,
				ExceptionConstants.INTERNAL_SERVER_ERROR,
				Instant.now().toString(),
				error
			);
		}
	}

	private JsonNode getSpeciesData(String pokemonName) {
		String cacheKey = POKEMON_SPECIES_CACHE_PREFIX + pokemonName;
		String cachedData = stringRedisTemplate.opsForValue().get(cacheKey);

		if (cachedData != null) {
			try {
				return objectMapper.readTree(cachedData);
			} catch (Exception error) {
				throw new APIException(
					"Error parsing species data for: " + pokemonName,
					ExceptionConstants.CACHE_ERROR,
					ExceptionConstants.SERVICE_UNAVAILABLE,
					Instant.now().toString(),
					error
				);
			}
		}

		// Fetch from API
		String url = POKEAPI_BASE_URL + "/pokemon-species/" + pokemonName;
		JsonNode speciesData = restTemplate.getForObject(url, JsonNode.class);

		// Cache the result
		try {
			stringRedisTemplate.opsForValue().set(cacheKey,
					objectMapper.writeValueAsString(speciesData),
					CACHE_TTL_HOURS, TimeUnit.HOURS);
		} catch (Exception error) {
			throw new APIException(
				String.format(ExceptionConstants.UNEXPECTED_ERROR_MESSAGE, "Species data"),
				ExceptionConstants.CACHE_ERROR,
				ExceptionConstants.SERVICE_UNAVAILABLE,
				Instant.now().toString(),
				error
			);
		}

		return speciesData;
	}

	private JsonNode getEvolutionChainData(String chainId) {
		String cacheKey = POKEMON_EVOLUTION_CHAIN_CACHE_PREFIX + chainId;
		String cachedData = stringRedisTemplate.opsForValue().get(cacheKey);

		if (cachedData != null) {
			try {
				return objectMapper.readTree(cachedData);
			} catch (Exception error) {
				throw new APIException(
					ExceptionConstants.POKEMON_API_FETCH_EVOLUTION_CHAIN_ERROR_MESSAGE,
					ExceptionConstants.CACHE_ERROR,
					ExceptionConstants.SERVICE_UNAVAILABLE,
					Instant.now().toString(),
					error
				);
			}
		}

		// Fetch from API
		String url = POKEAPI_BASE_URL + "/evolution-chain/" + chainId;
		JsonNode chainData = restTemplate.getForObject(url, JsonNode.class);

		// Cache the result
		try {
			stringRedisTemplate.opsForValue().set(cacheKey,
					objectMapper.writeValueAsString(chainData),
					CACHE_TTL_HOURS, TimeUnit.HOURS);
		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMON_API_FETCH_EVOLUTION_CHAIN_ERROR_MESSAGE,
				ExceptionConstants.CACHE_ERROR,
				ExceptionConstants.SERVICE_UNAVAILABLE,
				Instant.now().toString(),
				error
			);
		}

		return chainData;
	}

	private List<Pokemon.EvolutionStage> parseEvolutionChain(JsonNode chainNode) {
		List<Pokemon.EvolutionStage> evolutionStages = new ArrayList<>();

		if (chainNode == null || !chainNode.has("species")) {
			return evolutionStages;
		}

		// Parse current stage
		JsonNode speciesNode = chainNode.get("species");
		if (speciesNode != null && speciesNode.has("name")) {
			String speciesName = speciesNode.get("name").asText();
			Pokemon.EvolutionStage stage = createEvolutionStage(speciesName);
			if (stage != null) {
				evolutionStages.add(stage);
			}
		}

		// Recursively parse evolves_to
		JsonNode evolvesToArray = chainNode.get("evolves_to");
		if (evolvesToArray != null && evolvesToArray.isArray()) {
			for (JsonNode evolveToNode : evolvesToArray) {
				evolutionStages.addAll(parseEvolutionChain(evolveToNode));
			}
		}

		return evolutionStages;
	}

	private Pokemon.EvolutionStage createEvolutionStage(String pokemonName) {
		try {
			// Check if we already have this Pokemon's basic data cached
			Pokemon cachedPokemon = pokemonRedisTemplate.opsForValue().get(POKEMON_DETAIL_CACHE_PREFIX + pokemonName);

			if (cachedPokemon != null) {
				// Use cached data
				Pokemon.EvolutionStage stage = new Pokemon.EvolutionStage();
				stage.setId(cachedPokemon.getId());
				stage.setName(cachedPokemon.getName());
				stage.setSprites(cachedPokemon.getSprites());
				return stage;
			}

			// Need to fetch basic Pokemon data for ID and sprites
			String url = POKEAPI_BASE_URL + "/pokemon/" + pokemonName;
			JsonNode pokemonData = restTemplate.getForObject(url, JsonNode.class);

			if (pokemonData == null) {
				return null;
			}

			Pokemon.EvolutionStage stage = new Pokemon.EvolutionStage();
			stage.setId(pokemonData.get("id").asInt());
			stage.setName(pokemonData.get("name").asText());

			Pokemon.Sprites sprites = new Pokemon.Sprites();
			JsonNode spritesNode = pokemonData.path("sprites");

			// Try showdown .gif sprites first
			JsonNode showdownNode = spritesNode.path("other").path("showdown");

			if (!showdownNode.isMissingNode() && !showdownNode.isNull()) {
				sprites.setFrontDefault(showdownNode.path("front_default").asText(null));
				sprites.setBackDefault(showdownNode.path("back_default").asText(null));
				sprites.setFrontShiny(showdownNode.path("front_shiny").asText(null));
				sprites.setBackShiny(showdownNode.path("back_shiny").asText(null));
			}

			// Fallback to static .png sprites if any are still missing
			if (sprites.getFrontDefault() == null && spritesNode.has("front_default")
					&& !spritesNode.get("front_default").isNull()) {
				sprites.setFrontDefault(spritesNode.get("front_default").asText());
			}
			if (sprites.getBackDefault() == null && spritesNode.has("back_default")
					&& !spritesNode.get("back_default").isNull()) {
				sprites.setBackDefault(spritesNode.get("back_default").asText());
			}
			if (sprites.getFrontShiny() == null && spritesNode.has("front_shiny")
					&& !spritesNode.get("front_shiny").isNull()) {
				sprites.setFrontShiny(spritesNode.get("front_shiny").asText());
			}
			if (sprites.getBackShiny() == null && spritesNode.has("back_shiny")
					&& !spritesNode.get("back_shiny").isNull()) {
				sprites.setBackShiny(spritesNode.get("back_shiny").asText());
			}

			stage.setSprites(sprites);

			return stage;

		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMON_API_FETCH_EVOLUTION_STAGE_ERROR_MESSAGE,
				ExceptionConstants.INTERNAL_ERROR,
				ExceptionConstants.INTERNAL_SERVER_ERROR,
				Instant.now().toString(),
				error
			);
		}
	}

	public List<String> getPokemonLocationAreaEncounters(String pokemonName) {
		try {
			String cacheKey = POKEMON_LOCATION_AREA_ENCOUNTERS_CACHE_PREFIX + pokemonName;
			String cachedData = stringRedisTemplate.opsForValue().get(cacheKey);

			if (cachedData != null) {
				try {
					return objectMapper.readValue(cachedData, new TypeReference<List<String>>() {});
				} catch (Exception error) {
					throw new APIException(
						ExceptionConstants.POKEMON_API_FETCH_LOCATION_AREA_ENCOUNTERS_ERROR_MESSAGE,
						ExceptionConstants.CACHE_ERROR,
						ExceptionConstants.SERVICE_UNAVAILABLE,
						Instant.now().toString(),
						error
					);
				}
			}

			String url = POKEAPI_BASE_URL + "/pokemon/" + pokemonName + "/encounters";
			JsonNode encountersData = restTemplate.getForObject(url, JsonNode.class);

			List<String> encounters = new ArrayList<>();

			if (encountersData != null && encountersData.isArray()) {
				for (JsonNode encounterNode : encountersData) {
					JsonNode locationArea = encounterNode.get("location_area");
					if (locationArea != null && locationArea.has("name")) {
						encounters.add(locationArea.get("name").asText());
					}
				}
			}

			// Cache the result
			stringRedisTemplate.opsForValue().set(cacheKey,
					objectMapper.writeValueAsString(encounters),
					CACHE_TTL_HOURS, TimeUnit.HOURS);

			return encounters;

		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMON_API_FETCH_LOCATION_AREA_ENCOUNTERS_ERROR_MESSAGE,
				ExceptionConstants.API_ERROR,
				ExceptionConstants.BAD_GATEWAY,
				Instant.now().toString(),
				error
			);
		}
	}

	public List<Pokemon.Abilities> getPokemonAbilities(String pokemonName) {
		try {
			String cacheKey = POKEMON_ABILITIES_CACHE_PREFIX + pokemonName;
			List<Pokemon.Abilities> cachedAbilities = abilitiesRedisTemplate.opsForValue().get(cacheKey);

			if (cachedAbilities != null && !cachedAbilities.isEmpty()
					&& !cachedAbilities.get(0).getDescription().isEmpty()) {
				return cachedAbilities;
			}

			List<Pokemon.Abilities> abilities = new ArrayList<>();
			String url = POKEAPI_BASE_URL + "/pokemon/" + pokemonName;
			JsonNode pokemonData = restTemplate.getForObject(url, JsonNode.class);

			if (pokemonData != null) {
				JsonNode abilitiesNode = pokemonData.path("abilities");

				for (JsonNode abilityNode : abilitiesNode) {
					Pokemon.Abilities ability = new Pokemon.Abilities();
					String abilityName = abilityNode.get("ability").get("name").asText();
					ability.setName(abilityName);

					ability.setHidden(abilityNode.get("is_hidden").asBoolean());

					String abilityUrl = abilityNode.get("ability").get("url").asText();
					JsonNode abilityData = restTemplate.getForObject(abilityUrl, JsonNode.class);
					String description = "";
					if (abilityData != null) {
						JsonNode effectEntries = abilityData.path("effect_entries");
						for (JsonNode effect : effectEntries) {
							if (effect.get("language").get("name").asText().equals("en")) {
								description = effect.get("short_effect").asText();
								break;
							}
						}
					}
					ability.setDescription(description);
					abilities.add(ability);
				}
			}

			// Cache the updated abilities
			if (!abilities.isEmpty()) {
				abilitiesRedisTemplate.opsForValue().set(cacheKey, abilities, CACHE_TTL_HOURS, TimeUnit.HOURS);
			}

			return abilities;
		} catch (Exception error) {
			throw new APIException(
				ExceptionConstants.POKEMON_API_FETCH_ABILITIES_ERROR_MESSAGE,
				ExceptionConstants.API_ERROR,
				ExceptionConstants.BAD_GATEWAY,
				Instant.now().toString(),
				error
			);
		}
	}

}
