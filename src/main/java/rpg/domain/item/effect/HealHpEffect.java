package rpg.domain.item.effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.player.Player;
import rpg.shared.constant.GameConstants;

/**
 * HP 회복 효과
 */
public class HealHpEffect implements GameEffect {
  private static final Logger logger = LoggerFactory.getLogger(HealHpEffect.class);

  private final int value;
  private final boolean isPercentage;

  public HealHpEffect(int value, boolean isPercentage) {
    this.value = Math.max(GameConstants.NUMBER_ZERO, value);
    this.isPercentage = isPercentage;
    logger.debug("HealHpEffect 생성: {} {}", value, isPercentage ? "%" : "고정값");
  }

  @Override
  public boolean apply(Player target) {
    if (!canApplyTo(target)) {
      logger.debug("HP 회복 효과 적용 불가: {}", target != null ? target.getName() : "null");
      return false;
    }

    int oldHp = target.getHp();
    int healAmount;

    if (isPercentage) {
      healAmount = (int) (target.getTotalMaxHp() * value / 100.0);
    } else {
      healAmount = value;
    }

    target.heal(healAmount);
    int actualHealed = target.getHp() - oldHp;

    if (actualHealed > GameConstants.NUMBER_ZERO) {
      System.out.println(getApplyMessage(target, true));
      logger.info("{} HP 회복 적용: {} -> {} (+{})", target.getName(), oldHp, target.getHp(), actualHealed);
      return true;
    } else {
      System.out.println(getApplyMessage(target, false));
      return false;
    }
  }

  @Override
  public String getDescription() {
    if (isPercentage) {
      return "HP " + value + "% 회복";
    } else {
      return "HP +" + value;
    }
  }

  @Override
  public GameEffectType getType() {
    return isPercentage ? GameEffectType.HEAL_HP_PERCENT : GameEffectType.HEAL_HP;
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
    return String.format("HealHpEffect{value=%d, percentage=%b}", value, isPercentage);
  }
}
