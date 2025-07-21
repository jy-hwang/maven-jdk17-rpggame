package rpg.core.exploration;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
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

/**
 * 리팩토링된 탐험 시스템 엔진
 * - LocationManager 기반으로 동적 지역 관리
 * - RandomEventType enum 제거하고 직접 처리
 * - ExploreResult enum 활용
 */
public class ExploreEngine {
  private static final Logger logger = LoggerFactory.getLogger(ExploreEngine.class);

  private final Map<ExploreResult, BiFunction<Player, LocationData, ExploreResultData>> eventHandlers;

  private final Random random;
  private final BattleEngine battleController;
  private final QuestController questController;
  private final InventoryController inventoryController;
  private final GameState gameState;
  // 이벤트 확률 가중치 (필요시 설정 파일로 분리 가능)
  private static final ExploreResult[] EVENT_POOL = {
      ExploreResult.TREASURE, 
      ExploreResult.KNOWLEDGE,
      ExploreResult.REST, 
      ExploreResult.HEALING_SPRING, 
      ExploreResult.MAGIC_CRYSTAL, 
      ExploreResult.SHRINE_BLESSING
  };

  public ExploreEngine(BattleEngine battleController, QuestController questController, InventoryController inventoryController, GameState gameState) {
    this.random = new Random();
    this.battleController = battleController;
    this.questController = questController;
    this.inventoryController = inventoryController;
    this.gameState = gameState;

    // 이벤트 핸들러 초기화 - 메서드 레퍼런스 사용
    this.eventHandlers = Map.of(ExploreResult.TREASURE, this::handleTreasureEvent, ExploreResult.KNOWLEDGE, this::handleKnowledgeEvent,
        ExploreResult.REST, this::handleRestEvent, ExploreResult.HEALING_SPRING, this::handleHealingSpringEvent, ExploreResult.MAGIC_CRYSTAL,
        this::handleMagicCrystalEvent, ExploreResult.SHRINE_BLESSING, this::handleShrineEvent);

    initializeDependencies();
    logger.info("ExploreEngine 초기화 완료 (LocationManager 기반)");
  }

  /**
   * 의존성 시스템들 초기화
   */
  private void initializeDependencies() {
    try {
      LocationManager.initialize();
      MonsterDataLoader.loadAllMonsters();
      logger.info("탐험 시스템 의존성 초기화 완료");
    } catch (Exception e) {
      logger.error("탐험 시스템 의존성 초기화 실패", e);
      throw new RuntimeException("탐험 시스템 초기화 실패", e);
    }
  }

  /**
   * 특정 지역에서 탐험을 진행합니다.
   */
  public ExploreResultData exploreLocation(Player player, String locationId) {
    try {
      LocationData location = LocationManager.getLocation(locationId);
      if (location == null) {
        logger.warn("존재하지 않는 지역 ID: {}", locationId);
        return new ExploreResultData(ExploreResult.ERROR, "존재하지 않는 지역입니다.");
      }

      String locationName = location.getNameKo();
      System.out.println("\n📍 " + locationName + "에서 탐험을 진행합니다...");
      logger.info("지역별 탐험 시작: {} - {} ({})", player.getName(), locationName, locationId);

      gameState.setCurrentLocation(locationName);

      List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(locationId);

      if (locationMonsters.isEmpty()) {
        logger.warn("지역 {}({})에 몬스터 데이터가 없습니다. 랜덤 이벤트만 진행", locationName, locationId);
        return handleRandomEvent(player, location);
      }

      int eventChance = location.getEventChance();

      if (random.nextInt(100) < eventChance) {
        return handleRandomEvent(player, location);
      } else {
        return handleLocationMonsterEncounter(player, locationId, location);
      }

    } catch (Exception e) {
      logger.error("지역별 탐험 중 오류: {}", locationId, e);
      System.out.println("탐험 중 오류가 발생했습니다.");
      return new ExploreResultData(ExploreResult.ERROR, "탐험 중 오류 발생");
    }
  }

  /**
   *   대폭 간소화된 랜덤 이벤트 처리
   * - switch문 제거
   * - Map 기반 Strategy 패턴 사용
   * - 한 줄로 이벤트 실행
   */
  private ExploreResultData handleRandomEvent(Player player, LocationData location) {
    ExploreResult eventType = EVENT_POOL[random.nextInt(EVENT_POOL.length)];
    return eventHandlers.get(eventType).apply(player, location);
  }

  // === 개별 이벤트 핸들러들 (기존과 동일하지만 메서드 시그니처 통일) ===

  private ExploreResultData handleTreasureEvent(Player player, LocationData location) {
    System.out.println("✨ " + location.getNameKo() + "에서 보물을 발견했습니다!");

    ItemRarity rarity = calculateTreasureRarity(location);
    GameItem treasure = GameItemFactory.getInstance().createRandomItemByRarity(rarity);

    if (treasure != null && inventoryController.addItem(player, treasure, 1)) {
      questController.updateCollectionProgress(player, treasure.getId(), 1);
      System.out.println("🎁 " + treasure.getName() + "을(를) 획득했습니다!");

      String message = "보물 발견! " + treasure.getName() + " 획득!";
      logger.info("보물 이벤트: {} -> {} ({})", player.getName(), treasure.getName(), treasure.getId());
      return new ExploreResultData(ExploreResult.TREASURE, message);
    } else {
      return new ExploreResultData(ExploreResult.TREASURE, "보물을 발견했지만 인벤토리가 가득 참!");
    }
  }

  private ExploreResultData handleKnowledgeEvent(Player player, LocationData location) {
    System.out.println("📚 " + location.getNameKo() + "에서 고대의 지식을 얻었습니다!");

    int totalExp = calculateExpGain(location);
    player.gainExp(totalExp);

    System.out.println("🧠 경험치를 " + totalExp + " 획득했습니다!");
    logger.info("지식 이벤트: {} -> EXP +{} ({})", player.getName(), totalExp, location.getId());

    return new ExploreResultData(ExploreResult.KNOWLEDGE, "지식 습득! 경험치 +" + totalExp);
  }

  private ExploreResultData handleRestEvent(Player player, LocationData location) {
    System.out.println("🏕️ " + location.getNameKo() + "에서 안전한 휴식처를 발견했습니다!");

    int[] recovery = calculateRestRecovery(location);
    int healAmount = recovery[0], manaAmount = recovery[1];

    player.heal(healAmount);
    player.restoreMana(manaAmount);

    System.out.println("💤 체력과 마나가 회복되었습니다! (HP +" + healAmount + ", MP +" + manaAmount + ")");
    logger.info("휴식 이벤트: {} -> HP+{}, MP+{} ({})", player.getName(), healAmount, manaAmount, location.getId());

    return new ExploreResultData(ExploreResult.REST, String.format("휴식! HP +%d, MP +%d", healAmount, manaAmount));
  }

  private ExploreResultData handleHealingSpringEvent(Player player, LocationData location) {
    System.out.println("💧 " + location.getNameKo() + "에서 신비한 치유의 샘을 발견했습니다!");

    int healAmount = player.getMaxHp() - player.getHp();
    player.heal(healAmount);

    System.out.println("✨ 체력이 완전히 회복되었습니다!");
    logger.info("치유의 샘 이벤트: {} -> HP 완전 회복 ({})", player.getName(), location.getId());

    return new ExploreResultData(ExploreResult.HEALING_SPRING, "치유의 샘! 체력 완전 회복!");
  }

  private ExploreResultData handleMagicCrystalEvent(Player player, LocationData location) {
    System.out.println("💎 " + location.getNameKo() + "에서 빛나는 마법 크리스탈을 발견했습니다!");

    int manaAmount = player.getMaxMana() - player.getMana();
    player.restoreMana(manaAmount);

    System.out.println("✨ 마나가 완전히 회복되었습니다!");
    logger.info("마법 크리스탈 이벤트: {} -> MP 완전 회복 ({})", player.getName(), location.getId());

    return new ExploreResultData(ExploreResult.MAGIC_CRYSTAL, "마법 크리스탈! 마나 완전 회복!");
  }

  private ExploreResultData handleShrineEvent(Player player, LocationData location) {
    System.out.println("⛩️ " + location.getNameKo() + "에서 신비한 제단을 발견했습니다!");

    // 축복 처리를 더 간단하게
    return processShrineBlessing(player, location);
  }

  private int calculateExpGain(LocationData location) {
    int baseExp = location.getMinLevel() * 5;
    int bonusExp = random.nextInt(baseExp / 2 + 1);
    return baseExp + bonusExp;
  }

  private int[] calculateRestRecovery(LocationData location) {
    int baseHeal = 20;
    int baseMana = 15;

    Map<String, Object> properties = location.properties();
    if (properties.containsKey("healing") && (Boolean) properties.get("healing")) {
      baseHeal *= 1.5;
    }
    if (properties.containsKey("shelter") && (Boolean) properties.get("shelter")) {
      baseHeal *= 1.2;
    }
    if (properties.containsKey("magical") && (Boolean) properties.get("magical")) {
      baseMana *= 1.5;
    }

    return new int[] {baseHeal + random.nextInt(baseHeal / 2), baseMana + random.nextInt(baseMana / 2)};
  }

  private ExploreResultData processShrineBlessing(Player player, LocationData location) {
    String[] blessings = {"strength", "vitality", "wisdom", "fortune"};
    String blessing = blessings[random.nextInt(blessings.length)];

    String message = switch (blessing) {
      case "strength" -> {
        System.out.println("⚔️ 힘의 축복을 받았습니다! 공격력이 일시적으로 증가합니다!");
        yield "힘의 축복! 공격력 증가!";
      }
      case "vitality" -> {
        int healAmount = player.getMaxHp() / 2;
        player.heal(healAmount);
        System.out.println("❤️ 생명력의 축복을 받았습니다! 체력이 회복됩니다!");
        yield "생명력의 축복! 체력 회복!";
      }
      case "wisdom" -> {
        int expGain = player.getLevel() * 10;
        player.gainExp(expGain);
        System.out.println("🧠 지혜의 축복을 받았습니다! 경험치를 획득합니다!");
        yield "지혜의 축복! 경험치 +" + expGain;
      }
      case "fortune" -> {
        System.out.println("🍀 행운의 축복을 받았습니다! 운이 일시적으로 증가합니다!");
        yield "행운의 축복! 운 증가!";
      }
      default -> "알 수 없는 축복";
    };

    logger.info("제단 축복 이벤트: {} -> {} ({})", player.getName(), blessing, location.getId());
    return new ExploreResultData(ExploreResult.SHRINE_BLESSING, message);
  }

  /**
   * 지역별 몬스터 조우 처리
   */
  private ExploreResultData handleLocationMonsterEncounter(Player player, String locationId, LocationData location) {
    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(locationId);
    List<MonsterData> suitableMonsters = locationMonsters.stream()
        .filter(monster -> player.getLevel() >= monster.getMinLevel() && player.getLevel() <= monster.getMaxLevel() + 2).collect(Collectors.toList());

    if (suitableMonsters.isEmpty()) {
      suitableMonsters =
          locationMonsters.stream().filter(monster -> Math.abs(player.getLevel() - monster.getMinLevel()) <= 5).collect(Collectors.toList());
    }

    if (suitableMonsters.isEmpty()) {
      logger.warn("지역 {}에서 적합한 몬스터를 찾을 수 없음", locationId);
      return handleRandomEvent(player, location);
    }

    MonsterData selectedMonsterData = selectMonsterByWeight(suitableMonsters);
    Monster monster = Monster.fromMonsterData(selectedMonsterData);

    System.out.println("👹 " + monster.getName() + "을(를) 만났습니다!");

    BattleEngine.BattleResult result = battleController.startBattle(player, monster);

    return processBattleResult(result, player, monster, location);
  }

  /**
   * 전투 결과 처리
   */
  private ExploreResultData processBattleResult(BattleEngine.BattleResult result, Player player, Monster monster, LocationData location) {
    String message = switch (result) {
      case VICTORY -> {
        String monsterId = monster.getId();
        logger.debug("몬스터 처치: {} ({}) -> 퀘스트 진행도 업데이트", monster.getName(), monsterId);

        questController.updateKillProgress(monsterId);

        GameItem droppedItem = handleMonsterDrops(monster);
        if (droppedItem != null && inventoryController.addItem(player, droppedItem, 1)) {
          String itemId = droppedItem.getId();
          questController.updateCollectionProgress(player, itemId, 1);

          System.out.println("🎁 " + droppedItem.getName() + "을(를) 획득했습니다!");
          logger.debug("아이템 획득: {} ({}) -> 퀘스트 진행도 업데이트", droppedItem.getName(), itemId);

          yield "전투 승리! " + droppedItem.getName() + " 획득!";
        } else {
          yield "전투 승리!";
        }
      }
      case DEFEAT -> "전투 패배...";
      case ESCAPED -> "성공적으로 도망쳤습니다!";
      case ERROR -> "전투 중 오류 발생";
    };

    ExploreResult resultType = switch (result) {
      case VICTORY -> ExploreResult.BATTLE_VICTORY;
      case DEFEAT -> ExploreResult.BATTLE_DEFEAT;
      case ESCAPED -> ExploreResult.BATTLE_ESCAPED;
      case ERROR -> ExploreResult.ERROR;
    };

    logger.debug("전투 결과: {} vs {} at {} ({})", player.getName(), monster.getName(), location.getNameKo(), result);
    return new ExploreResultData(resultType, message);
  }

  // === 유틸리티 메서드들 ===

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

    return monsters.get(0);
  }

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

  private GameItem generateMerchantItem(LocationData location) {
    return ItemDataLoader.generateSpecialMerchantItem();
  }

  private int calculateRestHeal(LocationData location) {
    int baseHeal = 20;

    Map<String, Object> properties = location.properties();
    if (properties.containsKey("healing") && (Boolean) properties.get("healing")) {
      baseHeal *= 1.5;
    }
    if (properties.containsKey("shelter") && (Boolean) properties.get("shelter")) {
      baseHeal *= 1.2;
    }

    return baseHeal + random.nextInt(baseHeal / 2);
  }

  private int calculateRestMana(LocationData location) {
    int baseMana = 15;

    Map<String, Object> properties = location.properties();
    if (properties.containsKey("magical") && (Boolean) properties.get("magical")) {
      baseMana *= 1.5;
    }

    return baseMana + random.nextInt(baseMana / 2);
  }

  // === 정보 조회 메서드들 ===

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
}
