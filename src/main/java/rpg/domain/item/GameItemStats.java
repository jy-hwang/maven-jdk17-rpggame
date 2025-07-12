package rpg.domain.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.shared.constant.GameConstants;

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
    this.attackBonus = attackBonus != null ? attackBonus : GameConstants.NUMBER_ZERO;
    this.defenseBonus = defenseBonus != null ? defenseBonus : GameConstants.NUMBER_ZERO;
    this.hpBonus = hpBonus != null ? hpBonus : GameConstants.NUMBER_ZERO;
    this.hpRestore = hpRestore != null ? hpRestore : GameConstants.NUMBER_ZERO;
    this.expGain = expGain != null ? expGain : GameConstants.NUMBER_ZERO;
    this.stackable = stackable != null ? stackable : false;
    this.cooldown = cooldown != null ? cooldown : GameConstants.NUMBER_ZERO;
    this.combatUsable = combatUsable != null ? combatUsable : false;
  }

  public GameItemStats() {
    this(GameConstants.NUMBER_ZERO, GameConstants.NUMBER_ZERO, GameConstants.NUMBER_ZERO, GameConstants.NUMBER_ZERO, GameConstants.NUMBER_ZERO, false,
        GameConstants.NUMBER_ZERO, false);
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
