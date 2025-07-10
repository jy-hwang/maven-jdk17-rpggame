package controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;
import model.GameConsumable;
import model.GameEquipment;
import model.GameInventory;
import model.GameItem;
import util.InputValidator;

/**
 * 인벤토리 및 장비 시스템을 전담하는 컨트롤러 (GameInventory 기반)
 */
public class InventoryController {
  private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

  public InventoryController() {
    logger.debug("InventoryController 초기화 완료");
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
   * 아이템 등급을 한국어로 변환합니다.
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
   * 인벤토리 관리 메뉴를 실행합니다.
   * 
   * @param player 플레이어 캐릭터
   */
  public void manageInventory(GameCharacter player) {
    while (true) {
      displayInventoryMenu(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 7);

      switch (choice) {
        case 1:
          useInventoryItem(player);
          break;
        case 2:
          manageEquipment(player);
          break;
        case 3:
          showItemInfo(player);
          break;
        case 4:
          sortInventory(player);
          break;
        case 5:
          showInventoryStatistics(player);
          break;
        case 6:
          showEquipmentComparison(player);
          break;
        case 7:
          return;
      }
    }
  }

  /**
   * 인벤토리 메뉴를 표시합니다.
   */
  private void displayInventoryMenu(GameCharacter player) {
    player.getInventory().displayInventory();

    System.out.println("\n=== 인벤토리 관리 ===");
    System.out.println("1. 🧪 아이템 사용");
    System.out.println("2. ⚔️ 장비 관리");
    System.out.println("3. 📋 아이템 정보");
    System.out.println("4. 📦 인벤토리 정렬");
    System.out.println("5. 📊 인벤토리 통계");
    System.out.println("6. 🔍 장비 비교");
    System.out.println("7. 🔙 돌아가기");

    // 상태 알림
    showInventoryAlerts(player);
  }

  /**
   * 인벤토리 상태 알림을 표시합니다.
   */
  private void showInventoryAlerts(GameCharacter player) {
    GameInventory inventory = player.getInventory();
    double usageRate = inventory.getUsageRate();

    if (usageRate >= 0.9) {
      System.out.println("⚠️ 인벤토리가 거의 가득 찼습니다! (" + String.format("%.0f%%", usageRate * 100) + ")");
    } else if (usageRate >= 0.7) {
      System.out.println("💡 인벤토리 사용률: " + String.format("%.0f%%", usageRate * 100));
    }

    // 장비 추천
    suggestEquipmentUpgrades(player);
  }

  /**
   * 장비 업그레이드를 추천합니다.
   */
  private void suggestEquipmentUpgrades(GameCharacter player) {
    GameInventory inventory = player.getInventory();
    List<GameEquipment> equippableItems = inventory.getEquippableItems();

    for (GameEquipment equipment : equippableItems) {
      GameEquipment currentEquipment = getCurrentEquipment(player, equipment.getEquipmentType());

      if (currentEquipment == null || isUpgrade(equipment, currentEquipment)) {
        System.out.println("💡 추천: " + equipment.getName() + " 착용을 고려해보세요!");
        break; // 한 번에 하나씩만 추천
      }
    }
  }

  /**
   * 장비가 업그레이드인지 확인합니다.
   */
  private boolean isUpgrade(GameEquipment newEquipment, GameEquipment currentEquipment) {
    if (currentEquipment == null)
      return true;

    // 등급 비교
    if (newEquipment.getRarity().ordinal() > currentEquipment.getRarity().ordinal()) {
      return true;
    }

    // 스탯 합계 비교
    int newTotal = newEquipment.getAttackBonus() + newEquipment.getDefenseBonus() + newEquipment.getHpBonus();
    int currentTotal = currentEquipment.getAttackBonus() + currentEquipment.getDefenseBonus() + currentEquipment.getHpBonus();

    return newTotal > currentTotal;
  }



  /**
   * 인벤토리 아이템을 사용합니다.
   */
  private void useInventoryItem(GameCharacter player) {
    List<GameConsumable> usableItems = player.getInventory().getUsableItems();

    if (usableItems.isEmpty()) {
      System.out.println("사용할 수 있는 아이템이 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 사용 가능한 아이템 ===");
    for (int i = 0; i < usableItems.size(); i++) {
      GameConsumable item = usableItems.get(i);
      int quantity = player.getInventory().getItemCount(item.getName());

      System.out.printf("%d. %s x%d%n", i + 1, item.getName(), quantity);
      System.out.printf("   📝 %s%n", item.getDescription());

      if (item.getHpRestore() > 0) {
        System.out.printf("   ❤️ 체력 회복: %d%n", item.getHpRestore());
      }

      if (item instanceof GameConsumable && item.getExpGain() > 0) {
        System.out.printf("   📈 경험치: %d%n", item.getExpGain());
      }
    }

    int itemIndex = InputValidator.getIntInput("사용할 아이템 번호 (0: 취소): ", 0, usableItems.size()) - 1;
    if (itemIndex < 0)
      return;

    GameConsumable selectedItem = usableItems.get(itemIndex);

    // 사용 전 효과 확인
    if (selectedItem.getHpRestore() > 0 && player.getHp() >= player.getTotalMaxHp()) {
      if (!InputValidator.getConfirmation("체력이 이미 가득합니다. 정말 사용하시겠습니까?")) {
        return;
      }
    }

    if (player.getInventory().useItem(selectedItem.getName(), player)) {
      System.out.println("✅ " + selectedItem.getName() + "을(를) 사용했습니다!");
      logger.debug("아이템 사용 성공: {}", selectedItem.getName());
    } else {
      System.out.println("❌ 아이템 사용에 실패했습니다.");
      logger.debug("아이템 사용 실패: {}", selectedItem.getName());
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 장비를 관리합니다.
   */
  private void manageEquipment(GameCharacter player) {
    while (true) {
      displayEquipmentMenu(player);

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

      switch (choice) {
        case 1:
          equipItem(player);
          break;
        case 2:
          unequipItem(player);
          break;
        case 3:
          displayCurrentEquipment(player);
          break;
        case 4:
          quickEquipBest(player);
          break;
        case 5:
          return;
      }
    }
  }

  /**
   * 장비 관리 메뉴를 표시합니다.
   */
  private void displayEquipmentMenu(GameCharacter player) {
    System.out.println("\n=== 장비 관리 ===");
    System.out.println("1. ⚔️ 장비 착용");
    System.out.println("2. 📤 장비 해제");
    System.out.println("3. 👁️ 현재 장비 보기");
    System.out.println("4. ⚡ 최적 장비 자동 착용");
    System.out.println("5. 🔙 돌아가기");
  }

  /**
   * 장비를 착용합니다.
   */
  private void equipItem(GameCharacter player) {
    List<GameEquipment> equipments = player.getInventory().getEquippableItems();

    if (equipments.isEmpty()) {
      System.out.println("착용할 수 있는 장비가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 착용 가능한 장비 ===");
    for (int i = 0; i < equipments.size(); i++) {
      GameEquipment equipment = equipments.get(i);
      System.out.printf("%d. %s [%s]%n", i + 1, equipment.getName(), getEquipmentTypeKorean(equipment.getEquipmentType()));

      // 현재 착용 중인 장비와 비교
      GameEquipment currentEquipment = getCurrentEquipment(player, equipment.getEquipmentType());
      displayEquipmentComparison(equipment, currentEquipment);
    }

    int equipIndex = InputValidator.getIntInput("착용할 장비 번호 (0: 취소): ", 0, equipments.size()) - 1;
    if (equipIndex < 0)
      return;

    GameEquipment newEquipment = equipments.get(equipIndex);
    GameEquipment oldEquipment = player.getInventory().equipItem(newEquipment);

    System.out.println("✅ " + newEquipment.getName() + "을(를) 착용했습니다!");
    if (oldEquipment != null) {
      System.out.println("기존 " + oldEquipment.getName() + "은(는) 인벤토리로 이동했습니다.");
    }

    // 스탯 변화 표시
    displayStatChanges(player, newEquipment, oldEquipment);

    logger.info("장비 착용: {} -> {}", player.getName(), newEquipment.getName());
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 장비 비교를 표시합니다.
   */
  private void displayEquipmentComparison(GameEquipment newEquipment, GameEquipment currentEquipment) {
    System.out.printf("   📊 효과: %s%n", getEquipmentEffectDescription(newEquipment));

    if (currentEquipment != null) {
      System.out.printf("   🔄 현재: %s%n", getEquipmentEffectDescription(currentEquipment));

      // 변화량 표시
      int attackChange = newEquipment.getAttackBonus() - currentEquipment.getAttackBonus();
      int defenseChange = newEquipment.getDefenseBonus() - currentEquipment.getDefenseBonus();
      int hpChange = newEquipment.getHpBonus() - currentEquipment.getHpBonus();

      if (attackChange != 0 || defenseChange != 0 || hpChange != 0) {
        System.out.print("   📈 변화: ");
        List<String> changes = new java.util.ArrayList<>();
        if (attackChange != 0)
          changes.add("공격" + (attackChange > 0 ? "+" : "") + attackChange);
        if (defenseChange != 0)
          changes.add("방어" + (defenseChange > 0 ? "+" : "") + defenseChange);
        if (hpChange != 0)
          changes.add("HP" + (hpChange > 0 ? "+" : "") + hpChange);
        System.out.println(String.join(", ", changes));
      }
    } else {
      System.out.println("   ✨ 새로운 장비!");
    }
    System.out.println();
  }

  /**
   * 스탯 변화를 표시합니다.
   */
  private void displayStatChanges(GameCharacter player, GameEquipment newEquipment, GameEquipment oldEquipment) {
    System.out.println("\n📊 스탯 변화:");

    if (oldEquipment != null) {
      int attackChange = newEquipment.getAttackBonus() - oldEquipment.getAttackBonus();
      int defenseChange = newEquipment.getDefenseBonus() - oldEquipment.getDefenseBonus();
      int hpChange = newEquipment.getHpBonus() - oldEquipment.getHpBonus();

      if (attackChange > 0)
        System.out.println("⚔️ 공격력이 " + attackChange + " 증가했습니다!");
      else if (attackChange < 0)
        System.out.println("⚔️ 공격력이 " + (-attackChange) + " 감소했습니다.");

      if (defenseChange > 0)
        System.out.println("🛡️ 방어력이 " + defenseChange + " 증가했습니다!");
      else if (defenseChange < 0)
        System.out.println("🛡️ 방어력이 " + (-defenseChange) + " 감소했습니다.");

      if (hpChange > 0)
        System.out.println("❤️ 최대 체력이 " + hpChange + " 증가했습니다!");
      else if (hpChange < 0)
        System.out.println("❤️ 최대 체력이 " + (-hpChange) + " 감소했습니다.");
    } else {
      System.out.println("⚔️ 공격력 +" + newEquipment.getAttackBonus());
      System.out.println("🛡️ 방어력 +" + newEquipment.getDefenseBonus());
      System.out.println("❤️ 최대 체력 +" + newEquipment.getHpBonus());
    }
  }

  /**
   * 최적 장비를 자동으로 착용합니다.
   */
  private void quickEquipBest(GameCharacter player) {
    System.out.println("🔍 최적의 장비를 찾는 중...");

    List<GameEquipment> equipments = player.getInventory().getEquippableItems();
    boolean anyEquipped = false;

    for (GameEquipment.EquipmentType type : GameEquipment.EquipmentType.values()) {
      GameEquipment currentEquipment = getCurrentEquipment(player, type);
      GameEquipment bestEquipment = findBestEquipment(equipments, type);

      if (bestEquipment != null && (currentEquipment == null || isUpgrade(bestEquipment, currentEquipment))) {
        player.getInventory().equipItem(bestEquipment);
        System.out.println("✅ " + bestEquipment.getName() + " 착용!");
        anyEquipped = true;
      }
    }

    if (!anyEquipped) {
      System.out.println("💡 현재 장비가 이미 최적입니다!");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 특정 타입의 최적 장비를 찾습니다.
   */
  private GameEquipment findBestEquipment(List<GameEquipment> equipments, GameEquipment.EquipmentType type) {
    return equipments.stream().filter(equipment -> equipment.getEquipmentType() == type).max((a, b) -> {
      // 등급 우선, 그 다음 스탯 합계
      int rarityCompare = Integer.compare(a.getRarity().ordinal(), b.getRarity().ordinal());
      if (rarityCompare != 0)
        return rarityCompare;

      int aTotal = a.getAttackBonus() + a.getDefenseBonus() + a.getHpBonus();
      int bTotal = b.getAttackBonus() + b.getDefenseBonus() + b.getHpBonus();
      return Integer.compare(aTotal, bTotal);
    }).orElse(null);
  }

  /**
   * 장비를 해제합니다.
   */
  private void unequipItem(GameCharacter player) {
    System.out.println("\n=== 장비 해제 ===");
    System.out.println("1. ⚔️ 무기 해제");
    System.out.println("2. 🛡️ 방어구 해제");
    System.out.println("3. 💍 장신구 해제");
    System.out.println("4. 🔄 모든 장비 해제");
    System.out.println("5. 🔙 취소");

    int choice = InputValidator.getIntInput("선택: ", 1, 5);
    if (choice == 5)
      return;

    if (choice == 4) {
      // 모든 장비 해제
      if (InputValidator.getConfirmation("정말로 모든 장비를 해제하시겠습니까?")) {
        unequipAllItems(player);
      }
      return;
    }

    GameEquipment.EquipmentType[] types = {GameEquipment.EquipmentType.WEAPON, GameEquipment.EquipmentType.ARMOR, GameEquipment.EquipmentType.ACCESSORY};

    String[] typeNames = {"무기", "방어구", "장신구"};

    GameEquipment.EquipmentType targetType = types[choice - 1];
    GameEquipment currentEquipment = getCurrentEquipment(player, targetType);

    if (currentEquipment == null) {
      System.out.println("해제할 " + typeNames[choice - 1] + "가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("현재 착용 중: " + currentEquipment.getName());
    if (!InputValidator.getConfirmation("정말로 해제하시겠습니까?")) {
      return;
    }

    if (player.getInventory().getFreeSlots() == 0) {
      System.out.println("❌ 인벤토리가 가득 차서 장비를 해제할 수 없습니다!");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    GameEquipment unequipped = player.getInventory().unequipItem(targetType);
    if (unequipped != null) {
      System.out.println("✅ " + unequipped.getName() + "을(를) 해제했습니다!");
      logger.info("장비 해제: {} -> {}", player.getName(), unequipped.getName());
    } else {
      System.out.println("❌ 장비 해제에 실패했습니다.");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 모든 장비를 해제합니다.
   */
  private void unequipAllItems(GameCharacter player) {
    int requiredSlots = 0;
    if (player.getInventory().getEquippedWeapon() != null)
      requiredSlots++;
    if (player.getInventory().getEquippedArmor() != null)
      requiredSlots++;
    if (player.getInventory().getEquippedAccessory() != null)
      requiredSlots++;

    if (player.getInventory().getFreeSlots() < requiredSlots) {
      System.out.println("❌ 인벤토리 공간이 부족합니다! (필요: " + requiredSlots + "슬롯)");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    int unequippedCount = 0;

    for (GameEquipment.EquipmentType type : GameEquipment.EquipmentType.values()) {
      GameEquipment equipment = player.getInventory().unequipItem(type);
      if (equipment != null) {
        System.out.println("✅ " + equipment.getName() + " 해제");
        unequippedCount++;
      }
    }

    System.out.println("🎯 총 " + unequippedCount + "개의 장비를 해제했습니다!");
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 현재 착용 중인 장비를 표시합니다.
   */
  private void displayCurrentEquipment(GameCharacter player) {
    System.out.println("\n=== 현재 착용 장비 상세 정보 ===");

    GameInventory inventory = player.getInventory();

    displayEquipmentSlot("⚔️ 무기", inventory.getEquippedWeapon());
    displayEquipmentSlot("🛡️ 방어구", inventory.getEquippedArmor());
    displayEquipmentSlot("💍 장신구", inventory.getEquippedAccessory());

    // 총 장비 보너스 표시
    GameInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    System.out.println("\n📊 총 장비 보너스:");
    if (bonus.getAttackBonus() > 0 || bonus.getDefenseBonus() > 0 || bonus.getHpBonus() > 0) {
      if (bonus.getAttackBonus() > 0)
        System.out.println("⚔️ 공격력: +" + bonus.getAttackBonus());
      if (bonus.getDefenseBonus() > 0)
        System.out.println("🛡️ 방어력: +" + bonus.getDefenseBonus());
      if (bonus.getHpBonus() > 0)
        System.out.println("❤️ 체력: +" + bonus.getHpBonus());
    } else {
      System.out.println("없음");
    }

    System.out.println("==================");
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 개별 장비 슬롯 정보를 표시합니다.
   */
  private void displayEquipmentSlot(String slotName, GameEquipment equipment) {
    System.out.println(slotName + ":");
    if (equipment != null) {
      System.out.println("  📦 " + equipment.getName());
      System.out.println("  ⭐ 등급: " + getRarityKorean(equipment.getRarity()));
      System.out.println("  📝 " + equipment.getDescription());
      System.out.println("  🔥 효과: " + getEquipmentEffectDescription(equipment));
      System.out.println("  💰 가치: " + equipment.getValue() + " 골드");
    } else {
      System.out.println("  📭 착용 중인 장비 없음");
    }
    System.out.println();
  }

  /**
   * 인벤토리를 정렬합니다.
   */
  private void sortInventory(GameCharacter player) {
    System.out.println("📦 인벤토리를 정렬하는 중...");

    player.getInventory().sortInventory();

    System.out.println("✅ 인벤토리가 정렬되었습니다!");
    System.out.println("💡 아이템이 타입별, 등급별로 정리되었습니다.");

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 인벤토리 통계를 표시합니다.
   */
  private void showInventoryStatistics(GameCharacter player) {
    GameInventory inventory = player.getInventory();

    System.out.println("\n=== 인벤토리 통계 ===");
    System.out.printf("📦 용량: %d/%d (%.1f%% 사용중)%n", inventory.getCurrentSize(), inventory.getMaxSize(), inventory.getUsageRate() * 100);
    System.out.printf("🆓 여유 공간: %d슬롯%n", inventory.getFreeSlots());

    // 아이템 타입별 개수
    var consumables = inventory.getItemsByType(GameConsumable.class);
    var equipments = inventory.getItemsByType(GameEquipment.class);

    System.out.println("\n📊 아이템 분류:");
    System.out.printf("🧪 소비 아이템: %d개%n", consumables.size());
    System.out.printf("⚔️ 장비: %d개%n", equipments.size());

    // 등급별 통계
    displayRarityStatistics(inventory);

    // 가치 통계
    displayValueStatistics(inventory);

    System.out.println("==================");
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 등급별 통계를 표시합니다.
   */
  private void displayRarityStatistics(GameInventory inventory) {
    System.out.println("\n⭐ 등급별 분포:");

    var rarityCount = new java.util.HashMap<GameItem.ItemRarity, Integer>();
    for (var stack : inventory.getItems()) {
      GameItem.ItemRarity rarity = stack.getItem().getRarity();
      rarityCount.put(rarity, rarityCount.getOrDefault(rarity, 0) + 1);
    }

    for (GameItem.ItemRarity rarity : GameItem.ItemRarity.values()) {
      int count = rarityCount.getOrDefault(rarity, 0);
      if (count > 0) {
        System.out.printf("  %s: %d개%n", getRarityKorean(rarity), count);
      }
    }
  }

  /**
   * 가치 통계를 표시합니다.
   */
  private void displayValueStatistics(GameInventory inventory) {
    int totalValue = 0;
    int mostValuableItemValue = 0;
    String mostValuableItemName = "";

    for (var stack : inventory.getItems()) {
      GameItem item = stack.getItem();
      int itemValue = item.getValue() * stack.getQuantity();
      totalValue += itemValue;

      if (item.getValue() > mostValuableItemValue) {
        mostValuableItemValue = item.getValue();
        mostValuableItemName = item.getName();
      }
    }

    System.out.println("\n💰 가치 통계:");
    System.out.printf("💎 총 가치: %d골드%n", totalValue);
    if (!mostValuableItemName.isEmpty()) {
      System.out.printf("🏆 최고가 아이템: %s (%d골드)%n", mostValuableItemName, mostValuableItemValue);
    }
  }

  /**
   * 장비 비교 도구를 표시합니다.
   */
  private void showEquipmentComparison(GameCharacter player) {
    System.out.println("\n=== 장비 비교 도구 ===");
    System.out.println("1. 📊 착용 장비 vs 인벤토리 장비");
    System.out.println("2. 🔍 인벤토리 장비 간 비교");
    System.out.println("3. 🔙 돌아가기");

    int choice = InputValidator.getIntInput("선택: ", 1, 3);

    switch (choice) {
      case 1:
        compareEquippedVsInventory(player);
        break;
      case 2:
        compareInventoryEquipments(player);
        break;
      case 3:
        return;
    }
  }

  /**
   * 착용 장비와 인벤토리 장비를 비교합니다.
   */
  private void compareEquippedVsInventory(GameCharacter player) {
    List<GameEquipment> equipments = player.getInventory().getEquippableItems();

    if (equipments.isEmpty()) {
      System.out.println("비교할 장비가 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 장비 성능 비교 ===");

    for (GameEquipment.EquipmentType type : GameEquipment.EquipmentType.values()) {
      GameEquipment currentEquipment = getCurrentEquipment(player, type);
      List<GameEquipment> sameTypeEquipments = equipments.stream().filter(eq -> eq.getEquipmentType() == type).toList();

      if (!sameTypeEquipments.isEmpty()) {
        System.out.println("\n" + getEquipmentTypeKorean(type) + " 비교:");

        if (currentEquipment != null) {
          System.out.println("🟢 현재 착용: " + formatEquipmentForComparison(currentEquipment));
        } else {
          System.out.println("🟢 현재 착용: 없음");
        }

        System.out.println("📦 인벤토리:");
        for (GameEquipment equipment : sameTypeEquipments) {
          String comparison = "";
          if (currentEquipment != null) {
            if (isUpgrade(equipment, currentEquipment)) {
              comparison = " ⬆️ 업그레이드";
            } else {
              comparison = " ⬇️ 다운그레이드";
            }
          } else {
            comparison = " ✨ 새 장비";
          }

          System.out.println("  " + formatEquipmentForComparison(equipment) + comparison);
        }
      }
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 비교용 장비 포맷을 생성합니다.
   */
  private String formatEquipmentForComparison(GameEquipment equipment) {
    return String.format("%s [%s] (공격+%d, 방어+%d, HP+%d)", equipment.getName(), getRarityKorean(equipment.getRarity()), equipment.getAttackBonus(), equipment.getDefenseBonus(), equipment.getHpBonus());
  }

  /**
   * 인벤토리 장비 간 비교를 수행합니다.
   */
  private void compareInventoryEquipments(GameCharacter player) {
    List<GameEquipment> equipments = player.getInventory().getEquippableItems();

    if (equipments.size() < 2) {
      System.out.println("비교할 장비가 부족합니다. (최소 2개 필요)");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 첫 번째 장비 선택 ===");
    for (int i = 0; i < equipments.size(); i++) {
      GameEquipment equipment = equipments.get(i);
      System.out.printf("%d. %s%n", i + 1, formatEquipmentForComparison(equipment));
    }

    int firstIndex = InputValidator.getIntInput("첫 번째 장비 번호 (0: 취소): ", 0, equipments.size()) - 1;
    if (firstIndex < 0)
      return;

    System.out.println("\n=== 두 번째 장비 선택 ===");
    for (int i = 0; i < equipments.size(); i++) {
      if (i == firstIndex)
        continue;
      GameEquipment equipment = equipments.get(i);
      System.out.printf("%d. %s%n", i + 1, formatEquipmentForComparison(equipment));
    }

    int secondIndex = InputValidator.getIntInput("두 번째 장비 번호 (0: 취소): ", 0, equipments.size()) - 1;
    if (secondIndex < 0 || secondIndex == firstIndex)
      return;

    // 상세 비교 표시
    GameEquipment first = equipments.get(firstIndex);
    GameEquipment second = equipments.get(secondIndex);

    System.out.println("\n=== 상세 비교 결과 ===");
    System.out.println("🥇 " + first.getName() + " vs 🥈 " + second.getName());
    System.out.println();

    compareEquipmentStats("⚔️ 공격력", first.getAttackBonus(), second.getAttackBonus());
    compareEquipmentStats("🛡️ 방어력", first.getDefenseBonus(), second.getDefenseBonus());
    compareEquipmentStats("❤️ 체력", first.getHpBonus(), second.getHpBonus());
    compareEquipmentStats("💰 가치", first.getValue(), second.getValue());

    System.out.printf("⭐ 등급: %s vs %s%n", getRarityKorean(first.getRarity()), getRarityKorean(second.getRarity()));

    // 총합 비교
    int firstTotal = first.getAttackBonus() + first.getDefenseBonus() + first.getHpBonus();
    int secondTotal = second.getAttackBonus() + second.getDefenseBonus() + second.getHpBonus();

    System.out.println("\n🏆 종합 평가:");
    if (firstTotal > secondTotal) {
      System.out.println("🥇 " + first.getName() + "이(가) 더 우수합니다!");
    } else if (secondTotal > firstTotal) {
      System.out.println("🥈 " + second.getName() + "이(가) 더 우수합니다!");
    } else {
      System.out.println("🤝 두 장비의 성능이 동일합니다!");
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 장비 스탯을 비교합니다.
   */
  private void compareEquipmentStats(String statName, int first, int second) {
    if (first > second) {
      System.out.printf("%s: %d > %d (+%d)%n", statName, first, second, first - second);
    } else if (second > first) {
      System.out.printf("%s: %d < %d (-%d)%n", statName, first, second, second - first);
    } else {
      System.out.printf("%s: %d = %d%n", statName, first, second);
    }
  }

  /**
   * 아이템 정보를 표시합니다.
   */
  private void showItemInfo(GameCharacter player) {
    var items = player.getInventory().getItems();
    if (items.isEmpty()) {
      System.out.println("인벤토리가 비어있습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    System.out.println("\n=== 아이템 목록 ===");
    for (int i = 0; i < items.size(); i++) {
      var stack = items.get(i);
      System.out.printf("%d. %s", i + 1, stack.getItem().getName());
      if (stack.getQuantity() > 1) {
        System.out.printf(" x%d", stack.getQuantity());
      }
      System.out.printf(" [%s]%n", getRarityKorean(stack.getItem().getRarity()));
    }

    int itemIndex = InputValidator.getIntInput("정보를 볼 아이템 번호 (0: 취소): ", 0, items.size()) - 1;
    if (itemIndex < 0)
      return;

    var selectedStack = items.get(itemIndex);
    displayDetailedItemInfo(player, selectedStack);
  }

  /**
   * 아이템의 상세 정보를 표시합니다.
   */
  private void displayDetailedItemInfo(GameCharacter player, GameInventory.ItemStack stack) {
    GameItem item = stack.getItem();

    System.out.println("\n" + "=".repeat(50));
    System.out.println("📦 아이템 상세 정보");
    System.out.println("=".repeat(50));

    System.out.println("📛 이름: " + item.getName());
    System.out.println("📝 설명: " + item.getDescription());
    System.out.println("⭐ 등급: " + getRarityKorean(item.getRarity()));
    System.out.println("💰 가격: " + item.getValue() + " 골드");
    System.out.println("📊 수량: " + stack.getQuantity() + "개");

    if (item instanceof GameEquipment equipment) {
      System.out.println("🏷️ 타입: " + getEquipmentTypeKorean(equipment.getEquipmentType()));
      System.out.println("🔥 효과: " + getEquipmentEffectDescription(equipment));

      // 현재 착용 중인 같은 타입 장비와 비교
      GameEquipment currentEquipment = getCurrentEquipment(player, equipment.getEquipmentType());
      if (currentEquipment != null && !currentEquipment.getName().equals(equipment.getName())) {
        System.out.println("\n🔄 현재 착용 중인 " + getEquipmentTypeKorean(equipment.getEquipmentType()) + ":");
        System.out.println("   " + currentEquipment.getName() + " - " + getEquipmentEffectDescription(currentEquipment));

        if (isUpgrade(equipment, currentEquipment)) {
          System.out.println("✅ 이 장비가 더 우수합니다!");
        } else {
          System.out.println("⚠️ 현재 장비가 더 우수합니다.");
        }
      }

    } else if (item instanceof GameConsumable consumable) {
      System.out.println("🧪 타입: 소비 아이템");
      if (consumable.getHpRestore() > 0) {
        System.out.println("❤️ 체력 회복: " + consumable.getHpRestore());
      }
      if (consumable.getExpGain() > 0) {
        System.out.println("📈 경험치: " + consumable.getExpGain());
      }
      System.out.println("🔄 스택 가능: " + (consumable.isStackable() ? "예" : "아니오"));
    }

    System.out.println("=".repeat(50));
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 현재 착용 중인 특정 타입의 장비를 반환합니다.
   */
  private GameEquipment getCurrentEquipment(GameCharacter player, GameEquipment.EquipmentType type) {
    return switch (type) {
      case WEAPON -> player.getInventory().getEquippedWeapon();
      case ARMOR -> player.getInventory().getEquippedArmor();
      case ACCESSORY -> player.getInventory().getEquippedAccessory();
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
   * 빠른 아이템 사용 (전투 중 등에서 호출)
   * 
   * @param player 플레이어 캐릭터
   * @param itemName 사용할 아이템 이름
   * @return 사용 성공 여부
   */
  public boolean quickUseItem(GameCharacter player, String itemName) {
    boolean success = player.getInventory().useItem(itemName, player);
    if (success) {
      logger.debug("빠른 아이템 사용 성공: {}", itemName);
    } else {
      logger.debug("빠른 아이템 사용 실패: {}", itemName);
    }
    return success;
  }


  /**
   * 아이템 추가 (드롭, 구매 등에서 호출)
   * 
   * @param player 플레이어 캐릭터
   * @param item 추가할 아이템
   * @param quantity 수량
   * @return 추가 성공 여부
   */
  public boolean addItem(GameCharacter player, GameItem item, int quantity) {
      boolean success = player.getInventory().addItem(item, quantity);
      if (success) {
          logger.debug("아이템 추가 성공: {} x{}", item.getName(), quantity);
      } else {
          logger.debug("아이템 추가 실패 (인벤토리 가득참): {} x{}", item.getName(), quantity);
      }
      return success;
  }
  
  /**
   * 아이템 제거 (판매, 사용 등에서 호출)
   * 
   * @param player 플레이어 캐릭터
   * @param itemName 제거할 아이템 이름
   * @param quantity 제거할 수량
   * @return 제거 성공 여부
   */
  public boolean removeItem(GameCharacter player, String itemName, int quantity) {
      boolean success = player.getInventory().removeItem(itemName, quantity);
      if (success) {
          logger.debug("아이템 제거 성공: {} x{}", itemName, quantity);
      } else {
          logger.debug("아이템 제거 실패 (아이템 없음): {} x{}", itemName, quantity);
      }
      return success;
  }
  
  /**
   * 특정 아이템이 인벤토리에 있는지 확인
   * 
   * @param player 플레이어 캐릭터
   * @param itemName 확인할 아이템 이름
   * @return 보유 여부
   */
  public boolean hasItem(GameCharacter player, String itemName) {
      return player.getInventory().getItemCount(itemName) > 0;
  }
  
  /**
   * 특정 수량만큼 아이템이 있는지 확인
   * 
   * @param player 플레이어 캐릭터
   * @param itemName 확인할 아이템 이름
   * @param requiredQuantity 필요한 수량
   * @return 충분한 수량 보유 여부
   */
  public boolean hasEnoughItems(GameCharacter player, String itemName, int requiredQuantity) {
      return player.getInventory().getItemCount(itemName) >= requiredQuantity;
  }
  
  /**
   * 여러 아이템을 한 번에 추가
   * 
   * @param player 플레이어 캐릭터
   * @param itemsToAdd 추가할 아이템과 수량의 맵
   * @return 모든 아이템 추가 성공 여부
   */
  public boolean addMultipleItems(GameCharacter player, java.util.Map<GameItem, Integer> itemsToAdd) {
      // 먼저 공간이 충분한지 확인
      int requiredSlots = 0;
      for (var entry : itemsToAdd.entrySet()) {
          GameItem item = entry.getKey();
          // 스택 가능한 아이템이 아니라면 각각 슬롯이 필요
          if (!(item instanceof GameConsumable && ((GameConsumable) item).isStackable()) ||
              !hasItem(player, item.getName())) {
              requiredSlots++;
          }
      }
      
      if (player.getInventory().getFreeSlots() < requiredSlots) {
          logger.warn("인벤토리 공간 부족: 필요 {}, 여유 {}", requiredSlots, player.getInventory().getFreeSlots());
          return false;
      }
      
      // 모든 아이템 추가
      boolean allSuccess = true;
      for (var entry : itemsToAdd.entrySet()) {
          GameItem item = entry.getKey();
          int quantity = entry.getValue();
          
          if (!addItem(player, item, quantity)) {
              allSuccess = false;
              logger.warn("다중 아이템 추가 중 실패: {} x{}", item.getName(), quantity);
          }
      }
      
      return allSuccess;
  }
  
  /**
   * 인벤토리 공간 확인
   * 
   * @param player 플레이어 캐릭터
   * @param requiredSlots 필요한 슬롯 수
   * @return 충분한 공간 여부
   */
  public boolean hasEnoughSpace(GameCharacter player, int requiredSlots) {
      return player.getInventory().getFreeSlots() >= requiredSlots;
  }
  
  /**
   * 인벤토리 요약 정보 반환
   * 
   * @param player 플레이어 캐릭터
   * @return 인벤토리 요약 문자열
   */
  public String getInventorySummary(GameCharacter player) {
      GameInventory inventory = player.getInventory();
      return String.format("인벤토리: %d/%d (%.0f%%) | 장비: %s | 총 가치: %d골드", 
                         inventory.getCurrentSize(), 
                         inventory.getMaxSize(), 
                         inventory.getUsageRate() * 100,
                         getEquipmentSummary(player),
                         calculateInventoryValue(player));
  }
  
  /**
   * 인벤토리 총 가치를 계산합니다.
   */
  private int calculateInventoryValue(GameCharacter player) {
      return player.getInventory().getItems().stream()
              .mapToInt(stack -> stack.getItem().getValue() * stack.getQuantity())
              .sum();
  }
  
  /**
   * 인벤토리 상태 확인
   * 
   * @param player 플레이어 캐릭터
   * @return 인벤토리 사용률 (0.0 ~ 1.0)
   */
  public double getInventoryUsageRate(GameCharacter player) {
      return player.getInventory().getUsageRate();
  }
  
  /**
   * 특정 아이템의 보유 수량 확인
   * 
   * @param player 플레이어 캐릭터
   * @param itemName 아이템 이름
   * @return 보유 수량
   */
  public int getItemQuantity(GameCharacter player, String itemName) {
      return player.getInventory().getItemCount(itemName);
  }
  
  /**
   * 인벤토리 상태 요약을 반환합니다.
   */
  public String getInventoryStatusSummary(GameCharacter player) {
      GameInventory inventory = player.getInventory();
      return String.format("인벤토리: %d/%d (%.0f%%)", 
                         inventory.getCurrentSize(), 
                         inventory.getMaxSize(), 
                         inventory.getUsageRate() * 100);
  }
  
  /**
   * 착용 장비 요약을 반환합니다.
   */
  public String getEquipmentSummary(GameCharacter player) {
      GameInventory inventory = player.getInventory();
      int equippedCount = 0;
      
      if (inventory.getEquippedWeapon() != null) equippedCount++;
      if (inventory.getEquippedArmor() != null) equippedCount++;
      if (inventory.getEquippedAccessory() != null) equippedCount++;
      
      return String.format("착용 장비: %d/3개", equippedCount);
  }
}