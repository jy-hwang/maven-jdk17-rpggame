package model.factory;

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
import config.BaseConstant;
import loader.GameDataLoader;
import model.effect.GameEffect;
import model.item.GameConsumable;
import model.item.GameEffectData;
import model.item.GameEquipment;
import model.item.GameItem;
import model.item.GameItemData;
import model.item.ItemRarity;

/**
 * 통일된 게임 아이템 팩토리 (최신 버전) BasicItemFactory를 대체하는 메인 팩토리
 */
public class GameItemFactory {
  private static final Logger logger = LoggerFactory.getLogger(GameItemFactory.class);

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
      itemDatabase = GameDataLoader.loadAllItems();

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
   * 소비 아이템 생성
   */
  private GameConsumable createConsumableItem(GameItemData data) {
    try {
      // 효과 생성
      List<GameEffect> effects = GameEffectFactory.createEffects(data.getEffects());

      if (effects.isEmpty()) {
        logger.warn("효과가 없는 소비 아이템: {}", data.getName());
        return null;
      }

      return new GameConsumable(data.getName(), data.getDescription(), data.getValue(), data.getRarity(), effects, 0 // 기본 쿨다운 없음
      );

    } catch (Exception e) {
      logger.error("소비 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 무기 아이템 생성 (구현)
   */
  private GameItem createWeaponItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.WEAPON;

      return new GameEquipment(data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, data.getAttackBonus(),
          data.getDefenseBonus(), data.getHpBonus());

    } catch (Exception e) {
      logger.error("무기 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 방어구 아이템 생성 (구현)
   */
  private GameItem createArmorItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.ARMOR;

      return new GameEquipment(data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, data.getAttackBonus(),
          data.getDefenseBonus(), data.getHpBonus());

    } catch (Exception e) {
      logger.error("방어구 아이템 생성 실패: {}", data.getName(), e);
      return null;
    }
  }

  /**
   * 액세서리 아이템 생성 (구현)
   */
  private GameItem createAccessoryItem(GameItemData data) {
    try {
      GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.ACCESSORY;

      return new GameEquipment(data.getName(), data.getDescription(), data.getValue(), data.getRarity(), equipType, data.getAttackBonus(),
          data.getDefenseBonus(), data.getHpBonus());

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
    return itemDatabase.entrySet().stream().filter(entry -> entry.getValue().getType().equalsIgnoreCase(itemType)).map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * 특정 효과를 가진 아이템 검색
   */
  public List<String> findItemsByEffect(String effectType) {
    return itemDatabase.entrySet().stream().filter(entry -> hasEffectType(entry.getValue(), effectType)).map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * 아이템이 특정 효과를 가지고 있는지 확인
   */
  private boolean hasEffectType(GameItemData data, String effectType) {
    return data.getEffects().stream().anyMatch(effect -> effect.getType().equalsIgnoreCase(effectType));
  }

  /**
   * 레벨에 맞는 랜덤 아이템 생성
   */
  public GameItem createRandomItemForLevel(int level) {
    List<String> availableItems = getItemsForLevel(level);

    if (availableItems.isEmpty()) {
      logger.warn("레벨 {}에 맞는 아이템이 없음", level);
      return null;
    }

    Random random = new Random();
    String randomItemId = availableItems.get(random.nextInt(availableItems.size()));

    logger.debug("레벨 {} 랜덤 아이템 선택: {}", level, randomItemId);
    return createItem(randomItemId);
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
        case COMMON -> level >= BaseConstant.NUMBER_ONE;
        case UNCOMMON -> level >= BaseConstant.BEGINNER_LEVEL;
        case RARE -> level >= BaseConstant.INTERMEDIATE_LEVEL;
        case EPIC -> level >= BaseConstant.HIGH_LEVEL;
        case LEGENDARY -> level >= BaseConstant.ULTRA_HIGH_LEVEL;
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
    return itemDatabase.entrySet().stream().filter(entry -> entry.getValue().getRarity() == rarity).map(Map.Entry::getKey)
        .collect(Collectors.toList());
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
    typeStats.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .forEach(entry -> System.out.printf("   %s: %d개%n", entry.getKey(), entry.getValue()));

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
   * 기본 아이템 생성 (JSON 파일이 없을 때)
   */
  private void createDefaultItems() {
    logger.info("기본 아이템 데이터 생성 중...");

    itemDatabase = new HashMap<>();

    // 기본 HP 물약들
    addDefaultItem("HEALTH_POTION", "체력 물약", "HP를 50 회복합니다", "CONSUMABLE", 50, ItemRarity.COMMON, true, List.of(new GameEffectData("HEAL_HP", 50)));

    addDefaultItem("LARGE_HEALTH_POTION", "큰 체력 물약", "HP를 100 회복합니다", "CONSUMABLE", 120, ItemRarity.UNCOMMON, true,
        List.of(new GameEffectData("HEAL_HP", 100)));

    addDefaultItem("SUPER_HEALTH_POTION", "고급 체력 물약", "HP를 200 회복합니다", "CONSUMABLE", 250, ItemRarity.RARE, true,
        List.of(new GameEffectData("HEAL_HP", 200)));

    // 기본 MP 물약들
    addDefaultItem("MANA_POTION", "마나 물약", "MP를 40 회복합니다", "CONSUMABLE", 60, ItemRarity.COMMON, true, List.of(new GameEffectData("HEAL_MP", 40)));

    addDefaultItem("LARGE_MANA_POTION", "큰 마나 물약", "MP를 80 회복합니다", "CONSUMABLE", 140, ItemRarity.UNCOMMON, true,
        List.of(new GameEffectData("HEAL_MP", 80)));

    logger.info("기본 아이템 생성 완료: {}개", itemDatabase.size());
  }

  /**
   * 기본 아이템 추가 헬퍼 메서드
   */
  private void addDefaultItem(String id, String name, String description, String type, int value, ItemRarity rarity, boolean stackable,
      List<GameEffectData> effects) {
    GameItemData item = new GameItemData(id, name, description, type, value, rarity.name(), // ItemRarity enum을 String으로 변환
        stackable, effects, null, null, null, null, null);
    itemDatabase.put(id, item);
    logger.debug("기본 아이템 추가: {}", name);
  }

  /**
   * 로드된 아이템 로그 출력
   */
  private void logLoadedItems() {
    logger.debug("=== 로드된 아이템 목록 ===");
    for (GameItemData item : itemDatabase.values()) {
      logger.debug("아이템: {} (ID: {}, 타입: {}, 등급: {}, 효과: {}개)", item.getName(), item.getId(), item.getType(), item.getRarity(),
          item.getEffects().size());
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

}
