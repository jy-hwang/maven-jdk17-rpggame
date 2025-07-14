package rpg.presentation.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.validator.InputValidator;
import rpg.domain.inventory.ItemStack;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.domain.player.Player;
import rpg.domain.shop.ShopEvent;
import rpg.domain.shop.ShopItem;
import rpg.domain.shop.ShopItemCategory;
import rpg.presentation.menu.ShopMenu;
import rpg.shared.constant.GameConstants;
import rpg.shared.constant.ItemConstants;

/**
 * @formatter:off
 * 상점 시스템을 전담하는 컨트롤러 (GameItemFactory 통합 버전)
 * - GameItemFactory를 사용하여 아이템 생성
 * - 통일된 아이템 시스템 사용
 * - 동적 상점 재고 관리
 * @formatter:on
 */
public class ShopController {
  private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

  private final List<ShopItem> shopItems;
  private final InventoryController inventoryController;
  private final GameItemFactory itemFactory;
  private final Random random;
  private final ShopMenu shopMenu;

  private boolean currentEventActive = false;
  private ShopEvent currentEvent = null;

  public ShopController(InventoryController inventoryController) {
    this.inventoryController = inventoryController;
    this.itemFactory = GameItemFactory.getInstance();
    this.shopItems = new ArrayList<>();
    this.random = new Random();
    this.shopMenu = new ShopMenu();
    initializeShopItems();
    logger.debug("ShopController 초기화 완료 (GameItemFactory 통합)");
  }

  /**
   * 상점 아이템을 초기화합니다 (GameItemFactory 사용)
   */
  private void initializeShopItems() {
    logger.info("상점 아이템 초기화 중... (GameItemFactory 기반)");

    try {
      // GameItemFactory에서 사용 가능한 아이템들 가져오기
      List<String> availableItemIds = itemFactory.getAllItemIds();

      if (availableItemIds.isEmpty()) {
        logger.warn("GameItemFactory에 아이템이 없음 - 기본 아이템 생성");
        createBasicShopItems();
        return;
      }

      // 팩토리에서 아이템 생성하여 상점에 추가
      for (String itemId : availableItemIds) {
        GameItem item = itemFactory.createItem(itemId);
        if (item != null) {
          addItemToShop(item, itemId);
        }
      }

      // 추가 특별 아이템들 (상점 전용)
      addSpecialShopItems();

      logger.info("상점 아이템 초기화 완료: {}개 아이템", shopItems.size());

    } catch (Exception e) {
      logger.error("상점 아이템 초기화 실패", e);
      createBasicShopItems();
    }
  }

  /**
   * 아이템을 상점에 추가
   */
  private void addItemToShop(GameItem item, String itemId) {
    try {
      // 아이템 타입에 따른 카테고리 결정
      ShopItemCategory category = determineItemCategory(item);

      // 가격 결정 (팩토리 아이템의 기본 가격 사용)
      int price = item.getValue();

      // 재고 결정
      int stock = determineStock(item, category);

      ShopItem shopItem = new ShopItem(item, price, stock, category);
      shopItems.add(shopItem);

      logger.debug("상점 아이템 추가: {} (가격: {}G, 재고: {})", item.getName(), price, stock);

    } catch (Exception e) {
      logger.error("상점 아이템 추가 실패: {}", item.getName(), e);
    }
  }

  /**
   * 아이템 카테고리 결정
   */
  private ShopItemCategory determineItemCategory(GameItem item) {
    if (item instanceof GameConsumable) {
      return ShopItemCategory.CONSUMABLE;
    } else if (item instanceof GameEquipment equipment) {
      return switch (equipment.getEquipmentType()) {
        case WEAPON -> ShopItemCategory.WEAPON;
        case ARMOR -> ShopItemCategory.ARMOR;
        case ACCESSORY -> ShopItemCategory.ACCESSORY;
      };
    }
    return ShopItemCategory.CONSUMABLE; // 기본값
  }

  /**
   * 아이템 재고 결정
   */
  private int determineStock(GameItem item, ShopItemCategory category) {
    // 소비 아이템은 무제한, 장비는 제한적
    return switch (category) {
      case CONSUMABLE -> 999; // 무제한
      case WEAPON, ARMOR -> 5; // 제한적
      case ACCESSORY -> 3; // 더 제한적
    };
  }

  /**
   * 특별 상점 전용 아이템 추가
   */
  private void addSpecialShopItems() {
    try {
      // 상점에서만 구매할 수 있는 특별 아이템들
      // (GameItemFactory에 없는 아이템들)

      // 고급 장비들 - 현재는 GameEquipment만 지원
      // TODO: GameEquipment도 팩토리 시스템으로 전환 예정

      GameEquipment steelSword =
          new GameEquipment("STEEL_SWORD", "강철검", "단단한 강철로 만든 검", 200, ItemRarity.UNCOMMON, GameEquipment.EquipmentType.WEAPON, 15, 0, 0);
      addItemToShop(steelSword, "STEEL_SWORD_SHOP");

      GameEquipment ironArmor =
          new GameEquipment("IRON_ARMOR", "철갑옷", "튼튼한 철로 만든 갑옷", 160, ItemRarity.UNCOMMON, GameEquipment.EquipmentType.ARMOR, 0, 12, 25);
      addItemToShop(ironArmor, "IRON_ARMOR_SHOP");

      GameEquipment healthRing =
          new GameEquipment("HEALTH_RING", "체력의 반지", "체력을 증가시켜주는 반지", 150, ItemRarity.RARE, GameEquipment.EquipmentType.ACCESSORY, 0, 0, 15);
      addItemToShop(healthRing, "HEALTH_RING_SHOP");

      // 특별 소비 아이템들 (GameEffectFactory 사용)
      List<GameEffect> superHealEffect = List.of(GameEffectFactory.createHealHpEffect(100));

      GameConsumable superHealthPotion =
          new GameConsumable("SUPER_HEALTH_POTION", "고급 체력 물약", "HP를 100 회복합니다", 80, ItemRarity.UNCOMMON, superHealEffect, 0);
      addItemToShop(superHealthPotion, "SUPER_HEALTH_POTION_SHOP");

      // 복합 효과 물약 (HP + MP 동시 회복)
      List<GameEffect> hybridEffects = List.of(GameEffectFactory.createHealHpEffect(60), GameEffectFactory.createHealMpEffect(40));

      GameConsumable hybridPotion = new GameConsumable("HYBRID_POTION", "만능 물약", "HP를 60, MP를 40 회복합니다", 120, ItemRarity.RARE, hybridEffects, 1 // 1턴
                                                                                                                                                // 쿨다운
      );
      addItemToShop(hybridPotion, "HYBRID_POTION_SHOP");

      logger.debug("특별 상점 아이템 {}개 추가 (GameEffectFactory 기반 포함)", 5);

    } catch (Exception e) {
      logger.error("특별 상점 아이템 추가 실패", e);
    }
  }

  /**
   * 기본 상점 아이템 생성 (팩토리 실패 시)
   */
  private void createBasicShopItems() {
    logger.warn("기본 상점 아이템 생성 중... (GameEffectFactory 사용)");

    try {
      // GameEffectFactory를 사용하여 효과 생성
      List<GameEffect> healHpEffect = List.of(GameEffectFactory.createHealHpEffect(50));

      List<GameEffect> healMpEffect = List.of(GameEffectFactory.createHealMpEffect(30));

      // GameEffect 시스템을 사용하는 새 생성자로 아이템 생성
      GameConsumable healthPotion = new GameConsumable("HEALTH_POTION", "체력 물약", "HP를 50 회복합니다", 20, ItemRarity.COMMON, healHpEffect, 0 // 쿨다운 없음
      );
      addItemToShop(healthPotion, "HEALTH_POTION");

      GameConsumable manaPotion = new GameConsumable("MANA_POTION", "마나 물약", "MP를 30 회복합니다", 25, ItemRarity.COMMON, healMpEffect, 0 // 쿨다운 없음
      );
      addItemToShop(manaPotion, "MANA_POTION");

      logger.info("기본 상점 아이템 생성 완료: {}개 (GameEffectFactory 기반)", shopItems.size());

    } catch (Exception e) {
      logger.error("기본 상점 아이템 생성 실패", e);
      logger.warn("GameEffectFactory를 사용한 아이템 생성에 실패했습니다.");
      logger.info("상점이 빈 상태로 시작됩니다. 게임 진행 중 동적으로 아이템이 추가될 수 있습니다.");

      // 빈 상점으로 시작 - 런타임에 GameItemFactory에서 아이템을 로드할 수 있음
      // 이는 실제로 더 안전한 방식입니다 (레거시 코드 의존성 제거)
    }
  }

  /**
   * 상점 메뉴를 실행합니다.
   */
  public void openShop(Player player) {
    // 상점 진입 시 랜덤 이벤트 체크
    checkForRandomEvent();

    while (true) {
      displayShopMenuMain(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

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
          showShopStatistics(player);
          break;
        case 5:
          return;
      }
    }
  }

  /**
   * 상점 메인 메뉴를 표시합니다.
   */
  private void displayShopMenuMain(Player player) {
    
//    System.out.println("\n🏪 === 마을 상점 ===");
//    System.out.println("💰 보유 골드: " + player.getGold());
//
//    // GameItemFactory 상태 표시
//    System.out.println("📦 상품 종류: " + shopItems.size() + "개 (팩토리 기반)");
//
//    // 이벤트가 활성화되어 있으면 표시
//    if (currentEventActive && currentEvent != null) {
//      displayActiveEventInfo();
//    }
//
//    System.out.println();
//    System.out.println("1. 🛒 아이템 사기");
//    System.out.println("2. 💰 아이템 팔기");
//    System.out.println("3. 📊 판매 시세 확인");
//    System.out.println("4. 📈 상점 통계");
//    System.out.println("5. 🚪 상점 나가기");
//    System.out.println("====================");
    //boolean hasEvent = true;
    
    shopMenu.displayShopMenuMain(player, shopItems.size(), currentEventActive, currentEvent); 
      
      
    
  }

  /**
   * 구매 메뉴를 실행합니다.
   */
  public void openShopBuy(Player player) {
    while (true) {
      displayShopMenuBuy(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 6);

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
          showRandomRecommendations(player);
          break;
        case 6:
          return;
      }
    }
  }

  /**
   * 구매 메뉴를 표시합니다.
   */
  private void displayShopMenuBuy(Player player) {
    System.out.println("\n🏪 === 마을 상점 구매 ===");
    System.out.println("💰 보유 골드: " + player.getGold());

    // 카테고리별 아이템 수 표시
    displayCategoryStats();

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
   * 카테고리별 통계 표시
   */
  private void displayCategoryStats() {
    for (ShopItemCategory category : ShopItemCategory.values()) {
      long count = shopItems.stream().filter(item -> item.getCategory() == category).filter(item -> item.getStock() > 0).count();

      if (count > 0) {
        System.out.printf("   %s: %d개%n", getCategoryKorean(category), count);
      }
    }
  }

  /**
   * 랜덤 추천 아이템 표시
   */
  private void showRandomRecommendations(Player player) {
    System.out.println("\n🎲 === 오늘의 추천 아이템 ===");

    // 플레이어 레벨에 맞는 아이템 추천
    List<ShopItem> recommendations = getRecommendationsForPlayer(player);

    if (recommendations.isEmpty()) {
      System.out.println("현재 추천할 아이템이 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("💡 " + player.getName() + "님께 추천하는 아이템:");

    for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
      ShopItem shopItem = recommendations.get(i);
      GameItem item = shopItem.getItem();

      System.out.printf("%d. %s - %d골드%n", i + 1, item.getName(), shopItem.getPrice());
      System.out.printf("   📝 %s%n", item.getDescription());
      System.out.printf("   ⭐ %s | 💰 구매 가능: %s%n", getRarityKorean(item.getRarity()), player.getGold() >= shopItem.getPrice() ? "예" : "아니오");

      if (item instanceof GameEquipment equipment) {
        System.out.printf("   🔥 효과: %s%n", getEquipmentEffectDescription(equipment));
      } else if (item instanceof GameConsumable consumable) {
        System.out.printf("   ✨ 효과: %s%n", consumable.getEffectsDescription());
      }
      System.out.println();
    }

    int choice = InputValidator.getIntInput("구매할 아이템 번호 (0: 취소): ", 0, Math.min(3, recommendations.size()));
    if (choice > 0) {
      ShopItem selectedItem = recommendations.get(choice - 1);
      handleItemPurchase(player, selectedItem);
    }
  }

  /**
   * 플레이어에게 맞는 추천 아이템 생성
   */
  private List<ShopItem> getRecommendationsForPlayer(Player player) {
    List<ShopItem> recommendations = new ArrayList<>();

    // 구매 가능한 아이템들 중에서 선택
    List<ShopItem> affordableItems =
        shopItems.stream().filter(item -> item.getStock() > 0).filter(item -> player.getGold() >= item.getPrice()).toList();

    if (affordableItems.isEmpty()) {
      return recommendations;
    }

    // 등급별로 가중치를 두고 랜덤 선택
    for (ShopItem shopItem : affordableItems) {
      GameItem item = shopItem.getItem();

      // 플레이어 레벨에 맞는 아이템인지 확인
      boolean suitable = switch (item.getRarity()) {
        case COMMON -> true;
        case UNCOMMON -> player.getLevel() >= 3;
        case RARE -> player.getLevel() >= 7;
        case EPIC -> player.getLevel() >= 15;
        case LEGENDARY -> player.getLevel() >= 25;
      };

      if (suitable && random.nextBoolean()) {
        recommendations.add(shopItem);
      }
    }

    // 최대 5개까지만
    if (recommendations.size() > 5) {
      recommendations = recommendations.subList(0, 5);
    }

    return recommendations;
  }

  /**
   * 플레이어 레벨에 맞는 스마트 정렬을 반환합니다 콘솔 UX를 고려하여 적합한 아이템을 맨 아래(보기 쉬운 곳)에 표시
   */
  private Comparator<ShopItem> getSmartSorting(Player player) {
    return Comparator.comparing((ShopItem item) -> getRarityPriority(item.getItem().getRarity(), player.getLevel()))
        .thenComparingInt(ShopItem::getPrice).reversed(); // 전체 순서를 뒤집어서 적합한 아이템이 아래로
  }

  /**
   * 플레이어 레벨에 따른 등급별 우선순위를 반환합니다 낮은 숫자일수록 먼저 표시됨
   */
  private int getRarityPriority(ItemRarity rarity, int playerLevel) {
    if (playerLevel <= 5) {
      // 초보자 (1-5레벨): 기본 아이템 위주
      return switch (rarity) {
        case COMMON -> 1; // 가장 먼저
        case UNCOMMON -> 2;
        case RARE -> 3;
        case EPIC -> 4;
        case LEGENDARY -> 5; // 가장 나중
      };
    } else if (playerLevel <= 15) {
      // 중급자 (6-15레벨): 일반~고급 아이템 위주
      return switch (rarity) {
        case UNCOMMON -> 1; // 가장 먼저
        case COMMON -> 2;
        case RARE -> 2; // COMMON과 같은 우선순위
        case EPIC -> 3;
        case LEGENDARY -> 4;
      };
    } else if (playerLevel <= 25) {
      // 고급자 (16-25레벨): 고급 아이템 위주
      return switch (rarity) {
        case RARE -> 1; // 가장 먼저
        case UNCOMMON -> 2;
        case EPIC -> 2; // UNCOMMON과 같은 우선순위
        case COMMON -> 3;
        case LEGENDARY -> 3; // COMMON과 같은 우선순위
      };
    } else {
      // 전문가 (26+레벨): 최고급 아이템 위주
      return switch (rarity) {
        case EPIC -> 1; // 가장 먼저
        case LEGENDARY -> 1; // EPIC과 같은 우선순위
        case RARE -> 2;
        case UNCOMMON -> 3;
        case COMMON -> 4; // 가장 나중
      };
    }
  }

  /**
   * 카테고리별 아이템 목록을 표시하고 구매를 처리합니다 (스마트 정렬 적용)
   */
  private void browseCategoryItems(Player player, ShopItemCategory category) {
    var categoryItems =
        shopItems.stream().filter(item -> item.getCategory() == category).filter(item -> item.getStock() > 0).sorted(getSmartSorting(player)) // 스마트
                                                                                                                                              // 정렬 적용
            .toList();

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

      // 재고가 떨어진 아이템 제거 후 다시 정렬
      categoryItems = categoryItems.stream().filter(item -> item.getStock() > 0).sorted(getSmartSorting(player)) // 재정렬
          .toList();

      if (categoryItems.isEmpty()) {
        System.out.println("🔄 해당 카테고리의 모든 아이템이 품절되었습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        break;
      }
    }
  }

  /**
   * 스마트 정렬 정보를 플레이어에게 표시합니다 (선택적)
   */
  private void displaySmartSortingInfo(Player player, ShopItemCategory category) {
    String sortingInfo = getSortingDescription(player.getLevel());
    System.out.println("💡 정렬 기준: " + sortingInfo);
    System.out.println();
  }

  /**
   * 현재 정렬 방식에 대한 설명을 반환합니다
   */
  private String getSortingDescription(int playerLevel) {
    if (playerLevel <= 5) {
      return "기본 아이템 위주 (레벨 " + playerLevel + ")";
    } else if (playerLevel <= 15) {
      return "일반~고급 아이템 위주 (레벨 " + playerLevel + ")";
    } else if (playerLevel <= 25) {
      return "고급 아이템 위주 (레벨 " + playerLevel + ")";
    } else {
      return "최고급 아이템 위주 (레벨 " + playerLevel + ")";
    }
  }

  /**
   * 카테고리별 아이템 목록을 표시합니다 (정렬 정보 포함)
   */
  private void displayCategoryItems(Player player, ShopItemCategory category, List<ShopItem> items) {
    System.out.println("\n🏪 === " + getCategoryKorean(category) + " ===");

    for (int i = 0; i < items.size(); i++) {
      ShopItem shopItem = items.get(i);
      GameItem item = shopItem.getItem();

      System.out.printf("%d. %s - %d골드", i + 1, item.getName(), shopItem.getPrice());

      if (shopItem.getStock() < 999) {
        System.out.printf(" (재고: %d개)", shopItem.getStock());
      }

      // 구매 가능 여부 표시
      if (player.getGold() < shopItem.getPrice()) {
        System.out.print(" ❌");
      } else {
        System.out.print(" ✅");
      }

      System.out.println();
      System.out.printf("   📝 %s%n", item.getDescription());
      System.out.printf("   ⭐ %s%n", getRarityKorean(item.getRarity()));

      // 아이템 효과 표시
      if (item instanceof GameEquipment equipment) {
        System.out.printf("   🔥 효과: %s%n", getEquipmentEffectDescription(equipment));
      } else if (item instanceof GameConsumable consumable) {
        System.out.printf("   ✨ 효과: %s%n", consumable.getEffectsDescription());
      }

      System.out.println();
    }

    System.out.println("💰 보유 골드: " + player.getGold());

    // 스마트 정렬 정보 표시 (선택적)
    displaySmartSortingInfo(player, category);

    System.out.println("0. 🔙 뒤로가기");
    System.out.println("====================");
  }

  /**
   * 아이템 구매를 처리합니다.
   */
  private void handleItemPurchase(Player player, ShopItem shopItem) {
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

    // 중첩 가능한 아이템이고 여러 개 구매 가능한 경우
    if (itemFactory.isStackable(getItemId(item)) && maxQuantity > 1) {
      quantity = InputValidator.getIntInput(String.format("구매할 수량 (1~%d): ", maxQuantity), 1, maxQuantity);
    }

    int totalPrice = finalPrice * quantity;

    // 구매 확인
    System.out.printf("\n📦 구매 정보:%n");
    System.out.printf("아이템: %s x%d%n", item.getName(), quantity);

    if (currentEventActive && currentEvent != null && currentEvent.isBuyEvent()) {
      System.out.printf("원래 가격: %d골드%n", originalPrice * quantity);
      System.out.printf("할인 가격: %d골드%n", totalPrice);
    } else {
      System.out.printf("총 가격: %d골드%n", totalPrice);
    }

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
   * 아이템의 ID를 추정하여 반환 (팩토리에서 중첩 가능 여부 확인용)
   */
  private String getItemId(GameItem item) {
    // 아이템 이름을 기반으로 ID 추정
    return item.getName().toUpperCase().replace(" ", "_").replace("물약", "_POTION");
  }

  // ==================== 판매 관련 메서드들 ====================

  /**
   * 아이템 판매 메뉴를 실행합니다.
   */
  private void openShopSell(Player player) {
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
  private void displaySellMenu(Player player) {
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

  // ==================== 이벤트 관련 메서드들 ====================

  /**
   * 랜덤 상점 이벤트를 체크합니다.
   */
  private void checkForRandomEvent() {
    if (random.nextInt(GameConstants.NUMBER_HUNDRED) < ItemConstants.EVENT_CHANCE) {
      triggerRandomEvent();
    }
  }

  /**
   * 랜덤 이벤트를 발생시킵니다.
   */
  private void triggerRandomEvent() {
    logger.info("triggerRandomEvent() executed");
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
   * 이벤트 할인을 적용합니다.
   */
  private int applyEventDiscount(int originalPrice) {
    if (currentEventActive && currentEvent == ShopEvent.DISCOUNT_SALE) {
      return (int) (originalPrice * 0.8); // 20% 할인
    }
    return originalPrice;
  }

  // ==================== 상점 통계 및 관리 ====================

  /**
   * 상점 통계를 표시합니다.
   */
  private void showShopStatistics(Player player) {
    System.out.println("\n📊 === 상점 통계 ===");
    System.out.println("💰 플레이어 골드: " + player.getGold());

    // GameItemFactory 정보
    System.out.println("🏭 아이템 팩토리 정보:");
    System.out.printf("   📦 등록된 아이템: %d개%n", itemFactory.getItemCount());
    System.out.printf("   🔄 초기화 상태: %s%n", itemFactory.isInitialized() ? "완료" : "미완료");

    // 상점 재고 정보
    System.out.println("\n🏪 상점 재고 정보:");
    for (ShopItemCategory category : ShopItemCategory.values()) {
      long totalItems = shopItems.stream().filter(item -> item.getCategory() == category).count();
      long availableItems = shopItems.stream().filter(item -> item.getCategory() == category).filter(item -> item.getStock() > 0).count();

      System.out.printf("   %s: %d/%d개 판매중%n", getCategoryKorean(category), availableItems, totalItems);
    }

    // 가격 범위 정보
    if (!shopItems.isEmpty()) {
      int minPrice = shopItems.stream().mapToInt(ShopItem::getPrice).min().orElse(0);
      int maxPrice = shopItems.stream().mapToInt(ShopItem::getPrice).max().orElse(0);
      double avgPrice = shopItems.stream().mapToInt(ShopItem::getPrice).average().orElse(0);

      System.out.println("\n💰 가격 정보:");
      System.out.printf("   최저가: %d골드%n", minPrice);
      System.out.printf("   최고가: %d골드%n", maxPrice);
      System.out.printf("   평균가: %.1f골드%n", avgPrice);
    }

    // 플레이어 구매력 분석
    long affordableItems = shopItems.stream().filter(item -> item.getStock() > 0).filter(item -> player.getGold() >= item.getPrice()).count();

    System.out.println("\n💳 구매력 분석:");
    System.out.printf("   구매 가능한 아이템: %d개%n", affordableItems);

    if (affordableItems == 0) {
      System.out.println("   💡 더 많은 골드를 모아보세요!");
    } else if (affordableItems < shopItems.size() / 2) {
      System.out.println("   💡 중급 수준의 구매력입니다.");
    } else {
      System.out.println("   💡 우수한 구매력입니다!");
    }

    System.out.println("==================");
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  // ==================== 판매 관련 구현 ====================

  /**
   * 카테고리별 아이템 판매를 처리합니다.
   */
  private void sellItemsByCategory(Player player, Class<? extends GameItem> itemType, String categoryName) {
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
        System.out.printf("   ✨ 효과: %s%n", consumable.getEffectsDescription());
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
  private void sellEquipmentByType(Player player, GameEquipment.EquipmentType equipmentType) {
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
   * 아이템 판매를 처리합니다.
   */
  private void handleItemSale(Player player, ItemStack stack) {
    GameItem item = stack.getItem();
    int sellPrice = calculateSellPrice(item);
    int maxQuantity = stack.getQuantity();

    // 판매 수량 결정
    int quantity = 1;
    if (maxQuantity > 1) {
      quantity = InputValidator.getIntInput(String.format("판매할 수량 (1~%d): ", maxQuantity), 1, maxQuantity);
    }

    int totalPrice = sellPrice * quantity;

    // 이벤트 보너스 적용
    if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
      totalPrice = (int) (totalPrice * 1.3); // 30% 보너스
    }

    // 판매 확인
    System.out.printf("\n💰 판매 정보:%n");
    System.out.printf("아이템: %s x%d%n", item.getName(), quantity);
    System.out.printf("기본 판매가: %d골드%n", sellPrice * quantity);

    if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
      System.out.printf("이벤트 보너스: %d골드 (30%% 추가!)%n", totalPrice - (sellPrice * quantity));
      System.out.printf("최종 판매가: %d골드%n", totalPrice);
    }

    System.out.printf("판매 후 골드: %d골드%n", player.getGold() + totalPrice);

    if (!InputValidator.getConfirmation("판매하시겠습니까?")) {
      return;
    }

    // 판매 처리
    if (player.getInventory().removeItem(item.getName(), quantity)) {
      player.setGold(player.getGold() + totalPrice);

      System.out.printf("✅ %s x%d을(를) %d골드에 판매했습니다!%n", item.getName(), quantity, totalPrice);

      if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
        System.out.println("🎉 이벤트 보너스가 적용되었습니다!");
      }

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
  private void quickSellLowValueItems(Player player) {
    System.out.println("\n⚡ === 일괄 판매 ===");
    System.out.println("💡 일반 등급 아이템들을 빠르게 판매합니다.");
    System.out.println();

    var commonItems = player.getInventory().getItems().stream().filter(stack -> stack.getItem().getRarity() == ItemRarity.COMMON)
        .filter(stack -> !isCurrentlyEquipped(player, stack.getItem())).toList();

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

    // 이벤트 보너스 적용
    if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
      int bonusValue = (int) (totalValue * 0.3);
      totalValue += bonusValue;
      System.out.printf("\n🎉 이벤트 보너스: +%d골드%n", bonusValue);
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

      if (player.getInventory().removeItem(item.getName(), quantity)) {
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
   * 판매 시세를 확인합니다.
   */
  private void showSellPrices(Player player) {
    System.out.println("\n📊 === 판매 시세 정보 ===");
    System.out.println("💡 상점에서는 아이템을 원가의 60%에 매입합니다.");

    if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
      System.out.println("🎉 현재 고가 매입 이벤트로 30% 보너스 지급중!");
    }

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
  private void displaySellPricesByCategory(Player player, Class<? extends GameItem> itemType, String categoryName) {
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

  // ==================== 유틸리티 메서드들 ====================

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

    // 이벤트 보너스는 handleItemSale에서 적용
    return finalPrice;
  }

  /**
   * 전체 아이템의 판매 가치를 계산합니다.
   */
  private int calculateTotalSellValue(Player player) {
    int totalValue = player.getInventory().getItems().stream().mapToInt(stack -> calculateSellPrice(stack.getItem()) * stack.getQuantity()).sum();

    // 이벤트 보너스 적용
    if (currentEventActive && currentEvent == ShopEvent.BONUS_SELL) {
      totalValue = (int) (totalValue * 1.3);
    }

    return totalValue;
  }

  /**
   * 장비가 현재 착용 중인지 확인합니다.
   */
  private boolean isCurrentlyEquipped(Player player, GameEquipment equipment) {
    PlayerInventory inventory = player.getInventory();
    return equipment.equals(inventory.getEquippedWeapon()) || equipment.equals(inventory.getEquippedArmor())
        || equipment.equals(inventory.getEquippedAccessory());
  }

  /**
   * 장비가 현재 착용 중인지 확인합니다 (GameItem 버전).
   */
  private boolean isCurrentlyEquipped(Player player, GameItem item) {
    if (!(item instanceof GameEquipment equipment)) {
      return false;
    }
    return isCurrentlyEquipped(player, equipment);
  }

  /**
   * 상점 재고를 보충합니다.
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
   * 새로운 아이템을 상점에 추가 (동적 추가)
   */
  public void addNewItemToShop(String itemId) {
    GameItem item = itemFactory.createItem(itemId);
    if (item != null) {
      // 기존에 없는 아이템인지 확인
      boolean exists = shopItems.stream().anyMatch(shopItem -> shopItem.getItem().getName().equals(item.getName()));

      if (!exists) {
        addItemToShop(item, itemId);
        logger.info("새 아이템을 상점에 추가: {}", item.getName());
      }
    }
  }

  /**
   * 상점에서 아이템 제거
   */
  public void removeItemFromShop(String itemName) {
    shopItems.removeIf(item -> item.getItem().getName().equals(itemName));
    logger.info("상점에서 아이템 제거: {}", itemName);
  }

  // ==================== 한국어 변환 메서드들 ====================

  /**
   * 등급을 한국어로 변환합니다.
   */
  private String getRarityKorean(ItemRarity rarity) {
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

  // ==================== Getters ====================

  /**
   * 특정 아이템의 재고를 확인합니다.
   */
  public int getItemStock(String itemName) {
    return shopItems.stream().filter(item -> item.getItem().getName().equals(itemName)).mapToInt(ShopItem::getStock).findFirst().orElse(0);
  }

  /**
   * 상점 아이템 목록 반환 (읽기 전용)
   */
  public List<ShopItem> getShopItems() {
    return new ArrayList<>(shopItems);
  }

  /**
   * 현재 이벤트 정보 반환
   */
  public ShopEvent getCurrentEvent() {
    return currentEvent;
  }

  /**
   * 이벤트 활성화 상태 반환
   */
  public boolean isEventActive() {
    return currentEventActive;
  }
}
