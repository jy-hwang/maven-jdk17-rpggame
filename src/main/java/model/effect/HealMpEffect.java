package model.effect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.GameCharacter;

public class HealMpEffect implements GameEffect {
  private final int mpAmount;
  private final boolean isPercentage;

  @JsonCreator
  public HealMpEffect(@JsonProperty("value") int mpAmount, @JsonProperty("isPercentage") Boolean isPercentage) {
    this.mpAmount = mpAmount;
    this.isPercentage = isPercentage != null ? isPercentage : false;
  }

  @Override
  public boolean apply(GameCharacter target) {
    int actualRestore;
    if (isPercentage) {
      actualRestore = (int) (target.getMaxMana() * (mpAmount / 100.0));
    } else {
      actualRestore = mpAmount;
    }

    int oldMp = target.getMana();
    target.restoreMana(actualRestore);
    int restoredAmount = target.getMana() - oldMp;

    System.out.println("💙 " + restoredAmount + " MP 회복! (현재: " + target.getMana() + "/" + target.getMaxMana() + ")");
    return restoredAmount > 0;
  }

  @Override
  public String getDescription() {
    if (isPercentage) {
      return "MP를 " + mpAmount + "% 회복";
    } else {
      return "MP를 " + mpAmount + " 회복";
    }
  }

  @Override
  public GameEffectType getType() {
    return GameEffectType.HEAL_MP;
  }

  @Override
  public int getValue() {
    return mpAmount;
  }
}
