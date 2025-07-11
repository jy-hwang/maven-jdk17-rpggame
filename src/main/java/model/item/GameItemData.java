package model.item;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameItemData {
  private final String id;
  private final String name;
  private final String description;
  private final String type;
  private final int value;
  private final ItemRarity rarity;
  private final boolean stackable;
  private final List<GameEffectData> effects;
  
  @JsonCreator
  public GameItemData(
          @JsonProperty("id") String id,
          @JsonProperty("name") String name,
          @JsonProperty("description") String description,
          @JsonProperty("type") String type,
          @JsonProperty("value") int value,
          @JsonProperty("rarity") String rarity,
          @JsonProperty("stackable") boolean stackable,
          @JsonProperty("effects") List<GameEffectData> effects) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.type = type;
      this.value = value;
      this.rarity = ItemRarity.valueOf(rarity.toUpperCase());
      this.stackable = stackable;
      this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
  }
  
  // Getters
  public String getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getType() { return type; }
  public int getValue() { return value; }
  public ItemRarity getRarity() { return rarity; }
  public boolean isStackable() { return stackable; }
  public List<GameEffectData> getEffects() { return new ArrayList<>(effects); }
  
}
