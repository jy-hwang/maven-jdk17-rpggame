package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.BaseConstant;

public class GameItemStats {
  private final int attackBonus;
  private final int defenseBonus;
  private final int hpBonus;
  private final int hpRestore;
  private final int expGain;
  private final boolean stackable;
  private final int cooldown;
  private final boolean combatUsable;

  @JsonCreator
  public GameItemStats(
//@formatter:off
  @JsonProperty("attackBonus") Integer attackBonus
, @JsonProperty("defenseBonus") Integer defenseBonus
, @JsonProperty("hpBonus") Integer hpBonus
, @JsonProperty("hpRestore") Integer hpRestore
, @JsonProperty("expGain") Integer expGain
, @JsonProperty("stackable") Boolean stackable
, @JsonProperty("cooldown") Integer cooldown
, @JsonProperty("combatUsable") Boolean combatUsable
//@formatter:on
  ) {
    this.attackBonus = attackBonus != null ? attackBonus : BaseConstant.NUMBER_ZERO;
    this.defenseBonus = defenseBonus != null ? defenseBonus : BaseConstant.NUMBER_ZERO;
    this.hpBonus = hpBonus != null ? hpBonus : BaseConstant.NUMBER_ZERO;
    this.hpRestore = hpRestore != null ? hpRestore : BaseConstant.NUMBER_ZERO;
    this.expGain = expGain != null ? expGain : BaseConstant.NUMBER_ZERO;
    this.stackable = stackable != null ? stackable : false;
    this.cooldown = cooldown != null ? cooldown : BaseConstant.NUMBER_ZERO;
    this.combatUsable = combatUsable != null ? combatUsable : false;
  }

  public GameItemStats() {
    this(BaseConstant.NUMBER_ZERO, BaseConstant.NUMBER_ZERO, BaseConstant.NUMBER_ZERO, BaseConstant.NUMBER_ZERO, BaseConstant.NUMBER_ZERO, false,
        BaseConstant.NUMBER_ZERO, false);
  }

  // Getters
  public int getAttackBonus() {
    return attackBonus;
  }

  public int getDefenseBonus() {
    return defenseBonus;
  }

  public int getHpBonus() {
    return hpBonus;
  }

  public int getHpRestore() {
    return hpRestore;
  }

  public int getExpGain() {
    return expGain;
  }

  public boolean isStackable() {
    return stackable;
  }

  public int getCooldown() {
    return cooldown;
  }

  public boolean isCombatUsable() {
    return combatUsable;
  }
}
