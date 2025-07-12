package rpg.domain.item.effect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.player.Player;

public class GainExpEffect implements GameEffect {

  private final int expAmount;

  @JsonCreator
  public GainExpEffect(@JsonProperty("value") int expAmount) {
    this.expAmount = expAmount;
  }

  @Override
  public boolean apply(Player target) {
    target.gainExp(expAmount);
    System.out.println("📈 " + expAmount + " 경험치를 획득했습니다!");
    return true;
  }

  @Override
  public String getDescription() {
    return expAmount + " 경험치 획득";
  }

  @Override
  public GameEffectType getType() {
    return GameEffectType.GAIN_EXP;
  }

  @Override
  public int getValue() {
    return expAmount;
  }
}
