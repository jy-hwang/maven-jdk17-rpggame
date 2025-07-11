package model.effect;

public enum GameEffectType {
  HEAL_HP("체력 회복"), HEAL_MP("마나 회복"), CURE_POISON("중독 치료"), CURE_PARALYSIS("마비 치료"), CURE_SLEEP("수면 치료"), CURE_ALL_STATUS("모든 상태이상 치료"), BUFF_ATTACK("공격력 증가"), BUFF_DEFENSE("방어력 증가"), BUFF_SPEED(
      "속도 증가"), GAIN_EXP("경험치 획득"), REVIVE("부활"), MAX_HP_INCREASE("최대 체력 증가"), TEMPORARY_INVINCIBILITY("일시 무적");

  private final String description;

  GameEffectType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
