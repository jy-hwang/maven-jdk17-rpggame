package rpg.domain.inventory;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.application.factory.GameItemFactory;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.player.Player;
import rpg.shared.constant.GameConstants;

/**
 * 캐릭터의 인벤토리를 관리하는 클래스 (GameInventory 기반)
 */
public class PlayerInventory {
  private static final Logger logger = LoggerFactory.getLogger(PlayerInventory.class);

  private final List<ItemStack> items;
  private final int maxSlots;

  // 착용 장비
  private GameEquipment equippedWeapon;
  private GameEquipment equippedArmor;
  private GameEquipment equippedAccessory;

  // 기존 생성자 (새 캐릭터용)
  public PlayerInventory(int maxSlots) {
    this.items = new ArrayList<>();
    this.maxSlots = maxSlots;
    this.equippedWeapon = null;
    this.equippedArmor = null;
    this.equippedAccessory = null;

    logger.debug("GameInventory 생성: 최대 {}슬롯", maxSlots);
  }

  // Jackson 역직렬화용 생성자 추가
  @JsonCreator
  public PlayerInventory(
 //@formatter:off
   @JsonProperty("items") List<ItemStack> items
 , @JsonProperty("maxSlots") int maxSlots
 , @JsonProperty("equippedWeapon") GameEquipment equippedWeapon
 , @JsonProperty("equippedArmor") GameEquipment equippedArmor
 , @JsonProperty("equippedAccessory") GameEquipment equippedAccessory
 , @JsonProperty("maxSize") int maxSize
 , @JsonProperty("usageRate") double usageRate
 , @JsonProperty("equippableItems") List<GameEquipment> equippableItems
 , @JsonProperty("usableItems") List<GameConsumable> usableItems
 , @JsonProperty("freeSlots") int freeSlots
 , @JsonProperty("currentSize") int currentSize
 , @JsonProperty("totalBonus") EquipmentBonus totalBonus
 //@formatter:on
  ) {
    // items 초기화
    this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();

    // maxSlots 설정 (maxSize도 같은 값일 것)
    this.maxSlots = maxSlots > GameConstants.NUMBER_ZERO ? maxSlots : (maxSize > GameConstants.NUMBER_ZERO ? maxSize : GameConstants.NUMBER_TWENTY);

    // 장비 설정
    this.equippedWeapon = equippedWeapon;
    this.equippedArmor = equippedArmor;
    this.equippedAccessory = equippedAccessory;

    logger.debug("GameInventory 역직렬화: 최대 {}슬롯, 아이템 {}개", this.maxSlots, this.items.size());
  }

  /**
   * 아이템 추가 (중첩 고려)
   */
  public boolean addItem(GameItem item, int quantity) {
    if (item == null || quantity <= GameConstants.NUMBER_ZERO) {
      logger.warn("잘못된 아이템 추가 시도: item={}, quantity={}", item, quantity);
      return false;
    }

    // 중첩 가능한 아이템인지 팩토리에서 확인
    GameItemFactory factory = GameItemFactory.getInstance();
    boolean stackable = false;

    // 아이템 ID를 얻어서 중첩 가능 여부 확인
    String itemId = findItemId(item);
    if (itemId != null) {
      stackable = factory.isStackable(itemId);
    }

    if (stackable) {
      // 기존 스택에 추가 시도
      for (ItemStack stack : items) {
        if (stack.getItem().getName().equals(item.getName())) {
          stack.addQuantity(quantity);
          logger.debug("기존 스택에 아이템 추가: {} x{}", item.getName(), quantity);
          return true;
        }
      }
    }

    // 새 스택 생성
    if (items.size() >= maxSlots) {
      logger.warn("인벤토리 가득참: {}/{}", items.size(), maxSlots);
      return false;
    }

    items.add(new ItemStack(item, quantity));
    logger.debug("새 스택으로 아이템 추가: {} x{}", item.getName(), quantity);
    return true;
  }

  /**
   * 아이템에서 ID 찾기 (임시 구현)
   */
  private String findItemId(GameItem item) {
    GameItemFactory factory = GameItemFactory.getInstance();

    // 모든 아이템 ID를 순회하며 이름으로 매칭 (비효율적이지만 임시)
    for (String id : factory.getAllItemIds()) {
      try {
        GameItem factoryItem = factory.createItem(id);
        if (factoryItem.getName().equals(item.getName())) {
          return id;
        }
      } catch (Exception e) {
        // 무시
      }
    }
    return null;
  }

  /**
   * 아이템 제거
   */
  public boolean removeItem(String itemName, int quantity) {
    for (ItemStack stack : items) {
      if (stack.getItem().getName().equals(itemName)) {
        if (stack.getQuantity() >= quantity) {
          stack.removeQuantity(quantity);
          if (stack.getQuantity() <= GameConstants.NUMBER_ZERO) {
            items.remove(stack);
          }
          logger.debug("아이템 제거: {} x{}", itemName, quantity);
          return true;
        } else {
          logger.warn("아이템 수량 부족: {} (요청: {}, 보유: {})", itemName, quantity, stack.getQuantity());
          return false;
        }
      }
    }

    logger.warn("아이템을 찾을 수 없음: {}", itemName);
    return false;
  }

  /**
   * 아이템 사용
   */
  public boolean useItem(String itemName, Player character) {
    for (ItemStack stack : items) {
      if (stack.getItem().getName().equals(itemName)) {
        if (stack.getItem().use(character)) {
          stack.removeQuantity(1);
          if (stack.getQuantity() <= 0) {
            items.remove(stack);
          }
          logger.info("아이템 사용: {} -> {}", character.getName(), itemName);
          return true;
        } else {
          logger.debug("아이템 사용 실패: {}", itemName);
          return false;
        }
      }
    }

    logger.warn("사용할 아이템을 찾을 수 없음: {}", itemName);
    return false;
  }

  /**
   * 아이템 개수 확인
   */
  public int getItemCount(String itemName) {
    return items.stream().filter(stack -> stack.getItem().getName().equals(itemName)).mapToInt(ItemStack::getQuantity).sum();
  }

  /**
   * 사용 가능한 아이템 목록 (소비 아이템만)
   */
  public List<GameConsumable> getUsableItems() {
    return items.stream().map(ItemStack::getItem).filter(item -> item instanceof GameConsumable).map(item -> (GameConsumable) item).distinct()
        .collect(Collectors.toList());
  }

  /**
   * 착용 가능한 장비 목록
   */
  public List<GameEquipment> getEquippableItems() {
    return items.stream().map(ItemStack::getItem).filter(item -> item instanceof GameEquipment).map(item -> (GameEquipment) item).distinct()
        .collect(Collectors.toList());
  }

  /**
   * 장비 착용
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

    // 인벤토리에서 새 장비 제거
    removeItem(equipment.getName(), 1);

    // 기존 장비가 있으면 인벤토리에 추가
    if (oldEquipment != null) {
      addItem(oldEquipment, 1);
    }

    logger.info("장비 착용: {} (기존: {})", equipment.getName(), oldEquipment != null ? oldEquipment.getName() : "없음");

    return oldEquipment;
  }

  /**
   * 장비 해제
   */
  public GameEquipment unequipItem(GameEquipment.EquipmentType type) {
    GameEquipment equipment = null;

    switch (type) {
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
      addItem(equipment, GameConstants.NUMBER_ONE);
      logger.info("장비 해제: {}", equipment.getName());
    }

    return equipment;
  }

  /**
   * 총 장비 보너스 계산
   */
  public EquipmentBonus getTotalBonus() {
    int attackBonus = GameConstants.NUMBER_ZERO;
    int defenseBonus = GameConstants.NUMBER_ZERO;
    int hpBonus = GameConstants.NUMBER_ZERO;

    if (equippedWeapon != null) {
      attackBonus += equippedWeapon.getAttackBonus();
      defenseBonus += equippedWeapon.getDefenseBonus();
      hpBonus += equippedWeapon.getHpBonus();
    }

    if (equippedArmor != null) {
      attackBonus += equippedArmor.getAttackBonus();
      defenseBonus += equippedArmor.getDefenseBonus();
      hpBonus += equippedArmor.getHpBonus();
    }

    if (equippedAccessory != null) {
      attackBonus += equippedAccessory.getAttackBonus();
      defenseBonus += equippedAccessory.getDefenseBonus();
      hpBonus += equippedAccessory.getHpBonus();
    }

    return new EquipmentBonus(attackBonus, defenseBonus, hpBonus);
  }

  /**
   * 인벤토리 정렬
   */
  public void sortInventory() {
    items.sort((a, b) -> {
      // 1. 타입별 정렬 (CONSUMABLE -> WEAPON -> ARMOR -> ACCESSORY)
      int typeCompare = getTypeOrder(a.getItem()) - getTypeOrder(b.getItem());
      if (typeCompare != GameConstants.NUMBER_ZERO)
        return typeCompare;

      // 2. 등급별 정렬
      int rarityCompare = b.getItem().getRarity().ordinal() - a.getItem().getRarity().ordinal();
      if (rarityCompare != GameConstants.NUMBER_ZERO)
        return rarityCompare;

      // 3. 이름순 정렬
      return a.getItem().getName().compareTo(b.getItem().getName());
    });

    logger.debug("인벤토리 정렬 완료");
  }

  /**
   * 아이템 타입 순서 반환
   */
  private int getTypeOrder(GameItem item) {
    if (item instanceof GameConsumable)
      return GameConstants.NUMBER_ZERO;
    if (item instanceof GameEquipment equipment) {
      return switch (equipment.getEquipmentType()) {
        case WEAPON -> 1;
        case ARMOR -> 2;
        case ACCESSORY -> 3;
      };
    }
    return 4;
  }

  /**
   * 인벤토리 표시
   */
  public void displayInventory() {
    System.out.println("\n=== 🎒 인벤토리 ===");
    System.out.printf("용량: %d/%d 슬롯%n", items.size(), maxSlots);

    if (items.isEmpty()) {
      System.out.println("인벤토리가 비어있습니다.");
    } else {
      Map<String, List<ItemStack>> itemsByType = items.stream().collect(Collectors.groupingBy(stack -> {
        if (stack.getItem() instanceof GameConsumable)
          return "소비 아이템";
        if (stack.getItem() instanceof GameEquipment)
          return "장비";
        return "기타";
      }));

      itemsByType.forEach((type, stacks) -> {
        System.out.println("\n" + type + ":");
        stacks.forEach(stack -> {
          String quantityStr = stack.getQuantity() > 1 ? " x" + stack.getQuantity() : "";
          System.out.printf("• %s%s [%s]%n", stack.getItem().getName(), quantityStr, stack.getItem().getRarity().getDisplayName());
        });
      });
    }

    System.out.println("==================");
  }

  // Getters
  public List<ItemStack> getItems() {
    return new ArrayList<>(items);
  }

  public int getCurrentSize() {
    return items.size();
  }

  public int getMaxSize() {
    return maxSlots;
  }

  public int getFreeSlots() {
    return maxSlots - items.size();
  }

  public double getUsageRate() {
    return (double) items.size() / maxSlots;
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

  public void setEquippedWeapon(GameEquipment equippedWeapon) {
    this.equippedWeapon = equippedWeapon;
  }

  public void setEquippedArmor(GameEquipment equippedArmor) {
    this.equippedArmor = equippedArmor;
  }

  public void setEquippedAccessory(GameEquipment equippedAccessory) {
    this.equippedAccessory = equippedAccessory;
  }


  /**
   * 장비 보너스 클래스
   */
  public static class EquipmentBonus {
    private final int attackBonus;
    private final int defenseBonus;
    private final int hpBonus;

    // Jackson 역직렬화용 생성자 추가
    @JsonCreator
    public EquipmentBonus(
//@formatter:off
  @JsonProperty("attackBonus") int attackBonus
, @JsonProperty("defenseBonus") int defenseBonus
, @JsonProperty("hpBonus") int hpBonus
//@formatter:on
    ) {
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

  /**
   * 착용 장비 상태를 검증합니다.
   */
  public void validateEquippedItems() {
    logger.debug("착용 장비 검증 시작");

    if (equippedWeapon != null) {
      logger.debug("착용 무기: {}", equippedWeapon.getName());
    }
    if (equippedArmor != null) {
      logger.debug("착용 방어구: {}", equippedArmor.getName());
    }
    if (equippedAccessory != null) {
      logger.debug("착용 장신구: {}", equippedAccessory.getName());
    }

    logger.debug("착용 장비 검증 완료");
  }

  /**
   * 착용 장비를 강제로 설정합니다 (로드용)
   */
  public void forceSetEquippedWeapon(GameEquipment weapon) {
    this.equippedWeapon = weapon;
    logger.debug("무기 강제 설정: {}", weapon != null ? weapon.getName() : "없음");
  }

  public void forceSetEquippedArmor(GameEquipment armor) {
    this.equippedArmor = armor;
    logger.debug("방어구 강제 설정: {}", armor != null ? armor.getName() : "없음");
  }

  public void forceSetEquippedAccessory(GameEquipment accessory) {
    this.equippedAccessory = accessory;
    logger.debug("장신구 강제 설정: {}", accessory != null ? accessory.getName() : "없음");
  }
}
