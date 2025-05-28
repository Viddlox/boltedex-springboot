package com.example.boltedex.pokemon;

import java.util.*;

public class PokemonTypeEffectiveness {
	public static final Map<String, Map<String, Double>> TYPE_MAP = new HashMap<>();

	public static final String[] TYPES = { "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison",
			"ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy" };

	static {
		for (String type : TYPES) {
			TYPE_MAP.put(type, new HashMap<>());
			for (String target : TYPES) {
				TYPE_MAP.get(type).put(target, 1.0);
			}
		}

		// Normal
		TYPE_MAP.get("normal").put("rock", 0.5);
		TYPE_MAP.get("normal").put("steel", 0.5);
		TYPE_MAP.get("normal").put("ghost", 0.0);

		// Fire
		TYPE_MAP.get("fire").put("grass", 2.0);
		TYPE_MAP.get("fire").put("ice", 2.0);
		TYPE_MAP.get("fire").put("bug", 2.0);
		TYPE_MAP.get("fire").put("steel", 2.0);
		TYPE_MAP.get("fire").put("fire", 0.5);
		TYPE_MAP.get("fire").put("water", 0.5);
		TYPE_MAP.get("fire").put("rock", 0.5);
		TYPE_MAP.get("fire").put("dragon", 0.5);

		// Water
		TYPE_MAP.get("water").put("fire", 2.0);
		TYPE_MAP.get("water").put("ground", 2.0);
		TYPE_MAP.get("water").put("rock", 2.0);
		TYPE_MAP.get("water").put("water", 0.5);
		TYPE_MAP.get("water").put("grass", 0.5);
		TYPE_MAP.get("water").put("dragon", 0.5);

		// ELECTRIC
		TYPE_MAP.get("electric").put("water", 2.0);
		TYPE_MAP.get("electric").put("flying", 2.0);
		TYPE_MAP.get("electric").put("electric", 0.5);
		TYPE_MAP.get("electric").put("grass", 0.5);
		TYPE_MAP.get("electric").put("dragon", 0.5);
		TYPE_MAP.get("electric").put("ground", 0.0);

		// GRASS
		TYPE_MAP.get("grass").put("water", 2.0);
		TYPE_MAP.get("grass").put("rock", 2.0);
		TYPE_MAP.get("grass").put("ground", 2.0);
		TYPE_MAP.get("grass").put("fire", 0.5);
		TYPE_MAP.get("grass").put("grass", 0.5);
		TYPE_MAP.get("grass").put("poison", 0.5);
		TYPE_MAP.get("grass").put("flying", 0.5);
		TYPE_MAP.get("grass").put("bug", 0.5);
		TYPE_MAP.get("grass").put("dragon", 0.5);
		TYPE_MAP.get("grass").put("steel", 0.5);

		// Ice
		TYPE_MAP.get("ice").put("grass", 2.0);
		TYPE_MAP.get("ice").put("ground", 2.0);
		TYPE_MAP.get("ice").put("flying", 2.0);
		TYPE_MAP.get("ice").put("dragon", 2.0);
		TYPE_MAP.get("ice").put("fire", 0.5);
		TYPE_MAP.get("ice").put("water", 0.5);
		TYPE_MAP.get("ice").put("ice", 0.5);
		TYPE_MAP.get("ice").put("steel", 0.5);

		// Fighting
		TYPE_MAP.get("fighting").put("normal", 2.0);
		TYPE_MAP.get("fighting").put("ice", 2.0);
		TYPE_MAP.get("fighting").put("rock", 2.0);
		TYPE_MAP.get("fighting").put("dark", 2.0);
		TYPE_MAP.get("fighting").put("steel", 2.0);
		TYPE_MAP.get("fighting").put("poison", 0.5);
		TYPE_MAP.get("fighting").put("flying", 0.5);
		TYPE_MAP.get("fighting").put("psychic", 0.5);
		TYPE_MAP.get("fighting").put("bug", 0.5);
		TYPE_MAP.get("fighting").put("fairy", 0.5);

		// Poison
		TYPE_MAP.get("poison").put("grass", 2.0);
		TYPE_MAP.get("poison").put("fairy", 2.0);
		TYPE_MAP.get("poison").put("poison", 0.5);
		TYPE_MAP.get("poison").put("ground", 0.5);
		TYPE_MAP.get("poison").put("rock", 0.5);
		TYPE_MAP.get("poison").put("ghost", 0.5);
		TYPE_MAP.get("poison").put("steel", 0.0);

		// Ground
		TYPE_MAP.get("ground").put("fire", 2.0);
		TYPE_MAP.get("ground").put("electric", 2.0);
		TYPE_MAP.get("ground").put("poison", 2.0);
		TYPE_MAP.get("ground").put("rock", 2.0);
		TYPE_MAP.get("ground").put("steel", 2.0);
		TYPE_MAP.get("ground").put("grass", 0.5);
		TYPE_MAP.get("ground").put("bug", 0.5);
		TYPE_MAP.get("ground").put("flying", 0.0);

		// Flying
		TYPE_MAP.get("flying").put("grass", 2.0);
		TYPE_MAP.get("flying").put("fighting", 2.0);
		TYPE_MAP.get("flying").put("bug", 2.0);
		TYPE_MAP.get("flying").put("electric", 0.5);
		TYPE_MAP.get("flying").put("rock", 0.5);
		TYPE_MAP.get("flying").put("steel", 0.5);

		// Psychic
		TYPE_MAP.get("psychic").put("fighting", 2.0);
		TYPE_MAP.get("psychic").put("poison", 2.0);
		TYPE_MAP.get("psychic").put("psychic", 0.5);
		TYPE_MAP.get("psychic").put("steel", 0.5);
		TYPE_MAP.get("psychic").put("dark", 0.0);

		// Bug
		TYPE_MAP.get("bug").put("grass", 2.0);
		TYPE_MAP.get("bug").put("psychic", 2.0);
		TYPE_MAP.get("bug").put("dark", 2.0);
		TYPE_MAP.get("bug").put("fire", 0.5);
		TYPE_MAP.get("bug").put("fighting", 0.5);
		TYPE_MAP.get("bug").put("poison", 0.5);
		TYPE_MAP.get("bug").put("flying", 0.5);
		TYPE_MAP.get("bug").put("ghost", 0.5);
		TYPE_MAP.get("bug").put("steel", 0.5);
		TYPE_MAP.get("bug").put("fairy", 0.5);

		// Rock
		TYPE_MAP.get("rock").put("fire", 2.0);
		TYPE_MAP.get("rock").put("ice", 2.0);
		TYPE_MAP.get("rock").put("flying", 2.0);
		TYPE_MAP.get("rock").put("bug", 2.0);
		TYPE_MAP.get("rock").put("fighting", 0.5);
		TYPE_MAP.get("rock").put("ground", 0.5);
		TYPE_MAP.get("rock").put("steel", 0.5);

		// Ghost
		TYPE_MAP.get("ghost").put("ghost", 2.0);
		TYPE_MAP.get("ghost").put("psychic", 2.0);
		TYPE_MAP.get("ghost").put("dark", 0.5);
		TYPE_MAP.get("ghost").put("normal", 0.0);

		// Dragon
		TYPE_MAP.get("dragon").put("dragon", 2.0);
		TYPE_MAP.get("dragon").put("fairy", 0.0);
		TYPE_MAP.get("dragon").put("steel", 0.5);

		// Dark
		TYPE_MAP.get("dark").put("ghost", 2.0);
		TYPE_MAP.get("dark").put("psychic", 2.0);
		TYPE_MAP.get("dark").put("fighting", 0.5);
		TYPE_MAP.get("dark").put("dark", 0.5);
		TYPE_MAP.get("dark").put("fairy", 0.5);

		// Steel
		TYPE_MAP.get("steel").put("ice", 2.0);
		TYPE_MAP.get("steel").put("rock", 2.0);
		TYPE_MAP.get("steel").put("fairy", 2.0);
		TYPE_MAP.get("steel").put("fire", 0.5);
		TYPE_MAP.get("steel").put("water", 0.5);
		TYPE_MAP.get("steel").put("electric", 0.5);
		TYPE_MAP.get("steel").put("steel", 0.5);

		// Fairy
		TYPE_MAP.get("fairy").put("fighting", 2.0);
		TYPE_MAP.get("fairy").put("dragon", 2.0);
		TYPE_MAP.get("fairy").put("dark", 2.0);
		TYPE_MAP.get("fairy").put("fire", 0.5);
		TYPE_MAP.get("fairy").put("poison", 0.5);
		TYPE_MAP.get("fairy").put("steel", 0.5);
	}

	public static double getEffectiveness(String attackerType, List<String> defenderTypes) {
		double multiplier = 1.0;
		for (String defenseType : defenderTypes) {
			multiplier *= TYPE_MAP.getOrDefault(attackerType, Collections.emptyMap())
					.getOrDefault(defenseType, 1.0);
		}
		return multiplier;
	}
}
