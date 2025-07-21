package rpg.core.exploration;

/**
 * 탐험 결과를 나타내는 Enum 클래스
 * 각 결과 타입별로 메시지와 추가 정보를 포함
 */
public enum ExploreResult {
  //@formatter:off
  // 전투 관련 결과
    BATTLE_VICTORY("전투에서 승리했습니다!", true)
  , BATTLE_DEFEAT("전투에서 패배했습니다...", false)
  , BATTLE_ESCAPED("성공적으로 도망쳤습니다!", true)

  // 이벤트 관련 결과
  , TREASURE("보물을 발견했습니다!", true)
  , KNOWLEDGE("고대의 지식을 습득했습니다!", true)
  , REST("안전한 휴식처를 발견했습니다!", true)

  // 추가 이벤트 타입
  , HEALING_SPRING("치유의 샘을 발견했습니다!", true)
  , MAGIC_CRYSTAL("마법 크리스탈을 발견했습니다!", true)
  , SHRINE_BLESSING("신비한 제단의 축복을 받았습니다!", true)

  // 오류
  , ERROR("오류가 발생했습니다.", false);
  //@formatter:on
  
  private final String defaultMessage;
  private final boolean isPositive;

  ExploreResult(String defaultMessage, boolean isPositive) {
    this.defaultMessage = defaultMessage;
    this.isPositive = isPositive;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }

  public boolean isPositive() {
    return isPositive;
  }

  /**
   * 전투 관련 결과인지 확인
   */
  public boolean isBattleResult() {
    return this == BATTLE_VICTORY || this == BATTLE_DEFEAT || this == BATTLE_ESCAPED;
  }

  /**
   * 이벤트 관련 결과인지 확인
   */
  public boolean isEventResult() {
    return !isBattleResult() && this != ERROR;
  }

  /**
   * 보상이 있는 결과인지 확인
   */
  public boolean hasReward() {
    return this == BATTLE_VICTORY || this == TREASURE || this == KNOWLEDGE || this == HEALING_SPRING || this == MAGIC_CRYSTAL
        || this == SHRINE_BLESSING;
  }
}
