package com.example.boltedex.pokemon;

import java.util.List;

public class PokemonAPIClientDTO {

	private List<Pokemon> results;
	private String nextCursor;
	private long totalCount;

	public PokemonAPIClientDTO(List<Pokemon> results, String nextCursor, long totalCount) {
		this.results = results;
		this.nextCursor = nextCursor;
		this.totalCount = totalCount;
	}

	public List<Pokemon> getResults() {
		return results;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public long getTotalCount() {
		return totalCount;
	}
}
