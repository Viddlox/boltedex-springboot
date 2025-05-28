package com.example.boltedex.pokemon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;

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
		List<Pokemon> pokemons = pokemonAPIClientService.getPokemons(cursor, limit, query);
		String nextCursor = pokemons.isEmpty() ? null : pokemons.get(pokemons.size() - 1).getName();
	
		return ResponseEntity.ok(new PokemonAPIClientDTO(pokemons, nextCursor));
	}

	@GetMapping("/evolution")
	public List<Pokemon.EvolutionStage> getPokemonEvolutionChain(@RequestParam String name) {
		return pokemonAPIClientService.getPokemonEvolutionChain(name);
	}

	@GetMapping("/location")
	public List<String> getPokemonLocationAreaEncounters(@RequestParam String name) {
		return pokemonAPIClientService.getPokemonLocationAreaEncounters(name);
	}

	@GetMapping("/abilities")
	public List<Pokemon.Abilities> getPokemonAbilities(@RequestParam String name) {
		return pokemonAPIClientService.getPokemonAbilities(name);
	}
}
