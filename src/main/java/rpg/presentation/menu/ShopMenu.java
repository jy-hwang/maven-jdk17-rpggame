package rpg.presentation.menu;

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

      // displayActiveEventInfo();
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

    if (totalSellValue > 0) {
      System.out.println("💡 전체 아이템 판매 시 예상 수익: " + totalSellValue + "골드");
    }
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
  // private void displayActiveEventInfo() {
  // System.out.println("\n🎉 현재 진행 중인 이벤트:");
  // switch (currentEvent) {
  // case DISCOUNT_SALE -> System.out.println("🏷️ 할인 세일 (20% 할인)");
  // case BONUS_SELL -> System.out.println("💰 고가 매입 (30% 보너스)");
  // case FREE_POTION -> System.out.println("🎁 무료 체력 물약 (미수령)");
  // case RARE_ITEMS -> System.out.println("⭐ 희귀 아이템 특별 판매");
  // }
  // }
}
