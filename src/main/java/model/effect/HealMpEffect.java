package model.effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;

/**
 * MP 회복 효과
 */
public class HealMpEffect implements GameEffect {
  private static final Logger logger = LoggerFactory.getLogger(HealMpEffect.class);

  private final int value;
  private final boolean isPercentage;

  public HealMpEffect(int value, boolean isPercentage) {
    this.value = Math.max(0, value);
    this.isPercentage = isPercentage;
    logger.debug("HealMpEffect 생성: {} {}", value, isPercentage ? "%" : "고정값");
  }

  @Override
  public boolean apply(GameCharacter target) {
    if (!canApplyTo(target)) {
      logger.debug("MP 회복 효과 적용 불가: {}", target != null ? target.getName() : "null");
      return false;
    }

    int oldMp = target.getMana();
    int restoreAmount;

    if (isPercentage) {
      restoreAmount = (int) (target.getMaxMana() * value / 100.0);
    } else {
      restoreAmount = value;
    }

    target.restoreMana(restoreAmount);
    int actualRestored = target.getMana() - oldMp;

    if (actualRestored > 0) {
      System.out.println(getApplyMessage(target, true));
      logger.info("{} MP 회복 적용: {} -> {} (+{})", target.getName(), oldMp, target.getMana(), actualRestored);
      return true;
    } else {
      System.out.println(getApplyMessage(target, false));
      return false;
    }
  }

  @Override
  public String getDescription() {
    if (isPercentage) {
      return "MP " + value + "% 회복";
    } else {
      return "MP +" + value;
    }
  }

  @Override
  public GameEffectType getType() {
    return isPercentage ? GameEffectType.HEAL_MP_PERCENT : GameEffectType.HEAL_MP;
  }

  @Override
  public int getValue() {
    return value;
  }

  @Override
  public boolean isPercentage() {
    return isPercentage;
  }

  @Override
  public String toString() {
    return String.format("HealMpEffect{value=%d, percentage=%b}", value, isPercentage);
  }
}
