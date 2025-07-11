package model.effect;

public enum GameStatusCondition {
  //@formatter:off
  NORMAL("정상","정상 상태 입니다."),
  HEALTHY("건강","건강한 상태입니다."),
  POISON("중독", "매 턴 체력이 감소합니다"),
  PARALYSIS("마비", "행동할 수 없습니다"),
  SLEEP("수면", "잠들어 있어 행동할 수 없습니다"),
  BURN("화상", "매 턴 화상 피해를 입습니다"),
  FREEZE("빙결", "얼어붙어 행동할 수 없습니다"),
  CONFUSION("혼란", "랜덤한 대상을 공격합니다");
  //@formatter:on
  private final String name;
  private final String description;

  GameStatusCondition(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
