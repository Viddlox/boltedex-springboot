# BolteDex - Spring Boot Pokemon API Client

A high-performance Pokemon API client built with Spring Boot, featuring Redis-backed caching, cursor-based pagination, and efficient search capabilities.

## Features

### Redis-Backed Search and Pagination
- Efficient pagination using Redis Sorted Sets (ZSets)
- Lexicographical ordering of Pokemon names for consistent pagination
- Cursor-based pagination support for seamless navigation
- Search functionality with cached results and TTL

### Intelligent Caching System
- Preloading of essential pokemon data via startup and CRON schedule
- Multi-level caching strategy:
  - Pokemon name index caching (24-hour TTL)
  - Individual Pokemon details caching (24-hour TTL)
  - Search results caching (1-hour TTL)
- Zero-latency subsequent requests for cached data
- Automatic cache invalidation using TTL
- Fault-tolerant cache miss handling

### Pokemon Data Features
- Comprehensive Pokemon information:
  - Basic details (ID, name, height, weight)
  - Types and type effectiveness
  - Base stats (HP, Attack, Defense, etc.)
  - Sprite URLs for different variations
  - Evolution chain information
  - Location area encounters
  - Abilities with descriptions

### Error Handling
- Graceful API error handling
- Fallback mechanisms for failed requests
- Detailed error messages and logging
- Cache resilience during API downtime

## Technical Stack

### Core Technologies
- Spring Boot
- Redis
- PokeAPI (External Service)

### Key Dependencies
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Architecture

### Caching Strategy
1. **Name Index Caching**
   - Uses Redis ZSet for sorted Pokemon names
   - Enables efficient pagination and search
   - 24-hour TTL with automatic refresh

2. **Detail Caching**
   - Individual Pokemon details cached in Redis
   - JSON serialization for complex objects
   - Automatic cache population on first request

3. **Search Caching**
   - Search results cached in dedicated ZSets
   - 1-hour TTL for search results
   - Prefix-based search implementation

### API Client Implementation
- Interface-based design for flexibility
- Comprehensive error handling
- Configurable timeouts and retries
- Modular architecture for easy extension

## Testing

### Unit Tests
- Comprehensive test coverage
- Mock-based testing using Mockito and Junit
- Redis operation testing
- Error scenario coverage

### Test Categories
1. **Cache Behavior Tests**
   - Empty cache scenarios
   - Cached data retrieval
   - Cache miss handling
   - TTL verification

2. **Search and Pagination Tests**
   - Cursor-based pagination
   - Search functionality
   - Result ordering
   - Edge cases

3. **Error Handling Tests**
   - API error scenarios
   - Cache failure handling
   - Invalid input handling

### Test Implementation
```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PokemonAPIClientImplementationTest {
    // Comprehensive test suite with Redis and API mocking
    // Covers all major functionality and edge cases
}
```

## Usage Examples

### Basic Pokemon Retrieval
```java
// Get a paginated list of Pokemon (cursor, limit, searchQuery)
List<Pokemon> pokemons = pokemonAPIClient.getPokemons(null, 10, null);

// Access Pokemon details
Pokemon pokemon = pokemons.get(0);
System.out.println("ID: " + pokemon.getId());
System.out.println("Name: " + pokemon.getName());
System.out.println("Height: " + pokemon.getHeight());
System.out.println("Weight: " + pokemon.getWeight());
System.out.println("Types: " + pokemon.getTypes());

// Access base stats
Pokemon.Stats stats = pokemon.getBaseStats();
System.out.println("HP: " + stats.getHp());
System.out.println("Attack: " + stats.getAttack());
System.out.println("Defense: " + stats.getDefense());
System.out.println("Speed: " + stats.getSpeed());
System.out.println("Special Attack: " + stats.getSpecialAttack());
System.out.println("Special Defense: " + stats.getSpecialDefense());

// Access sprite URLs
Pokemon.Sprites sprites = pokemon.getSprites();
System.out.println("Default Front: " + sprites.getFrontDefault());
```

### Cursor-Based Pagination
```java
// First page (10 Pokemon)
List<Pokemon> page1 = pokemonAPIClient.getPokemons(null, 10, null);

// Next page using last Pokemon name as cursor
String lastPokemonName = page1.get(page1.size() - 1).getName();
List<Pokemon> page2 = pokemonAPIClient.getPokemons(lastPokemonName, 10, null);
```

### Search with Pagination
```java
// Search for Pokemon containing "char"
List<Pokemon> results = pokemonAPIClient.getPokemons(null, 10, "char");

// Get next page of search results
if (!results.isEmpty()) {
    String lastPokemonName = results.get(results.size() - 1).getName();
    List<Pokemon> nextPage = pokemonAPIClient.getPokemons(lastPokemonName, 10, "char");
}
```

### Evolution Chain Information
```java
// Get the evolution chain for a Pokemon
List<Pokemon.EvolutionStage> evoChain = pokemonAPIClient.getPokemonEvolutionChain("charmander");

// Process evolution stages
for (Pokemon.EvolutionStage stage : evoChain) {
    System.out.println("Pokemon: " + stage.getPokemonName());
    if (stage.getLevel() != null) {
        System.out.println("Evolution Level: " + stage.getLevel());
    }
}
```

### Pokemon Abilities
```java
// Get abilities for a Pokemon
List<Pokemon.Abilities> abilities = pokemonAPIClient.getPokemonAbilities("pikachu");

// Process abilities
for (Pokemon.Abilities ability : abilities) {
    System.out.println("Name: " + ability.getName());
    System.out.println("Description: " + ability.getDescription());
    System.out.println("Is Hidden: " + ability.isHidden());
}
```

### Location Area Encounters
```java
// Get locations where a Pokemon can be encountered
List<String> locations = pokemonAPIClient.getPokemonLocationAreaEncounters("charizard");

// Process locations
for (String location : locations) {
    System.out.println("Can be found in: " + location);
}
```

### Comprehensive Example
```java
// Get complete Pokemon information
String pokemonName = "pikachu";

// Get basic info with search
List<Pokemon> searchResult = pokemonAPIClient.getPokemons(null, 1, pokemonName);
if (!searchResult.isEmpty()) {
    Pokemon pokemon = searchResult.get(0);
    
    // Get additional information
    List<Pokemon.Abilities> abilities = pokemonAPIClient.getPokemonAbilities(pokemonName);
    List<Pokemon.EvolutionStage> evolutionChain = pokemonAPIClient.getPokemonEvolutionChain(pokemonName);
    List<String> locations = pokemonAPIClient.getPokemonLocationAreaEncounters(pokemonName);

    // Build comprehensive Pokemon profile
    System.out.println("=== Pokemon Profile: " + pokemon.getName() + " ===");
    System.out.println("ID: " + pokemon.getId());
    System.out.println("Types: " + pokemon.getTypes());
    
    Pokemon.Stats stats = pokemon.getBaseStats();
    System.out.println("\nBase Stats:");
    System.out.println("HP: " + stats.getHp());
    System.out.println("Attack: " + stats.getAttack());
    System.out.println("Defense: " + stats.getDefense());
    System.out.println("Speed: " + stats.getSpeed());
    System.out.println("Special Attack: " + stats.getSpecialAttack());
    System.out.println("Special Defense: " + stats.getSpecialDefense());
    
    System.out.println("\nAbilities:");
    for (Pokemon.Abilities ability : abilities) {
        System.out.println("- " + ability.getName() + 
                         (ability.isHidden() ? " (Hidden)" : "") + 
                         ": " + ability.getDescription());
    }
    
    System.out.println("\nEvolution Chain:");
    for (Pokemon.EvolutionStage stage : evolutionChain) {
        System.out.println("- " + stage.getPokemonName() + 
                         (stage.getLevel() != null ? " (Level " + stage.getLevel() + ")" : "") +
                         (!stage.getConditions().isEmpty() ? " " + stage.getConditions() : ""));
    }
    
    System.out.println("\nLocations:");
    locations.forEach(location -> System.out.println("- " + location));
}
```

## Configuration

### Application Properties
```properties
# Application Name
spring.application.name=boltedex

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis

# Spring Configuration
spring.main.allow-bean-definition-overriding=true
```

### Environment Setup

1. **Redis Installation**
   ```bash
   # Windows (using Chocolatey)
   choco install redis-64
   
   # macOS (using Homebrew)
   brew install redis
   
   # Ubuntu/Debian
   sudo apt-get install redis-server
   ```

2. **Redis Service**
   ```bash
   # Start Redis service
   redis-server

   # Verify Redis is running
   redis-cli ping
   # Should respond with "PONG"
   ```

3. **Application Setup**
   ```bash
   # Clone the repository
   git clone https://github.com/yourusername/boltedex-springboot.git
   cd boltedex-springboot

   # Build the application
   ./mvnw clean install

   # Run the application
   ./mvnw spring-boot:run
   ```

### Cache Configuration

The application uses Redis for various caching purposes with different TTLs:

```java
// Cache Keys
private static final String POKEMON_NAMES_ZSET_KEY = "pokemon:names:sorted";
private static final String POKEMON_DETAIL_CACHE_PREFIX = "pokemon:detail:";
private static final String POKEMON_SEARCH_PREFIX = "pokemon:search:";
private static final String POKEMON_EVOLUTION_CHAIN_CACHE_PREFIX = "pokemon:evolution:chain:";
private static final String POKEMON_LOCATION_AREA_ENCOUNTERS_CACHE_PREFIX = "pokemon:location:encounters:";
private static final String POKEMON_SPECIES_CACHE_PREFIX = "pokemon:species:";
private static final String POKEMON_ABILITIES_CACHE_PREFIX = "pokemon:abilities:";

// TTL Configuration
private static final int CACHE_TTL_HOURS = 24;  // For most Pokemon data
private static final int SEARCH_CACHE_TTL_HOURS = 1;  // For search results

// Cache Scheduler Configuration
@Scheduled(fixedDelay = Long.MAX_VALUE) // First preload only on startup
public void preloadOnStartup();

@Scheduled(cron = "0 0 3 * * *") // Preload subroutine for basic name-keys of Pokemons
public void preloadPokemonCache() // Only run on startup and 3am daily

@Scheduled(cron = "0 2 3 * * *") // Preload subroutine for basic data-values of Pokemons
public void preloadPokemonDetails() // Runs 2 minutes after preloadPokemonCache()
```

### Redis Data Structures Used

1. **Sorted Sets (ZSET)**
   - Used for Pokemon name index
   - Used for search results
   - Enables efficient pagination and ordering

2. **String Values**
   - Used for Pokemon details
   - Used for evolution chains
   - Used for location data
   - Used for ability information

3. **Key Patterns**
   - `pokemon:names:sorted` - Sorted set of all Pokemon names
   - `pokemon:detail:{name}` - Individual Pokemon details
   - `pokemon:search:{query}` - Search results
   - `pokemon:evolution:chain:{id}` - Evolution chain data
   - `pokemon:location:encounters:{name}` - Location encounter data
   - `pokemon:abilities:{name}` - Pokemon abilities

### Development Environment

- Java 17 or higher
- Spring Boot 3.x
- Redis 6.x or higher
- Maven 3.x

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. 
