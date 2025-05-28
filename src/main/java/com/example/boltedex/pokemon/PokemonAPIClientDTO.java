package com.example.boltedex.pokemon;

import java.util.List;

public class PokemonAPIClientDTO {

	private List<Pokemon> results;
	private String nextCursor;

	public PokemonAPIClientDTO(List<Pokemon> results, String nextCursor) {
		this.results = results;
		this.nextCursor = nextCursor;
	}

	public List<Pokemon> getResults() {
		return results;
	}

	public String getNextCursor() {
		return nextCursor;
	}
}
