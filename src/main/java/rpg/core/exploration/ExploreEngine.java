package rpg.core.exploration;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.manager.LocationManager;
import rpg.core.battle.BattleEngine;
import rpg.core.engine.GameState;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.location.LocationData;
import rpg.domain.monster.Monster;
import rpg.domain.monster.MonsterData;
import rpg.domain.player.Player;
import rpg.infrastructure.data.loader.ItemDataLoader;
import rpg.infrastructure.data.loader.MonsterDataLoader;
import rpg.presentation.controller.InventoryController;
import rpg.presentation.controller.QuestController;
import rpg.shared.constant.SystemConstants;

/**
 * 리팩토링된 탐험 시스템 엔진
 * - LocationManager 기반으로 동적 지역 관리
 * - 하드코딩 제거 및 확장성 개선
 */
public class ExploreEngine {
  private static final Logger logger = LoggerFactory.getLogger(ExploreEngine.class);

  private final Random random;
  private final BattleEngine battleController;
  private final QuestController questController;
  private final InventoryController inventoryController;
  private final GameState gameState;

  public ExploreEngine(BattleEngine battleController, QuestController questController, InventoryController inventoryController, GameState gameState) {
    this.random = new Random();
    this.battleController = battleController;
    this.questController = questController;
    this.inventoryController = inventoryController;
    this.gameState = gameState;

    // 의존성 초기화
    initializeDependencies();
    logger.info("ExploreEngine 초기화 완료 (LocationManager 기반)");
  }

  /**
   * 의존성 시스템들 초기화
   */
  private void initializeDependencies() {
    try {
      // LocationManager 초기화
      LocationManager.initialize();

      // MonsterDataLoader 초기화 (통합 JSON 사용)
      MonsterDataLoader.loadAllMonsters();

      logger.info("탐험 시스템 의존성 초기화 완료");

      // 디버그 모드에서 통계 출력
      if (SystemConstants.DEBUG_MODE) {
        LocationManager.printLocationStatistics();
        MonsterDataLoader.printMonsterStatistics();
      }

    } catch (Exception e) {
      logger.error("탐험 시스템 의존성 초기화 실패", e);
      throw new RuntimeException("탐험 시스템 초기화 실패", e);
    }
  }

  /**
   * 특정 지역에서 탐험을 진행합니다.
   */
  public ExploreResult exploreLocation(Player player, String locationId) {
    try {
      LocationData location = LocationManager.getLocation(locationId);
      if (location == null) {
        logger.warn("존재하지 않는 지역 ID: {}", locationId);
        return new ExploreResult(ExploreResult.ResultType.ERROR, "존재하지 않는 지역입니다.");
      }

      String locationName = location.getNameKo();
      System.out.println("\n📍 " + locationName + "에서 탐험을 진행합니다...");
      logger.info("지역별 탐험 시작: {} - {} ({})", player.getName(), locationName, locationId);

      // 현재 위치 설정
      gameState.setCurrentLocation(locationName);

      // 해당 지역의 몬스터 데이터 확인
      List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(locationId);

      if (locationMonsters.isEmpty()) {
        logger.warn("지역 {}({})에 몬스터 데이터가 없습니다. 랜덤 이벤트만 진행", locationName, locationId);
        return handleRandomEvent(player, location);
      }

      // 지역별 이벤트 확률 사용
      int eventChance = location.getEventChance();

      if (random.nextInt(100) < eventChance) {
        return handleRandomEvent(player, location);
      } else {
        return handleLocationMonsterEncounter(player, locationId, location);
      }

    } catch (Exception e) {
      logger.error("지역별 탐험 중 오류: {}", locationId, e);
      System.out.println("탐험 중 오류가 발생했습니다.");
      return new ExploreResult(ExploreResult.ResultType.ERROR, "탐험 중 오류 발생");
    }
  }

  /**
   * 랜덤 이벤트 처리 (LocationData 사용)
   */
  private ExploreResult handleRandomEvent(Player player, LocationData location) {
    String[] eventTypes = {"treasure", "knowledge", "merchant", "rest"};
    String eventType = eventTypes[random.nextInt(eventTypes.length)];

    return switch (eventType) {
      case "treasure" -> handleTreasureEvent(player, location);
      case "knowledge" -> handleKnowledgeEvent(player, location);
      case "merchant" -> handleMerchantEvent(player, location);
      case "rest" -> handleRestEvent(player, location);
      default -> new ExploreResult(ExploreResult.ResultType.ERROR, "알 수 없는 이벤트");
    };
  }

  /**
   * 보물 이벤트 처리
   */
  private ExploreResult handleTreasureEvent(Player player, LocationData location) {
    System.out.println("✨ " + location.getNameKo() + "에서 보물을 발견했습니다!");

    // 지역 특성에 따른 보물 등급 조정
    ItemRarity rarity = calculateTreasureRarity(location);
    GameItem treasure = GameItemFactory.getInstance().createRandomItemByRarity(rarity);

    if (treasure != null && inventoryController.addItem(player, treasure, 1)) {
      String message = "보물 발견! " + treasure.getName() + " 획득!";
      System.out.println("🎁 " + treasure.getName() + "을(를) 획득했습니다!");
      logger.info("보물 이벤트: {} -> {} ({})", player.getName(), treasure.getName(), location.getId());
      return new ExploreResult(ExploreResult.ResultType.TREASURE, message);
    } else {
      return new ExploreResult(ExploreResult.ResultType.TREASURE, "보물을 발견했지만 인벤토리가 가득 참!");
    }
  }

  /**
   * 지역 특성에 따른 보물 등급 계산
   */
  private ItemRarity calculateTreasureRarity(LocationData location) {
    return switch (location.getDangerLevel()) {
      case EASY -> ItemRarity.COMMON;
      case NORMAL -> random.nextInt(100) < 70 ? ItemRarity.COMMON : ItemRarity.UNCOMMON;
      case HARD -> random.nextInt(100) < 50 ? ItemRarity.UNCOMMON : ItemRarity.RARE;
      case VERY_HARD -> random.nextInt(100) < 40 ? ItemRarity.RARE : ItemRarity.EPIC;
      case EXTREME, NIGHTMARE -> random.nextInt(100) < 60 ? ItemRarity.EPIC : ItemRarity.LEGENDARY;
      case DIVINE, IMPOSSIBLE -> ItemRarity.LEGENDARY;
    };
  }

  /**
   * 지식 이벤트 처리
   */
  private ExploreResult handleKnowledgeEvent(Player player, LocationData location) {
    System.out.println("📚 " + location.getNameKo() + "에서 고대의 지식을 얻었습니다!");

    // 지역 레벨에 따른 경험치 계산
    int baseExp = location.getMinLevel() * 5;
    int bonusExp = random.nextInt(baseExp / 2 + 1);
    int totalExp = baseExp + bonusExp;

    player.gainExp(totalExp);
    String message = "지식 습득! 경험치 +" + totalExp;
    System.out.println("🧠 경험치를 " + totalExp + " 획득했습니다!");

    logger.info("지식 이벤트: {} -> EXP +{} ({})", player.getName(), totalExp, location.getId());
    return new ExploreResult(ExploreResult.ResultType.KNOWLEDGE, message);
  }

  /**
   * 상인 이벤트 처리
   */
  private ExploreResult handleMerchantEvent(Player player, LocationData location) {
    System.out.println("🧙‍♂️ " + location.getNameKo() + "에서 신비한 상인을 만났습니다!");

    // 지역 특성에 따른 상인 아이템
    GameItem merchantItem = generateMerchantItem(location);
    if (merchantItem != null) {
      System.out.println("💰 상인이 " + merchantItem.getName() + "을(를) 판매하고 있습니다!");
      // 실제 구매 로직은 별도 구현 필요
    }

    return new ExploreResult(ExploreResult.ResultType.MERCHANT, "신비한 상인과 만남!");
  }

  /**
   * 휴식 이벤트 처리
   */
  private ExploreResult handleRestEvent(Player player, LocationData location) {
    System.out.println("🏕️ " + location.getNameKo() + "에서 안전한 휴식처를 발견했습니다!");

    // 지역 특성에 따른 회복량 조정
    int healAmount = calculateRestHeal(location);
    int manaAmount = calculateRestMana(location);

    player.heal(healAmount);
    player.restoreMana(manaAmount);

    String message = String.format("휴식! HP +%d, MP +%d", healAmount, manaAmount);
    System.out.println("💤 체력과 마나가 회복되었습니다! (HP +" + healAmount + ", MP +" + manaAmount + ")");

    logger.info("휴식 이벤트: {} -> HP+{}, MP+{} ({})", player.getName(), healAmount, manaAmount, location.getId());
    return new ExploreResult(ExploreResult.ResultType.REST, message);
  }

  /**
   * 지역별 몬스터 조우 처리
   */
  private ExploreResult handleLocationMonsterEncounter(Player player, String locationId, LocationData location) {
    // 해당 지역의 몬스터 중에서 플레이어 레벨에 적합한 몬스터 선택
    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(locationId);
    List<MonsterData> suitableMonsters = locationMonsters.stream()
        .filter(monster -> player.getLevel() >= monster.getMinLevel() && player.getLevel() <= monster.getMaxLevel() + 2).collect(Collectors.toList());

    if (suitableMonsters.isEmpty()) {
      // 적합한 몬스터가 없으면 레벨 제한 완화
      suitableMonsters =
          locationMonsters.stream().filter(monster -> Math.abs(player.getLevel() - monster.getMinLevel()) <= 5).collect(Collectors.toList());
    }

    if (suitableMonsters.isEmpty()) {
      logger.warn("지역 {}에서 적합한 몬스터를 찾을 수 없음", locationId);
      return handleRandomEvent(player, location);
    }

    // 가중 랜덤 선택으로 몬스터 선택
    MonsterData selectedMonsterData = selectMonsterByWeight(suitableMonsters);
    Monster monster = Monster.fromMonsterData(selectedMonsterData);

    System.out.println("👹 " + monster.getName() + "을(를) 만났습니다!");

    // 전투 실행
    BattleEngine.BattleResult result = battleController.startBattle(player, monster);

    // 결과 처리
    return processBattleResult(result, player, monster, location);
  }

  /**
   * 전투 결과 처리
   */
  private ExploreResult processBattleResult(BattleEngine.BattleResult result, Player player, Monster monster, LocationData location) {
    String message = switch (result) {
      case VICTORY -> {
        // 퀘스트 진행도 업데이트
        questController.updateKillProgress(monster.getName());

        // 몬스터 드롭 아이템 처리
        GameItem droppedItem = handleMonsterDrops(monster);
        if (droppedItem != null && inventoryController.addItem(player, droppedItem, 1)) {
          System.out.println("🎁 " + droppedItem.getName() + "을(를) 획득했습니다!");
          yield "전투 승리! " + droppedItem.getName() + " 획득!";
        } else {
          yield "전투 승리!";
        }
      }
      case DEFEAT -> "전투 패배...";
      case ESCAPED -> "성공적으로 도망쳤습니다!";
      case ERROR -> "전투 중 오류 발생";
    };

    ExploreResult.ResultType resultType = switch (result) {
      case VICTORY -> ExploreResult.ResultType.BATTLE_VICTORY;
      case DEFEAT -> ExploreResult.ResultType.BATTLE_DEFEAT;
      case ESCAPED -> ExploreResult.ResultType.BATTLE_ESCAPED;
      case ERROR -> ExploreResult.ResultType.ERROR;
    };

    logger.debug("전투 결과: {} vs {} at {} ({})", player.getName(), monster.getName(), location.getNameKo(), result);
    return new ExploreResult(resultType, message);
  }

  /**
   * 가중치에 따른 몬스터 선택
   */
  private MonsterData selectMonsterByWeight(List<MonsterData> monsters) {
    double totalWeight = monsters.stream().mapToDouble(MonsterData::getSpawnRate).sum();
    double randomValue = random.nextDouble() * totalWeight;

    double currentWeight = 0;
    for (MonsterData monster : monsters) {
      currentWeight += monster.getSpawnRate();
      if (randomValue <= currentWeight) {
        return monster;
      }
    }

    // 폴백: 첫 번째 몬스터 반환
    return monsters.get(0);
  }

  /**
   * 몬스터 드롭 아이템 처리
   */
  private GameItem handleMonsterDrops(Monster monster) {
    if (monster.getMonsterData() == null || monster.getMonsterData().getRewards() == null
        || monster.getMonsterData().getRewards().getDropItems() == null) {
      return null;
    }

    var dropItems = monster.getMonsterData().getRewards().getDropItems();
    for (var dropItem : dropItems) {
      if (random.nextDouble() < dropItem.getDropRate()) {
        GameItemFactory factory = GameItemFactory.getInstance();
        return factory.createItem(dropItem.getItemId());
      }
    }

    return null;
  }

  /**
   * 지역 기반 상인 아이템 생성
   */
  private GameItem generateMerchantItem(LocationData location) {
    // 지역 특성에 따른 상인 아이템 로직
    return ItemDataLoader.generateSpecialMerchantItem();
  }

  /**
   * 지역별 휴식 회복량 계산
   */
  private int calculateRestHeal(LocationData location) {
    int baseHeal = 20;

    // 지역 특성에 따른 보너스
    Map<String, Object> properties = location.properties();
    if (properties.containsKey("healing") && (Boolean) properties.get("healing")) {
      baseHeal *= 1.5; // 치유 속성 지역에서 50% 보너스
    }
    if (properties.containsKey("shelter") && (Boolean) properties.get("shelter")) {
      baseHeal *= 1.2; // 은신처가 있는 지역에서 20% 보너스
    }

    return baseHeal + random.nextInt(baseHeal / 2);
  }

  /**
   * 지역별 휴식 마나 회복량 계산
   */
  private int calculateRestMana(LocationData location) {
    int baseMana = 15;

    // 지역 특성에 따른 보너스
    Map<String, Object> properties = location.properties();
    if (properties.containsKey("magical") && (Boolean) properties.get("magical")) {
      baseMana *= 1.5; // 마법 속성 지역에서 50% 보너스
    }

    return baseMana + random.nextInt(baseMana / 2);
  }

  /**
   * 현재 위치의 몬스터 정보 표시
   */
  public void showCurrentLocationMonsters(int playerLevel) {
    String currentLocation = gameState.getCurrentLocation();
    String locationId = LocationManager.getLocationIdByKoreanName(currentLocation);

    if (locationId == null) {
      System.out.println("❌ 현재 위치의 몬스터 정보를 찾을 수 없습니다.");
      return;
    }

    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(locationId);

    if (locationMonsters.isEmpty()) {
      System.out.println("🕊️ 이 지역에는 몬스터가 서식하지 않습니다.");
      return;
    }

    System.out.println("\n👹 === " + currentLocation + " 서식 몬스터 ===");

    for (MonsterData monster : locationMonsters) {
      String difficulty = calculateDifficulty(monster, playerLevel);
      System.out.printf("• %s (레벨 %d-%d) - %s%n", monster.getName(), monster.getMinLevel(), monster.getMaxLevel(), difficulty);
    }
  }

  /**
   * 몬스터 난이도 계산
   */
  private String calculateDifficulty(MonsterData monster, int playerLevel) {
    int avgMonsterLevel = (monster.getMinLevel() + monster.getMaxLevel()) / 2;
    int levelDiff = avgMonsterLevel - playerLevel;

    if (levelDiff <= -3)
      return "매우 쉬움";
    else if (levelDiff <= -1)
      return "쉬움";
    else if (levelDiff <= 1)
      return "적정";
    else if (levelDiff <= 3)
      return "어려움";
    else
      return "매우 어려움";
  }

  /**
   * 랜덤 이벤트 타입 열거형
   */
  private enum RandomEventType {
    TREASURE_FOUND, // 보물 발견
    HEALING_SPRING, // 치유의 샘
    ANCIENT_KNOWLEDGE, // 고대 지식
    MAGIC_CRYSTAL, // 마법 크리스탈
    MERCHANT_ENCOUNTER, // 상인 조우
    MYSTERIOUS_SHRINE // 신비한 제단
  }

  /**
   * 제단 축복 타입 열거형
   */
  private enum ShrineBlessing {
    STRENGTH, // 힘의 축복
    VITALITY, // 생명력의 축복
    WISDOM, // 지혜의 축복
    FORTUNE // 행운의 축복
  }

}
