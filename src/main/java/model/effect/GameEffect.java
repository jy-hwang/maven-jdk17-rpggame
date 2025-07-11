package model.effect;

import model.GameCharacter;

/**
 * 게임 효과 인터페이스 모든 게임 효과가 구현해야 하는 기본 메서드들을 정의
 */
public interface GameEffect {

  /**
   * 효과를 적용합니다
   * 
   * @param target 효과를 받을 대상
   * @return 효과 적용 성공 여부
   */
  boolean apply(GameCharacter target);

  /**
   * 효과 설명을 반환합니다
   * 
   * @return 효과에 대한 설명 문자열
   */
  String getDescription();

  /**
   * 효과 타입을 반환합니다
   * 
   * @return 효과의 타입
   */
  GameEffectType getType();

  /**
   * 효과 값을 반환합니다
   * 
   * @return 효과의 수치값
   */
  int getValue();

  /**
   * 효과가 백분율 기반인지 확인합니다
   * 
   * @return 백분율 효과면 true, 고정값 효과면 false
   */
  default boolean isPercentage() {
    return false;
  }

  /**
   * 효과의 지속시간을 반환합니다 (즉시 효과는 0)
   * 
   * @return 지속시간 (턴 수)
   */
  default int getDuration() {
    return 0;
  }

  /**
   * 효과가 즉시 적용되는지 확인합니다
   * 
   * @return 즉시 효과면 true, 지속 효과면 false
   */
  default boolean isInstant() {
    return getDuration() == 0;
  }

  /**
   * 효과가 유효한지 검증합니다
   * 
   * @return 유효하면 true
   */
  default boolean isValid() {
    return getType() != null && getValue() >= 0;
  }

  /**
   * 효과의 우선순위를 반환합니다 (낮을수록 먼저 적용)
   * 
   * @return 우선순위 값
   */
  default int getPriority() {
    return switch (getType()) {
      case CURE_ALL, CURE_POISON, CURE_PARALYSIS, CURE_SLEEP -> 1; // 치료 효과 우선
      case HEAL_HP, HEAL_MP, HEAL_HP_PERCENT, HEAL_MP_PERCENT -> 2; // 회복 효과
      case GAIN_EXP -> 3; // 성장 효과
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> 4; // 버프 효과
      case REVIVE, FULL_RESTORE, TELEPORT -> 5; // 특수 효과
    };
  }

  /**
   * 다른 효과와 중복 적용 가능한지 확인합니다
   * 
   * @param other 비교할 다른 효과
   * @return 중복 적용 가능하면 true
   */
  default boolean canStackWith(GameEffect other) {
    if (other == null)
      return true;

    // 같은 타입의 효과는 기본적으로 중복 불가
    if (this.getType() == other.getType()) {
      return false;
    }

    // 특정 조합은 중복 불가
    return switch (this.getType()) {
      case FULL_RESTORE -> other.getType() != GameEffectType.HEAL_HP && other.getType() != GameEffectType.HEAL_MP;
      case CURE_ALL -> !other.getType().getCategory().equals("치료");
      default -> true;
    };
  }

  /**
   * 효과의 강도를 계산합니다 (AI 판단용)
   * 
   * @return 효과 강도 점수
   */
  default double getEffectPower() {
    double baseValue = getValue();

    return switch (getType()) {
      case HEAL_HP, HEAL_MP -> baseValue;
      case HEAL_HP_PERCENT, HEAL_MP_PERCENT -> baseValue * 5; // 퍼센트는 더 강력
      case GAIN_EXP -> baseValue * 0.1; // 경험치는 상대적으로 약함
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> baseValue * 2; // 버프는 강력
      case CURE_POISON, CURE_PARALYSIS, CURE_SLEEP -> 50; // 치료는 고정 강도
      case CURE_ALL -> 100; // 전체 치료는 매우 강력
      case REVIVE -> 200; // 부활은 최고 강도
      case FULL_RESTORE -> 150; // 완전 회복도 매우 강력
      case TELEPORT -> 75; // 순간이동은 중간 강도
    };
  }

  /**
   * 효과 적용 시 출력할 메시지를 반환합니다
   * 
   * @param target 대상 캐릭터
   * @param success 적용 성공 여부
   * @return 출력할 메시지
   */
  default String getApplyMessage(GameCharacter target, boolean success) {
    if (!success) {
      return "💫 " + getDescription() + " 효과가 없었습니다.";
    }

    String emoji = getType().getEmoji();
    String targetName = target != null ? target.getName() : "대상";

    return switch (getType()) {
      case HEAL_HP -> String.format("%s %s이(가) HP %d 회복!", emoji, targetName, getValue());
      case HEAL_MP -> String.format("%s %s이(가) MP %d 회복!", emoji, targetName, getValue());
      case HEAL_HP_PERCENT -> String.format("%s %s이(가) HP %d%% 회복!", emoji, targetName, getValue());
      case HEAL_MP_PERCENT -> String.format("%s %s이(가) MP %d%% 회복!", emoji, targetName, getValue());
      case GAIN_EXP -> String.format("%s %s이(가) 경험치 %d 획득!", emoji, targetName, getValue());
      default -> String.format("%s %s!", emoji, getDescription());
    };
  }

  /**
   * 효과를 JSON 형태의 문자열로 변환합니다
   * 
   * @return JSON 문자열
   */
  default String toJsonString() {
    return String.format("{\"type\":\"%s\",\"value\":%d,\"percentage\":%b,\"duration\":%d}", getType().name(), getValue(), isPercentage(),
        getDuration());
  }

  /**
   * 효과가 적용 가능한 최소 레벨을 반환합니다
   * 
   * @return 최소 레벨
   */
  default int getMinimumLevel() {
    return getType().getMinimumLevel();
  }

  /**
   * 효과 적용 전 유효성 검사
   * 
   * @param target 대상 캐릭터
   * @return 적용 가능하면 true
   */
  default boolean canApplyTo(GameCharacter target) {
    if (target == null) {
      return false;
    }

    if (!isValid()) {
      return false;
    }

    if (target.getLevel() < getMinimumLevel()) {
      return false;
    }

    return switch (getType()) {
      case HEAL_HP -> target.getHp() < target.getTotalMaxHp();
      case HEAL_MP -> target.getMana() < target.getMaxMana();
      case HEAL_HP_PERCENT -> target.getHp() < target.getTotalMaxHp();
      case HEAL_MP_PERCENT -> target.getMana() < target.getMaxMana();
      case GAIN_EXP -> true;
      case REVIVE -> !target.isAlive();
      default -> true; // 미구현 효과들은 일단 true
    };
  }
}
