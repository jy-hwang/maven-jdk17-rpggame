package controller;

/**
 * 상점 이벤트 타입을 정의하는 열거형
 */
public enum ShopEvent {
  DISCOUNT_SALE("할인 세일", "🏷️", "모든 아이템 20% 할인!", 0.8, 1.0), BONUS_SELL("고가 매입", "💰", "판매 시 30% 보너스!", 1.0, 1.3), FREE_POTION("무료 증정", "🎁", "체력 물약 구매 시 1개 추가 증정!", 1.0, 1.0), RARE_ITEMS("희귀 아이템",
      "⭐", "특별한 아이템들이 입고되었습니다!", 1.0, 1.0), DOUBLE_DISCOUNT("대폭 할인", "💥", "모든 아이템 40% 할인!", 0.6,
          1.0), LUCKY_DRAW("행운의 뽑기", "🎰", "구매 시 50% 확률로 골드 환급!", 1.0, 1.0), BULK_DISCOUNT("대량 할인", "📦", "3개 이상 구매 시 추가 10% 할인!", 1.0, 1.0), VIP_BONUS("VIP 혜택", "👑", "모든 거래에서 특별 혜택!", 0.85, 1.4);

  private final String name;
  private final String icon;
  private final String description;
  private final double buyMultiplier; // 구매 가격 배수 (1.0 = 100%, 0.8 = 20% 할인)
  private final double sellMultiplier; // 판매 가격 배수 (1.0 = 100%, 1.3 = 30% 보너스)

  ShopEvent(String name, String icon, String description, double buyMultiplier, double sellMultiplier) {
    this.name = name;
    this.icon = icon;
    this.description = description;
    this.buyMultiplier = buyMultiplier;
    this.sellMultiplier = sellMultiplier;
  }

  /**
   * 이벤트 이름을 반환합니다.
   */
  public String getName() {
    return name;
  }

  /**
   * 이벤트 아이콘을 반환합니다.
   */
  public String getIcon() {
    return icon;
  }

  /**
   * 이벤트 설명을 반환합니다.
   */
  public String getDescription() {
    return description;
  }

  /**
   * 구매 가격 배수를 반환합니다.
   */
  public double getBuyMultiplier() {
    return buyMultiplier;
  }

  /**
   * 판매 가격 배수를 반환합니다.
   */
  public double getSellMultiplier() {
    return sellMultiplier;
  }

  /**
   * 할인율을 퍼센트로 반환합니다.
   */
  public int getDiscountPercent() {
    return (int) ((1.0 - buyMultiplier) * 100);
  }

  /**
   * 판매 보너스를 퍼센트로 반환합니다.
   */
  public int getSellBonusPercent() {
    return (int) ((sellMultiplier - 1.0) * 100);
  }

  /**
   * 이벤트가 구매 할인 이벤트인지 확인합니다.
   */
  public boolean isBuyEvent() {
    return buyMultiplier < 1.0;
  }

  /**
   * 이벤트가 판매 보너스 이벤트인지 확인합니다.
   */
  public boolean isSellEvent() {
    return sellMultiplier > 1.0;
  }

  /**
   * 이벤트가 특별 기능 이벤트인지 확인합니다.
   */
  public boolean isSpecialEvent() {
    return this == FREE_POTION || this == RARE_ITEMS || this == LUCKY_DRAW || this == BULK_DISCOUNT;
  }

  /**
   * 원래 가격에 이벤트 할인을 적용합니다.
   */
  public int applyBuyDiscount(int originalPrice) {
    return (int) (originalPrice * buyMultiplier);
  }

  /**
   * 원래 판매가에 이벤트 보너스를 적용합니다.
   */
  public int applySellBonus(int originalPrice) {
    return (int) (originalPrice * sellMultiplier);
  }

  /**
   * 이벤트 상세 정보를 반환합니다.
   */
  public String getDetailedInfo() {
    StringBuilder info = new StringBuilder();
    info.append(icon).append(" ").append(name).append("\n");
    info.append("📝 ").append(description).append("\n");

    if (isBuyEvent()) {
      info.append("💰 구매 할인: ").append(getDiscountPercent()).append("%\n");
    }

    if (isSellEvent()) {
      info.append("📈 판매 보너스: ").append(getSellBonusPercent()).append("%\n");
    }

    if (isSpecialEvent()) {
      info.append("✨ 특별 효과: ").append(getSpecialEffectDescription()).append("\n");
    }

    return info.toString();
  }

  /**
   * 특별 효과 설명을 반환합니다.
   */
  public String getSpecialEffectDescription() {
    return switch (this) {
      case FREE_POTION -> "체력 물약 구매 시 1개 추가 증정";
      case RARE_ITEMS -> "특별한 아이템들 판매 중";
      case LUCKY_DRAW -> "구매 시 50% 확률로 골드 환급";
      case BULK_DISCOUNT -> "3개 이상 구매 시 추가 10% 할인";
      default -> "특별한 효과 없음";
    };
  }

  /**
   * 이벤트 알림 메시지를 반환합니다.
   */
  public String getNotificationMessage() {
    return String.format("%s %s이(가) 시작되었습니다!\n%s", icon, name, description);
  }

  /**
   * 이벤트 메뉴 표시용 문자열을 반환합니다.
   */
  public String getMenuDisplay() {
    StringBuilder display = new StringBuilder();
    display.append(icon).append(" ").append(name);

    if (isBuyEvent()) {
      display.append(" (").append(getDiscountPercent()).append("% 할인)");
    }

    if (isSellEvent()) {
      display.append(" (").append(getSellBonusPercent()).append("% 보너스)");
    }

    return display.toString();
  }

  /**
   * 랜덤하게 이벤트를 선택합니다.
   */
  public static ShopEvent getRandomEvent() {
    ShopEvent[] events = values();
    return events[(int) (Math.random() * events.length)];
  }

  /**
   * 가중치를 적용한 랜덤 이벤트를 선택합니다.
   */
  public static ShopEvent getWeightedRandomEvent() {
    // 일반적인 이벤트들의 가중치가 더 높음
    ShopEvent[] weightedEvents = {DISCOUNT_SALE, DISCOUNT_SALE, DISCOUNT_SALE, // 높은 가중치
        BONUS_SELL, BONUS_SELL, BONUS_SELL, // 높은 가중치
        FREE_POTION, FREE_POTION, // 중간 가중치
        RARE_ITEMS, // 낮은 가중치
        DOUBLE_DISCOUNT, // 낮은 가중치 (강력한 이벤트)
        LUCKY_DRAW, // 낮은 가중치
        BULK_DISCOUNT, // 중간 가중치
        VIP_BONUS // 낮은 가중치 (강력한 이벤트)
    };

    return weightedEvents[(int) (Math.random() * weightedEvents.length)];
  }

  /**
   * 플레이어 레벨에 따른 이벤트를 선택합니다.
   */
  public static ShopEvent getEventForLevel(int playerLevel) {
    if (playerLevel <= 3) {
      // 초보자용 이벤트
      ShopEvent[] beginnerEvents = {DISCOUNT_SALE, FREE_POTION, BULK_DISCOUNT};
      return beginnerEvents[(int) (Math.random() * beginnerEvents.length)];
    } else if (playerLevel <= 7) {
      // 중급자용 이벤트
      ShopEvent[] intermediateEvents = {DISCOUNT_SALE, BONUS_SELL, LUCKY_DRAW, RARE_ITEMS};
      return intermediateEvents[(int) (Math.random() * intermediateEvents.length)];
    } else {
      // 고급자용 이벤트
      ShopEvent[] advancedEvents = {DOUBLE_DISCOUNT, VIP_BONUS, RARE_ITEMS, LUCKY_DRAW};
      return advancedEvents[(int) (Math.random() * advancedEvents.length)];
    }
  }
}
