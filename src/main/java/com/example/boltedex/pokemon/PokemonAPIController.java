package com.example.boltedex.pokemon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonAPIController {
	
	@Autowired
	private PokemonAPIClientService pokemonAPIClientService;

	@GetMapping("/search")
	public ResponseEntity<PokemonAPIClientDTO> searchPokemons(
		@RequestParam(required = false) String query,
		@RequestParam(defaultValue = "") String cursor,
		@RequestParam(defaultValue = "30") int limit
	) {
		PokemonAPIClientDTO result = pokemonAPIClientService.getPokemons(cursor, limit, query);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/detail/{name}")
	public ResponseEntity<Pokemon> getPokemon(@PathVariable String name) {
		Pokemon pokemon = pokemonAPIClientService.getPokemon(name);
		return ResponseEntity.ok(pokemon);
	}

	@GetMapping("/evolution/{name}")
	public List<Pokemon.EvolutionStage> getPokemonEvolutionChain(@PathVariable String name) {
		return pokemonAPIClientService.getPokemonEvolutionChain(name);
	}

	@GetMapping("/location/{name}")
	public List<String> getPokemonLocationAreaEncounters(@PathVariable String name) {
		return pokemonAPIClientService.getPokemonLocationAreaEncounters(name);
	}

	@GetMapping("/abilities/{name}")
	public List<Pokemon.Abilities> getPokemonAbilities(@PathVariable String name) {
		return pokemonAPIClientService.getPokemonAbilities(name);
	}
}
