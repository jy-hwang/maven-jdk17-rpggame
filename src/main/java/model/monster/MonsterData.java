package model.monster;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MonsterData {
  private String id;
  private String name;
  private String description;
  private MonsterStats stats;
  private MonsterRewards rewards;
  private List<String> locations; // 출현 지역
  private int minLevel; // 최소 출현 레벨
  private int maxLevel; // 최대 출현 레벨
  private double spawnRate; // 출현 확률 (0.0 ~ 1.0)
  private String rarity; // COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
  private List<String> abilities; // 특수 능력
  private Map<String, Object> properties; // 확장 가능한 속성

  @JsonCreator
  public MonsterData(
//@formatter:off
  @JsonProperty("id") String id
, @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("stats") MonsterStats stats
, @JsonProperty("rewards") MonsterRewards rewards
, @JsonProperty("locations") List<String> locations
, @JsonProperty("minLevel") int minLevel
, @JsonProperty("maxLevel") int maxLevel
, @JsonProperty("spawnRate") double spawnRate
, @JsonProperty("rarity") String rarity
, @JsonProperty("abilities") List<String> abilities
, @JsonProperty("properties") Map<String, Object> properties
//@formatter:on
  ) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.stats = stats;
    this.rewards = rewards;
    this.locations = locations != null ? locations : List.of();
    this.minLevel = minLevel;
    this.maxLevel = maxLevel;
    this.spawnRate = spawnRate;
    this.rarity = rarity != null ? rarity : "COMMON";
    this.abilities = abilities != null ? abilities : List.of();
    this.properties = properties != null ? properties : Map.of();
  }

  // Getters
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public MonsterStats getStats() {
    return stats;
  }

  public MonsterRewards getRewards() {
    return rewards;
  }

  public List<String> getLocations() {
    return locations;
  }

  public int getMinLevel() {
    return minLevel;
  }

  public int getMaxLevel() {
    return maxLevel;
  }

  public double getSpawnRate() {
    return spawnRate;
  }

  public String getRarity() {
    return rarity;
  }

  public List<String> getAbilities() {
    return abilities;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}
