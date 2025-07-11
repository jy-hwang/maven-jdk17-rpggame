package model.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
          @JsonProperty("attackBonus") Integer attackBonus,
          @JsonProperty("defenseBonus") Integer defenseBonus,
          @JsonProperty("hpBonus") Integer hpBonus,
          @JsonProperty("hpRestore") Integer hpRestore,
          @JsonProperty("expGain") Integer expGain,
          @JsonProperty("stackable") Boolean stackable,
          @JsonProperty("cooldown") Integer cooldown,
          @JsonProperty("combatUsable") Boolean combatUsable) {
      this.attackBonus = attackBonus != null ? attackBonus : 0;
      this.defenseBonus = defenseBonus != null ? defenseBonus : 0;
      this.hpBonus = hpBonus != null ? hpBonus : 0;
      this.hpRestore = hpRestore != null ? hpRestore : 0;
      this.expGain = expGain != null ? expGain : 0;
      this.stackable = stackable != null ? stackable : false;
      this.cooldown = cooldown != null ? cooldown : 0;
      this.combatUsable = combatUsable != null ? combatUsable : false;
  }
  
  public GameItemStats() {
      this(0, 0, 0, 0, 0, false, 0, false);
  }
  
  // Getters
  public int getAttackBonus() { return attackBonus; }
  public int getDefenseBonus() { return defenseBonus; }
  public int getHpBonus() { return hpBonus; }
  public int getHpRestore() { return hpRestore; }
  public int getExpGain() { return expGain; }
  public boolean isStackable() { return stackable; }
  public int getCooldown() { return cooldown; }
  public boolean isCombatUsable() { return combatUsable; }
}
