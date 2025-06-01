package com.example.boltedex.pokemon;

import java.util.List;
import org.springframework.lang.Nullable;

public interface PokemonAPIClient {
	PokemonAPIClientDTO getPokemons(String cursor, int limit, @Nullable String searchQuery);
	Pokemon getPokemon(String name);
	List<Pokemon.EvolutionStage> getPokemonEvolutionChain(String pokemonName);
	List<String> getPokemonLocationAreaEncounters(String pokemonName);
	List<Pokemon.Abilities> getPokemonAbilities(String pokemonName);
}
