package rpg.presentation.menu;

import rpg.application.validator.InputValidator;
import rpg.domain.player.Player;
import rpg.domain.shop.CategoryStats;
import rpg.domain.shop.ShopEvent;
import rpg.domain.shop.ShopItemCategory;

public class ShopMenu {
  /**
   * 상점 메인 메뉴를 표시합니다
   */
  public void displayShopMenuMain(Player player, int itemCount, boolean hasEvent, ShopEvent eventInfo) {
    System.out.println("\n🏪 === 마을 상점 ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println("📦 상품 종류: " + itemCount + "개 (팩토리 기반)");

    // 이벤트가 활성화되어 있으면 표시
    if (hasEvent && eventInfo != null) {
      System.out.println("🎉 " + eventInfo.getDetailedInfo());

      displayActiveEventInfo(eventInfo);
    }

    System.out.println();
    System.out.println("1. 🛒 아이템 사기");
    System.out.println("2. 💰 아이템 팔기");
    System.out.println("3. 📊 판매 시세 확인");
    System.out.println("4. 📈 상점 통계");
    System.out.println("5. 🚪 상점 나가기");
    System.out.println("====================");
  }

  /**
   * 구매 메뉴를 표시합니다
   */
  public void displayShopMenuBuy(Player player, CategoryStats stats) {
    System.out.println("\n🏪 === 마을 상점 구매 ===");
    System.out.println("💰 보유 골드: " + player.getGold());

    // 카테고리별 아이템 수 표시
    displayCategoryStats(stats);

    System.out.println();
    System.out.println("1. 🧪 소비 아이템");
    System.out.println("2. ⚔️ 무기");
    System.out.println("3. 🛡️ 방어구");
    System.out.println("4. 💍 장신구");
    System.out.println("5. 🎲 랜덤 추천");
    System.out.println("6. 🔙 돌아가기");
    System.out.println("========================");
  }


  /**
   * 판매 메뉴를 표시합니다.
   */
  public void displaySellMenu(Player player, int totalSellValue) {
    System.out.println("\n💰 === 아이템 판매 ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println();
    System.out.println("1. 🧪 소비 아이템 판매");
    System.out.println("2. ⚔️ 무기 판매");
    System.out.println("3. 🛡️ 방어구 판매");
    System.out.println("4. 💍 장신구 판매");
    System.out.println("5. ⚡ 일반 아이템 일괄 판매");
    System.out.println("6. 🔙 돌아가기");
    System.out.println("====================");

    // 예상 수익 표시
    if (totalSellValue > 0) {
      System.out.println("💡 전체 아이템 판매 시 예상 수익: " + totalSellValue + "골드");
    }
  }

  /**
   * 이벤트 알림을 표시합니다.
   */
  public void displayEventNotification(ShopEvent currentEvent) {
    System.out.println("\n" + "🎉".repeat(20));
    System.out.println("✨ 특별 이벤트 발생! ✨");

    switch (currentEvent) {
      case DISCOUNT_SALE -> {
        System.out.printf("%s %s%n", ShopEvent.DISCOUNT_SALE.getIcon(), ShopEvent.DISCOUNT_SALE.getName());
        System.out.printf("%s %s%n", ShopEvent.DISCOUNT_SALE.getIcon(), ShopEvent.DISCOUNT_SALE.getDescription());
      }
      case BONUS_SELL -> {
        System.out.printf("%s %s%n", ShopEvent.BONUS_SELL.getIcon(), ShopEvent.BONUS_SELL.getName());
        System.out.printf("%s %s%n", ShopEvent.BONUS_SELL.getIcon(), ShopEvent.BONUS_SELL.getDescription());
      }
      case FREE_POTION -> {
        System.out.printf("%s %s%n", ShopEvent.FREE_POTION.getIcon(), ShopEvent.FREE_POTION.getName());
        System.out.printf("%s %s%n", ShopEvent.FREE_POTION.getIcon(), ShopEvent.FREE_POTION.getDescription());
      }
      case RARE_ITEMS -> {
        System.out.printf("%s %s%n", ShopEvent.RARE_ITEMS.getIcon(), ShopEvent.RARE_ITEMS.getName());
        System.out.printf("%s %s%n", ShopEvent.RARE_ITEMS.getIcon(), ShopEvent.RARE_ITEMS.getDescription());
      }
      case VIP_BONUS -> {
        System.out.printf("%s %s%n", ShopEvent.VIP_BONUS.getIcon(), ShopEvent.VIP_BONUS.getName());
        System.out.printf("%s %s%n", ShopEvent.VIP_BONUS.getIcon(), ShopEvent.VIP_BONUS.getDescription());
      }
      case LUCKY_DRAW -> {
        System.out.printf("%s %s%n", ShopEvent.LUCKY_DRAW.getIcon(), ShopEvent.LUCKY_DRAW.getName());
        System.out.printf("%s %s%n", ShopEvent.LUCKY_DRAW.getIcon(), ShopEvent.LUCKY_DRAW.getDescription());
      }
      case DOUBLE_DISCOUNT -> {
        System.out.printf("%s %s%n", ShopEvent.DOUBLE_DISCOUNT.getIcon(), ShopEvent.DOUBLE_DISCOUNT.getName());
        System.out.printf("%s %s%n", ShopEvent.DOUBLE_DISCOUNT.getIcon(), ShopEvent.DOUBLE_DISCOUNT.getDescription());
      }
      case BULK_DISCOUNT -> {
        System.out.printf("%s %s%n", ShopEvent.BULK_DISCOUNT.getIcon(), ShopEvent.BULK_DISCOUNT.getName());
        System.out.printf("%s %s%n", ShopEvent.BULK_DISCOUNT.getIcon(), ShopEvent.BULK_DISCOUNT.getDescription());
      }
    }

    System.out.println("🎉".repeat(20));
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 카테고리별 통계 표시
   */
  private void displayCategoryStats(CategoryStats stats) {
    for (ShopItemCategory category : ShopItemCategory.values()) {
      int count = stats.getCount(category);
      if (count > 0) {
        System.out.printf("   %s: %d개%n", getCategoryKorean(category), count);
      }
    }
  }

  /**
   * 카테고리 한국어명 반환
   */
  private String getCategoryKorean(ShopItemCategory category) {
    return switch (category) {
      case CONSUMABLE -> "🧪 소비 아이템";
      case WEAPON -> "⚔️ 무기";
      case ARMOR -> "🛡️ 방어구";
      case ACCESSORY -> "💍 장신구";
    };
  }


  /**
   * 활성화된 이벤트 정보를 표시합니다.
   */
  private void displayActiveEventInfo(ShopEvent currentEvent) {
    System.out.println("\n🎉 현재 진행 중인 이벤트:");
    switch (currentEvent) {
      case DISCOUNT_SALE -> {
        System.out.printf("%s %s%n", ShopEvent.DISCOUNT_SALE.getIcon(), ShopEvent.DISCOUNT_SALE.getName());
      }
      case BONUS_SELL -> {
        System.out.printf("%s %s%n", ShopEvent.BONUS_SELL.getIcon(), ShopEvent.BONUS_SELL.getName());
      }
      case FREE_POTION -> {
        System.out.printf("%s %s%n", ShopEvent.FREE_POTION.getIcon(), ShopEvent.FREE_POTION.getName());
      }
      case RARE_ITEMS -> {
        System.out.printf("%s %s%n", ShopEvent.RARE_ITEMS.getIcon(), ShopEvent.RARE_ITEMS.getName());
      }
      case VIP_BONUS -> {
        System.out.printf("%s %s%n", ShopEvent.VIP_BONUS.getIcon(), ShopEvent.VIP_BONUS.getName());
      }
      case LUCKY_DRAW -> {
        System.out.printf("%s %s%n", ShopEvent.LUCKY_DRAW.getIcon(), ShopEvent.LUCKY_DRAW.getName());
      }
      case DOUBLE_DISCOUNT -> {
        System.out.printf("%s %s%n", ShopEvent.DOUBLE_DISCOUNT.getIcon(), ShopEvent.DOUBLE_DISCOUNT.getName());
      }
      case BULK_DISCOUNT -> {
        System.out.printf("%s %s%n", ShopEvent.BULK_DISCOUNT.getIcon(), ShopEvent.BULK_DISCOUNT.getName());
      }
    }
  }
}
