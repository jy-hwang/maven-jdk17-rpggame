package rpg.domain.item.effect;

/**
 * 게임 효과 타입 enum 모든 게임 효과의 타입을 정의
 */
public enum GameEffectType {
  //@formatter:off
  // 회복 효과
  HEAL_HP("HP 회복", "체력을 회복합니다", "💚")
, HEAL_MP("MP 회복", "마나를 회복합니다", "💙")
, HEAL_HP_PERCENT("HP % 회복", "최대 체력의 %만큼 회복합니다", "💚")
, HEAL_MP_PERCENT("MP % 회복", "최대 마나의 %만큼 회복합니다", "💙")
  
  // 성장 효과
, GAIN_EXP("경험치 획득", "경험치를 획득합니다", "📈")
  
  // 버프 효과 (향후 구현)
, BUFF_ATTACK("공격력 증가", "일시적으로 공격력이 증가합니다", "⚔️")
, BUFF_DEFENSE("방어력 증가", "일시적으로 방어력이 증가합니다", "🛡️")
, BUFF_SPEED("속도 증가", "일시적으로 속도가 증가합니다", "💨")
  
  // 치료 효과 (향후 구현)
, CURE_POISON("독 치료", "독 상태를 치료합니다", "🟢")
, CURE_PARALYSIS("마비 치료", "마비 상태를 치료합니다", "⚡")
, CURE_SLEEP("수면 치료", "수면 상태를 치료합니다", "😴")
, CURE_ALL("전체 치료", "모든 상태이상을 치료합니다", "✨")
  
  // 특수 효과 (향후 구현)
, TELEPORT("순간이동", "다른 장소로 순간이동합니다", "🌀")
, REVIVE("부활", "사망 상태에서 부활합니다", "👼")
, FULL_RESTORE("완전 회복", "HP와 MP를 완전히 회복합니다", "🌟")
;
  //@formatter:on

  private final String displayName;
  private final String description;
  private final String emoji;

  /**
   * GameEffectType 생성자
   */
  GameEffectType(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  /**
   * 표시명 반환
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * 설명 반환
   */
  public String getDescription() {
    return description;
  }

  /**
   * 이모지 반환
   */
  public String getEmoji() {
    return emoji;
  }

  /**
   * 이모지와 표시명 조합
   */
  public String getFullDisplay() {
    return emoji + " " + displayName;
  }

  /**
   * 효과 카테고리 반환
   */
  public String getCategory() {
    return switch (this) {
      case HEAL_HP, HEAL_MP, HEAL_HP_PERCENT, HEAL_MP_PERCENT -> "회복";
      case GAIN_EXP -> "성장";
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> "버프";
      case CURE_POISON, CURE_PARALYSIS, CURE_SLEEP, CURE_ALL -> "치료";
      case TELEPORT, REVIVE, FULL_RESTORE -> "특수";
    };
  }

  /**
   * 즉시 효과인지 확인 (버프가 아닌 즉시 적용되는 효과)
   */
  public boolean isInstantEffect() {
    return switch (this) {
      case HEAL_HP, HEAL_MP, HEAL_HP_PERCENT, HEAL_MP_PERCENT, GAIN_EXP, CURE_POISON, CURE_PARALYSIS, CURE_SLEEP, CURE_ALL, TELEPORT, REVIVE, FULL_RESTORE -> true;
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> false;
    };
  }

  /**
   * 구현 상태 확인
   */
  public boolean isImplemented() {
    return switch (this) {
      case HEAL_HP, HEAL_MP, GAIN_EXP -> true;
      case HEAL_HP_PERCENT, HEAL_MP_PERCENT, BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED, CURE_POISON, CURE_PARALYSIS, CURE_SLEEP, CURE_ALL, TELEPORT, REVIVE, FULL_RESTORE -> false;
    };
  }

  /**
   * 최소 요구 레벨 반환
   */
  public int getMinimumLevel() {
    return switch (this) {
      case HEAL_HP, HEAL_MP, GAIN_EXP -> 1;
      case HEAL_HP_PERCENT, HEAL_MP_PERCENT -> 5;
      case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> 10;
      case CURE_POISON, CURE_PARALYSIS, CURE_SLEEP -> 15;
      case CURE_ALL, FULL_RESTORE -> 20;
      case TELEPORT, REVIVE -> 25;
    };
  }

  /**
   * 문자열로부터 GameEffectType 찾기
   */
  public static GameEffectType fromString(String typeStr) {
    if (typeStr == null || typeStr.trim().isEmpty()) {
      return null;
    }

    String normalized = typeStr.trim().toUpperCase();

    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException e) {
      // 표시명으로 찾기
      for (GameEffectType type : values()) {
        if (type.displayName.equals(typeStr.trim())) {
          return type;
        }
      }
      return null;
    }
  }

  /**
   * 카테고리별 효과 타입 목록 반환
   */
  public static java.util.List<GameEffectType> getByCategory(String category) {
    return java.util.Arrays.stream(values()).filter(type -> type.getCategory().equals(category)).collect(java.util.stream.Collectors.toList());
  }

  /**
   * 구현된 효과 타입만 반환
   */
  public static java.util.List<GameEffectType> getImplementedTypes() {
    return java.util.Arrays.stream(values()).filter(GameEffectType::isImplemented).collect(java.util.stream.Collectors.toList());
  }

  /**
   * 효과 타입 통계 출력
   */
  public static void printStatistics() {
    System.out.println("\n=== 🎭 효과 타입 통계 ===");

    java.util.Map<String, Long> categoryStats = java.util.Arrays.stream(values()).collect(java.util.stream.Collectors.groupingBy(GameEffectType::getCategory, java.util.stream.Collectors.counting()));

    long implementedCount = java.util.Arrays.stream(values()).mapToLong(type -> type.isImplemented() ? 1 : 0).sum();

    System.out.println("총 효과 타입: " + values().length + "개");
    System.out.println("구현된 효과: " + implementedCount + "개");
    System.out.println("미구현 효과: " + (values().length - implementedCount) + "개");

    System.out.println("\n📂 카테고리별:");
    categoryStats.entrySet().stream().sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()).forEach(entry -> System.out.printf("   %s: %d개%n", entry.getKey(), entry.getValue()));

    System.out.println("===================");
  }

  @Override
  public String toString() {
    return displayName;
  }
}
