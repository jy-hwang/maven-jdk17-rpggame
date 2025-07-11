package dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameItemDto {

  private String name;
  private String description;
  private int value;
  private String rarity; // enum -> String
  private String itemType; // "CONSUMABLE", "EQUIPMENT"

  // 소비 아이템용 필드들
  private List<GameEffectDto> effects;
  private int cooldown;

  // 장비 아이템용 필드들
  private String equipmentType; // "WEAPON", "ARMOR", "ACCESSORY"
  private int attackBonus;
  private int defenseBonus;
  private int hpBonus;

  public GameItemDto() {}

  @JsonCreator
  public GameItemDto(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("value") int value
, @JsonProperty("rarity") String rarity
, @JsonProperty("itemType") String itemType
, @JsonProperty("effects") List<GameEffectDto> effects
, @JsonProperty("cooldown") int cooldown
, @JsonProperty("equipmentType") String equipmentType
, @JsonProperty("attackBonus") int attackBonus
, @JsonProperty("defenseBonus") int defenseBonus
, @JsonProperty("hpBonus") int hpBonus
//@formatter:on
  ) {
    this.name = name;
    this.description = description;
    this.value = value;
    this.rarity = rarity;
    this.itemType = itemType;
    this.effects = effects;
    this.cooldown = cooldown;
    this.equipmentType = equipmentType;
    this.attackBonus = attackBonus;
    this.defenseBonus = defenseBonus;
    this.hpBonus = hpBonus;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public String getRarity() {
    return rarity;
  }

  public void setRarity(String rarity) {
    this.rarity = rarity;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  public List<GameEffectDto> getEffects() {
    return effects;
  }

  public void setEffects(List<GameEffectDto> effects) {
    this.effects = effects;
  }

  public int getCooldown() {
    return cooldown;
  }

  public void setCooldown(int cooldown) {
    this.cooldown = cooldown;
  }

  public String getEquipmentType() {
    return equipmentType;
  }

  public void setEquipmentType(String equipmentType) {
    this.equipmentType = equipmentType;
  }

  public int getAttackBonus() {
    return attackBonus;
  }

  public void setAttackBonus(int attackBonus) {
    this.attackBonus = attackBonus;
  }

  public int getDefenseBonus() {
    return defenseBonus;
  }

  public void setDefenseBonus(int defenseBonus) {
    this.defenseBonus = defenseBonus;
  }

  public int getHpBonus() {
    return hpBonus;
  }

  public void setHpBonus(int hpBonus) {
    this.hpBonus = hpBonus;
  }


}
