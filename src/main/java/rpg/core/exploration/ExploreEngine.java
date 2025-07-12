package rpg.core.exploration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.core.battle.BattleEngine;
import rpg.core.engine.GameState;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameEquipment;
import rpg.domain.item.GameItem;
import rpg.domain.item.ItemRarity;
import rpg.domain.monster.Monster;
import rpg.domain.monster.MonsterData;
import rpg.domain.player.Player;
import rpg.infrastructure.data.loader.ConfigDataLoader;
import rpg.infrastructure.data.loader.ItemDataLoader;
import rpg.infrastructure.data.loader.MonsterDataLoader;
import rpg.presentation.controller.InventoryController;
import rpg.presentation.controller.QuestController;
import rpg.shared.constant.BattleConstants;
import rpg.shared.constant.GameConstants;

/**
 * 탐험 시스템을 전담하는 컨트롤러
 */
public class ExploreEngine {
  private static final Logger logger = LoggerFactory.getLogger(ExploreEngine.class);

  private final Random random;
  private final List<Monster> monsterTemplates;
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
    this.monsterTemplates = new ArrayList<>();

    // JSON에서 몬스터 데이터 로드
    initializeMonsterData();
    logger.debug("ExploreController 초기화 완료 (JSON 기반)");
  }

  /**
   * JSON에서 몬스터 데이터를 초기화합니다.
   */
  private void initializeMonsterData() {
    try {
      // MonsterDataLoader를 통해 JSON 데이터 로드
      var monsterData = MonsterDataLoader.loadAllMonsters();

      logger.info("JSON에서 몬스터 데이터 로드 완료: {}종류", monsterData.size());

      // 개발 모드에서 몬스터 통계 출력
      if (logger.isDebugEnabled()) {
        MonsterDataLoader.printMonsterStatistics();
      }

    } catch (Exception e) {
      logger.error("몬스터 데이터 초기화 실패", e);
      logger.warn("기본 몬스터 데이터로 대체됩니다.");
    }
  }

  /**
   * 탐험을 시작합니다.
   */
  public ExploreResult startExploration(Player player) {
    try {
      System.out.println("\n🌲 탐험을 시작합니다...");
      logger.info("탐험 시작: {}", player.getName());

      // 현재 위치 업데이트
      updatePlayerLocation(player);

      // 랜덤 이벤트 또는 몬스터 조우
      if (random.nextInt(GameConstants.NUMBER_HUNDRED) < BattleConstants.RANDOM_EVENT_CHANCE) {
        return handleRandomEvent(player);
      } else {
        return handleMonsterEncounter(player);
      }

    } catch (Exception e) {
      logger.error("탐험 중 오류", e);
      System.out.println("탐험 중 오류가 발생했습니다.");
      return new ExploreResult(ExploreResult.ResultType.ERROR, "탐험 중 오류 발생");
    }
  }

  /**
   * 플레이어의 현재 위치를 업데이트합니다.
   */
  private void updatePlayerLocation(Player player) {
    String[] locations = {"숲속 깊은 곳", "고대 유적", "어두운 동굴", "험준한 산길", "신비한 호수", "폐허가 된 성", "마법의 숲", "용암 동굴"};

    // 플레이어 레벨에 따른 지역 가중치 적용
    String newLocation = getLocationByLevel(player.getLevel(), locations);
    gameState.setCurrentLocation(newLocation);

    System.out.println("📍 현재 위치: " + newLocation);
    showLocationDescription(newLocation);

    // 현재 위치의 몬스터 정보 표시 (옵션)
    if (random.nextInt(100) < 30) { // 30% 확률로 몬스터 정보 힌트
      showLocationMonsterHint(newLocation, player.getLevel());
    }
  }

  /**
   * 플레이어 레벨에 따라 적절한 지역을 선택합니다.
   */
  private String getLocationByLevel(int level, String[] locations) {
    if (level <= 3) {
      // 초보자는 숲속 깊은 곳 가능성 높음
      return random.nextInt(100) < 70 ? "숲속 깊은 곳" : locations[random.nextInt(3)];
    } else if (level <= 6) {
      // 중급자는 다양한 지역 가능
      String[] midLevelLocations = {"숲속 깊은 곳", "어두운 동굴", "험준한 산길", "마법의 숲"};
      return midLevelLocations[random.nextInt(midLevelLocations.length)];
    } else if (level <= 10) {
      // 고급자는 위험한 지역 포함
      String[] highLevelLocations = {"폐허가 된 성", "신비한 호수", "고대 유적", "마법의 숲"};
      return highLevelLocations[random.nextInt(highLevelLocations.length)];
    } else {
      // 최고급자는 모든 지역 가능, 용암 동굴 가능성 높음
      return random.nextInt(100) < 40 ? "용암 동굴" : locations[random.nextInt(locations.length)];
    }
  }

  /**
   * 지역별 설명을 표시합니다.
   */
  private void showLocationDescription(String location) {
    String description = switch (location) {
      case "숲속 깊은 곳" -> "🌲 울창한 숲에서 작은 소리들이 들려옵니다. 초보자에게 적합한 곳입니다.";
      case "어두운 동굴" -> "🕳️ 어둠이 깊게 드리워진 동굴입니다. 위험하지만 보물이 있을 수 있습니다.";
      case "험준한 산길" -> "⛰️ 험준한 산길이 이어집니다. 강한 몬스터들이 서식하고 있습니다.";
      case "신비한 호수" -> "🏞️ 신비로운 기운이 감도는 호수입니다. 물속에서 무언가가 움직입니다.";
      case "폐허가 된 성" -> "🏰 오래된 성의 폐허입니다. 망령들의 기운이 느껴집니다.";
      case "마법의 숲" -> "🌟 마법의 기운이 흐르는 숲입니다. 신비한 존재들이 살고 있습니다.";
      case "용암 동굴" -> "🌋 뜨거운 용암이 흐르는 위험한 동굴입니다. 최고 수준의 위험 지역입니다.";
      case "고대 유적" -> "🏛️ 고대 문명의 유적입니다. 시간을 초월한 강력한 존재들이 지키고 있습니다.";
      default -> "🗺️ 알 수 없는 지역입니다.";
    };
    System.out.println(description);
  }

  /**
   * 현재 위치의 몬스터 힌트를 표시합니다.
   */
  private void showLocationMonsterHint(String location, int playerLevel) {
    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(location);

    if (locationMonsters.isEmpty()) {
      return;
    }

    // 플레이어 레벨에 적합한 몬스터만 필터링
    List<MonsterData> suitableMonsters = locationMonsters.stream()
        .filter(monster -> playerLevel >= monster.getMinLevel() && playerLevel <= monster.getMaxLevel()).collect(Collectors.toList());

    if (!suitableMonsters.isEmpty()) {
      MonsterData hintMonster = suitableMonsters.get(random.nextInt(suitableMonsters.size()));
      String difficulty = getDifficultyString(estimateMonsterLevel(hintMonster), playerLevel);

      System.out.println("👀 이 지역에서 " + hintMonster.getName() + "의 흔적이 보입니다. " + difficulty);
    }
  }

  /**
   * 몬스터 조우를 처리합니다.
   */
  private ExploreResult handleMonsterEncounter(Player player) {
    Monster monster = getRandomMonster(player.getLevel());
    System.out.println("👹 " + monster.getName() + "을(를) 만났습니다!");

    BattleEngine.BattleResult result = battleController.startBattle(player, monster);

    String message = switch (result) {
      case VICTORY -> {
        // 퀘스트 진행도 업데이트
        questController.updateKillProgress(monster.getName());

        // JSON 기반 드롭 아이템 처리
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

    logger.debug("몬스터 조우: {} vs {} (결과: {})", player.getName(), monster.getName(), result);
    return new ExploreResult(resultType, message);
  }

  /**
   * JSON 데이터를 기반으로 몬스터 드롭을 처리합니다.
   */
  private GameItem handleMonsterDrops(Monster monster) {
    // 몬스터 이름으로 MonsterData 찾기
    var monsterDataMap = MonsterDataLoader.loadAllMonsters();
    MonsterData monsterData = monsterDataMap.values().stream().filter(data -> data.getName().equals(monster.getName())).findFirst().orElse(null);

    if (monsterData == null || monsterData.getRewards().getDropItems().isEmpty()) {
      // 기본 드롭 아이템 (레거시)
      return generateRandomDropItem();
    }

    // JSON에 정의된 드롭 아이템 처리
    for (var dropItem : monsterData.getRewards().getDropItems()) {
      if (random.nextDouble() < dropItem.getDropRate()) {
        // 드롭 성공! 아이템 생성
        int quantity = random.nextInt(dropItem.getMaxQuantity() - dropItem.getMinQuantity() + 1) + dropItem.getMinQuantity();

        // 실제 게임에서는 ItemFactory에서 itemId로 아이템 생성
        return createDropItem(dropItem.getItemId(), quantity);
      }
    }

    return null; // 드롭 실패
  }

  /**
   * 드롭 아이템 ID를 기반으로 아이템을 생성합니다.
   */
  private GameItem createDropItem(String itemId, int quantity) {
    // 실제 구현에서는 ItemFactory나 GameDataLoader 사용
    //@formatter:off
    return switch (itemId) {
      case "SLIME_GEL" -> GameItemFactory.getInstance().createItem("SLIME_GEL");
      case "GOBLIN_EAR" -> new GameConsumable("GOBLIN_EAR", "고블린 귀", "고블린의 귀", 20, ItemRarity.COMMON, 0, 0, 0, true);
      case "WOLF_PELT" -> new GameConsumable("WOLF_PELT", "늑대 가죽", "부드러운 늑대 가죽", 30, ItemRarity.COMMON, 0, 0, 0, true);
      case "NATURE_ESSENCE" -> new GameConsumable("NATURE_ESSENCE", "자연의 정수", "자연의 마력이 담긴 정수", 50, ItemRarity.UNCOMMON, 0, 30, 0, true);
      case "DRAGON_SCALE" -> new GameConsumable("DRAGON_SCALE", "드래곤 비늘", "전설적인 드래곤의 비늘", 200, ItemRarity.LEGENDARY, 0, 0, 0, true);
      default -> generateRandomDropItem(); // 기본 아이템
    };
  //@formatter:on
  }

  /**
   * 현재 위치에 따라 적절한 랜덤 몬스터를 생성합니다.
   */
  public Monster getRandomMonster(int playerLevel) {
    String currentLocation = gameState.getCurrentLocation();

    // JSON에서 해당 지역과 레벨에 맞는 몬스터 가져오기
    List<MonsterData> suitableMonsters = MonsterDataLoader.getMonstersByLocationAndLevel(currentLocation, playerLevel);

    // 적합한 몬스터가 없으면 레벨만 고려
    if (suitableMonsters.isEmpty()) {
      suitableMonsters = MonsterDataLoader.getMonstersByLevel(playerLevel);
    }

    // 그래도 없으면 전체 몬스터에서 선택
    if (suitableMonsters.isEmpty()) {
      var allMonsters = MonsterDataLoader.loadAllMonsters();
      suitableMonsters = new ArrayList<>(allMonsters.values());
    }

    // 출현 확률을 고려한 몬스터 선택
    MonsterData selectedData = selectMonsterBySpawnRate(suitableMonsters);

    // MonsterData를 Monster 객체로 변환
    Monster monster = convertToMonster(selectedData);

    logger.debug("몬스터 생성: {} (위치: {}, 플레이어 레벨: {}, 출현율: {})", monster.getName(), currentLocation, playerLevel, selectedData.getSpawnRate());

    return monster;
  }

  /**
   * 출현 확률을 고려하여 몬스터를 선택합니다.
   */
  private MonsterData selectMonsterBySpawnRate(List<MonsterData> monsters) {
    // 가중치가 있는 랜덤 선택
    double totalWeight = monsters.stream().mapToDouble(MonsterData::getSpawnRate).sum();
    double randomValue = random.nextDouble() * totalWeight;

    double currentWeight = 0;
    for (MonsterData monster : monsters) {
      currentWeight += monster.getSpawnRate();
      if (randomValue <= currentWeight) {
        return monster;
      }
    }

    // 기본값으로 첫 번째 몬스터 반환
    return monsters.get(0);
  }

  /**
   * MonsterData를 Monster 객체로 변환합니다.
   */
  private Monster convertToMonster(MonsterData data) {
    // 새로운 팩토리 메서드 사용 (JSON 데이터 포함)
    return Monster.fromMonsterData(data);
  }

  /**
   * 몬스터의 추정 레벨을 계산합니다.
   */
  private int estimateMonsterLevel(MonsterData monsterData) {
    return Math.max(1, (monsterData.getStats().getHp() + monsterData.getStats().getAttack() * 2) / 15);
  }

  /**
   * 몬스터 난이도를 문자열로 반환합니다.
   */
  private String getDifficultyString(int monsterLevel, int playerLevel) {
    int diff = monsterLevel - playerLevel;
    if (diff <= -3)
      return "😴 (매우 쉬움)";
    if (diff <= -1)
      return "😊 (쉬움)";
    if (diff <= 1)
      return "😐 (보통)";
    if (diff <= 3)
      return "😰 (어려움)";
    return "💀 (매우 어려움)";
  }

  /**
   * 현재 위치의 몬스터 정보를 표시합니다.
   */
  public void showCurrentLocationMonsters(int playerLevel) {
    String currentLocation = gameState.getCurrentLocation();
    List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(currentLocation);

    if (locationMonsters.isEmpty()) {
      System.out.println("이 지역에는 특별한 몬스터가 없습니다.");
      return;
    }

    System.out.println("\n🏞️ " + currentLocation + "의 몬스터들:");
    for (MonsterData monster : locationMonsters) {
      int level = estimateMonsterLevel(monster);
      String difficulty = getDifficultyString(level, playerLevel);
      String rarity = getRarityIcon(monster.getRarity());

      System.out.printf("   %s %s %s (레벨 %d, 출현율: %.0f%%)%n", rarity, monster.getName(), difficulty, level, monster.getSpawnRate() * 100);

      if (!monster.getAbilities().isEmpty()) {
        System.out.printf("      💫 특수능력: %s%n", String.join(", ", monster.getAbilities()));
      }
    }
  }

  /**
   * 등급에 따른 아이콘을 반환합니다.
   */
  private String getRarityIcon(String rarity) {
    return switch (rarity.toUpperCase()) {
      case "COMMON" -> "⚪";
      case "UNCOMMON" -> "🟢";
      case "RARE" -> "🔵";
      case "EPIC" -> "🟣";
      case "LEGENDARY" -> "🟡";
      default -> "❓";
    };
  }


  /**
   * 몬스터 및 아이템 데이터를 다시 로드합니다. (개발/디버그용)
   */
  public void reloadAllData() {
    logger.info("전체 게임 데이터 리로드 중...");

    // 몬스터 데이터 리로드
    MonsterDataLoader.reloadMonsterData();

    // 아이템 데이터 리로드
    ConfigDataLoader.reloadGameData();

    System.out.println("몬스터 및 아이템 데이터가 다시 로드되었습니다!");
    logger.info("전체 게임 데이터 리로드 완료");
  }

  /**
   * 몬스터 데이터를 다시 로드합니다. (개발/디버그용)
   */
  public void reloadMonsterData() {
    logger.info("몬스터 데이터 리로드 중...");
    MonsterDataLoader.reloadMonsterData();
    System.out.println("몬스터 데이터가 다시 로드되었습니다!");
  }


  /**
   * 랜덤 이벤트를 처리합니다.
   */
  private ExploreResult handleRandomEvent(Player player) {
    RandomEventType eventType = RandomEventType.values()[random.nextInt(RandomEventType.values().length)];

    return switch (eventType) {
      case TREASURE_FOUND -> handleTreasureEvent(player);
      case HEALING_SPRING -> handleHealingEvent(player);
      case ANCIENT_KNOWLEDGE -> handleKnowledgeEvent(player);
      case MAGIC_CRYSTAL -> handleManaEvent(player);
      case MERCHANT_ENCOUNTER -> handleMerchantEvent(player);
      case MYSTERIOUS_SHRINE -> handleShrineEvent(player);
    };
  }

  /**
   * 보물 발견 이벤트를 처리합니다.
   */
  private ExploreResult handleTreasureEvent(Player player) {
    int foundGold = random.nextInt(50) + 20;
    player.setGold(player.getGold() + foundGold);

    String message = "💰 보물 상자를 발견했습니다! " + foundGold + " 골드를 획득했습니다!";
    System.out.println(message);

    // 추가로 아이템도 발견할 수 있음 (30% 확률)
    if (random.nextInt(100) < 30) {
      GameItem treasureItem = generateRandomTreasureItem();
      if (inventoryController.addItem(player, treasureItem, 1)) {
        message += "\n🎁 추가로 " + treasureItem.getName() + "을(를) 발견했습니다!";
        System.out.println("🎁 추가로 " + treasureItem.getName() + "을(를) 발견했습니다!");
      }
    }

    logger.debug("보물 이벤트: {} (골드: {})", player.getName(), foundGold);
    return new ExploreResult(ExploreResult.ResultType.TREASURE, message);
  }

  /**
   * 치유의 샘 이벤트를 처리합니다.
   */
  private ExploreResult handleHealingEvent(Player player) {
    int healAmount = random.nextInt(40) + 30;
    int oldHp = player.getHp();
    player.heal(healAmount);
    int actualHeal = player.getHp() - oldHp;

    String message = "💚 신비한 치유의 샘을 발견했습니다! " + actualHeal + " 체력을 회복했습니다!";
    System.out.println(message);

    logger.debug("치유 이벤트: {} (회복량: {})", player.getName(), actualHeal);
    return new ExploreResult(ExploreResult.ResultType.HEALING, message);
  }

  /**
   * 고대 지식 이벤트를 처리합니다.
   */
  private ExploreResult handleKnowledgeEvent(Player player) {
    int expAmount = random.nextInt(30) + 15;
    boolean levelUp = player.gainExp(expAmount);

    String message = "📚 고대 문서를 발견했습니다! " + expAmount + " 경험치를 획득했습니다!";
    System.out.println(message);

    if (levelUp) {
      message += "\n🎉 깨달음을 얻어 레벨이 올랐습니다!";
      System.out.println("🎉 깨달음을 얻어 레벨이 올랐습니다!");
      questController.updateLevelProgress(player);
    }

    logger.debug("지식 이벤트: {} (경험치: {}, 레벨업: {})", player.getName(), expAmount, levelUp);
    return new ExploreResult(ExploreResult.ResultType.KNOWLEDGE, message);
  }

  /**
   * 마법 크리스탈 이벤트를 처리합니다.
   */
  private ExploreResult handleManaEvent(Player player) {
    int manaAmount = random.nextInt(25) + 20;
    int oldMana = player.getMana();
    player.restoreMana(manaAmount);
    int actualRestore = player.getMana() - oldMana;

    String message = "✨ 마법의 크리스탈을 발견했습니다! " + actualRestore + " 마나를 회복했습니다!";
    System.out.println(message);

    logger.debug("마나 이벤트: {} (회복량: {})", player.getName(), actualRestore);
    return new ExploreResult(ExploreResult.ResultType.MANA_RESTORE, message);
  }

  /**
   * 떠돌이 상인 이벤트를 처리합니다.
   */
  private ExploreResult handleMerchantEvent(Player player) {
    String message = "🧙‍♂️ 떠돌이 상인을 만났습니다! 특별한 거래를 제안합니다.";
    System.out.println(message);

    // 골드로 특별한 아이템 구매 기회
    if (player.getGold() >= 50) {
      GameItem specialItem = generateSpecialMerchantItem();
      System.out.println("🛍️ " + specialItem.getName() + "을(를) 50골드에 판매합니다.");
      System.out.println("💰 현재 골드: " + player.getGold());

      // 간단한 자동 구매 로직 (나중에 선택지로 확장 가능)
      if (random.nextBoolean() && inventoryController.addItem(player, specialItem, 1)) {
        player.setGold(player.getGold() - 50);
        message += "\n🎁 " + specialItem.getName() + "을(를) 구매했습니다!";
        System.out.println("🎁 " + specialItem.getName() + "을(를) 구매했습니다!");
      } else {
        message += "\n💭 이번에는 거래하지 않기로 했습니다.";
        System.out.println("💭 이번에는 거래하지 않기로 했습니다.");
      }
    } else {
      message += "\n💸 골드가 부족해 거래할 수 없습니다.";
      System.out.println("💸 골드가 부족해 거래할 수 없습니다.");
    }

    logger.debug("상인 이벤트: {}", player.getName());
    return new ExploreResult(ExploreResult.ResultType.MERCHANT, message);
  }

  /**
   * 신비한 제단 이벤트를 처리합니다.
   */
  private ExploreResult handleShrineEvent(Player player) {
    String message = "🗿 신비한 제단을 발견했습니다!";
    System.out.println(message);

    // 다양한 축복 효과 중 하나
    ShrineBlessing blessing = ShrineBlessing.values()[random.nextInt(ShrineBlessing.values().length)];

    switch (blessing) {
      case STRENGTH -> {
        // 임시 공격력 증가 효과 (실제 구현에서는 버프 시스템 필요)
        message += "\n⚔️ 힘의 축복을 받았습니다! (다음 전투에서 공격력 증가)";
        System.out.println("⚔️ 힘의 축복을 받았습니다!");
      }
      case VITALITY -> {
        int bonusHp = 20;
        player.heal(bonusHp);
        message += "\n❤️ 생명력의 축복을 받았습니다! " + bonusHp + " 체력을 회복했습니다!";
        System.out.println("❤️ 생명력의 축복을 받았습니다!");
      }
      case WISDOM -> {
        int bonusMana = 15;
        player.restoreMana(bonusMana);
        message += "\n🔮 지혜의 축복을 받았습니다! " + bonusMana + " 마나를 회복했습니다!";
        System.out.println("🔮 지혜의 축복을 받았습니다!");
      }
      case FORTUNE -> {
        int bonusGold = 30;
        player.setGold(player.getGold() + bonusGold);
        message += "\n💰 행운의 축복을 받았습니다! " + bonusGold + " 골드를 획득했습니다!";
        System.out.println("💰 행운의 축복을 받았습니다!");
      }
    }

    logger.debug("제단 이벤트: {} (축복: {})", player.getName(), blessing);
    return new ExploreResult(ExploreResult.ResultType.SHRINE, message);
  }

  /**
   * 랜덤 보물 아이템을 생성합니다. ItemDataLoader의 JSON 기반 시스템을 사용합니다.
   */
  private GameItem generateRandomTreasureItem() {
    try {
      // ItemDataLoader의 JSON 기반 메서드 사용
      GameItem treasureItem = ItemDataLoader.generateRandomTreasureItem();

      if (treasureItem != null) {
        logger.debug("보물 아이템 생성: {}", treasureItem.getName());
        return treasureItem;
      }

      // 폴백: GameItemFactory 사용
      GameItemFactory factory = GameItemFactory.getInstance();
      GameItem fallbackItem = factory.createItem("HEALTH_POTION");

      if (fallbackItem != null) {
        logger.warn("폴백 보물 아이템 사용: {}", fallbackItem.getName());
        return fallbackItem;
      }

      // 최후의 수단: 직접 생성
      logger.warn("모든 방법 실패, 기본 보물 아이템 생성");
      return new GameConsumable("MYSTERY_POTION", "신비한 물약", "HP를 75 회복합니다", 50, ItemRarity.UNCOMMON, List.of(GameEffectFactory.createHealHpEffect(75)), 0);

    } catch (Exception e) {
      logger.error("보물 아이템 생성 실패", e);
      // 응급 폴백
      return new GameConsumable("HEALTH_POTION", "기본 물약", "HP를 50 회복합니다", 30, ItemRarity.COMMON, List.of(GameEffectFactory.createHealHpEffect(50)), 0);
    }
  }

  /**
   * 특별한 상인 아이템을 생성합니다. ItemDataLoader의 JSON 기반 시스템을 사용합니다.
   */
  private GameItem generateSpecialMerchantItem() {
    try {
      // ItemDataLoader의 JSON 기반 메서드 사용
      GameItem merchantItem = ItemDataLoader.generateSpecialMerchantItem();

      if (merchantItem != null) {
        logger.debug("상인 아이템 생성: {}", merchantItem.getName());
        return merchantItem;
      }

      // 폴백: GameItemFactory 사용 (상인용 고급 아이템)
      GameItemFactory factory = GameItemFactory.getInstance();
      String[] merchantItems = {"LARGE_HEALTH_POTION", "MANA_POTION", "STEEL_SWORD", "CHAIN_MAIL", "POWER_RING"};

      for (String itemId : merchantItems) {
        GameItem fallbackItem = factory.createItem(itemId);
        if (fallbackItem != null) {
          logger.warn("폴백 상인 아이템 사용: {}", fallbackItem.getName());
          return fallbackItem;
        }
      }

      // 최후의 수단: 직접 생성 (특별한 상인 아이템)
      logger.warn("모든 방법 실패, 기본 상인 아이템 생성");
      return new GameEquipment("MERCHANT_RING", "상인의 반지", "상인이 파는 특별한 반지", 150, ItemRarity.RARE, GameEquipment.EquipmentType.ACCESSORY, 3, 3, 15);

    } catch (Exception e) {
      logger.error("상인 아이템 생성 실패", e);
      // 응급 폴백
      return new GameConsumable("MERCHANT_POTION", "상인의 물약", "HP와 MP를 모두 회복", 80, ItemRarity.RARE,
          List.of(GameEffectFactory.createHealHpEffect(80), GameEffectFactory.createHealMpEffect(80)), 0);
    }
  }

  /**
   * 랜덤 드롭 아이템을 생성합니다. ItemDataLoader의 JSON 기반 시스템을 사용합니다.
   */
  private GameItem generateRandomDropItem() {
    try {
      // ItemDataLoader의 JSON 기반 메서드 사용
      GameItem dropItem = ItemDataLoader.generateRandomDropItem();

      if (dropItem != null) {
        logger.debug("드롭 아이템 생성: {}", dropItem.getName());
        return dropItem;
      }

      // 폴백: GameItemFactory 사용 (기본 아이템들)
      GameItemFactory factory = GameItemFactory.getInstance();
      String[] dropItems = {"SMALL_HEALTH_POTION", "SMALL_MANA_POTION", "WOODEN_SWORD", "LEATHER_ARMOR"};

      String selectedItemId = dropItems[random.nextInt(dropItems.length)];
      GameItem fallbackItem = factory.createItem(selectedItemId);

      if (fallbackItem != null) {
        logger.warn("폴백 드롭 아이템 사용: {}", fallbackItem.getName());
        return fallbackItem;
      }

      // 최후의 수단: 직접 생성 (기본 드롭 아이템)
      logger.warn("모든 방법 실패, 기본 드롭 아이템 생성");
      return new GameConsumable("SLIME_GEL","슬라임 젤", "끈적한 슬라임의 젤", 10, ItemRarity.COMMON, List.of(GameEffectFactory.createHealHpEffect(20)), 0);

    } catch (Exception e) {
      logger.error("드롭 아이템 생성 실패", e);
      // 응급 폴백
      return new GameConsumable("BROKEN_JAR","부서진 물약병", "깨진 물약병의 잔여물", 5, ItemRarity.COMMON, List.of(GameEffectFactory.createHealHpEffect(10)), 0);
    }
  }

  /**
   * 탐험 결과 클래스
   */
  public static class ExploreResult {
    private final ResultType type;
    private final String message;

    public ExploreResult(ResultType type, String message) {
      this.type = type;
      this.message = message;
    }

    public ResultType getType() {
      return type;
    }

    public String getMessage() {
      return message;
    }

    public enum ResultType {
      TREASURE, // 보물 발견
      HEALING, // 치유 이벤트
      KNOWLEDGE, // 지식 획득
      MANA_RESTORE, // 마나 회복
      MERCHANT, // 상인 조우
      SHRINE, // 제단 이벤트
      BATTLE_VICTORY, // 전투 승리
      BATTLE_DEFEAT, // 전투 패배
      BATTLE_ESCAPED, // 전투 도망
      ERROR // 오류
    }
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
