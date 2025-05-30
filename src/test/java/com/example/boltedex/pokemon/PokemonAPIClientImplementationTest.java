package com.example.boltedex.pokemon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PokemonAPIClientImplementationTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private RedisTemplate<String, Pokemon> pokemonRedisTemplate;

	@Mock
	private RedisTemplate<String, String> stringRedisTemplate;

	@Mock
	private RedisTemplate<String, List<Pokemon.Abilities>> abilitiesRedisTemplate;

	@Mock
	private ZSetOperations<String, String> zSetOperations;

	@Mock
	private ValueOperations<String, Pokemon> valueOperations;

	@InjectMocks
	private PokemonAPIClientImplementation pokemonAPIClient;

	private ObjectMapper objectMapper = new ObjectMapper();
	private JsonNode mockPokemonResponse;
	private JsonNode mockPokemonListResponse;

	@BeforeEach
	void setUp() throws Exception {
		// Mock Redis operations
		when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(pokemonRedisTemplate.opsForValue()).thenReturn(valueOperations);

		// Create mock JSON responses
		mockPokemonListResponse = objectMapper.readTree("""
				{
				    "results": [
				        {"name": "pikachu", "url": "https://pokeapi.co/api/v2/pokemon/25/"},
				        {"name": "charizard", "url": "https://pokeapi.co/api/v2/pokemon/6/"},
				        {"name": "blastoise", "url": "https://pokeapi.co/api/v2/pokemon/9/"}
				    ]
				}
				""");

		mockPokemonResponse = objectMapper.readTree("""
				{
				    "id": 25,
				    "name": "pikachu",
				    "height": 4,
				    "weight": 60,
				    "types": [
				        {"type": {"name": "electric"}}
				    ],
				    "stats": [
				        {"stat": {"name": "hp"}, "base_stat": 35},
				        {"stat": {"name": "attack"}, "base_stat": 55},
				        {"stat": {"name": "defense"}, "base_stat": 40},
				        {"stat": {"name": "speed"}, "base_stat": 90},
				        {"stat": {"name": "special-attack"}, "base_stat": 50},
				        {"stat": {"name": "special-defense"}, "base_stat": 50}
				    ],
				    "sprites": {
				        "front_default": "https://example.com/pikachu-front.png",
				        "back_default": "https://example.com/pikachu-back.png",
				        "front_shiny": "https://example.com/pikachu-shiny-front.png",
				        "back_shiny": "https://example.com/pikachu-shiny-back.png",
				        "other": {
				            "showdown": {
				                "front_default": "https://example.com/pikachu-showdown.gif",
				                "back_default": "https://example.com/pikachu-showdown-back.gif",
				                "front_shiny": "https://example.com/pikachu-showdown-shiny.gif",
				                "back_shiny": "https://example.com/pikachu-showdown-shiny-back.gif"
				            }
				        }
				    }
				}
				""");
	}

	@Test
	void shouldGetPokemonsWithEmptyCache() {
		// Arrange

		// Simulate empty name cache
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(0L);

		// Mock fetching all Pokémon names from the API
		when(restTemplate.getForObject(
				eq("https://pokeapi.co/api/v2/pokemon?limit=2000"),
				eq(JsonNode.class))).thenReturn(mockPokemonListResponse);

		// Mock successful caching of fetched names
		when(zSetOperations.add(eq("pokemon:names:sorted"), eq("pikachu"), eq(0.0))).thenReturn(true);
		when(zSetOperations.add(eq("pokemon:names:sorted"), eq("charizard"), eq(0.0))).thenReturn(true);
		when(zSetOperations.add(eq("pokemon:names:sorted"), eq("blastoise"), eq(0.0))).thenReturn(true);

		// Simulate paginated name retrieval
		Set<String> mockNames = new LinkedHashSet<>(Arrays.asList("pikachu", "charizard"));
		when(zSetOperations.range("pokemon:names:sorted", 0, 1)).thenReturn(mockNames);

		// Mock cache miss for both Pokémon
		when(valueOperations.get("pokemon:detail:pikachu")).thenReturn(null);
		when(valueOperations.get("pokemon:detail:charizard")).thenReturn(null);

		// Mock successful API fetch for individual Pokémon
		when(restTemplate.getForObject(
				eq("https://pokeapi.co/api/v2/pokemon/pikachu"),
				eq(JsonNode.class))).thenReturn(mockPokemonResponse);

		when(restTemplate.getForObject(
				eq("https://pokeapi.co/api/v2/pokemon/charizard"),
				eq(JsonNode.class))).thenReturn(mockPokemonResponse);

		// Act
		PokemonAPIClientDTO result = pokemonAPIClient.getPokemons(null, 2, null);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getResults().size());

		// Verify flow and interactions
		verify(zSetOperations, times(2)).size("pokemon:names:sorted");
		verify(restTemplate).getForObject("https://pokeapi.co/api/v2/pokemon?limit=2000", JsonNode.class);
		verify(zSetOperations).range("pokemon:names:sorted", 0, 1);
		verify(restTemplate).getForObject("https://pokeapi.co/api/v2/pokemon/pikachu", JsonNode.class);
		verify(restTemplate).getForObject("https://pokeapi.co/api/v2/pokemon/charizard", JsonNode.class);

		// Verify that caching was triggered for each fetched Pokémon
		verify(valueOperations, times(2)).set(anyString(), any(Pokemon.class), eq(24L), eq(TimeUnit.HOURS));
	}

	@Test
	void shouldGetPokemonsWithCachedData() {
		// Arrange

		// Mock cached pokemon names exist
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(3L);

		Set<String> mockNames = new LinkedHashSet<>(Arrays.asList("pikachu", "charizard"));
		when(zSetOperations.range("pokemon:names:sorted", 0, 1)).thenReturn(mockNames);

		// Mock cached pokemon details (cache hit)
		Pokemon cachedPikachu = createMockPokemon("pikachu", 25);
		Pokemon cachedCharizard = createMockPokemon("charizard", 6);

		when(valueOperations.get("pokemon:detail:pikachu")).thenReturn(cachedPikachu);
		when(valueOperations.get("pokemon:detail:charizard")).thenReturn(cachedCharizard);

		// Act
		PokemonAPIClientDTO result = pokemonAPIClient.getPokemons(null, 2, null);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getResults().size());
		assertEquals("pikachu", result.getResults().get(0).getName());
		assertEquals("charizard", result.getResults().get(1).getName());

		// Verify no API calls were made (all cached)
		verify(restTemplate, never()).getForObject(contains("pokemon/"), eq(JsonNode.class));

		// Verify cache was checked
		verify(valueOperations).get("pokemon:detail:pikachu");
		verify(valueOperations).get("pokemon:detail:charizard");
	}

	@Test
	void shouldGetPokemonsWithSearchQuery() {
		// Arrange

		// Mock cached pokemon names exist
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(3L);

		// Mock search cache miss
		String searchCacheKey = "pokemon:search:pika";
		when(zSetOperations.size(searchCacheKey)).thenReturn(0L).thenReturn(1L); // First call: cache miss, second call: total count

		// Mock all pokemon names for search
		Set<String> allNames = new LinkedHashSet<>(Arrays.asList("pikachu", "charizard", "blastoise"));
		when(zSetOperations.range("pokemon:names:sorted", 0, -1)).thenReturn(allNames);

		// Mock search results caching
		when(zSetOperations.add(eq(searchCacheKey), eq("pikachu"), eq(0.0))).thenReturn(true);

		// Mock getting search results
		Set<String> searchResults = new LinkedHashSet<>(Collections.singletonList("pikachu"));
		when(zSetOperations.range(searchCacheKey, 0, 0)).thenReturn(searchResults); // Changed from 0,1 to 0,0 for limit=1

		// Mock cached pokemon details
		Pokemon cachedPikachu = createMockPokemon("pikachu", 25);
		when(valueOperations.get("pokemon:detail:pikachu")).thenReturn(cachedPikachu);

		// Act
		PokemonAPIClientDTO result = pokemonAPIClient.getPokemons(null, 1, "pika");

		// Assert
		assertNotNull(result);
		assertEquals(1, result.getResults().size());
		assertEquals("pikachu", result.getResults().get(0).getName());

		// Verify search caching
		verify(zSetOperations).add(searchCacheKey, "pikachu", 0.0);
		verify(stringRedisTemplate).expire(searchCacheKey, 1, TimeUnit.HOURS);
	}

	@Test
	void shouldGetPokemonsWithCursor() {
		// Arrange

		// Mock cached pokemon names exist
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(5L);

		// Mock cursor rank
		when(zSetOperations.rank("pokemon:names:sorted", "pikachu")).thenReturn(0L);

		// Mock getting names after cursor
		Set<String> mockNames = new LinkedHashSet<>(Arrays.asList("charizard", "blastoise"));
		when(zSetOperations.range("pokemon:names:sorted", 1, 2)).thenReturn(mockNames);

		// Mock cached pokemon details
		Pokemon cachedCharizard = createMockPokemon("charizard", 6);
		Pokemon cachedBlastoise = createMockPokemon("blastoise", 9);

		when(valueOperations.get("pokemon:detail:charizard")).thenReturn(cachedCharizard);
		when(valueOperations.get("pokemon:detail:blastoise")).thenReturn(cachedBlastoise);

		// Act
		PokemonAPIClientDTO result = pokemonAPIClient.getPokemons("pikachu", 2, null);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getResults().size());
		assertEquals("charizard", result.getResults().get(0).getName());
		assertEquals("blastoise", result.getResults().get(1).getName());

		verify(zSetOperations).rank("pokemon:names:sorted", "pikachu");
		verify(zSetOperations).range("pokemon:names:sorted", 1, 2);
	}

	@Test
	void shouldHandleAPIError() {
		// Arrange
		// Mock empty cache
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(0L);
		// Mock API error when fetching pokemon list
		when(restTemplate.getForObject(
				eq("https://pokeapi.co/api/v2/pokemon?limit=2000"),
				eq(JsonNode.class))).thenThrow(new RuntimeException("API Error"));

		// Act
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			pokemonAPIClient.getPokemons(null, 1, null);
		});

		// Assert
		assertTrue(exception.getMessage().contains("Error fetching Pokemons"));
	}

	@Test
	void shouldHandleIndividualPokemonAPIError() {
		// Arrange
		// Simulate one Pokémon in cache (pikachu), no detail in cache, and API throws
		// on fetch
		when(zSetOperations.size("pokemon:names:sorted")).thenReturn(1L).thenReturn(1L); // Two calls: empty check + total count
		Set<String> mockNames = new LinkedHashSet<>(Collections.singletonList("pikachu"));
		when(zSetOperations.range("pokemon:names:sorted", 0, 0)).thenReturn(mockNames);
		when(valueOperations.get("pokemon:detail:pikachu")).thenReturn(null);
		when(restTemplate.getForObject(
				eq("https://pokeapi.co/api/v2/pokemon/pikachu"),
				eq(JsonNode.class))).thenThrow(new RuntimeException("Pokemon API Error"));

		// Act
		PokemonAPIClientDTO result = pokemonAPIClient.getPokemons(null, 1, null);

		// Assert
		assertNotNull(result);
		assertEquals(0, result.getResults().size()); // API failed, Pokemon not added to results
		assertEquals(1, result.getTotalCount()); // Total count is still 1 (from cache size)
	}

	private Pokemon createMockPokemon(String name, int id) {
		Pokemon pokemon = new Pokemon();
		pokemon.setId(id);
		pokemon.setName(name);
		pokemon.setHeight(4);
		pokemon.setWeight(60);
		pokemon.setTypes(Collections.singletonList("electric"));

		// Set basic stats
		Pokemon.Stats stats = new Pokemon.Stats();
		stats.setHp(35);
		stats.setAttack(55);
		stats.setDefense(40);
		stats.setSpeed(90);
		stats.setSpecialAttack(50);
		stats.setSpecialDefense(50);
		pokemon.setBaseStats(stats);

		// Set sprites
		Pokemon.Sprites sprites = new Pokemon.Sprites();
		sprites.setFrontDefault("https://example.com/" + name + "-front.png");
		pokemon.setSprites(sprites);

		return pokemon;
	}
}