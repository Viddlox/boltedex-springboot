package com.example.boltedex.pokemon;

import org.springframework.data.annotation.Id;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pokemon {

	@Id
	private int id;
	private String name;

	private int height;
	private int weight;
	private Stats baseStats;
	private List<String> types;
	private Map<String, Double> weaknesses;
	private Map<String, Double> resistances;
	private Map<String, Double> immunities;
	private Sprites sprites;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Stats {
		private int hp;
		private int attack;
		private int defense;
		private int speed;
		private int specialAttack;
		private int specialDefense;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Sprites {
		private String frontDefault;
		private String backDefault;
		private String frontShiny;
		private String backShiny;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EvolutionStage {
		private int id;
		private String name;
		private Sprites sprites;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Abilities {
		private String name;
		private String description;
		private boolean hidden;
	}
}
