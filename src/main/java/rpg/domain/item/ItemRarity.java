package rpg.domain.item;

import rpg.shared.constant.ItemConstants;

/**
 * 아이템 등급 enum (최신 버전) 이모지, 색상 코드, 확률 등 포함
 */
public enum ItemRarity {
  //@formatter:off
  COMMON("일반", "⚪", "#FFFFFF", "#808080", ItemConstants.COMMON_RATE, ItemConstants.COMMON_MULTIPL)
, UNCOMMON("고급", "🟢", "#00FF00", "#00AA00", ItemConstants.UNCOMMON_RATE, ItemConstants.UNCOMMON_MULTIPL)
, RARE("희귀", "🔵", "#0080FF", "#0060CC", ItemConstants.RARE_RATE, ItemConstants.RARE_MULTIPL)
, EPIC("영웅", "🟣", "#8000FF", "#6000CC", ItemConstants.EPIC_RATE, ItemConstants.EPIC_MULTIPL)
, LEGENDARY("전설", "🟡", "#FFD700", "#CC9900", ItemConstants.LEGENDARY_RATE, ItemConstants.LEGENDARY_MULTIPL);
  //@formatter:on    
  private final String displayName;
  private final String emoji;
  private final String colorCode;
  private final String darkColorCode;
  private final double dropChance; // 드롭 확률 (%)
  private final double valueMultiplier; // 가격 배율

  /**
   * ItemRarity 생성자
   */
  ItemRarity(String displayName, String emoji, String colorCode, String darkColorCode, double dropChance, double valueMultiplier) {
    this.displayName = displayName;
    this.emoji = emoji;
    this.colorCode = colorCode;
    this.darkColorCode = darkColorCode;
    this.dropChance = dropChance;
    this.valueMultiplier = valueMultiplier;
  }

  /**
   * 표시명 반환
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * 이모지 반환
   */
  public String getEmoji() {
    return emoji;
  }

  /**
   * 색상 코드 반환 (밝은 테마용)
   */
  public String getColorCode() {
    return colorCode;
  }

  /**
   * 어두운 색상 코드 반환 (어두운 테마용)
   */
  public String getDarkColorCode() {
    return darkColorCode;
  }

  /**
   * 드롭 확률 반환 (%)
   */
  public double getDropChance() {
    return dropChance;
  }

  /**
   * 가격 배율 반환
   */
  public double getValueMultiplier() {
    return valueMultiplier;
  }

  /**
   * 등급별 기본 가격 계산
   */
  public int calculateBaseValue(int basePrice) {
    return (int) (basePrice * valueMultiplier);
  }

  /**
   * 이모지와 이름을 포함한 전체 표시
   */
  public String getFullDisplay() {
    return emoji + " " + displayName;
  }

  /**
   * 등급별 최소 레벨 요구사항
   */
  public int getMinimumLevel() {
    return switch (this) {
      case COMMON -> 1;
      case UNCOMMON -> 5;
      case RARE -> 15;
      case EPIC -> 30;
      case LEGENDARY -> 50;
    };
  }

  /**
   * 문자열로부터 ItemRarity 찾기 (대소문자 무시)
   */
  public static ItemRarity fromString(String rarityStr) {
    if (rarityStr == null || rarityStr.trim().isEmpty()) {
      return COMMON;
    }

    String normalized = rarityStr.trim().toUpperCase();

    // 영어명으로 찾기
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException e) {
      // 한글명으로 찾기
      for (ItemRarity rarity : values()) {
        if (rarity.displayName.equals(rarityStr.trim())) {
          return rarity;
        }
      }
    }

    return COMMON; // 기본값
  }

  /**
   * 확률 기반 랜덤 등급 선택
   */
  public static ItemRarity getRandomRarity() {
    double random = Math.random() * 100.0;
    double cumulative = 0.0;

    for (ItemRarity rarity : values()) {
      cumulative += rarity.dropChance;
      if (random <= cumulative) {
        return rarity;
      }
    }

    return COMMON; // 기본값 (여기까지 올 일은 없음)
  }

  /**
   * 레벨 기반 등급 선택 (높은 레벨일수록 좋은 아이템)
   */
  public static ItemRarity getRandomRarityForLevel(int level) {
    // 레벨이 높을수록 좋은 아이템 확률 증가
    double levelBonus = Math.min(level * 2.0, 50.0); // 최대 50% 보너스

    // 확률 조정
    double[] adjustedChances = new double[values().length];
    for (int i = 0; i < values().length; i++) {
      ItemRarity rarity = values()[i];
      if (level >= rarity.getMinimumLevel()) {
        // 레벨 요구사항을 만족하면 확률 증가
        adjustedChances[i] = rarity.dropChance + (levelBonus * (i + 1) / values().length);
      } else {
        // 레벨 요구사항을 만족하지 않으면 확률 0
        adjustedChances[i] = 0.0;
      }
    }

    // 확률 정규화
    double total = java.util.Arrays.stream(adjustedChances).sum();
    if (total <= 0)
      return COMMON;

    for (int i = 0; i < adjustedChances.length; i++) {
      adjustedChances[i] = (adjustedChances[i] / total) * 100.0;
    }

    // 랜덤 선택
    double random = Math.random() * 100.0;
    double cumulative = 0.0;

    for (int i = 0; i < adjustedChances.length; i++) {
      cumulative += adjustedChances[i];
      if (random <= cumulative) {
        return values()[i];
      }
    }

    return COMMON;
  }

  /**
   * 확률 분포 시뮬레이션
   */
  public static void simulateDrops(int trials) {
    if (trials <= 0) {
      System.out.println("❌ 시뮬레이션 횟수는 1 이상이어야 합니다.");
      return;
    }

    int[] counts = new int[values().length];

    for (int i = 0; i < trials; i++) {
      ItemRarity rarity = getRandomRarity();
      counts[rarity.ordinal()]++;
    }

    System.out.println("\n=== 📊 드롭 시뮬레이션 결과 (" + trials + "회) ===");
    for (int i = 0; i < values().length; i++) {
      ItemRarity rarity = values()[i];
      double actualPercent = (double) counts[i] / trials * 100.0;
      System.out.printf("%s %-6s: %5d개 (%5.1f%% | 예상: %4.1f%%)%n", rarity.emoji, rarity.displayName, counts[i], actualPercent, rarity.dropChance);
    }
    System.out.println("=".repeat(50));
  }

  @Override
  public String toString() {
    return displayName;
  }
}
