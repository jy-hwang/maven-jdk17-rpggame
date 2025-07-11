package model.effect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.GameCharacter;

public class CureStatusEffect implements GameEffect {
  private final GameStatusCondition statusType;

  @JsonCreator
  public CureStatusEffect(@JsonProperty("statusType") String statusType) {
    this.statusType = GameStatusCondition.valueOf(statusType.toUpperCase());
  }

  @Override
  public boolean apply(GameCharacter target) {
    if (target.hasStatusCondition(statusType)) {
      target.removeStatusCondition(statusType);
      System.out.println("✨ " + statusType.getDescription() + " 상태가 치료되었습니다!");
      return true;
    } else {
      System.out.println("💭 치료할 " + statusType.getDescription() + " 상태가 없습니다.");
      return false;
    }
  }

  @Override
  public String getDescription() {
    return statusType.getDescription() + " 상태를 치료";
  }

  @Override
  public GameEffectType getType() {
    return switch (statusType) {
      case POISON -> GameEffectType.CURE_POISON;
      case PARALYSIS -> GameEffectType.CURE_PARALYSIS;
      case SLEEP -> GameEffectType.CURE_SLEEP;
    };
  }

  @Override
  public int getValue() {
    return 1;
  }

}
