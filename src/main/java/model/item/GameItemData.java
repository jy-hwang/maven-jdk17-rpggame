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

  // 장비 전용 필드들 (nullable)
  private final String equipmentType; // "WEAPON", "ARMOR", "ACCESSORY"
  private final Integer attackBonus; // null이면 0으로 처리
  private final Integer defenseBonus; // null이면 0으로 처리
  private final Integer hpBonus; // null이면 0으로 처리

  @JsonCreator
  public GameItemData(
//@formatter:off
  @JsonProperty("id") String id
, @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("type") String type
, @JsonProperty("value") int value
, @JsonProperty("rarity") String rarity
, @JsonProperty("stackable") boolean stackable
, @JsonProperty("effects") List<GameEffectData> effects
// 장비 전용 필드들 (Optional)
, @JsonProperty("equipmentType") String equipmentType
, @JsonProperty("attackBonus") Integer attackBonus
, @JsonProperty("defenseBonus") Integer defenseBonus
, @JsonProperty("hpBonus") Integer hpBonus
//@formatter:on
  ) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.value = value;
    this.rarity = ItemRarity.valueOf(rarity.toUpperCase());
    this.stackable = stackable;
    this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
    this.equipmentType = equipmentType;
    this.attackBonus = attackBonus;
    this.defenseBonus = defenseBonus;
    this.hpBonus = hpBonus;
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

  public String getType() {
    return type;
  }

  public int getValue() {
    return value;
  }

  public ItemRarity getRarity() {
    return rarity;
  }

  public boolean isStackable() {
    return stackable;
  }

  public List<GameEffectData> getEffects() {
    return new ArrayList<>(effects);
  }

  public String getEquipmentType() {
    return equipmentType;
  }

  public int getAttackBonus() {
    return attackBonus != null ? attackBonus : 0;
  }

  public int getDefenseBonus() {
    return defenseBonus != null ? defenseBonus : 0;
  }

  public int getHpBonus() {
    return hpBonus != null ? hpBonus : 0;
  }
}
