package rpg.application.service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.manager.LocationManager;
import rpg.domain.item.GameItemData;
import rpg.domain.location.LocationData;
import rpg.domain.monster.MonsterData;
import rpg.infrastructure.data.loader.ConfigDataLoader;
import rpg.infrastructure.data.loader.MonsterDataLoader;

public class DynamicQuestDataProvider {
  private static final Logger logger = LoggerFactory.getLogger(DynamicQuestDataProvider.class);
  private static final Random random = new Random();

  private static DynamicQuestDataProvider instance;

  private DynamicQuestDataProvider() {}

  public static synchronized DynamicQuestDataProvider getInstance() {
    if (instance == null) {
      instance = new DynamicQuestDataProvider();
    }
    return instance;
  }

  // ==================== KILL 퀘스트용 몬스터 선택 ====================

  /**
   * 플레이어 레벨에 적합한 랜덤 몬스터 선택
   */
  public MonsterData selectRandomMonsterForLevel(int playerLevel) {
    List<MonsterData> suitableMonsters = MonsterDataLoader.getMonstersByLevel(playerLevel);

    if (suitableMonsters.isEmpty()) {
      // 레벨 범위를 넓혀서 재시도
      suitableMonsters = MonsterDataLoader.getMonstersByLevelRange(Math.max(1, playerLevel - 3), playerLevel + 3);
    }

    if (suitableMonsters.isEmpty()) {
      logger.warn("레벨 {}에 적합한 몬스터가 없음", playerLevel);
      return getDefaultMonster();
    }

    MonsterData selected = suitableMonsters.get(random.nextInt(suitableMonsters.size()));
    logger.debug("레벨 {} 퀘스트용 몬스터 선택: {} ({})", playerLevel, selected.getName(), selected.getId());

    return selected;
  }

  /**
   * 특정 희귀도의 몬스터 선택
   */
  public MonsterData selectMonsterByRarity(String rarity, int playerLevel) {
    List<MonsterData> monsters = MonsterDataLoader.getMonstersByRarity(rarity).stream().filter(monster -> Math.abs(monster.getMinLevel() - playerLevel) <= 5).collect(Collectors.toList());

    if (monsters.isEmpty()) {
      return selectRandomMonsterForLevel(playerLevel);
    }

    return monsters.get(random.nextInt(monsters.size()));
  }

  /**
   * 특정 지역의 몬스터 선택
   */
  public MonsterData selectMonsterFromLocation(String locationId, int playerLevel) {
    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocationAndLevel(locationId, playerLevel);

    if (locationMonsters.isEmpty()) {
      return selectRandomMonsterForLevel(playerLevel);
    }

    return locationMonsters.get(random.nextInt(locationMonsters.size()));
  }

  /**
   * 다중 몬스터 선택 (여러 종류 사냥 퀘스트용)
   */
  public List<MonsterData> selectMultipleMonsters(int count, int playerLevel) {
    List<MonsterData> allSuitable = MonsterDataLoader.getMonstersByLevel(playerLevel);

    if (allSuitable.size() <= count) {
      return allSuitable;
    }

    return allSuitable.stream().sorted((a, b) -> random.nextBoolean() ? 1 : -1) // 랜덤 셔플
        .limit(count).collect(Collectors.toList());
  }

  // ==================== COLLECT 퀘스트용 아이템 선택 ====================

  /**
   * 수집 가능한 랜덤 아이템 선택
   */
  public GameItemData selectRandomCollectableItem() {
    List<GameItemData> collectableItems = getCollectableItems();

    if (collectableItems.isEmpty()) {
      logger.warn("수집 가능한 아이템이 없음");
      return getDefaultItem();
    }

    GameItemData selected = collectableItems.get(random.nextInt(collectableItems.size()));
    logger.debug("수집 퀘스트용 아이템 선택: {} ({})", selected.getName(), selected.getId());

    return selected;
  }

  /**
   * 특정 타입의 아이템 선택
   */
  public GameItemData selectItemByType(String itemType) {
    List<GameItemData> typeItems = ConfigDataLoader.loadAllItems().values().stream().filter(item -> itemType.equalsIgnoreCase(item.getType())).filter(this::isCollectable).collect(Collectors.toList());

    if (typeItems.isEmpty()) {
      return selectRandomCollectableItem();
    }

    return typeItems.get(random.nextInt(typeItems.size()));
  }

  /**
   * 특정 희귀도의 아이템 선택
   */
  public GameItemData selectItemByRarity(String rarity) {
    List<GameItemData> rarityItems =
        ConfigDataLoader.loadAllItems().values().stream().filter(item -> rarity.equalsIgnoreCase(item.getRarity().name())).filter(this::isCollectable).collect(Collectors.toList());

    if (rarityItems.isEmpty()) {
      return selectRandomCollectableItem();
    }

    return rarityItems.get(random.nextInt(rarityItems.size()));
  }

  /**
   * 다중 아이템 선택 (여러 종류 수집 퀘스트용)
   */
  public List<GameItemData> selectMultipleItems(int count) {
    List<GameItemData> allCollectable = getCollectableItems();

    if (allCollectable.size() <= count) {
      return allCollectable;
    }

    return allCollectable.stream().sorted((a, b) -> random.nextBoolean() ? 1 : -1) // 랜덤 셔플
        .limit(count).collect(Collectors.toList());
  }

  // ==================== EXPLORE 퀘스트용 지역 선택 ====================

  /**
   * 플레이어 레벨에 적합한 랜덤 지역 선택
   */
  public LocationData selectRandomLocationForLevel(int playerLevel) {
    List<LocationData> suitableLocations = LocationManager.getAvailableLocations(playerLevel);

    if (suitableLocations.isEmpty()) {
      logger.warn("레벨 {}에 적합한 지역이 없음", playerLevel);
      return getDefaultLocation();
    }

    LocationData selected = suitableLocations.get(random.nextInt(suitableLocations.size()));
    logger.debug("레벨 {} 탐험 퀘스트용 지역 선택: {} ({})", playerLevel, selected.getNameKo(), selected.getId());

    return selected;
  }

  /**
   * 특정 난이도의 지역 선택
   */
  public LocationData selectLocationByDifficulty(String difficulty, int playerLevel) {
    List<LocationData> difficultyLocations =
        LocationManager.getAllLocations().stream().filter(loc -> difficulty.equalsIgnoreCase(loc.getDangerLevel().name())).filter(loc -> playerLevel >= loc.getMinLevel()).collect(Collectors.toList());

    if (difficultyLocations.isEmpty()) {
      return selectRandomLocationForLevel(playerLevel);
    }

    return difficultyLocations.get(random.nextInt(difficultyLocations.size()));
  }

  /**
   * 다중 지역 선택 (여러 지역 탐험 퀘스트용)
   */
  public List<LocationData> selectMultipleLocations(int count, int playerLevel) {
    List<LocationData> allSuitable = LocationManager.getAvailableLocations(playerLevel);

    if (allSuitable.size() <= count) {
      return allSuitable;
    }

    return allSuitable.stream().sorted((a, b) -> random.nextBoolean() ? 1 : -1) // 랜덤 셔플
        .limit(count).collect(Collectors.toList());
  }

  // ==================== 헬퍼 메서드들 ====================

  /**
   * 수집 가능한 아이템 목록 반환
   */
  private List<GameItemData> getCollectableItems() {
    return ConfigDataLoader.loadAllItems().values().stream().filter(this::isCollectable).collect(Collectors.toList());
  }

  /**
   * 아이템이 수집 가능한지 확인
   */
  private boolean isCollectable(GameItemData item) {
    // 스택 가능하고, 너무 비싸지 않은 아이템만 수집 대상
    return item.isStackable() && item.getValue() <= 200;
  }

  /**
   * 기본 몬스터 반환 (폴백)
   */
  private MonsterData getDefaultMonster() {
    return MonsterDataLoader.getMonsterById("FOREST_SLIME");
  }

  /**
   * 기본 아이템 반환 (폴백)
   */
  private GameItemData getDefaultItem() {
    return ConfigDataLoader.loadAllItems().get("HEALTH_POTION");
  }

  /**
   * 기본 지역 반환 (폴백)
   */
  private LocationData getDefaultLocation() {
    return LocationManager.getLocation("DEEP_FOREST");
  }
}
