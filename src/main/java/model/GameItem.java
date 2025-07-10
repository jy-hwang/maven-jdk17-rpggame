package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 게임 아이템의 기본 클래스
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = GameEquipment.class, name = "equipment"), @JsonSubTypes.Type(value = GameConsumable.class, name = "GameConsumable")})
public abstract class GameItem {
  protected String name;
  protected String description;
  protected int value; // 판매 가격
  protected ItemRarity rarity;

  public enum ItemRarity {
    COMMON("일반", 1.0), UNCOMMON("고급", 1.5), RARE("희귀", 2.0), EPIC("영웅", 3.0), LEGENDARY("전설", 5.0);

    private final String displayName;
    private final double valueMultiplier;

    ItemRarity(String displayName, double valueMultiplier) {
      this.displayName = displayName;
      this.valueMultiplier = valueMultiplier;
    }

    public String getDisplayName() {
      return displayName;
    }

    public double getValueMultiplier() {
      return valueMultiplier;
    }
  }

  @JsonCreator
  public GameItem(@JsonProperty("name") String name, @JsonProperty("description") String description, @JsonProperty("value") int value, @JsonProperty("rarity") ItemRarity rarity) {
    this.name = name;
    this.description = description;
    this.value = value;
    this.rarity = rarity != null ? rarity : ItemRarity.COMMON;
  }

  public abstract boolean use(GameCharacter character);

  public abstract String getItemInfo();

  // Getters
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getValue() {
    return (int) (value * rarity.getValueMultiplier());
  }

  public ItemRarity getRarity() {
    return rarity;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s - %s", rarity.getDisplayName(), name, description);
  }

}
