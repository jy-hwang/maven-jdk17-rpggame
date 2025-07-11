package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;
import model.GameInventory;
import model.ShopItem;
import model.ShopItemCategory;
import model.item.GameConsumable;
import model.item.GameEquipment;
import model.item.GameItem;
import util.InputValidator;

/**
 * 상점 시스템을 전담하는 컨트롤러
 */
public class ShopController {
  private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

  private final List<ShopItem> shopItems;
  private final InventoryController inventoryController;
  private final Random random;

  // 상점 아이템 가격 상수
  private static final int HEALTH_POTION_PRICE = 20;
  private static final int MANA_POTION_PRICE = 25;
  private static final int IRON_SWORD_PRICE = 100;
  private static final int LEATHER_ARMOR_PRICE = 80;
  private static final int MAGIC_RING_PRICE = 150;

  // 이벤트 관련 상수
  private static final int EVENT_CHANCE = 15; // 15% 확률로 이벤트 발생
  private boolean currentEventActive = false;
  private ShopEvent currentEvent = null;

  public ShopController(InventoryController inventoryController) {
    this.inventoryController = inventoryController;
    this.shopItems = new ArrayList<>();
    this.random = new Random();
    initializeShopItems();
    logger.debug("ShopController 초기화 완료");
  }

  /**
   * 상점 아이템을 초기화합니다.
   */
  private void initializeShopItems() {
    // 소비 아이템
    shopItems.add(new ShopItem(new GameConsumable("체력 물약", "HP를 50 회복합니다", HEALTH_POTION_PRICE, GameItem.ItemRarity.COMMON, 50, 0, 0, true), HEALTH_POTION_PRICE, 999, ShopItemCategory.CONSUMABLE));

    shopItems.add(new ShopItem(new GameConsumable("마나 물약", "MP를 30 회복합니다", MANA_POTION_PRICE, GameItem.ItemRarity.COMMON, 0, 30, 0, true), MANA_POTION_PRICE, 999, ShopItemCategory.CONSUMABLE));

    // 무기
    shopItems.add(
        new ShopItem(new GameEquipment("철검", "날카로운 철검", IRON_SWORD_PRICE, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 8, 0, 0), IRON_SWORD_PRICE, 10, ShopItemCategory.WEAPON));

    shopItems.add(new ShopItem(new GameEquipment("강철검", "단단한 강철로 만든 검", 200, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 15, 0, 0), 200, 5, ShopItemCategory.WEAPON));

    // 방어구
    shopItems.add(new ShopItem(new GameEquipment("가죽 갑옷", "질긴 가죽으로 만든 갑옷", LEATHER_ARMOR_PRICE, GameItem.ItemRarity.COMMON, GameEquipment.EquipmentType.ARMOR, 0, 6, 10), LEATHER_ARMOR_PRICE, 10,
        ShopItemCategory.ARMOR));

    shopItems.add(new ShopItem(new GameEquipment("철갑옷", "튼튼한 철로 만든 갑옷", 160, GameItem.ItemRarity.UNCOMMON, GameEquipment.EquipmentType.ARMOR, 0, 12, 25), 160, 5, ShopItemCategory.ARMOR));

    // 장신구
    shopItems.add(new ShopItem(new GameEquipment("체력의 반지", "체력을 증가시켜주는 반지", MAGIC_RING_PRICE, GameItem.ItemRarity.RARE, GameEquipment.EquipmentType.ACCESSORY, 0, 0, 15), MAGIC_RING_PRICE, 3,
        ShopItemCategory.ACCESSORY));

    logger.debug("상점 아이템 초기화 완료: {}개", shopItems.size());
  }


  /**
   * 상점 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void openShop(GameCharacter player) {

    // 상점 진입 시 랜덤 이벤트 체크
    checkForRandomEvent();

    while (true) {
      displayShopMenuMain(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 4);

      switch (choice) {
        case 1:
          openShopBuy(player);
          break;
        case 2:
          openShopSell(player);
          break;
        case 3:
          showSellPrices(player);
          break;
        case 4:
          return;
      }
    }
  }

  /**
   * 랜덤 상점 이벤트를 체크합니다.
   */
  private void checkForRandomEvent() {
    if (random.nextInt(100) < EVENT_CHANCE) {
      triggerRandomEvent();
    }
  }

  /**
   * 랜덤 이벤트를 발생시킵니다.
   */
  private void triggerRandomEvent() {
    ShopEvent[] events = ShopEvent.values();
    currentEvent = events[random.nextInt(events.length)];
    currentEventActive = true;

    displayEventNotification();
    logger.info("상점 이벤트 발생: {}", currentEvent);
  }

  /**
   * 이벤트 알림을 표시합니다.
   */
  private void displayEventNotification() {
    System.out.println("\n" + "🎉".repeat(20));
    System.out.println("✨ 특별 이벤트 발생! ✨");

    switch (currentEvent) {
      case DISCOUNT_SALE -> {
        System.out.println("🏷️ 할인 세일!");
        System.out.println("💥 모든 아이템 20% 할인!");
      }
      case BONUS_SELL -> {
        System.out.println("💰 고가 매입 이벤트!");
        System.out.println("📈 판매 시 30% 보너스!");
      }
      case FREE_POTION -> {
        System.out.println("🎁 무료 증정 이벤트!");
        System.out.println("🧪 체력 물약 1개 무료 증정!");
      }
      case RARE_ITEMS -> {
        System.out.println("⭐ 희귀 아이템 입고!");
        System.out.println("🔥 특별한 아이템들이 입고되었습니다!");
      }
    }

    System.out.println("🎉".repeat(20));
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 상점 메인 메뉴를 표시합니다.
   */
  private void displayShopMenuMain(GameCharacter player) {
    System.out.println("\n🏪 === 마을 상점 ===");
    System.out.println("💰 보유 골드: " + player.getGold());

    // 이벤트가 활성화되어 있으면 표시
    if (currentEventActive && currentEvent != null) {
      displayActiveEventInfo();
    }

    System.out.println();
    System.out.println("1. 🛒 아이템 사기");
    System.out.println("2. 💰 아이템 팔기");
    System.out.println("3. 📊 판매 시세 확인");
    System.out.println("4. 🚪 상점 나가기");
    System.out.println("====================");
  }

  /**
   * 활성화된 이벤트 정보를 표시합니다.
   */
  private void displayActiveEventInfo() {
    System.out.println("\n🎉 현재 진행 중인 이벤트:");
    switch (currentEvent) {
      case DISCOUNT_SALE -> System.out.println("🏷️ 할인 세일 (20% 할인)");
      case BONUS_SELL -> System.out.println("💰 고가 매입 (30% 보너스)");
      case FREE_POTION -> System.out.println("🎁 무료 체력 물약 (미수령)");
      case RARE_ITEMS -> System.out.println("⭐ 희귀 아이템 특별 판매");
    }
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
          browseCategoryItems(player, ShopItemCategory.CONSUMABLE);
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
    System.out.println("\n🏪 === 마을 상점 구매 ===");
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
    int originalPrice = shopItem.getPrice();
    int finalPrice = applyEventDiscount(originalPrice);

    // 골드 확인
    if (player.getGold() < finalPrice) {
      System.out.println("❌ 골드가 부족합니다!");
      System.out.printf("필요: %d골드, 보유: %d골드%n", finalPrice, player.getGold());
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 구매 수량 결정
    int maxQuantity = Math.min(shopItem.getStock(), player.getGold() / finalPrice);
    int quantity = 1;

    if (shopItem.getCategory() == ShopItemCategory.CONSUMABLE && maxQuantity > 1) {
      quantity = InputValidator.getIntInput(String.format("구매할 수량 (1~%d): ", maxQuantity), 1, maxQuantity);
    }

    // 대량 할인 이벤트 적용
    if (currentEventActive && currentEvent == ShopEvent.BULK_DISCOUNT && quantity >= 3) {
      finalPrice = (int) (finalPrice * 0.9); // 추가 10% 할인
      System.out.println("🎁 대량 구매 보너스! 추가 10% 할인 적용!");
    }

    int totalPrice = finalPrice * quantity;

    // 구매 확인
    System.out.printf("\n📦 구매 정보:%n");
    System.out.printf("아이템: %s x%d%n", item.getName(), quantity);

    if (currentEventActive && currentEvent != null && currentEvent.isBuyEvent()) {
      System.out.printf("원래 가격: %d골드%n", originalPrice * quantity);
      System.out.printf("할인 가격: %d골드 (%d%% 할인!)%n", totalPrice, Math.round(currentEvent.getDiscountPercent()));
    } else {
      System.out.printf("총 가격: %d골드%n", totalPrice);
    }

    System.out.printf("구매 후 잔액: %d골드%n", player.getGold() - totalPrice);

    if (!InputValidator.getConfirmation("구매하시겠습니까?")) {
      return;
    }

    // 특별 이벤트 효과 적용
    quantity = applySpecialEventEffects(item, quantity);

    // 행운의 뽑기 이벤트 처리
    boolean luckyRefund = false;
    if (currentEventActive && currentEvent == ShopEvent.LUCKY_DRAW) {
      if (random.nextBoolean()) { // 50% 확률
        luckyRefund = true;
        System.out.println("🎰 행운의 뽑기 당첨! 골드가 환급됩니다!");
      }
    }

    // 인벤토리 공간 확인
    if (!inventoryController.addItem(player, item, quantity)) {
      System.out.println("❌ 인벤토리 공간이 부족합니다!");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 구매 처리
    int finalPayment = luckyRefund ? 0 : totalPrice;
    player.setGold(player.getGold() - finalPayment);
    shopItem.reduceStock(quantity - getFreeBonusQuantity(item));

    System.out.printf("✅ %s x%d을(를) 구매했습니다!%n", item.getName(), quantity);
    if (luckyRefund) {
      System.out.println("🎰 행운의 뽑기로 골드가 환급되었습니다!");
    }
    System.out.printf("💰 잔액: %d골드%n", player.getGold());

    logger.info("아이템 구매: {} -> {} x{} ({}골드)", player.getName(), item.getName(), quantity, finalPayment);

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 특별 이벤트 효과를 적용합니다.
   */
  private int applySpecialEventEffects(GameItem item, int quantity) {
    if (!currentEventActive || currentEvent == null) {
      return quantity;
    }

    switch (currentEvent) {
      case FREE_POTION -> {
        if (item.getName().equals("체력 물약")) {
          quantity++; // 무료로 1개 추가
          System.out.println("🎁 이벤트 보너스로 체력 물약 1개를 추가로 드립니다!");
        }
      }
      case RARE_ITEMS -> {
        // 희귀 아이템 이벤트는 상점 아이템 목록에 특별 아이템 추가로 구현
        // 현재는 메시지만 표시
        System.out.println("⭐ 희귀 아이템 이벤트로 특별한 혜택을 받으실 수 있습니다!");
      }
    }

    return quantity;
  }

  /**
   * 무료 보너스 수량을 반환합니다.
   */
  private int getFreeBonusQuantity(GameItem item) {
    if (currentEventActive && currentEvent == ShopEvent.FREE_POTION && item.getName().equals("체력 물약")) {
      return 1;
    }
    return 0;
  }

  /**
   * 상점 이벤트를 실행합니다.
   */
  public void runShopEvent(GameCharacter player) {
    if (!currentEventActive) {
      System.out.println("현재 진행 중인 이벤트가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n🎉 === 현재 진행 중인 이벤트 ===");
    System.out.println(currentEvent.getDetailedInfo());
    System.out.println("================================");

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
   * 카테고리별 아이템 판매를 처리합니다.
   */
  private void sellItemsByCategory(GameCharacter player, Class<? extends GameItem> itemType, String categoryName) {
    var items = player.getInventory().getItems().stream().filter(stack -> itemType.isInstance(stack.getItem())).toList();

    if (items.isEmpty()) {
      System.out.println("판매할 " + categoryName + "가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== " + categoryName + " 판매 ===");
    System.out.println("💰 현재 골드: " + player.getGold());
    System.out.println();

    for (int i = 0; i < items.size(); i++) {
      var stack = items.get(i);
      GameItem item = stack.getItem();
      int sellPrice = calculateSellPrice(item);

      System.out.printf("%d. %s x%d - %d골드 (개당 %d골드)%n", i + 1, item.getName(), stack.getQuantity(), sellPrice * stack.getQuantity(), sellPrice);

      // 아이템 정보 표시
      if (item instanceof GameEquipment equipment) {
        System.out.printf("   📊 효과: %s%n", getEquipmentEffectDescription(equipment));
      } else if (item instanceof GameConsumable consumable) {
        if (consumable.getHpRestore() > 0) {
          System.out.printf("   ❤️ 체력 회복: %d%n", consumable.getHpRestore());
        }
      }
    }

    int itemIndex = InputValidator.getIntInput("판매할 아이템 번호 (0: 취소): ", 0, items.size()) - 1;
    if (itemIndex < 0)
      return;

    var selectedStack = items.get(itemIndex);
    handleItemSale(player, selectedStack);
  }

  /**
   * 장비 타입별 판매를 처리합니다.
   */
  private void sellEquipmentByType(GameCharacter player, GameEquipment.EquipmentType equipmentType) {
    var equipments = player.getInventory().getItems().stream().filter(stack -> stack.getItem() instanceof GameEquipment)
        .filter(stack -> ((GameEquipment) stack.getItem()).getEquipmentType() == equipmentType).toList();

    if (equipments.isEmpty()) {
      System.out.println("판매할 " + getEquipmentTypeKorean(equipmentType) + "가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== " + getEquipmentTypeKorean(equipmentType) + " 판매 ===");
    System.out.println("💰 현재 골드: " + player.getGold());
    System.out.println();

    for (int i = 0; i < equipments.size(); i++) {
      var stack = equipments.get(i);
      GameEquipment equipment = (GameEquipment) stack.getItem();
      int sellPrice = calculateSellPrice(equipment);

      System.out.printf("%d. %s [%s] - %d골드%n", i + 1, equipment.getName(), getRarityKorean(equipment.getRarity()), sellPrice);
      System.out.printf("   📊 효과: %s%n", getEquipmentEffectDescription(equipment));

      // 현재 착용 중인지 확인
      if (isCurrentlyEquipped(player, equipment)) {
        System.out.println("   ⚠️ 현재 착용 중인 장비입니다!");
      }
    }

    int itemIndex = InputValidator.getIntInput("판매할 장비 번호 (0: 취소): ", 0, equipments.size()) - 1;
    if (itemIndex < 0)
      return;

    var selectedStack = equipments.get(itemIndex);
    GameEquipment equipment = (GameEquipment) selectedStack.getItem();

    // 착용 중인 장비 판매 시 경고
    if (isCurrentlyEquipped(player, equipment)) {
      System.out.println("⚠️ 경고: 현재 착용 중인 장비입니다!");
      if (!InputValidator.getConfirmation("정말로 판매하시겠습니까? (자동으로 해제됩니다)")) {
        return;
      }

      // 장비 해제
      player.getInventory().unequipItem(equipment.getEquipmentType());
      System.out.println("✅ 장비를 해제했습니다.");
    }

    handleItemSale(player, selectedStack);
  }

  /**
   * 장비가 현재 착용 중인지 확인합니다.
   */
  private boolean isCurrentlyEquipped(GameCharacter player, GameEquipment equipment) {
    GameInventory inventory = player.getInventory();

    return (equipment.equals(inventory.getEquippedWeapon()) || equipment.equals(inventory.getEquippedArmor()) || equipment.equals(inventory.getEquippedAccessory()));
  }

  /**
   * 아이템 판매 메뉴를 실행합니다.
   */
  private void openShopSell(GameCharacter player) {
    while (true) {
      displaySellMenu(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 6);

      switch (choice) {
        case 1:
          sellItemsByCategory(player, GameConsumable.class, "소비 아이템");
          break;
        case 2:
          sellEquipmentByType(player, GameEquipment.EquipmentType.WEAPON);
          break;
        case 3:
          sellEquipmentByType(player, GameEquipment.EquipmentType.ARMOR);
          break;
        case 4:
          sellEquipmentByType(player, GameEquipment.EquipmentType.ACCESSORY);
          break;
        case 5:
          quickSellLowValueItems(player);
          break;
        case 6:
          return;
      }
    }
  }

  /**
   * 판매 메뉴를 표시합니다.
   */
  private void displaySellMenu(GameCharacter player) {
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
    int totalSellValue = calculateTotalSellValue(player);
    if (totalSellValue > 0) {
      System.out.println("💡 전체 아이템 판매 시 예상 수익: " + totalSellValue + "골드");
    }
  }

  /**
   * 아이템 판매를 처리합니다.
   */
  private void handleItemSale(GameCharacter player, GameInventory.ItemStack stack) {
    GameItem item = stack.getItem();
    int sellPrice = calculateSellPrice(item);
    int maxQuantity = stack.getQuantity();

    // 판매 수량 결정
    int quantity = 1;
    if (maxQuantity > 1) {
      quantity = InputValidator.getIntInput(String.format("판매할 수량 (1~%d): ", maxQuantity), 1, maxQuantity);
    }

    int totalPrice = sellPrice * quantity;

    // 판매 확인
    System.out.printf("\n💰 판매 정보:%n");
    System.out.printf("아이템: %s x%d%n", item.getName(), quantity);
    System.out.printf("판매 가격: %d골드%n", totalPrice);
    System.out.printf("판매 후 골드: %d골드%n", player.getGold() + totalPrice);

    if (!InputValidator.getConfirmation("판매하시겠습니까?")) {
      return;
    }

    // 판매 처리
    if (inventoryController.removeItem(player, item.getName(), quantity)) {
      player.setGold(player.getGold() + totalPrice);

      System.out.printf("✅ %s x%d을(를) %d골드에 판매했습니다!%n", item.getName(), quantity, totalPrice);
      System.out.printf("💰 현재 골드: %d골드%n", player.getGold());

      logger.info("아이템 판매: {} -> {} x{} ({}골드)", player.getName(), item.getName(), quantity, totalPrice);
    } else {
      System.out.println("❌ 판매에 실패했습니다.");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 낮은 가치의 아이템들을 일괄 판매합니다.
   */
  private void quickSellLowValueItems(GameCharacter player) {
    System.out.println("\n⚡ === 일괄 판매 ===");
    System.out.println("💡 일반 등급 아이템들을 빠르게 판매합니다.");
    System.out.println();

    var commonItems =
        player.getInventory().getItems().stream().filter(stack -> stack.getItem().getRarity() == GameItem.ItemRarity.COMMON).filter(stack -> !isCurrentlyEquipped(player, stack.getItem())).toList();

    if (commonItems.isEmpty()) {
      System.out.println("일괄 판매할 일반 아이템이 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("판매 예정 아이템:");
    int totalValue = 0;

    for (var stack : commonItems) {
      GameItem item = stack.getItem();
      int itemValue = calculateSellPrice(item) * stack.getQuantity();
      totalValue += itemValue;

      System.out.printf("• %s x%d - %d골드%n", item.getName(), stack.getQuantity(), itemValue);
    }

    System.out.printf("\n💰 총 판매 수익: %d골드%n", totalValue);
    System.out.printf("💰 판매 후 골드: %d골드%n", player.getGold() + totalValue);

    if (!InputValidator.getConfirmation("모든 일반 아이템을 판매하시겠습니까?")) {
      return;
    }

    // 일괄 판매 처리
    int soldCount = 0;
    for (var stack : commonItems) {
      GameItem item = stack.getItem();
      int quantity = stack.getQuantity();

      if (inventoryController.removeItem(player, item.getName(), quantity)) {
        soldCount++;
      }
    }

    player.setGold(player.getGold() + totalValue);

    System.out.printf("✅ 총 %d종류의 아이템을 %d골드에 판매했습니다!%n", soldCount, totalValue);
    System.out.printf("💰 현재 골드: %d골드%n", player.getGold());

    logger.info("일괄 판매: {} -> {}종류 아이템 ({}골드)", player.getName(), soldCount, totalValue);

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 장비가 현재 착용 중인지 확인합니다 (GameItem 버전).
   */
  private boolean isCurrentlyEquipped(GameCharacter player, GameItem item) {
    if (!(item instanceof GameEquipment equipment)) {
      return false;
    }
    return isCurrentlyEquipped(player, equipment);
  }

  /**
   * 인벤토리에서 아이템을 제거합니다.
   */
  private boolean removeItem(GameCharacter player, String itemName, int quantity) {
    return player.getInventory().removeItem(itemName, quantity);
  }

  /**
   * 판매 시세를 확인합니다.
   */
  private void showSellPrices(GameCharacter player) {
    System.out.println("\n📊 === 판매 시세 정보 ===");
    System.out.println("💡 상점에서는 아이템을 원가의 60%에 매입합니다.");
    System.out.println();

    var items = player.getInventory().getItems();
    if (items.isEmpty()) {
      System.out.println("인벤토리에 아이템이 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 카테고리별로 시세 표시
    displaySellPricesByCategory(player, GameConsumable.class, "🧪 소비 아이템");
    displaySellPricesByCategory(player, GameEquipment.class, "⚔️ 장비");

    // 총 예상 수익
    int totalValue = calculateTotalSellValue(player);
    System.out.printf("\n💰 전체 아이템 판매 시 예상 수익: %d골드%n", totalValue);

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 카테고리별 판매 시세를 표시합니다.
   */
  private void displaySellPricesByCategory(GameCharacter player, Class<? extends GameItem> itemType, String categoryName) {
    var items = player.getInventory().getItems().stream().filter(stack -> itemType.isInstance(stack.getItem())).toList();

    if (items.isEmpty())
      return;

    System.out.println("\n" + categoryName + ":");

    for (var stack : items) {
      GameItem item = stack.getItem();
      int sellPrice = calculateSellPrice(item);
      int totalValue = sellPrice * stack.getQuantity();

      System.out.printf("• %s x%d: %d골드 (개당 %d골드)%n", item.getName(), stack.getQuantity(), totalValue, sellPrice);

      if (item instanceof GameEquipment equipment && isCurrentlyEquipped(player, equipment)) {
        System.out.println("  ⚠️ 착용 중 (판매 시 자동 해제)");
      }
    }
  }

  /**
   * 이벤트 할인을 적용합니다.
   */
  private int applyEventDiscount(int originalPrice) {
    if (currentEventActive && currentEvent == ShopEvent.DISCOUNT_SALE) {
      return (int) (originalPrice * 0.8); // 20% 할인
    }
    return originalPrice;
  }

  /**
   * 아이템의 판매 가격을 계산합니다.
   */
  private int calculateSellPrice(GameItem item) {
    // 기본적으로 원가의 60%로 매입
    int basePrice = (int) (item.getValue() * 0.6);
    // 등급에 따른 보너스
    double rarityMultiplier = switch (item.getRarity()) {
      case COMMON -> 1.0;
      case UNCOMMON -> 1.1;
      case RARE -> 1.2;
      case EPIC -> 1.3;
      case LEGENDARY -> 1.5;
    };

    int finalPrice = Math.max(1, (int) (basePrice * rarityMultiplier));

    // 이벤트 보너스 적용
    if (currentEventActive && currentEvent != null) {
      
      finalPrice = currentEvent.applySellBonus(finalPrice);
    }
    logger.info("calculateSellPrice result : {},{},{},{}",currentEventActive, currentEvent, basePrice, finalPrice);

    return finalPrice;
  }

  /**
   * 전체 아이템의 판매 가치를 계산합니다.
   */
  private int calculateTotalSellValue(GameCharacter player) {
    return player.getInventory().getItems().stream().mapToInt(stack -> calculateSellPrice(stack.getItem()) * stack.getQuantity()).sum();
  }

  /**
   * 등급을 한국어로 변환합니다.
   */
  private String getRarityKorean(GameItem.ItemRarity rarity) {
    return switch (rarity) {
      case COMMON -> "일반";
      case UNCOMMON -> "고급";
      case RARE -> "희귀";
      case EPIC -> "영웅";
      case LEGENDARY -> "전설";
    };
  }

  /**
   * 장비 타입을 한국어로 변환합니다.
   */
  private String getEquipmentTypeKorean(GameEquipment.EquipmentType type) {
    return switch (type) {
      case WEAPON -> "무기";
      case ARMOR -> "방어구";
      case ACCESSORY -> "장신구";
    };
  }

  /**
   * 카테고리를 한국어로 변환합니다.
   */
  private String getCategoryKorean(ShopItemCategory category) {
    return switch (category) {
      case CONSUMABLE -> "소비 아이템";
      case WEAPON -> "무기";
      case ARMOR -> "방어구";
      case ACCESSORY -> "장신구";
    };
  }

  /**
   * 장비 효과 설명을 생성합니다.
   */
  private String getEquipmentEffectDescription(GameEquipment equipment) {
    StringBuilder effects = new StringBuilder();

    if (equipment.getAttackBonus() > 0) {
      effects.append("공격력 +").append(equipment.getAttackBonus()).append(" ");
    }

    if (equipment.getDefenseBonus() > 0) {
      effects.append("방어력 +").append(equipment.getDefenseBonus()).append(" ");
    }

    if (equipment.getHpBonus() > 0) {
      effects.append("체력 +").append(equipment.getHpBonus()).append(" ");
    }

    return effects.length() > 0 ? effects.toString().trim() : "특별한 효과 없음";
  }

  /**
   * 상점 통계를 표시합니다.
   */
  public void displayShopStatistics(GameCharacter player) {
    System.out.println("\n📊 === 상점 통계 ===");
    System.out.println("💰 플레이어 골드: " + player.getGold());

    int totalSellValue = calculateTotalSellValue(player);
    System.out.println("💎 인벤토리 총 가치: " + totalSellValue + "골드");

    // 재고 상황
    System.out.println("\n📦 상점 재고 상황:");
    long lowStockItems = shopItems.stream().filter(item -> item.getStock() < item.getMaxStock() * 0.3).count();

    System.out.println("⚠️ 재고 부족 아이템: " + lowStockItems + "개");
    System.out.println("📈 총 상품 종류: " + shopItems.size() + "개");

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }


  /**
   * 상점 재고를 보충합니다 (특정 조건에서 호출)
   */
  public void restockShop() {
    for (ShopItem item : shopItems) {
      if (item.getCategory() == ShopItemCategory.CONSUMABLE) {
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
}
