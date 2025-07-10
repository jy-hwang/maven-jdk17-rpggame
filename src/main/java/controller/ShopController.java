package controller;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;
import model.GameConsumable;
import model.GameEquipment;
import model.GameItem;
import util.InputValidator;

/**
 * 상점 시스템을 전담하는 컨트롤러
 */
public class ShopController {
  private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

  private final List<ShopItem> shopItems;
  private final InventoryController inventoryController;

  // 상점 아이템 가격 상수
  private static final int HEALTH_POTION_PRICE = 20;
  private static final int MANA_POTION_PRICE = 25;
  private static final int IRON_SWORD_PRICE = 100;
  private static final int LEATHER_ARMOR_PRICE = 80;
  private static final int MAGIC_RING_PRICE = 150;

  public ShopController(InventoryController inventoryController) {
    this.inventoryController = inventoryController;
    this.shopItems = new ArrayList<>();
    initializeShopItems();
    logger.debug("ShopController 초기화 완료");
  }

  /**
   * 상점 아이템을 초기화합니다.
   */
  private void initializeShopItems() {
    // 소비 아이템
    shopItems.add(new ShopItem(new GameConsumable("체력 물약", "HP를 50 회복합니다", HEALTH_POTION_PRICE, GameItem.ItemRarity.COMMON, 50, 0, 0, true), HEALTH_POTION_PRICE, 999, ShopItemCategory.GameConsumable));

    shopItems.add(new ShopItem(new GameConsumable("마나 물약", "MP를 30 회복합니다", MANA_POTION_PRICE, GameItem.ItemRarity.COMMON, 0, 30, 0, true), MANA_POTION_PRICE, 999, ShopItemCategory.GameConsumable));

    // 무기
    shopItems.add(
        new ShopItem(new GameEquipment("철검", "날카로운 철검", IRON_SWORD_PRICE, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 8, 0, 0), IRON_SWORD_PRICE, 10, ShopItemCategory.WEAPON));

    shopItems.add(new ShopItem(new GameEquipment("강철검", "단단한 강철로 만든 검", 200, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 15, 0, 0), 200, 5, ShopItemCategory.WEAPON));

    // 방어구
    shopItems.add(new ShopItem(new GameEquipment("가죽 갑옷", "질긴 가죽으로 만든 갑옷", LEATHER_ARMOR_PRICE, GameItem.ItemRarity.COMMON, GameEquipment.EquipmentType.ARMOR, 0, 6, 10), LEATHER_ARMOR_PRICE, 10,
        ShopItemCategory.ARMOR));

    shopItems.add(new ShopItem(new GameEquipment("철갑옷", "튼튼한 철로 만든 갑옷", 160, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.ARMOR, 0, 12, 25), 160, 5, ShopItemCategory.ARMOR));

    // 장신구
    shopItems.add(new ShopItem(new GameEquipment("마법의 반지", "마나를 증가시켜주는 반지", MAGIC_RING_PRICE, GameItem.ItemRarity.RARE, GameEquipment.EquipmentType.ACCESSORY, 0, 0, 15), MAGIC_RING_PRICE, 3,
        ShopItemCategory.ACCESSORY));

    logger.debug("상점 아이템 초기화 완료: {}개", shopItems.size());
  }


  /**
   * 상점 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void openShop(GameCharacter player) {
    while (true) {
      displayShopMenuMain(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 3);

      switch (choice) {
        case 1:
          openShopBuy(player);
          break;
        case 2:
          openShopSell(player);
          break;
        case 3:
          return;
      }
    }
  }

  /**
   * 상점 메인 메뉴를 표시합니다.
   */
  private void displayShopMenuMain(GameCharacter player) {
    System.out.println("\n🏪 === 마을 상점 ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println();
    System.out.println("1. 🛒 아이템 사기");
    System.out.println("2. 💰 아이템 팔기");
    System.out.println("3. 🚪 상점 나가기");
    System.out.println("====================");
  }

  /**
   * 상점 아이템 사기 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void openShopBuy(GameCharacter player) {
    while (true) {
      displayShopMenuBuy(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

      switch (choice) {
        case 1:
          browseCategoryItems(player, ShopItemCategory.GameConsumable);
          break;
        case 2:
          browseCategoryItems(player, ShopItemCategory.WEAPON);
          break;
        case 3:
          browseCategoryItems(player, ShopItemCategory.ARMOR);
          break;
        case 4:
          browseCategoryItems(player, ShopItemCategory.ACCESSORY);
          break;
        case 5:
          return;
      }
    }
  }

  
  /**
   * 상점 아이템 사기 메뉴를 표시합니다.
   */
  private void displayShopMenuBuy(GameCharacter player) {
    System.out.println("\n🏪 === 마을 상점 사기 ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println();
    System.out.println("1. 🧪 소비 아이템");
    System.out.println("2. ⚔️ 무기");
    System.out.println("3. 🛡️ 방어구");
    System.out.println("4. 💍 장신구");
    System.out.println("5. 🚪 상점 나가기");
    System.out.println("========================");
  }
  
  /**
   * 상점 아이템 팔기 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void openShopSell(GameCharacter player) {
    while (true) {
      displayShopMenuSell(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

      switch (choice) {
        case 1:
          browseCategoryItems(player, ShopItemCategory.GameConsumable);
          break;
        case 2:
          browseCategoryItems(player, ShopItemCategory.WEAPON);
          break;
        case 3:
          browseCategoryItems(player, ShopItemCategory.ARMOR);
          break;
        case 4:
          browseCategoryItems(player, ShopItemCategory.ACCESSORY);
          break;
        case 5:
          return;
      }
    }
  }

  
  /**
   * 상점 아이템 팔기 메뉴를 표시합니다.
   */
  private void displayShopMenuSell(GameCharacter player) {
    System.out.println("\n🏪 === 마을 상점 팔기 ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println();
    System.out.println("1. 🧪 소비 아이템");
    System.out.println("2. ⚔️ 무기");
    System.out.println("3. 🛡️ 방어구");
    System.out.println("4. 💍 장신구");
    System.out.println("5. 🚪 상점 나가기");
    System.out.println("========================");
  }
  
  /**
   * 카테고리별 아이템을 표시하고 구매를 처리합니다.
   */
  private void browseCategoryItems(GameCharacter player, ShopItemCategory category) {
    var categoryItems = shopItems.stream().filter(item -> item.getCategory() == category).filter(item -> item.getStock() > 0).toList();

    if (categoryItems.isEmpty()) {
      System.out.println("현재 " + getCategoryKorean(category) + " 카테고리에 판매 중인 아이템이 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    while (true) {
      displayCategoryItems(player, category, categoryItems);

      int choice = InputValidator.getIntInput("구매할 아이템 번호 (0: 뒤로가기): ", 0, categoryItems.size());

      if (choice == 0)
        break;

      ShopItem selectedItem = categoryItems.get(choice - 1);
      handleItemPurchase(player, selectedItem);
    }
  }

  /**
   * 카테고리별 아이템 목록을 표시합니다.
   */
  private void displayCategoryItems(GameCharacter player, ShopItemCategory category, List<ShopItem> items) {
    System.out.println("\n🏪 === " + getCategoryKorean(category) + " ===");
    System.out.println("💰 보유 골드: " + player.getGold());
    System.out.println();

    for (int i = 0; i < items.size(); i++) {
      ShopItem shopItem = items.get(i);
      GameItem item = shopItem.getItem();

      System.out.printf("%d. %s - %d골드", i + 1, item.getName(), shopItem.getPrice());

      if (shopItem.getStock() < 999) {
        System.out.printf(" (재고: %d개)", shopItem.getStock());
      }

      System.out.println();
      System.out.printf("   📝 %s%n", item.getDescription());

      // 아이템 효과 표시
      if (item instanceof GameEquipment GameEquipment) {
        System.out.printf("   🔥 효과: %s%n", getGameEquipmentEffectDescription(GameEquipment));
      } /*
         * else if (item instanceof GameConsumable GameConsumable) { System.out.printf("   ❤ 체력 회복: %d%n",
         * GameConsumable.getHpRestore()); } else if (item instanceof ManaPotion GameConsumable) {
         * System.out.printf("   💙 마나 회복: %d%n", GameConsumable.getHpRestore()); }
         */

      System.out.println();
    }

    System.out.println("0. 🔙 뒤로가기");
    System.out.println("====================");
  }

  /**
   * 아이템 구매를 처리합니다.
   */
  private void handleItemPurchase(GameCharacter player, ShopItem shopItem) {
    GameItem item = shopItem.getItem();

    // 골드 확인
    if (player.getGold() < shopItem.getPrice()) {
      System.out.println("❌ 골드가 부족합니다!");
      System.out.printf("필요: %d골드, 보유: %d골드%n", shopItem.getPrice(), player.getGold());
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 구매 수량 결정
    int maxQuantity = Math.min(shopItem.getStock(), player.getGold() / shopItem.getPrice());
    int quantity = 1;

    if (shopItem.getCategory() == ShopItemCategory.GameConsumable && maxQuantity > 1) {
      quantity = InputValidator.getIntInput(String.format("구매할 수량 (1~%d): ", maxQuantity), 1, maxQuantity);
    }

    int totalPrice = shopItem.getPrice() * quantity;

    // 구매 확인
    System.out.printf("\n📦 구매 정보:%n");
    System.out.printf("아이템: %s x%d%n", item.getName(), quantity);
    System.out.printf("총 가격: %d골드%n", totalPrice);
    System.out.printf("구매 후 잔액: %d골드%n", player.getGold() - totalPrice);

    if (!InputValidator.getConfirmation("구매하시겠습니까?")) {
      return;
    }

    // 인벤토리 공간 확인
    if (!inventoryController.addItem(player, item, quantity)) {
      System.out.println("❌ 인벤토리 공간이 부족합니다!");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 구매 처리
    player.setGold(player.getGold() - totalPrice);
    shopItem.reduceStock(quantity);

    System.out.printf("✅ %s x%d을(를) 구매했습니다!%n", item.getName(), quantity);
    System.out.printf("💰 잔액: %d골드%n", player.getGold());

    logger.info("아이템 구매: {} -> {} x{} ({}골드)", player.getName(), item.getName(), quantity, totalPrice);

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 장비 효과 설명을 생성합니다.
   */
  private String getGameEquipmentEffectDescription(GameEquipment GameEquipment) {
    StringBuilder effects = new StringBuilder();

    if (GameEquipment.getAttackBonus() > 0) {
      effects.append("공격력 +").append(GameEquipment.getAttackBonus()).append(" ");
    }

    if (GameEquipment.getDefenseBonus() > 0) {
      effects.append("방어력 +").append(GameEquipment.getDefenseBonus()).append(" ");
    }

    if (GameEquipment.getHpBonus() > 0) {
      effects.append("체력 +").append(GameEquipment.getHpBonus()).append(" ");
    }

    return effects.length() > 0 ? effects.toString().trim() : "특별한 효과 없음";
  }

  /**
   * 카테고리를 한국어로 변환합니다.
   */
  private String getCategoryKorean(ShopItemCategory category) {
    return switch (category) {
      case GameConsumable -> "소비 아이템";
      case WEAPON -> "무기";
      case ARMOR -> "방어구";
      case ACCESSORY -> "장신구";
    };
  }

  /**
   * 상점 재고를 보충합니다 (특정 조건에서 호출)
   */
  public void restockShop() {
    for (ShopItem item : shopItems) {
      if (item.getCategory() == ShopItemCategory.GameConsumable) {
        item.restockTo(999); // 소비 아이템은 무제한 재고
      } else {
        item.restockTo(item.getMaxStock()); // 장비는 최대 재고로 복구
      }
    }
    logger.info("상점 재고 보충 완료");
  }

  /**
   * 특정 아이템의 재고를 확인합니다.
   */
  public int getItemStock(String itemName) {
    return shopItems.stream().filter(item -> item.getItem().getName().equals(itemName)).mapToInt(ShopItem::getStock).findFirst().orElse(0);
  }

  /**
   * 상점 아이템 클래스
   */
  private static class ShopItem {
    private final GameItem item;
    private final int price;
    private int stock;
    private final int maxStock;
    private final ShopItemCategory category;

    public ShopItem(GameItem item, int price, int stock, ShopItemCategory category) {
      this.item = item;
      this.price = price;
      this.stock = stock;
      this.maxStock = stock;
      this.category = category;
    }

    public GameItem getItem() {
      return item;
    }

    public int getPrice() {
      return price;
    }

    public int getStock() {
      return stock;
    }

    public int getMaxStock() {
      return maxStock;
    }

    public ShopItemCategory getCategory() {
      return category;
    }

    public void reduceStock(int amount) {
      this.stock = Math.max(0, this.stock - amount);
    }

    public void restockTo(int amount) {
      this.stock = amount;
    }
  }

  /**
   * 상점 아이템 카테고리 열거형
   */
  public enum ShopItemCategory {
    GameConsumable, // 소비 아이템
    WEAPON, // 무기
    ARMOR, // 방어구
    ACCESSORY // 장신구
  }
}
