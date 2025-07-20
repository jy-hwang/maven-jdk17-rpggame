package rpg.domain.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.player.Player;
import rpg.shared.constant.GameConstants;

/**
 * 장비 아이템 클래스
 */
public class GameEquipment extends GameItem {
  private EquipmentType equipmentType;
  private int attackBonus;
  private int defenseBonus;
  private int hpBonus;
  private int mpBonus;

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
  @JsonProperty("id") String id
, @JsonProperty("name") String name
, @JsonProperty("description") String description
, @JsonProperty("value") int value
, @JsonProperty("rarity") ItemRarity rarity
, @JsonProperty("equipmentType") EquipmentType equipmentType
, @JsonProperty("attackBonus") int attackBonus
, @JsonProperty("defenseBonus") int defenseBonus
, @JsonProperty("hpBonus") int hpBonus
, @JsonProperty("mpBonus") int mpBonus
//@formatter:on
  ) {
    super(id, name, description, value, rarity);
    this.equipmentType = equipmentType;
    this.attackBonus = attackBonus;
    this.defenseBonus = defenseBonus;
    this.hpBonus = hpBonus;
    this.mpBonus = mpBonus;
  }

  @Override
  public boolean use(Player character) {
    // 장비는 직접 사용하지 않고 착용/해제로 처리
    return false;
  }

  @Override
  public String getItemInfo() {
    StringBuilder info = new StringBuilder();
    info.append(toString()).append("\n");
    info.append("종류: ").append(equipmentType.getDisplayName()).append("\n");
    getEffectDescription();
    info.append("가격: ").append(getValue()).append(" 골드");
    return info.toString();
  }

  public String getEffectDescription() {
    StringBuilder effects = new StringBuilder();

    if (attackBonus != GameConstants.NUMBER_ZERO) {
      effects.append("공격력 ");
      if (attackBonus > GameConstants.NUMBER_ZERO) {
        effects.append("+");
      }
      effects.append(attackBonus);
    }
    effects.append(" ");
    
    if (defenseBonus != GameConstants.NUMBER_ZERO) {
      effects.append("방어력 ");
      if (defenseBonus > GameConstants.NUMBER_ZERO) {
        effects.append("+");
      }
      effects.append(defenseBonus);
    }
    effects.append(" ");

    if (hpBonus != GameConstants.NUMBER_ZERO) {
      effects.append("체력 ");
      if (hpBonus > GameConstants.NUMBER_ZERO) {
        effects.append("+");
      }
      effects.append(hpBonus);
    }
    effects.append(" ");
    if (mpBonus != GameConstants.NUMBER_ZERO) {
      effects.append("마나 ");
      if (mpBonus > GameConstants.NUMBER_ZERO) {
        effects.append("+");
      }
      effects.append(mpBonus);
    }
    return effects.length() > GameConstants.NUMBER_ZERO ? effects.toString().trim() : "특별한 효과 없음";

  }

  public String compareWith(GameEquipment other) {
    if (other == null) {
      return "✨ 새로운 장비!";
    }

    StringBuilder comparison = new StringBuilder();

    int attackChange = this.attackBonus - other.attackBonus;
    int defenseChange = this.defenseBonus - other.defenseBonus;
    int hpChange = this.hpBonus - other.hpBonus;
    int mpChange = this.mpBonus - other.mpBonus;

    if (attackChange != GameConstants.NUMBER_ZERO || defenseChange != GameConstants.NUMBER_ZERO || hpChange != GameConstants.NUMBER_ZERO || mpChange != GameConstants.NUMBER_ZERO) {

      comparison.append("변화: ");

      if (attackChange != GameConstants.NUMBER_ZERO) {
        comparison.append("공격").append(attackChange > GameConstants.NUMBER_ZERO ? "+" : "").append(attackChange).append(" ");
      }

      if (defenseChange != GameConstants.NUMBER_ZERO) {
        comparison.append("방어").append(defenseChange > GameConstants.NUMBER_ZERO ? "+" : "").append(defenseChange).append(" ");
      }

      if (hpChange != GameConstants.NUMBER_ZERO) {
        comparison.append("HP").append(hpChange > GameConstants.NUMBER_ZERO ? "+" : "").append(hpChange).append(" ");
      }
      
      if (hpChange != GameConstants.NUMBER_ZERO) {
        comparison.append("MP").append(mpChange > GameConstants.NUMBER_ZERO ? "+" : "").append(hpChange).append(" ");
      }
    } else {
      comparison.append("스탯 변화 없음");
    }

    return comparison.toString().trim();
  }

  public boolean isBetterThan(GameEquipment other) {
    if (other == null) {
      return true;
    }

    // 등급 비교
    int rarityCompare = this.getRarity().ordinal() - other.getRarity().ordinal();
    if (rarityCompare != 0) {
      return rarityCompare > 0;
    }

    // 총 스탯 합계 비교
    int thisTotal = this.attackBonus + this.defenseBonus + this.hpBonus + this.mpBonus;
    int otherTotal = other.attackBonus + other.defenseBonus + other.hpBonus + other.mpBonus;

    return thisTotal > otherTotal;
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
  
  public int getMpBonus() {
    return mpBonus;
  }
}
