package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import model.GameCharacter;

/**
 * 게임 아이템의 기본 클래스
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
//@formatter:off
  @JsonSubTypes.Type(value = GameEquipment.class, name = "equipment")
, @JsonSubTypes.Type(value = GameConsumable.class, name = "GameConsumable"
//@formatter:on
    )})
public abstract class GameItem {
  protected String name;
  protected String description;
  protected int value; // 판매 가격
  protected ItemRarity rarity;


  @JsonCreator
  public GameItem(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("value") int value
, @JsonProperty("rarity") ItemRarity rarity
//@formatter:off
      ) {
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
