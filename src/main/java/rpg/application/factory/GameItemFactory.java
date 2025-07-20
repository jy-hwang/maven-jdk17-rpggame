package rpg.application.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEffectData;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.GameItemData;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.infrastructure.data.loader.ConfigDataLoader;
import rpg.shared.constant.GameConstants;
import rpg.shared.constant.ItemConstants;

/**
 * 통일된 게임 아이템 팩토리 (최신 버전) BasicItemFactory를 대체하는 메인 팩토리
 */
public class GameItemFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameItemFactory.class);
  private static final Random random = new Random();
  // 싱글톤 인스턴스
  private static GameItemFactory instance;

  // 아이템 데이터베이스
  private Map<String, GameItemData> itemDatabase;

  // 초기화 상태
  private boolean initialized = false;

  /**
   * private 생성자 (싱글톤)
   */
  private GameItemFactory() {
    initialize();
  }

  /**
   * 싱글톤 인스턴스 반환
   */
  public static synchronized GameItemFactory getInstance() {
    if (instance == null) {
      instance = new GameItemFactory();
    }
    return instance;
  }

  /**
   * 초기화 메서드 수정
   */
  private void initialize() {
    if (initialized) {
      return;
    }

    logger.info("GameItemFactory 초기화 중...");

    try {
      // 모든 아이템 데이터 로드 (통합)
      itemDatabase = ConfigDataLoader.loadAllItems();

      if (itemDatabase.isEmpty()) {
        logger.warn("아이템 데이터가 비어있음. 기본 아이템 생성...");
        createDefaultItems();
      }

      logger.info("GameItemFactory 초기화 완료: {}개 아이템", itemDatabase.size());
      logLoadedItems();

      initialized = true;

    } catch (Exception e) {
      logger.error("GameItemFactory 초기화 실패", e);
      createDefaultItems();
      initialized = true;
    }
  }

  /**
   * 아이템 생성 (메인 메서드)
   */
  public GameItem createItem(String itemId) {
    if (itemId == null || itemId.trim().isEmpty()) {
      logger.warn("아이템 ID가 비어있음");
      return null;
    }

    GameItemData data = itemDatabase.get(itemId.toUpperCase());
    if (data == null) {
      logger.warn("아이템을 찾을 수 없음: {}", itemId);
      return null;
    }

    try {
      GameItem item = createItemFromData(data);
      if (item != null) {
        logger.debug("아이템 생성 성공: {} -> {}", itemId, item.getName());
      }
      return item;
    } catch (Exception e) {
      logger.error("아이템 생성 중 오류: {}", itemId, e);
      return null;
    }
  }

  /**
   * 데이터로부터 실제 아이템 객체 생성
   */
  private GameItem createItemFromData(GameItemData data) {
    String type = data.getType();

    return switch (type.toUpperCase()) {
      case "CONSUMABLE" -> createConsumableItem(data);
      case "WEAPON" -> createWeaponItem(data);
      case "ARMOR" -> createArmorItem(data);
      case "ACCESSORY" -> createAccessoryItem(data);
      default -> {
        logger.warn("지원하지 않는 아이템 타입: {}", type);
        yield null;
      }
    };
  }

  /**
   * 소비 아이템 생성 (수정된 버전)
   */
  private GameConsumable createConsumableItem(GameItemData data) {
    try {
      // 효과 생성
      List<GameEffect> effects = GameEffectFactory.createEffects(data.getEffects());

      if (effects.isEmpty()) {
        logger.warn("효과가 없는 소비 아이템: {}", data.getName());
        return null;
      }

      // 🆕 cooldown 정보 추출 (새로운 방식)
      int cooldown = data.getCooldown();

      return new GameConsumable(data.getId(), data.getName(), data.getDescription(), data.getValue(), data.getRarity(), effects, cooldown);

    } catch (Exception e) {
      logger.error("소비 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 무기 아이템 생성 (수정된 버전)
   */
  private GameEquipment createWeaponItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.WEAPON;

      // 🆕 스탯 정보 추출 (stats 필드 우선 사용)
      int attack = data.getAttackBonus(); // 이미 stats 필드를 우선 확인하는 로직 포함
      int defense = data.getDefenseBonus();
      int hpBonus = data.getHpBonus();
      int mpBonus = data.getMpBonus();

      return new GameEquipment(data.getId(), data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, attack, defense, hpBonus, mpBonus);

    } catch (Exception e) {
      logger.error("무기 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 방어구 아이템 생성 (수정된 버전)
   */
  private GameEquipment createArmorItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.ARMOR;

      // 🆕 스탯 정보 추출 (stats 필드 우선 사용)
      int attack = data.getAttackBonus();
      int defense = data.getDefenseBonus();
      int hpBonus = data.getHpBonus();
      int mpBonus = data.getMpBonus();

      return new GameEquipment(data.getId(), data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, attack, defense, hpBonus, mpBonus);

    } catch (Exception e) {
      logger.error("방어구 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 액세서리 아이템 생성 (수정된 버전)
   */
  private GameEquipment createAccessoryItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.ACCESSORY;

      // 🆕 스탯 정보 추출 (stats 필드 우선 사용)
      int attack = data.getAttackBonus();
      int defense = data.getDefenseBonus();
      int hpBonus = data.getHpBonus();
      int mpBonus = data.getMpBonus();

      return new GameEquipment(data.getId(), data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, attack, defense, hpBonus, mpBonus);

    } catch (Exception e) {
      logger.error("액세서리 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }


  /**
   * 아이템 존재 여부 확인
   */
  public boolean itemExists(String itemId) {
    return itemId != null && itemDatabase.containsKey(itemId.toUpperCase());
  }

  /**
   * 중첩 가능 여부 확인
   */
  public boolean isStackable(String itemId) {
    GameItemData data = itemDatabase.get(itemId.toUpperCase());
    return data != null && data.isStackable();
  }

  /**
   * 모든 아이템 ID 목록 반환
   */
  public List<String> getAllItemIds() {
    return new ArrayList<>(itemDatabase.keySet());
  }

  /**
   * 특정 타입의 아이템 ID 목록 반환
   */
  public List<String> getItemIdsByType(String itemType) {
    return itemDatabase.entrySet().stream().filter(entry -> entry.getValue().getType().equalsIgnoreCase(itemType)).map(Map.Entry::getKey).collect(Collectors.toList());
  }

  /**
   * 특정 효과를 가진 아이템 검색
   */
  public List<String> findItemsByEffect(String effectType) {
    return itemDatabase.entrySet().stream().filter(entry -> hasEffectType(entry.getValue(), effectType)).map(Map.Entry::getKey).collect(Collectors.toList());
  }

  /**
   * 아이템이 특정 효과를 가지고 있는지 확인
   */
  private boolean hasEffectType(GameItemData data, String effectType) {
    return data.getEffects().stream().anyMatch(effect -> effect.getType().equalsIgnoreCase(effectType));
  }

  /**
   * 레벨에 적합한 아이템 목록 반환
   */
  private List<String> getItemsForLevel(int level) {
    // 간단한 로직: 레벨에 따라 등급 제한
    List<String> suitableItems = new ArrayList<>();

    for (Map.Entry<String, GameItemData> entry : itemDatabase.entrySet()) {
      GameItemData data = entry.getValue();
      ItemRarity rarity = data.getRarity();

      boolean suitable = switch (rarity) {
        case COMMON -> level >= GameConstants.NUMBER_ONE;
        case UNCOMMON -> level >= ItemConstants.BEGINNER_LEVEL;
        case RARE -> level >= ItemConstants.INTERMEDIATE_LEVEL;
        case EPIC -> level >= ItemConstants.HIGH_LEVEL;
        case LEGENDARY -> level >= ItemConstants.ULTRA_HIGH_LEVEL;
      };

      if (suitable) {
        suitableItems.add(entry.getKey());
      }
    }

    return suitableItems;
  }

  /**
   * 특정 등급의 아이템 목록 반환
   */
  public List<String> getItemsByRarity(ItemRarity rarity) {
    return itemDatabase.entrySet().stream().filter(entry -> entry.getValue().getRarity() == rarity).map(Map.Entry::getKey).collect(Collectors.toList());
  }

  /**
   * 아이템 정보 출력
   */
  public void printItemInfo(String itemId) {
    GameItemData data = itemDatabase.get(itemId.toUpperCase());
    if (data == null) {
      System.out.println("❌ 아이템을 찾을 수 없습니다: " + itemId);
      return;
    }

    System.out.println("\n=== 📋 아이템 정보 ===");
    System.out.println("🆔 ID: " + data.getId());
    System.out.println("📛 이름: " + data.getName());
    System.out.println("📝 설명: " + data.getDescription());
    System.out.println("🏷️ 타입: " + data.getType());
    System.out.println("💰 가격: " + data.getValue() + "G");
    System.out.println("⭐ 등급: " + data.getRarity().getDisplayName());
    System.out.println("📦 중첩 가능: " + (data.isStackable() ? "예" : "아니오"));

    if (!data.getEffects().isEmpty()) {
      System.out.println("✨ 효과:");
      for (GameEffectData effect : data.getEffects()) {
        System.out.printf("   - %s: %d%n", effect.getType(), effect.getValue());
      }
    }
    System.out.println("==================");
  }

  /**
   * 모든 아이템 목록 출력
   */
  public void printAllItems() {
    if (itemDatabase.isEmpty()) {
      System.out.println("❌ 등록된 아이템이 없습니다.");
      return;
    }

    System.out.println("\n=== 🎒 전체 아이템 목록 ===");

    // 타입별로 그룹화
    Map<String, List<GameItemData>> itemsByType = itemDatabase.values().stream().collect(Collectors.groupingBy(GameItemData::getType));

    for (Map.Entry<String, List<GameItemData>> typeGroup : itemsByType.entrySet()) {
      System.out.println("\n📂 " + typeGroup.getKey() + ":");

      List<GameItemData> items = typeGroup.getValue();
      items.sort(Comparator.comparing(GameItemData::getName));

      for (GameItemData item : items) {
        System.out.printf("   %s %s (%s) - %dG%n", item.getRarity().getEmoji(), item.getName(), item.getId(), item.getValue());
      }
    }

    System.out.println("\n총 " + itemDatabase.size() + "개 아이템");
    System.out.println("========================");
  }

  /**
   * 아이템 통계 출력
   */
  public void printStatistics() {
    if (itemDatabase.isEmpty()) {
      System.out.println("❌ 통계를 표시할 아이템이 없습니다.");
      return;
    }

    System.out.println("\n=== 📊 아이템 통계 ===");

    // 타입별 통계
    Map<String, Long> typeStats = itemDatabase.values().stream().collect(Collectors.groupingBy(GameItemData::getType, Collectors.counting()));

    System.out.println("📂 타입별:");
    typeStats.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).forEach(entry -> System.out.printf("   %s: %d개%n", entry.getKey(), entry.getValue()));

    // 등급별 통계
    Map<ItemRarity, Long> rarityStats = itemDatabase.values().stream().collect(Collectors.groupingBy(GameItemData::getRarity, Collectors.counting()));

    System.out.println("\n⭐ 등급별:");
    for (ItemRarity rarity : ItemRarity.values()) {
      long count = rarityStats.getOrDefault(rarity, 0L);
      System.out.printf("   %s %s: %d개%n", rarity.getEmoji(), rarity.getDisplayName(), count);
    }

    // 가격 통계
    IntSummaryStatistics priceStats = itemDatabase.values().stream().mapToInt(GameItemData::getValue).summaryStatistics();

    System.out.println("\n💰 가격 통계:");
    System.out.printf("   최저가: %dG%n", priceStats.getMin());
    System.out.printf("   최고가: %dG%n", priceStats.getMax());
    System.out.printf("   평균가: %.1fG%n", priceStats.getAverage());

    System.out.println("==================");
  }


  /**
   * 기본 아이템 생성 (수정된 버전)
   */
  private void createDefaultItems() {
    logger.warn("기본 아이템 생성 중...");
    itemDatabase = new HashMap<>();

    try {
      // 🆕 기본 효과 데이터 생성
      List<GameEffectData> hpEffect = List.of(new GameEffectData("HEAL_HP", 50));
      List<GameEffectData> mpEffect = List.of(new GameEffectData("HEAL_MP", 30));

      // 🆕 기본 스탯 맵 생성
      Map<String, Integer> basicSwordStats = Map.of("attack", 10, "defense", 0, "magic", 0);
      Map<String, Integer> basicArmorStats = Map.of("attack", 0, "defense", 5, "magic", 2);

      // 기본 소비 아이템 (cooldown 포함)
      addDefaultItem("HEALTH_POTION", "체력 물약", "HP를 50 회복합니다", "CONSUMABLE", 25, ItemRarity.COMMON, true, hpEffect, null, basicSwordStats);
      addDefaultItem("MANA_POTION", "마나 물약", "MP를 30 회복합니다", "CONSUMABLE", 30, ItemRarity.COMMON, true, mpEffect, null, null);

      // 기본 장비 아이템 (stats 포함)
      addDefaultItem("BASIC_SWORD", "기본 검", "초보자용 검입니다", "EQUIPMENT", 50, ItemRarity.COMMON, false, null, null, basicSwordStats);
      addDefaultItem("BASIC_ARMOR", "기본 갑옷", "초보자용 갑옷입니다", "EQUIPMENT", 40, ItemRarity.COMMON, false, null, null, basicArmorStats);

      logger.info("기본 아이템 생성 완료: {}개", itemDatabase.size());

    } catch (Exception e) {
      logger.error("기본 아이템 생성 실패", e);
    }
  }

  /**
   * 기본 아이템 추가 헬퍼 메서드 (기존 버전 - 호환성 유지)
   */
  private void addDefaultItem(String id, String name, String description, String type, int value, ItemRarity rarity, boolean stackable, List<GameEffectData> effects) {
    addDefaultItem(id, name, description, type, value, rarity, stackable, effects, null, null);
  }

  /**
   * 기본 아이템 추가 헬퍼 메서드 (확장 버전)
   */
  private void addDefaultItem(String id, String name, String description, String type, int value, ItemRarity rarity, boolean stackable, List<GameEffectData> effects, Integer cooldown,
      Map<String, Integer> stats) {
    try {
      GameItemData item = new GameItemData(id, name, description, type, value, rarity.name(), stackable, effects, null, null, null, null, null, // 기존 장비 필드들 (equipmentType, attackBonus, defenseBonus, hpBonus, mpBonus)
          cooldown, stats, null // 🆕 새로운 필드들 (cooldown, stats, properties)
      );
      itemDatabase.put(id, item);
      logger.debug("기본 아이템 추가: {} (타입: {}, 쿨다운: {}, 스탯: {})", name, type, cooldown, stats != null ? stats.size() : 0);
    } catch (Exception e) {
      logger.error("기본 아이템 추가 실패: {}", name, e);
    }
  }

  /**
   * 로드된 아이템 로그 출력
   */
  private void logLoadedItems() {
    logger.debug("=== 로드된 아이템 목록 ===");
    for (GameItemData item : itemDatabase.values()) {
      logger.debug("아이템: {} (ID: {}, 타입: {}, 등급: {}, 효과: {}개)", item.getName(), item.getId(), item.getType(), item.getRarity(), item.getEffects().size());
    }
    logger.debug("========================");
  }

  /**
   * 팩토리 재초기화 (데이터 리로드)
   */
  public void reinitialize() {
    logger.info("GameItemFactory 재초기화 중...");
    initialized = false;
    itemDatabase = null;
    initialize();
  }

  /**
   * 데이터베이스 직접 접근 (읽기 전용)
   */
  public Map<String, GameItemData> getItemDatabase() {
    return Collections.unmodifiableMap(itemDatabase);
  }

  /**
   * 초기화 상태 확인
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * 로드된 아이템 수 반환
   */
  public int getItemCount() {
    return itemDatabase.size();
  }

  // ==================== 랜덤 생성 메서드들 (클래스 하단에 추가) ====================

  /**
   * 특정 희귀도의 랜덤 아이템 생성 - QuestFactory에서 요청한 메서드
   */
  public GameItem createRandomItemByRarity(ItemRarity rarity) {
    List<GameItemData> itemsOfRarity = getItemDataByRarity(rarity);

    if (itemsOfRarity.isEmpty()) {
      logger.warn("희귀도 {}에 해당하는 아이템이 없음", rarity);
      return createFallbackItemByRarity(rarity);
    }

    GameItemData selectedData = itemsOfRarity.get(random.nextInt(itemsOfRarity.size()));
    GameItem item = createItem(selectedData.getId());

    if (item != null) {
      logger.debug("희귀도 {} 랜덤 아이템 생성: {}", rarity, item.getName());
    }
    return item;
  }

  /**
   * 희귀도별 아이템 데이터 목록 반환 (내부 헬퍼 메서드)
   */
  private List<GameItemData> getItemDataByRarity(ItemRarity rarity) {
    return itemDatabase.values().stream().filter(data -> data.getRarity() == rarity) // 직접 enum 비교
        .collect(Collectors.toList());
  }

  /**
   * 희귀도별 폴백 아이템 생성
   */
  private GameItem createFallbackItemByRarity(ItemRarity rarity) {
    logger.info("희귀도 {} 폴백 아이템 생성", rarity);

    String id = "fallback_" + rarity.name().toLowerCase();
    String name = rarity.getDisplayName() + " 아이템";
    String description = "자동 생성된 " + rarity.getDisplayName() + " 등급 아이템";
    int value = getFallbackValueByRarity(rarity);

    switch (rarity) {
      case COMMON:
        return new GameConsumable(id, name, description, value, rarity, List.of(GameEffectFactory.createHealHpEffect(30)), 0);
      case UNCOMMON:
        return new GameConsumable(id, name, description, value, rarity, List.of(GameEffectFactory.createHealHpEffect(60)), 0);
      case RARE:
        return new GameEquipment(id, name, description, value, rarity, GameEquipment.EquipmentType.ACCESSORY, 5, 5, 20, 20);
      case EPIC:
        return new GameEquipment(id, name, description, value, rarity, GameEquipment.EquipmentType.ACCESSORY, 10, 10, 50, 50);
      case LEGENDARY:
        return new GameEquipment(id, name, description, value, rarity, GameEquipment.EquipmentType.ACCESSORY, 20, 20, 100, 100);
      default:
        return new GameConsumable(id, name, description, value, rarity, List.of(GameEffectFactory.createHealHpEffect(25)), 0);
    }
  }

  /**
   * 희귀도별 기본 가치 반환
   */
  private int getFallbackValueByRarity(ItemRarity rarity) {
    return switch (rarity) {
      case COMMON -> 25;
      case UNCOMMON -> 75;
      case RARE -> 200;
      case EPIC -> 500;
      case LEGENDARY -> 1200;
      default -> 50;
    };
  }

  /**
   * 특정 타입의 랜덤 아이템 생성
   */
  public GameItem createRandomItemByType(String itemType) {
    List<GameItemData> itemsOfType = getItemDataByType(itemType);

    if (itemsOfType.isEmpty()) {
      logger.warn("타입 {}에 해당하는 아이템이 없음", itemType);
      return null;
    }

    GameItemData selectedData = itemsOfType.get(random.nextInt(itemsOfType.size()));
    GameItem item = createItem(selectedData.getId());

    if (item != null) {
      logger.debug("타입 {} 랜덤 아이템 생성: {}", itemType, item.getName());
    }
    return item;
  }

  /**
   * 타입별 아이템 데이터 목록 반환 (내부 헬퍼 메서드)
   */
  private List<GameItemData> getItemDataByType(String itemType) {
    return itemDatabase.values().stream().filter(data -> itemType.equalsIgnoreCase(data.getType())).collect(Collectors.toList());
  }

  /**
   * 희귀도 가중치를 적용한 랜덤 아이템 생성
   */
  public GameItem createWeightedRandomItem() {
    ItemRarity selectedRarity = selectRarityByWeight();
    return createRandomItemByRarity(selectedRarity);
  }

  /**
   * 희귀도 가중치 선택
   */
  private ItemRarity selectRarityByWeight() {
    // 희귀도별 가중치 (낮을수록 더 흔함)
    Map<ItemRarity, Integer> weights = Map.of(ItemRarity.COMMON, 50, ItemRarity.UNCOMMON, 25, ItemRarity.RARE, 15, ItemRarity.EPIC, 7, ItemRarity.LEGENDARY, 3);

    int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
    int randomValue = random.nextInt(totalWeight);

    int currentWeight = 0;
    for (Map.Entry<ItemRarity, Integer> entry : weights.entrySet()) {
      currentWeight += entry.getValue();
      if (randomValue < currentWeight) {
        return entry.getKey();
      }
    }

    return ItemRarity.COMMON; // 폴백
  }

  /**
   * 플레이어 레벨에 맞는 랜덤 아이템 생성 (기존 메서드 개선)
   */
  public GameItem createRandomItemForLevel(int playerLevel) {
    // 레벨에 따른 희귀도 확률 조정
    ItemRarity maxRarity = getMaxRarityForLevel(playerLevel);
    List<ItemRarity> availableRarities = getAvailableRarities(maxRarity);

    ItemRarity selectedRarity = availableRarities.get(random.nextInt(availableRarities.size()));
    GameItem item = createRandomItemByRarity(selectedRarity);

    if (item != null) {
      logger.debug("레벨 {} 적합 아이템 생성: {} ({})", playerLevel, item.getName(), selectedRarity);
    }
    return item;
  }

  /**
   * 레벨에 따른 최대 희귀도 결정
   */
  private ItemRarity getMaxRarityForLevel(int level) {
    if (level <= 2) {
      return ItemRarity.COMMON;
    } else if (level <= 5) {
      return ItemRarity.UNCOMMON;
    } else if (level <= 10) {
      return ItemRarity.RARE;
    } else if (level <= 15) {
      return ItemRarity.EPIC;
    } else {
      return ItemRarity.LEGENDARY;
    }
  }

  /**
   * 최대 희귀도까지의 사용 가능한 희귀도 목록
   */
  private List<ItemRarity> getAvailableRarities(ItemRarity maxRarity) {
    List<ItemRarity> available = new ArrayList<>();
    ItemRarity[] allRarities = ItemRarity.values();

    for (ItemRarity rarity : allRarities) {
      available.add(rarity);
      if (rarity == maxRarity) {
        break;
      }
    }

    return available;
  }

  /**
   * 보물 상자용 랜덤 아이템 생성
   */
  public GameItem createTreasureChestItem() {
    // 보물 상자는 좀 더 좋은 아이템이 나올 확률 높음
    ItemRarity rarity;
    int roll = random.nextInt(100);

    if (roll < 5) { // 5% - 전설
      rarity = ItemRarity.LEGENDARY;
    } else if (roll < 15) { // 10% - 에픽
      rarity = ItemRarity.EPIC;
    } else if (roll < 35) { // 20% - 레어
      rarity = ItemRarity.RARE;
    } else if (roll < 65) { // 30% - 언커먼
      rarity = ItemRarity.UNCOMMON;
    } else { // 35% - 커먼
      rarity = ItemRarity.COMMON;
    }

    GameItem item = createRandomItemByRarity(rarity);
    if (item != null) {
      logger.info("보물 상자에서 {} 등급 아이템 획득: {}", rarity, item.getName());
    }
    return item;
  }

  /**
   * 몬스터 드롭용 랜덤 아이템 생성
   */
  public GameItem createMonsterDropItem(int monsterLevel) {
    // 몬스터 레벨에 따른 드롭률 조정
    ItemRarity rarity;
    int roll = random.nextInt(100);
    int rareBuff = Math.min(monsterLevel * 2, 20); // 몬스터 레벨당 2%씩, 최대 20%

    if (roll < (2 + rareBuff)) { // 기본 2% + 몬스터 레벨 보너스
      rarity = ItemRarity.RARE;
    } else if (roll < (15 + rareBuff)) { // 기본 13% + 보너스
      rarity = ItemRarity.UNCOMMON;
    } else {
      rarity = ItemRarity.COMMON;
    }

    // 소비 아이템 위주로 드롭
    List<GameItemData> consumables = getItemDataByType("CONSUMABLE");
    List<GameItemData> targetItems = consumables.stream().filter(data -> data.getRarity() == rarity).collect(Collectors.toList());

    if (!targetItems.isEmpty()) {
      GameItemData selectedData = targetItems.get(random.nextInt(targetItems.size()));
      GameItem item = createItem(selectedData.getId());
      if (item != null) {
        logger.debug("몬스터 드롭 아이템: {} (레벨 {})", item.getName(), monsterLevel);
      }
      return item;
    }

    // 폴백: 희귀도 기반 랜덤 아이템
    return createRandomItemByRarity(rarity);
  }

  /**
   * 상점용 랜덤 아이템 생성
   */
  public GameItem createShopItem(int shopLevel) {
    // 상점 레벨에 따른 아이템 품질 조정
    ItemRarity maxRarity = getMaxRarityForLevel(shopLevel);

    // 상점은 장비류를 많이 팜
    String[] shopTypes = {"WEAPON", "ARMOR", "ACCESSORY", "CONSUMABLE"};
    String selectedType = shopTypes[random.nextInt(shopTypes.length)];

    List<GameItemData> typeItems = getItemDataByType(selectedType);
    List<GameItemData> availableItems = typeItems.stream().filter(data -> {
      return data.getRarity().ordinal() <= maxRarity.ordinal();
    }).collect(Collectors.toList());

    if (!availableItems.isEmpty()) {
      GameItemData selectedData = availableItems.get(random.nextInt(availableItems.size()));
      GameItem item = createItem(selectedData.getId());
      if (item != null) {
        logger.debug("상점 아이템 생성: {} (상점 레벨 {})", item.getName(), shopLevel);
      }
      return item;
    }

    // 폴백: 기본 아이템
    return createItem("HEALTH_POTION");
  }

  /**
   * 퀘스트 보상용 랜덤 아이템 생성
   */
  public GameItem createQuestRewardItem(int questLevel, ItemRarity minRarity) {
    // 퀘스트 레벨과 최소 희귀도를 고려한 보상 아이템
    ItemRarity maxRarity = getMaxRarityForLevel(questLevel);

    // 최소 희귀도보다 낮으면 최소 희귀도로 조정
    List<ItemRarity> availableRarities = new ArrayList<>();
    for (ItemRarity rarity : ItemRarity.values()) {
      if (rarity.ordinal() >= minRarity.ordinal() && rarity.ordinal() <= maxRarity.ordinal()) {
        availableRarities.add(rarity);
      }
    }

    if (availableRarities.isEmpty()) {
      availableRarities.add(minRarity);
    }

    ItemRarity selectedRarity = availableRarities.get(random.nextInt(availableRarities.size()));
    GameItem item = createRandomItemByRarity(selectedRarity);

    if (item != null) {
      logger.debug("퀘스트 보상 아이템: {} (레벨 {}, 희귀도 {})", item.getName(), questLevel, selectedRarity);
    }
    return item;
  }

  /**
   * 여러 개의 랜덤 아이템을 한 번에 생성
   */
  public List<GameItem> createMultipleRandomItems(int count, ItemRarity maxRarity) {
    List<GameItem> items = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      List<ItemRarity> availableRarities = getAvailableRarities(maxRarity);
      ItemRarity selectedRarity = availableRarities.get(random.nextInt(availableRarities.size()));

      GameItem item = createRandomItemByRarity(selectedRarity);
      if (item != null) {
        items.add(item);
      }
    }

    logger.debug("다중 랜덤 아이템 생성: {}개 (최대 희귀도: {})", items.size(), maxRarity);
    return items;
  }

  /**
   * 세트 아이템 생성 (같은 타입으로 구성)
   */
  public List<GameItem> createItemSet(String itemType, int count) {
    List<GameItem> itemSet = new ArrayList<>();
    List<GameItemData> typeItems = getItemDataByType(itemType);

    if (typeItems.isEmpty()) {
      logger.warn("타입 {}에 해당하는 아이템이 없어서 세트 생성 불가", itemType);
      return itemSet;
    }

    for (int i = 0; i < count; i++) {
      GameItemData selectedData = typeItems.get(random.nextInt(typeItems.size()));
      GameItem item = createItem(selectedData.getId());
      if (item != null) {
        itemSet.add(item);
      }
    }

    logger.debug("{} 타입 아이템 세트 생성: {}개", itemType, itemSet.size());
    return itemSet;
  }

  /**
   * 희귀도별 아이템 개수 반환
   */
  public Map<ItemRarity, Integer> getRarityDistribution() {
    Map<ItemRarity, Integer> distribution = new HashMap<>();

    for (ItemRarity rarity : ItemRarity.values()) {
      int count = getItemDataByRarity(rarity).size();
      distribution.put(rarity, count);
    }

    return distribution;
  }

  /**
   * 타입별 아이템 개수 반환
   */
  public Map<String, Integer> getTypeDistribution() {
    Map<String, Integer> distribution = new HashMap<>();

    for (GameItemData data : itemDatabase.values()) {
      String type = data.getType();
      distribution.merge(type, 1, Integer::sum);
    }

    return distribution;
  }

  /**
   * 특정 희귀도의 아이템 이름 목록 반환
   */
  public List<String> getItemNamesByRarity(ItemRarity rarity) {
    return getItemDataByRarity(rarity).stream().map(GameItemData::getName).sorted().collect(Collectors.toList());
  }

  /**
   * 랜덤 생성 통계 출력
   */
  public void printRandomGenerationStats() {
    System.out.println("\n=== 🎲 랜덤 아이템 생성 통계 ===");

    // 희귀도별 분포
    System.out.println("💎 희귀도별 아이템 분포:");
    Map<ItemRarity, Integer> rarityDist = getRarityDistribution();
    for (Map.Entry<ItemRarity, Integer> entry : rarityDist.entrySet()) {
      if (entry.getValue() > 0) {
        System.out.printf("   %s: %d개%n", entry.getKey().getDisplayName(), entry.getValue());
      }
    }

    // 타입별 분포
    System.out.println("\n🔧 타입별 아이템 분포:");
    Map<String, Integer> typeDist = getTypeDistribution();
    for (Map.Entry<String, Integer> entry : typeDist.entrySet()) {
      System.out.printf("   %s: %d개%n", entry.getKey(), entry.getValue());
    }

    // 가중치 시뮬레이션
    System.out.println("\n⚖️ 가중치 기반 생성 시뮬레이션 (100회):");
    Map<ItemRarity, Integer> simulationResults = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      ItemRarity rarity = selectRarityByWeight();
      simulationResults.merge(rarity, 1, Integer::sum);
    }

    for (Map.Entry<ItemRarity, Integer> entry : simulationResults.entrySet()) {
      System.out.printf("   %s: %d회 (%.1f%%)%n", entry.getKey().getDisplayName(), entry.getValue(), entry.getValue() / 100.0 * 100);
    }

    System.out.printf("\n📊 총 랜덤 생성 가능 아이템: %d개%n", getItemCount());
    System.out.println("================================");
  }

  /**
   * 랜덤 아이템 생성 테스트
   */
  public void testRandomGeneration() {
    System.out.println("\n=== 🧪 랜덤 아이템 생성 테스트 ===");

    // 각 희귀도별 생성 테스트
    System.out.println("💎 희귀도별 생성 테스트:");
    for (ItemRarity rarity : ItemRarity.values()) {
      GameItem item = createRandomItemByRarity(rarity);
      if (item != null) {
        System.out.printf("   ✅ %s: %s%n", rarity.getDisplayName(), item.getName());
      } else {
        System.out.printf("   ❌ %s: 생성 실패%n", rarity.getDisplayName());
      }
    }

    // 특수 생성 테스트
    System.out.println("\n🎁 특수 생성 테스트:");
    GameItem treasureItem = createTreasureChestItem();
    if (treasureItem != null) {
      System.out.printf("   보물 상자: %s (%s)%n", treasureItem.getName(), treasureItem.getRarity().getDisplayName());
    }

    GameItem dropItem = createMonsterDropItem(5);
    if (dropItem != null) {
      System.out.printf("   몬스터 드롭: %s (%s)%n", dropItem.getName(), dropItem.getRarity().getDisplayName());
    }

    GameItem shopItem = createShopItem(3);
    if (shopItem != null) {
      System.out.printf("   상점 아이템: %s (%s)%n", shopItem.getName(), shopItem.getRarity().getDisplayName());
    }

    // 레벨별 생성 테스트
    System.out.println("\n📈 레벨별 생성 테스트:");
    int[] testLevels = {1, 5, 10, 15, 20};
    for (int level : testLevels) {
      GameItem levelItem = createRandomItemForLevel(level);
      if (levelItem != null) {
        System.out.printf("   레벨 %d: %s (%s)%n", level, levelItem.getName(), levelItem.getRarity().getDisplayName());
      }
    }

    System.out.println("===============================");
  }

  /**
   * 특정 희귀도의 아이템 목록 출력
   */
  public void printItemsByRarity(ItemRarity rarity) {
    List<String> itemNames = getItemNamesByRarity(rarity);

    System.out.printf("\n=== %s 등급 아이템 목록 ===\n", rarity.getDisplayName());
    if (itemNames.isEmpty()) {
      System.out.println("해당 등급의 아이템이 없습니다.");
    } else {
      for (int i = 0; i < itemNames.size(); i++) {
        System.out.printf("%d. %s%n", i + 1, itemNames.get(i));
      }
    }
    System.out.printf("총 %d개%n", itemNames.size());
    System.out.println("========================");
  }
}


