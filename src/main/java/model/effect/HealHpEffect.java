package model.effect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.GameCharacter;

public class HealHpEffect implements GameEffect {

  private final int healAmount;
  private final boolean isPercentage;

  @JsonCreator
  public HealHpEffect(@JsonProperty("value") int healAmount, @JsonProperty("isPercentage") Boolean isPercentage) {
    this.healAmount = healAmount;
    this.isPercentage = isPercentage != null ? isPercentage : false;
  }

  @Override
  public boolean apply(GameCharacter target) {
    int actualHeal;
    if (isPercentage) {
      actualHeal = (int) (target.getTotalMaxHp() * (healAmount / 100.0));
    } else {
      actualHeal = healAmount;
    }

    int oldHp = target.getHp();
    target.heal(actualHeal);
    int healedAmount = target.getHp() - oldHp;

    System.out.println("💚 " + healedAmount + " HP 회복! (현재: " + target.getHp() + "/" + target.getTotalMaxHp() + ")");
    return healedAmount > 0;
  }

  @Override
  public String getDescription() {
    if (isPercentage) {
      return "HP를 " + healAmount + "% 회복";
    } else {
      return "HP를 " + healAmount + " 회복";
    }
  }

  @Override
  public GameEffectType getType() {
    return GameEffectType.HEAL_HP;
  }

  @Override
  public int getValue() {
    return healAmount;
  }

}
