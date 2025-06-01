package com.example.boltedex.pokemon;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.lang.Nullable;

@Service
public class PokemonAPIClientService {

	@Autowired
	private PokemonAPIClient pokemonAPIClient;

	public PokemonAPIClientDTO getPokemons(String cursor, int limit, @Nullable String searchQuery) {
		return pokemonAPIClient.getPokemons(cursor, limit, searchQuery);
	}

	public Pokemon getPokemon(String name) {
		return pokemonAPIClient.getPokemon(name);
	}

	public List<Pokemon.EvolutionStage> getPokemonEvolutionChain(String name) {
		return pokemonAPIClient.getPokemonEvolutionChain(name);
	}

	public List<String> getPokemonLocationAreaEncounters(String name) {
		return pokemonAPIClient.getPokemonLocationAreaEncounters(name);
	}

	public List<Pokemon.Abilities> getPokemonAbilities(String name) {
		return pokemonAPIClient.getPokemonAbilities(name);
	}
}
