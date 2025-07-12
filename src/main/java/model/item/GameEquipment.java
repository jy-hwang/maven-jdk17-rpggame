package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.BaseConstant;
import model.GameCharacter;

/**
 * 장비 아이템 클래스
 */
public class GameEquipment extends GameItem {
  private EquipmentType equipmentType;
  private int attackBonus;
  private int defenseBonus;
  private int hpBonus;

  public enum EquipmentType {
    WEAPON("무기"), ARMOR("방어구"), ACCESSORY("장신구");

    private final String displayName;

    EquipmentType(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  @JsonCreator
  public GameEquipment(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("value") int value
, @JsonProperty("rarity") ItemRarity rarity
, @JsonProperty("equipmentType") EquipmentType equipmentType
, @JsonProperty("attackBonus") int attackBonus
, @JsonProperty("defenseBonus") int defenseBonus
, @JsonProperty("hpBonus") int hpBonus
//@formatter:on
  ) {
    super(name, description, value, rarity);
    this.equipmentType = equipmentType;
    this.attackBonus = attackBonus;
    this.defenseBonus = defenseBonus;
    this.hpBonus = hpBonus;
  }

  @Override
  public boolean use(GameCharacter character) {
    // 장비는 직접 사용하지 않고 착용/해제로 처리
    return false;
  }

  @Override
  public String getItemInfo() {
    StringBuilder info = new StringBuilder();
    info.append(toString()).append("\n");
    info.append("종류: ").append(equipmentType.getDisplayName()).append("\n");
    if (attackBonus > BaseConstant.NUMBER_ZERO)
      info.append("공격력 +").append(attackBonus).append("\n");
    if (defenseBonus > BaseConstant.NUMBER_ZERO)
      info.append("방어력 +").append(defenseBonus).append("\n");
    if (hpBonus > BaseConstant.NUMBER_ZERO)
      info.append("체력 +").append(hpBonus).append("\n");
    info.append("가격: ").append(getValue()).append(" 골드");
    return info.toString();
  }

  // Getters
  public EquipmentType getEquipmentType() {
    return equipmentType;
  }

  public int getAttackBonus() {
    return attackBonus;
  }

  public int getDefenseBonus() {
    return defenseBonus;
  }

  public int getHpBonus() {
    return hpBonus;
  }
}
