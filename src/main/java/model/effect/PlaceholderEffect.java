package model.effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;

/**
 * 플레이스홀더 효과 클래스 미구현 효과들을 안전하게 처리하기 위한 임시 구현체
 */
public class PlaceholderEffect implements GameEffect {
  private static final Logger logger = LoggerFactory.getLogger(PlaceholderEffect.class);

  private final GameEffectType type;
  private final int value;
  private final String customMessage;

  /**
   * 기본 생성자
   * 
   * @param type 효과 타입
   * @param value 효과 값
   */
  public PlaceholderEffect(GameEffectType type, int value) {
    this(type, value, null);
  }

  /**
   * 커스텀 메시지 생성자
   * 
   * @param type 효과 타입
   * @param value 효과 값
   * @param customMessage 커스텀 메시지 (null이면 기본 메시지 사용)
   */
  public PlaceholderEffect(GameEffectType type, int value, String customMessage) {
    this.type = type != null ? type : GameEffectType.HEAL_HP;
    this.value = Math.max(0, value);
    this.customMessage = customMessage;

    logger.debug("PlaceholderEffect 생성: {} (값: {}, 메시지: {})", this.type, this.value, customMessage != null ? "커스텀" : "기본");
  }

  @Override
  public boolean apply(GameCharacter target) {
    String targetName = target != null ? target.getName() : "대상";
    String message = generateApplyMessage(targetName);

    System.out.println(message);
    logger.debug("플레이스홀더 효과 발동: {} -> {} (실제 효과 없음)", type, targetName);

    return false; // 실제 효과는 없음
  }

  @Override
  public String getDescription() {
    if (customMessage != null && !customMessage.trim().isEmpty()) {
      return customMessage;
    }

    // 기본 메시지 생성
    String baseDesc = type.getDisplayName();
    if (value > 0) {
      baseDesc += " +" + value;
    }
    return baseDesc + " (미구현)";
  }

  @Override
  public GameEffectType getType() {
    return type;
  }

  @Override
  public int getValue() {
    return value;
  }

  @Override
  public boolean isPercentage() {
    // 퍼센트 타입인지 확인
    return type == GameEffectType.HEAL_HP_PERCENT || type == GameEffectType.HEAL_MP_PERCENT;
  }

  @Override
  public boolean canApplyTo(GameCharacter target) {
    // 플레이스홀더는 항상 "적용 가능"하지만 실제 효과는 없음
    return target != null;
  }

  /**
   * 적용 메시지 생성
   */
  private String generateApplyMessage(String targetName) {
    if (customMessage != null && !customMessage.trim().isEmpty()) {
      return "🚧 " + customMessage;
    }

    // 타입별 기본 메시지
    String emoji = type.getEmoji();
    String typeName = type.getDisplayName();

    return switch (type) {
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> String.format("🚧 %s %s %s 효과 (미구현)", emoji, targetName, typeName);

      case CURE_POISON, CURE_PARALYSIS, CURE_SLEEP, CURE_ALL -> String.format("🚧 %s %s %s 효과 (미구현)", emoji, targetName, typeName);

      case TELEPORT -> String.format("🚧 %s %s 순간이동 효과 (미구현)", emoji, targetName);

      case REVIVE -> String.format("🚧 %s %s 부활 효과 (미구현)", emoji, targetName);

      case FULL_RESTORE -> String.format("🚧 %s %s 완전 회복 효과 (미구현)", emoji, targetName);

      default -> String.format("🚧 %s %s %s 효과 (미구현)", emoji, targetName, typeName);
    };
  }

  /**
   * 디버그 정보 반환
   */
  public String getDebugInfo() {
    return String.format("PlaceholderEffect{type=%s, value=%d, customMessage='%s'}", type, value, customMessage != null ? customMessage : "없음");
  }

  /**
   * 실제 구현이 필요한지 확인
   */
  public boolean needsImplementation() {
    return !type.isImplemented();
  }

  /**
   * 이 효과가 대체해야 할 실제 효과 클래스명 제안
   */
  public String getSuggestedImplementationClass() {
    return switch (type) {
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> "BuffEffect";
      case CURE_POISON, CURE_PARALYSIS, CURE_SLEEP, CURE_ALL -> "CureEffect";
      case TELEPORT -> "TeleportEffect";
      case REVIVE -> "ReviveEffect";
      case FULL_RESTORE -> "FullRestoreEffect";
      default -> type.name() + "Effect";
    };
  }

  @Override
  public String toString() {
    return String.format("PlaceholderEffect{type=%s, value=%d}", type, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;

    PlaceholderEffect other = (PlaceholderEffect) obj;
    return value == other.value && type == other.type;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(type, value);
  }
}
