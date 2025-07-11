package model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.item.GameConsumable;
import model.item.GameEquipment;
import model.item.GameItem;

/**
 * 캐릭터의 인벤토리를 관리하는 클래스 (GameInventory 기반)
 */
public class GameInventory {
  private static final Logger logger = LoggerFactory.getLogger(GameInventory.class);

  private static final int DEFAULT_MAX_SIZE = 20;

  private List<ItemStack> items;
  private GameEquipment equippedWeapon;
  private GameEquipment equippedArmor;
  private GameEquipment equippedAccessory;
  private int maxSize;

  public GameInventory() {
    this(DEFAULT_MAX_SIZE);
  }

  @JsonCreator
  public GameInventory(@JsonProperty("maxSize") int maxSize) {
    this.maxSize = maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE;
    this.items = new ArrayList<>();
    logger.debug("인벤토리 생성 (최대 크기: {})", this.maxSize);
  }

  /**
   * 아이템을 인벤토리에 추가합니다.
   * 
   * @param item 추가할 아이템
   * @param quantity 수량
   * @return 성공 시 true
   */
  public boolean addItem(GameItem item, int quantity) {
    if (item == null || quantity <= 0) {
      logger.warn("유효하지 않은 아이템 추가 시도: item={}, quantity={}", item, quantity);
      return false;
    }

    // 소비품이고 스택 가능한 경우 기존 스택에 추가
    if (item instanceof GameConsumable && ((GameConsumable) item).isStackable()) {
      for (ItemStack stack : items) {
        if (stack.getItem().getName().equals(item.getName())) {
          stack.addQuantity(quantity);
          logger.debug("기존 스택에 아이템 추가: {} x{}", item.getName(), quantity);
          return true;
        }
      }
    }

    // 새 스택 생성
    if (items.size() >= maxSize) {
      logger.warn("인벤토리 용량 초과: 현재 {}/{}", items.size(), maxSize);
      return false;
    }

    items.add(new ItemStack(item, quantity));
    logger.debug("새 아이템 스택 생성: {} x{}", item.getName(), quantity);
    return true;
  }

  /**
   * 아이템을 인벤토리에서 제거합니다.
   * 
   * @param itemName 제거할 아이템 이름
   * @param quantity 제거할 수량
   * @return 성공 시 true
   */
  public boolean removeItem(String itemName, int quantity) {
    for (Iterator<ItemStack> iterator = items.iterator(); iterator.hasNext();) {
      ItemStack stack = iterator.next();
      if (stack.getItem().getName().equals(itemName)) {
        if (stack.getQuantity() <= quantity) {
          iterator.remove();
          logger.debug("아이템 스택 완전 제거: {}", itemName);
        } else {
          stack.removeQuantity(quantity);
          logger.debug("아이템 부분 제거: {} x{}", itemName, quantity);
        }
        return true;
      }
    }

    logger.warn("제거할 아이템을 찾을 수 없음: {}", itemName);
    return false;
  }

  /**
   * 장비를 착용합니다.
   * 
   * @param equipment 착용할 장비
   * @return 기존에 착용하고 있던 장비 (없으면 null)
   */
  public GameEquipment equipItem(GameEquipment equipment) {
    if (equipment == null)
      return null;

    GameEquipment oldEquipment = null;

    switch (equipment.getEquipmentType()) {
      case WEAPON:
        oldEquipment = equippedWeapon;
        equippedWeapon = equipment;
        break;
      case ARMOR:
        oldEquipment = equippedArmor;
        equippedArmor = equipment;
        break;
      case ACCESSORY:
        oldEquipment = equippedAccessory;
        equippedAccessory = equipment;
        break;
    }

    // 인벤토리에서 장비 제거
    removeItem(equipment.getName(), 1);

    // 기존 장비가 있으면 인벤토리에 추가
    if (oldEquipment != null) {
      addItem(oldEquipment, 1);
    }

    logger.info("장비 착용: {} (종류: {})", equipment.getName(), equipment.getEquipmentType());
    return oldEquipment;
  }

  /**
   * 장비를 해제합니다.
   * 
   * @param equipmentType 해제할 장비 타입
   * @return 해제된 장비 (없으면 null)
   */
  public GameEquipment unequipItem(GameEquipment.EquipmentType equipmentType) {
    GameEquipment equipment = null;

    switch (equipmentType) {
      case WEAPON:
        equipment = equippedWeapon;
        equippedWeapon = null;
        break;
      case ARMOR:
        equipment = equippedArmor;
        equippedArmor = null;
        break;
      case ACCESSORY:
        equipment = equippedAccessory;
        equippedAccessory = null;
        break;
    }

    if (equipment != null) {
      if (!addItem(equipment, 1)) {
        // 인벤토리가 가득 찬 경우 다시 착용
        switch (equipmentType) {
          case WEAPON:
            equippedWeapon = equipment;
            break;
          case ARMOR:
            equippedArmor = equipment;
            break;
          case ACCESSORY:
            equippedAccessory = equipment;
            break;
        }
        logger.warn("인벤토리가 가득 차서 장비 해제 실패: {}", equipment.getName());
        return null;
      }
      logger.info("장비 해제: {} (종류: {})", equipment.getName(), equipmentType);
    }

    return equipment;
  }

  /**
   * 아이템을 사용합니다.
   * 
   * @param itemName 사용할 아이템 이름
   * @param character 아이템을 사용할 캐릭터
   * @return 성공 시 true
   */
  public boolean useItem(String itemName, GameCharacter character) {
    for (ItemStack stack : items) {
      if (stack.getItem().getName().equals(itemName)) {
        if (stack.getItem().use(character)) {
          removeItem(itemName, 1);
          logger.debug("아이템 사용: {}", itemName);
          return true;
        }
        return false;
      }
    }

    logger.warn("사용할 아이템을 찾을 수 없음: {}", itemName);
    return false;
  }

  /**
   * 인벤토리 내용을 표시합니다.
   */
  public void displayInventory() {
    System.out.println("\n=== 인벤토리 ===");
    System.out.printf("용량: %d/%d (%.1f%% 사용중)%n", items.size(), maxSize, (double) items.size() / maxSize * 100);

    if (items.isEmpty()) {
      System.out.println("📦 인벤토리가 비어있습니다.");
    } else {
      // 아이템을 카테고리별로 분류하여 표시
      displayItemsByCategory();
    }

    System.out.println("\n=== 착용 중인 장비 ===");
    displayEquippedItems();

    System.out.println("\n=== 장비 효과 ===");
    displayEquipmentBonuses();

    System.out.println("==================");
  }

  /**
   * 카테고리별로 아이템을 표시합니다.
   */
  private void displayItemsByCategory() {
    Map<String, List<ItemStack>> categories = categorizeItems();

    for (Map.Entry<String, List<ItemStack>> entry : categories.entrySet()) {
      String category = entry.getKey();
      List<ItemStack> categoryItems = entry.getValue();

      if (!categoryItems.isEmpty()) {
        System.out.println("\n" + getCategoryIcon(category) + " " + category + ":");

        for (int i = 0; i < categoryItems.size(); i++) {
          ItemStack stack = categoryItems.get(i);
          GameItem item = stack.getItem();

          System.out.printf("  %d. %s", getGlobalItemIndex(stack) + 1, formatItemDisplay(stack));

          // 아이템 등급 표시
          System.out.printf(" [%s]", getRarityDisplay(item.getRarity()));

          if (stack.getQuantity() > 1) {
            System.out.printf(" x%d", stack.getQuantity());
          }

          System.out.println();
        }
      }
    }
  }

  /**
   * 아이템을 카테고리별로 분류합니다.
   */
  private Map<String, List<ItemStack>> categorizeItems() {
    Map<String, List<ItemStack>> categories = new LinkedHashMap<>();
    categories.put("소비 아이템", new ArrayList<>());
    categories.put("무기", new ArrayList<>());
    categories.put("방어구", new ArrayList<>());
    categories.put("장신구", new ArrayList<>());
    categories.put("기타", new ArrayList<>());

    for (ItemStack stack : items) {
      GameItem item = stack.getItem();
      String category = "기타";

      if (item instanceof GameConsumable) {
        category = "소비 아이템";
      } else if (item instanceof GameEquipment equipment) {
        category = switch (equipment.getEquipmentType()) {
          case WEAPON -> "무기";
          case ARMOR -> "방어구";
          case ACCESSORY -> "장신구";
        };
      }

      categories.get(category).add(stack);
    }

    return categories;
  }

  /**
   * 카테고리 아이콘을 반환합니다.
   */
  private String getCategoryIcon(String category) {
    return switch (category) {
      case "소비 아이템" -> "🧪";
      case "무기" -> "⚔️";
      case "방어구" -> "🛡️";
      case "장신구" -> "💍";
      default -> "📦";
    };
  }

  /**
   * 아이템의 전체 인덱스를 반환합니다.
   */
  private int getGlobalItemIndex(ItemStack targetStack) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) == targetStack) {
        return i;
      }
    }
    return -1;
  }

  /**
   * 아이템 표시 형식을 지정합니다.
   */
  private String formatItemDisplay(ItemStack stack) {
    GameItem item = stack.getItem();
    StringBuilder display = new StringBuilder(item.getName());

    if (item instanceof GameEquipment equipment) {
      // 장비 효과 간략 표시
      List<String> effects = new ArrayList<>();
      if (equipment.getAttackBonus() > 0)
        effects.add("공격+" + equipment.getAttackBonus());
      if (equipment.getDefenseBonus() > 0)
        effects.add("방어+" + equipment.getDefenseBonus());
      if (equipment.getHpBonus() > 0)
        effects.add("HP+" + equipment.getHpBonus());

      if (!effects.isEmpty()) {
        display.append(" (").append(String.join(", ", effects)).append(")");
      }
    } else if (item instanceof GameConsumable consumable) {
      if (consumable.getHpRestore() > 0) {
        display.append(" (HP+").append(consumable.getHpRestore()).append(")");
      }
    }

    return display.toString();
  }

  /**
   * 아이템 등급 표시를 반환합니다.
   */
  private String getRarityDisplay(GameItem.ItemRarity rarity) {
    return switch (rarity) {
      case COMMON -> "일반";
      case UNCOMMON -> "고급";
      case RARE -> "희귀";
      case EPIC -> "영웅";
      case LEGENDARY -> "전설";
    };
  }

  /**
   * 착용 중인 장비를 표시합니다.
   */
  private void displayEquippedItems() {
    System.out.println("⚔️ 무기: " + getEquipmentDisplay(equippedWeapon));
    System.out.println("🛡️ 방어구: " + getEquipmentDisplay(equippedArmor));
    System.out.println("💍 장신구: " + getEquipmentDisplay(equippedAccessory));
  }

  /**
   * 장비 표시 형식을 지정합니다.
   */
  private String getEquipmentDisplay(GameEquipment equipment) {
    if (equipment == null) {
      return "없음";
    }

    StringBuilder display = new StringBuilder(equipment.getName());
    display.append(" [").append(getRarityDisplay(equipment.getRarity())).append("]");

    List<String> effects = new ArrayList<>();
    if (equipment.getAttackBonus() > 0)
      effects.add("공격+" + equipment.getAttackBonus());
    if (equipment.getDefenseBonus() > 0)
      effects.add("방어+" + equipment.getDefenseBonus());
    if (equipment.getHpBonus() > 0)
      effects.add("HP+" + equipment.getHpBonus());

    if (!effects.isEmpty()) {
      display.append(" (").append(String.join(", ", effects)).append(")");
    }

    return display.toString();
  }

  /**
   * 장비 보너스를 표시합니다.
   */
  private void displayEquipmentBonuses() {
    EquipmentBonus bonus = getTotalBonus();

    if (bonus.getAttackBonus() > 0 || bonus.getDefenseBonus() > 0 || bonus.getHpBonus() > 0) {
      System.out.printf("📊 총 장비 보너스: ");
      List<String> bonuses = new ArrayList<>();
      if (bonus.getAttackBonus() > 0)
        bonuses.add("공격력 +" + bonus.getAttackBonus());
      if (bonus.getDefenseBonus() > 0)
        bonuses.add("방어력 +" + bonus.getDefenseBonus());
      if (bonus.getHpBonus() > 0)
        bonuses.add("체력 +" + bonus.getHpBonus());
      System.out.println(String.join(", ", bonuses));
    } else {
      System.out.println("📊 장비 보너스: 없음");
    }
  }

  /**
   * 착용 중인 장비로부터 총 스탯 보너스를 계산합니다.
   */
  public EquipmentBonus getTotalBonus() {
    int totalAttackBonus = 0;
    int totalDefenseBonus = 0;
    int totalHpBonus = 0;

    if (equippedWeapon != null) {
      totalAttackBonus += equippedWeapon.getAttackBonus();
      totalDefenseBonus += equippedWeapon.getDefenseBonus();
      totalHpBonus += equippedWeapon.getHpBonus();
    }

    if (equippedArmor != null) {
      totalAttackBonus += equippedArmor.getAttackBonus();
      totalDefenseBonus += equippedArmor.getDefenseBonus();
      totalHpBonus += equippedArmor.getHpBonus();
    }

    if (equippedAccessory != null) {
      totalAttackBonus += equippedAccessory.getAttackBonus();
      totalDefenseBonus += equippedAccessory.getDefenseBonus();
      totalHpBonus += equippedAccessory.getHpBonus();
    }

    return new EquipmentBonus(totalAttackBonus, totalDefenseBonus, totalHpBonus);
  }

  /**
   * 특정 아이템의 보유 수량을 반환합니다.
   */
  public int getItemCount(String itemName) {
    for (ItemStack stack : items) {
      if (stack.getItem().getName().equals(itemName)) {
        return stack.getQuantity();
      }
    }
    return 0;
  }

  /**
   * 인덱스로 아이템을 가져옵니다.
   */
  public ItemStack getItemByIndex(int index) {
    if (index >= 0 && index < items.size()) {
      return items.get(index);
    }
    return null;
  }

  /**
   * 인벤토리 여유 공간을 반환합니다.
   */
  public int getFreeSlots() {
    return maxSize - items.size();
  }

  /**
   * 인벤토리 사용률을 반환합니다 (0.0 ~ 1.0).
   */
  public double getUsageRate() {
    return (double) items.size() / maxSize;
  }

  /**
   * 특정 타입의 아이템들을 반환합니다.
   */
  public List<ItemStack> getItemsByType(Class<? extends GameItem> itemType) {
    return items.stream().filter(stack -> itemType.isInstance(stack.getItem())).toList();
  }

  /**
   * 착용 가능한 장비 목록을 반환합니다.
   */
  public List<GameEquipment> getEquippableItems() {
    return new ArrayList<>(items.stream().filter(stack -> stack.getItem() instanceof GameEquipment).map(stack -> (GameEquipment) stack.getItem()).toList());
  }

  /**
   * 사용 가능한 소비 아이템 목록을 반환합니다.
   */
  public List<GameConsumable> getUsableItems() {
    return new ArrayList<>(items.stream().filter(stack -> stack.getItem() instanceof GameConsumable).map(stack -> (GameConsumable) stack.getItem()).toList());
  }

  /**
   * 인벤토리를 정렬합니다.
   */
  public void sortInventory() {
    items.sort((a, b) -> {
      // 먼저 타입별로 정렬 (소비품 > 무기 > 방어구 > 장신구)
      int typeCompare = getItemTypeOrder(a.getItem()) - getItemTypeOrder(b.getItem());
      if (typeCompare != 0)
        return typeCompare;

      // 같은 타입이면 등급별로 정렬 (높은 등급 우선)
      int rarityCompare = b.getItem().getRarity().ordinal() - a.getItem().getRarity().ordinal();
      if (rarityCompare != 0)
        return rarityCompare;

      // 같은 등급이면 이름순으로 정렬
      return a.getItem().getName().compareTo(b.getItem().getName());
    });

    logger.debug("인벤토리 정렬 완료");
  }

  /**
   * 아이템 타입의 정렬 순서를 반환합니다.
   */
  private int getItemTypeOrder(GameItem item) {
    if (item instanceof GameConsumable)
      return 0;
    if (item instanceof GameEquipment equipment) {
      return switch (equipment.getEquipmentType()) {
        case WEAPON -> 1;
        case ARMOR -> 2;
        case ACCESSORY -> 3;
      };
    }
    return 4;
  }

  // Getters
  public List<ItemStack> getItems() {
    return new ArrayList<>(items);
  }

  public GameEquipment getEquippedWeapon() {
    return equippedWeapon;
  }

  public GameEquipment getEquippedArmor() {
    return equippedArmor;
  }

  public GameEquipment getEquippedAccessory() {
    return equippedAccessory;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public int getCurrentSize() {
    return items.size();
  }

  // 호환성을 위한 메서드들
  public int getTotalAttackBonus() {
    return getTotalBonus().getAttackBonus();
  }

  public int getTotalDefenseBonus() {
    return getTotalBonus().getDefenseBonus();
  }

  public int getTotalHpBonus() {
    return getTotalBonus().getHpBonus();
  }

  /**
   * 아이템 스택 클래스
   */
  public static class ItemStack {
    private GameItem item;
    private int quantity;

    @JsonCreator
    public ItemStack(@JsonProperty("item") GameItem item, @JsonProperty("quantity") int quantity) {
      this.item = item;
      this.quantity = Math.max(1, quantity);
    }

    public void addQuantity(int amount) {
      this.quantity += amount;
    }

    public void removeQuantity(int amount) {
      this.quantity = Math.max(0, this.quantity - amount);
    }

    public GameItem getItem() {
      return item;
    }

    public int getQuantity() {
      return quantity;
    }
  }

  /**
   * 장비 보너스 클래스
   */
  public static class EquipmentBonus {
    private final int attackBonus;
    private final int defenseBonus;
    private final int hpBonus;

    public EquipmentBonus(int attackBonus, int defenseBonus, int hpBonus) {
      this.attackBonus = attackBonus;
      this.defenseBonus = defenseBonus;
      this.hpBonus = hpBonus;
    }

    public int getAttackBonus() {
      return attackBonus;
    }

    public int getDefenseBonus() {
      return defenseBonus;
    }

    public int getHpBonus() {
      return hpBonus;
    }
  }
}
