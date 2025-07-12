package loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.BaseConstant;
import model.effect.GameEffect;
import model.factory.GameEffectFactory;
import model.item.GameConsumable;
import model.item.GameEquipment;
import model.item.GameItem;
import model.item.GameItemData;
import model.item.ItemRarity;

/**
 * 보물, 이벤트, 드롭 아이템 데이터를 JSON에서 로드하는 서비스
 */
public class ItemDataLoader {
  private static final Logger logger = LoggerFactory.getLogger(ItemDataLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static Map<String, GameItemData> treasureItems = new HashMap<>();
  private static Map<String, GameItemData> equipmentItems = new HashMap<>();
  private static Map<String, GameItemData> dropItems = new HashMap<>();
  private static boolean dataLoaded = false;

  /**
   * 모든 아이템 데이터를 로드합니다.
   */
  public static void loadAllItemData() {
    if (dataLoaded) {
      return;
    }

    try {
      // 보물 아이템 로드
      treasureItems = loadItemsFromFile(BaseConstant.EXPLORE_TREASURE);

      // 장비 아이템 로드
      equipmentItems = loadItemsFromFile(BaseConstant.EXPLORE_EQUIPMENT);

      // 드롭 아이템 로드
      dropItems = loadItemsFromFile(BaseConstant.EXPLORE_DROP);

      // 데이터 검증
      validateItemData();

      dataLoaded = true;

      int totalItems = treasureItems.size() + equipmentItems.size() + dropItems.size();
      logger.info("아이템 데이터 로드 완료: 보물 {}개, 장비 {}개, 드롭 {}개 (총 {}개)", treasureItems.size(), equipmentItems.size(), dropItems.size(), totalItems);

    } catch (Exception e) {
      logger.error("아이템 데이터 로드 실패", e);
      createDefaultItemData();
    }
  }

  /**
   * 특정 파일에서 아이템 데이터를 로드합니다.
   */
  private static Map<String, GameItemData> loadItemsFromFile(String filePath) {
    try (InputStream inputStream = ItemDataLoader.class.getResourceAsStream(filePath)) {
      if (inputStream == null) {
        logger.warn("아이템 파일 없음: {}", filePath);
        return Map.of();
      }

      List<GameItemData> itemList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});

      return itemList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

    } catch (Exception e) {
      logger.error("아이템 파일 로드 실패: {}", filePath, e);
      return Map.of();
    }
  }

  /**
   * 랜덤 보물 아이템을 생성합니다.
   */
  public static GameItem generateRandomTreasureItem() {
    if (!dataLoaded) {
      loadAllItemData();
    }

    if (treasureItems.isEmpty()) {
      return createFallbackTreasureItem();
    }

    // 보물 카테고리 아이템들만 필터링
    List<GameItemData> treasureList =
        treasureItems.values().stream().filter(item -> "treasure".equals(getItemCategory(item))).collect(Collectors.toList());

    if (treasureList.isEmpty()) {
      treasureList = new ArrayList<>(treasureItems.values());
    }

    // 희귀도 가중치 적용하여 선택
    GameItemData selectedData = selectItemByRarity(treasureList);
    return convertToGameItem(selectedData);
  }

  /**
   * 특별한 상인 아이템을 생성합니다.
   */
  public static GameItem generateSpecialMerchantItem() {
    if (!dataLoaded) {
      loadAllItemData();
    }

    // 상인 카테고리 아이템들 선택
    List<GameItemData> merchantItems = new ArrayList<>();
    merchantItems.addAll(treasureItems.values().stream().filter(item -> "merchant".equals(getItemCategory(item))).collect(Collectors.toList()));
    merchantItems.addAll(equipmentItems.values().stream().filter(item -> "merchant".equals(getItemCategory(item))).collect(Collectors.toList()));

    if (merchantItems.isEmpty()) {
      return createFallbackMerchantItem();
    }

    Random random = new Random();
    GameItemData selectedData = merchantItems.get(random.nextInt(merchantItems.size()));
    return convertToGameItem(selectedData);
  }

  /**
   * 랜덤 드롭 아이템을 생성합니다.
   */
  public static GameItem generateRandomDropItem() {
    if (!dataLoaded) {
      loadAllItemData();
    }

    if (dropItems.isEmpty()) {
      return createFallbackDropItem();
    }

    // 일반적인 드롭 아이템들 선택 (COMMON, UNCOMMON 위주)
    List<GameItemData> commonDrops = dropItems.values().stream()
        .filter(item -> "COMMON".equals(item.getRarity().getDisplayName()) || "UNCOMMON".equals(item.getRarity().getDisplayName()))
        .collect(Collectors.toList());

    if (commonDrops.isEmpty()) {
      commonDrops = new ArrayList<>(dropItems.values());
    }

    Random random = new Random();
    GameItemData selectedData = commonDrops.get(random.nextInt(commonDrops.size()));
    return convertToGameItem(selectedData);
  }

  /**
   * 특정 아이템을 ID로 생성합니다.
   */
  public static GameItem createItemById(String itemId) {
    if (!dataLoaded) {
      loadAllItemData();
    }

    // 모든 카테고리에서 검색
    GameItemData itemData = treasureItems.get(itemId);
    if (itemData == null) {
      itemData = equipmentItems.get(itemId);
    }
    if (itemData == null) {
      itemData = dropItems.get(itemId);
    }

    if (itemData == null) {
      logger.warn("아이템 ID를 찾을 수 없음: {}", itemId);
      return createFallbackDropItem();
    }

    return convertToGameItem(itemData);
  }

  /**
   * 아이템 통계를 출력합니다.
   */
  public static void printItemStatistics() {
    if (!dataLoaded) {
      loadAllItemData();
    }

    System.out.println("\n🎁 === 아이템 통계 ===");
    System.out.println("보물 아이템: " + treasureItems.size() + "개");
    System.out.println("장비 아이템: " + equipmentItems.size() + "개");
    System.out.println("드롭 아이템: " + dropItems.size() + "개");
    System.out.println("총 아이템: " + (treasureItems.size() + equipmentItems.size() + dropItems.size()) + "개");

    // 등급별 통계
    Map<String, Long> rarityStats =
        getAllItems().stream().collect(Collectors.groupingBy(item -> item.getRarity().getDisplayName(), Collectors.counting()));

    System.out.println("\n📊 등급별 분포:");
    rarityStats.forEach((rarity, count) -> System.out.printf("   %s: %d개%n", rarity, count));

    System.out.println("==================");
  }

  /**
   * 아이템 데이터를 다시 로드합니다.
   */
  public static void reloadItemData() {
    logger.info("아이템 데이터 리로드 중...");

    treasureItems.clear();
    equipmentItems.clear();
    dropItems.clear();
    dataLoaded = false;

    loadAllItemData();

    logger.info("아이템 데이터 리로드 완료");
  }

  // === 헬퍼 메서드들 ===

  private static String getItemCategory(GameItemData item) {
    Map<String, Object> properties = item.getProperties();
    if (properties != null && properties.containsKey("category")) {
      Object category = properties.get("category");
      return category != null ? category.toString() : "unknown";
    }
    return "unknown";
  }

  private static GameItemData selectItemByRarity(List<GameItemData> items) {
    if (items.isEmpty()) {
      return null;
    }

    Random random = new Random();

    // 희귀도별 가중치 계산
    Map<String, Double> rarityWeights = Map.of("COMMON", 0.5, "UNCOMMON", 0.3, "RARE", 0.15, "EPIC", 0.04, "LEGENDARY", 0.01);

    double totalWeight = items.stream().mapToDouble(item -> rarityWeights.getOrDefault(item.getRarity().getDisplayName(), 0.1)).sum();

    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0;

    for (GameItemData item : items) {
      currentWeight += rarityWeights.getOrDefault(item.getRarity().getDisplayName(), 0.1);
      if (randomValue <= currentWeight) {
        return item;
      }
    }

    // 기본값으로 첫 번째 아이템 반환
    return items.get(0);
  }

  private static GameItem convertToGameItem(GameItemData itemData) {
    if (itemData == null) {
      return createFallbackDropItem();
    }

    try {
      if ("CONSUMABLE".equals(itemData.getType())) {
        return createConsumableItem(itemData);
      } else if ("EQUIPMENT".equals(itemData.getType())) {
        return createEquipmentItem(itemData);
      } else {
        logger.warn("알 수 없는 아이템 타입: {}", itemData.getType());
        return createFallbackDropItem();
      }
    } catch (Exception e) {
      logger.error("아이템 변환 실패: {}", itemData.getName(), e);
      return createFallbackDropItem();
    }
  }

  private static GameConsumable createConsumableItem(GameItemData itemData) {
    try {
      // GameEffect 시스템 사용
      List<GameEffect> effects = GameEffectFactory.createEffects(itemData.getEffects());

      int cooldown = 0;
      Map<String, Object> properties = itemData.getProperties();

      // properties null 체크
      if (properties != null && properties.containsKey("cooldown")) {
        Object cooldownObj = properties.get("cooldown");
        if (cooldownObj instanceof Integer) {
          cooldown = (Integer) cooldownObj;
        } else if (cooldownObj instanceof String) {
          try {
            cooldown = Integer.parseInt((String) cooldownObj);
          } catch (NumberFormatException e) {
            logger.warn("쿨다운 값 파싱 실패: {} - 기본값 0 사용", cooldownObj);
          }
        }
      }

      return new GameConsumable(itemData.getName(), itemData.getDescription(), itemData.getValue(), itemData.getRarity(), effects, cooldown);
    } catch (Exception e) {
      logger.error("소비 아이템 생성 실패: {}", itemData.getName(), e);

      // 레거시 생성자로 폴백
      int hpRestore = extractEffectValue(itemData, "HEAL_HP");
      int mpRestore = extractEffectValue(itemData, "HEAL_MP");

      @SuppressWarnings("deprecation")
      GameConsumable fallback = new GameConsumable(itemData.getName(), itemData.getDescription(), itemData.getValue(), itemData.getRarity(),
          hpRestore, mpRestore, 0, itemData.isStackable());

      return fallback;
    }
  }

  private static GameEquipment createEquipmentItem(GameItemData itemData) {
    // null 체크 추가
    Map<String, Object> properties = itemData.getProperties();
    if (properties == null) {
      logger.warn("아이템 속성이 null: {} - 기본값 사용", itemData.getName());
      properties = new HashMap<>();
    }

    // equipmentType과 stats 정보 추출 (null 안전)
    String equipTypeStr = (String) properties.get("equipmentType");
    GameEquipment.EquipmentType equipType;

    try {
      equipType = GameEquipment.EquipmentType.valueOf(equipTypeStr != null ? equipTypeStr : "WEAPON");
    } catch (IllegalArgumentException e) {
      logger.warn("잘못된 장비 타입: {} - WEAPON으로 대체", equipTypeStr);
      equipType = GameEquipment.EquipmentType.WEAPON;
    }

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) properties.get("stats");

    // stats가 null일 경우 기본값 사용
    if (stats == null) {
      logger.debug("아이템 스탯이 null: {} - 기본값 사용", itemData.getName());
      stats = new HashMap<>();
    }

    int attack = stats.getOrDefault("attack", 0);
    int defense = stats.getOrDefault("defense", 0);
    int magic = stats.getOrDefault("magic", 0);

    return new GameEquipment(itemData.getName(), itemData.getDescription(), itemData.getValue(), itemData.getRarity(), equipType, attack, defense,
        magic);
  }

  private static int extractEffectValue(GameItemData itemData, String effectType) {
    if (itemData == null || itemData.getEffects() == null || effectType == null) {
      return 0;
    }

    return itemData.getEffects().stream().filter(effect -> effect != null && effectType.equals(effect.getType()))
        .mapToInt(effect -> effect.getValue()).findFirst().orElse(0);
  }

  private static void validateItemData() {
    // 간단한 검증 로직
    int totalItems = treasureItems.size() + equipmentItems.size() + dropItems.size();
    logger.info("아이템 데이터 검증 완료: {}개 아이템", totalItems);
  }

  private static void createDefaultItemData() {
    logger.warn("기본 아이템 데이터 생성 중...");
    dataLoaded = true;
  }

  public static List<GameItemData> getAllItems() {
    if (!dataLoaded) {
      loadAllItemData();
    }

    List<GameItemData> allItems = new ArrayList<>();
    allItems.addAll(treasureItems.values());
    allItems.addAll(equipmentItems.values());
    allItems.addAll(dropItems.values());

    return allItems;
  }

  // === 폴백 아이템 생성 메서드들 ===

  @SuppressWarnings("deprecation")
  private static GameItem createFallbackTreasureItem() {
    logger.warn("폴백 보물 아이템 생성");
    return new GameConsumable("보물 물약", "HP를 75 회복", 40, ItemRarity.UNCOMMON, 75, 0, 0, true);
  }

  @SuppressWarnings("deprecation")
  private static GameItem createFallbackMerchantItem() {
    logger.warn("폴백 상인 아이템 생성");
    return new GameConsumable("상인의 물약", "HP를 60 회복", 35, ItemRarity.RARE, 60, 30, 0, true);
  }

  @SuppressWarnings("deprecation")
  private static GameItem createFallbackDropItem() {
    logger.warn("폴백 드롭 아이템 생성");
    return new GameConsumable("기본 물약", "HP를 30 회복", 15, ItemRarity.COMMON, 30, 0, 0, true);
  }
}
