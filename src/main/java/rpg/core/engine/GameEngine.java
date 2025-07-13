package rpg.core.engine;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.JsonBasedQuestFactory;
import rpg.application.manager.LocationManager;
import rpg.application.service.QuestManager;
import rpg.application.validator.InputValidator;
import rpg.core.battle.BattleEngine;
import rpg.core.exploration.ExploreEngine;
import rpg.core.exploration.ExploreResult;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameItem;
import rpg.domain.location.DangerLevel;
import rpg.domain.location.LocationData;
import rpg.domain.monster.MonsterData;
import rpg.domain.player.Player;
import rpg.domain.skill.Skill;
import rpg.infrastructure.data.loader.MonsterDataLoader;
import rpg.infrastructure.persistence.GameDataRepository;
import rpg.presentation.controller.InventoryController;
import rpg.presentation.controller.QuestController;
import rpg.presentation.controller.ShopController;
import rpg.shared.constant.SystemConstants;
import rpg.shared.debug.DebugController;
import rpg.shared.persistence.SaveGameController;
import rpg.shared.util.ConsoleColors;

/**
 * 리팩토링된 메인 게임 컨트롤러
 * 각 기능별 Controller들을 조율하는 역할
 */
public class GameEngine {
  private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);

  // 게임 상태
  private Player player;
  private GameState gameState;
  private boolean gameRunning;
  private boolean inGameLoop;
  private long gameStartTime;
  private int currentSaveSlot;

  // 컨트롤러들
  private BattleEngine battleController;
  private InventoryController inventoryController;
  private QuestController questController;
  private ShopController shopController;
  private ExploreEngine exploreController;

  private DebugController debugController;
  private SaveGameController saveGameController;

  public GameEngine() {
    this.gameRunning = true;
    this.inGameLoop = false;
    this.gameState = new GameState();
    this.gameStartTime = System.currentTimeMillis();
    this.currentSaveSlot = 0;

    initializeControllers();
    logger.info("게임 인스턴스 생성 완료 (v" + SystemConstants.GAME_VERSION + "- 몬스터 도감 추가)");
  }

  /**
   * 모든 컨트롤러를 초기화합니다.
   */
  private void initializeControllers() {
    try {
      // 순서 중요: 의존성이 있는 컨트롤러들을 순서대로 초기화
      inventoryController = new InventoryController();
      battleController = new BattleEngine(null, gameState); // 임시로 null
      shopController = new ShopController(inventoryController);
      exploreController = new ExploreEngine(battleController, null, inventoryController, gameState); // 임시로 null

      saveGameController = new SaveGameController();
      debugController = new DebugController(player);
      logger.debug("모든 컨트롤러 초기화 완료");
    } catch (Exception e) {
      logger.error("컨트롤러 초기화 실패", e);
      throw new RuntimeException("게임 초기화 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 게임을 시작합니다.
   */
  public void start() {
    try {
      logger.info("게임 시작 (v" + SystemConstants.GAME_VERSION + ")");
      showWelcomeMessage();

      // 메인 메뉴 루프
      while (gameRunning) {
        showMainMenu();
        int choice = InputValidator.getIntInput("선택: ", 1, 3);

        switch (choice) {
          case 1:
            startNewGame();
            break;
          case 2:
            loadGame();
            break;
          case 3:
            exitGame();
            break;
        }
      }

    } catch (Exception e) {
      logger.error("게임 실행 중 오류 발생", e);
      System.out.println("게임 실행 중 오류가 발생했습니다. 게임을 종료합니다.");
    } finally {
      logger.info("게임 종료");
    }
  }

  /**
   * 환영 메시지를 표시합니다.
   */
  private void showWelcomeMessage() {
    System.out.println(ConsoleColors.CYAN + "====================================");
    System.out.println("   🎮 " + ConsoleColors.BOLD + ConsoleColors.GOLD_FALLBACK + "RPG 게임 v" + SystemConstants.GAME_VERSION + ConsoleColors.RESET
        + ConsoleColors.CYAN + " 🎮   ");
    System.out.println("====================================" + ConsoleColors.RESET);

    System.out.println("새로운 기능:");
    System.out.println("• 📦 다중 저장 슬롯 시스템 (5개)");
    System.out.println("• 🏗️ 개선된 아키텍처 (Controller 분리)");
    System.out.println("• 🌟 향상된 탐험 시스템(탐험지역별 몬스터추가)");
    System.out.println("• 🛍️ 확장된 상점 시스템(구매 / 판매)");
    System.out.println("• 📋 고도화된 퀘스트 관리");
    System.out.println("====================================");
  }

  /**
   * 메인 메뉴를 표시합니다.
   */
  private void showMainMenu() {
    System.out.println("\n" + ConsoleColors.BRIGHT_GREEN + "=== 🎮 메인 메뉴 ===" + ConsoleColors.RESET);
    System.out.println(ConsoleColors.colorize("1. 🆕 새로하기", ConsoleColors.BRIGHT_GREEN));
    System.out.println(ConsoleColors.colorize("2. 📁 불러오기", ConsoleColors.BRIGHT_BLUE));
    System.out.println(ConsoleColors.colorize("3. 🚪 종료하기", ConsoleColors.BRIGHT_RED));
    System.out.println(ConsoleColors.CYAN + "==================" + ConsoleColors.RESET);
  }


  /**
   * 새 게임을 시작합니다.
   */
  private void startNewGame() {
    try {
      String name = InputValidator.getStringInput("캐릭터 이름을 입력하세요: ", 1, 20);
      player = new Player(name);

      // 게임 상태 초기화
      gameState = new GameState();
      gameStartTime = System.currentTimeMillis();
      currentSaveSlot = 0;

      // 🔥 시작 아이템으로 기본 물약 지급
      giveStartingItems();

      // 컨트롤러들에 새로운 게임 상태 적용
      updateControllersWithNewGameState();

      System.out.println("🎉 새로운 모험가 " + name + "님, 환영합니다!");
      player.displayStats();

      // 🆕 일일 퀘스트 초기 생성
      player.getQuestManager().generateDailyQuests(player);

      logger.info("새 게임 초기화 완료 - 일일 퀘스트 포함");

      // 시작 퀘스트 안내
      System.out.println("\n💡 퀘스트 메뉴에서 첫 번째 퀘스트를 수락해보세요!");

      logger.info("새 캐릭터 생성: {}", name);

      // 인게임 루프 시작
      startGameLoop();

    } catch (Exception e) {
      logger.error("새 게임 시작 실패", e);
      System.out.println("새 게임 시작 중 오류가 발생했습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
    }
  }

  /**
   * 시작 아이템 지급
   */
  private void giveStartingItems() {
    GameItemFactory factory = GameItemFactory.getInstance();

    // 기본 체력 물약 3개 지급
    GameItem healthPotion = factory.createItem("SMALL_HEALTH_POTION");
    if (healthPotion != null && healthPotion instanceof GameConsumable) {
      player.getInventory().addItem(healthPotion, 3);
      logger.info("시작 아이템 지급: {} x3", healthPotion.getName());
    } else {
      logger.error("체력 물약 생성 실패: HEALTH_POTION");
    }

    // 기본 마나 물약 2개 지급
    GameItem manaPotion = factory.createItem("SMALL_MANA_POTION");
    if (manaPotion != null && manaPotion instanceof GameConsumable) {
      player.getInventory().addItem(manaPotion, 2);
      logger.info("시작 아이템 지급: {} x2", manaPotion.getName());
    } else {
      logger.error("마나 물약 생성 실패: MANA_POTION");
    }

    // 결과 출력
    System.out.println("🎁 시작 아이템을 받았습니다!");
    if (healthPotion != null) {
      System.out.println("• " + healthPotion.getName() + " x3");
    }
    if (manaPotion != null) {
      System.out.println("• " + manaPotion.getName() + " x2");
    }

    logger.debug("시작 아이템 지급 완료");
  }

  // 메인 메뉴의 loadGame() 메서드도 교체
  private void loadGame() {
    SaveGameController.SaveLoadResult result = saveGameController.loadGame();
    if (result.isSuccess()) {
      player = result.getPlayer();
      gameState = result.getGameState();
      currentSaveSlot = result.getSlotNumber();
      updateControllersWithNewGameState();
      startGameLoop();
    }
  }

  /**
   * 게임을 종료합니다.
   */
  private void exitGame() {
    if (InputValidator.getConfirmation("정말로 게임을 종료하시겠습니까?")) {
      System.out.println("🎮 게임을 종료합니다. 안녕히 가세요!");
      gameRunning = false;
    }
  }


  /**
   * 새로운 게임 상태로 컨트롤러들을 업데이트합니다.
   */
  private void updateControllersWithNewGameState() {
    QuestManager questManager = player.getQuestManager();

    questController = new QuestController(questManager, gameState, player);
    battleController = new BattleEngine(questManager, gameState);
    exploreController = new ExploreEngine(battleController, questController, inventoryController, gameState);

    // 디버그 컨트롤러 초기화 (플레이어가 있을 때만)
    if (player != null && SystemConstants.DEBUG_MODE) {
      debugController = new DebugController(player);
    }
  }

  /**
   * 메인 게임 루프를 실행합니다.
   */
  private void startGameLoop() {
    inGameLoop = true;

    while (inGameLoop && player.isAlive()) {
      try {
        showInGameMenu();
        int maxChoice = SystemConstants.DEBUG_MODE ? 99 : 13;
        int choice = InputValidator.getIntInput("선택: ", 1, maxChoice);

        switch (choice) {
          case 1:
            handleExploration();
            break;
          case 2:
            displayPlayerStatus();
            break;
          case 3:
            inventoryController.manageInventory(player);
            break;
          case 4:
            manageSkills();
            break;
          case 5:
            questController.manageQuests(player);
            break;
          case 6:
            shopController.openShop(player);
            break;
          case 7:
            showLocationInfo();
            break;
          case 8:
            showMonsterEncyclopedia();
            break;
          case 9:
            SaveGameController.SaveLoadResult result = saveGameController.showSaveLoadMenu(player, gameState, gameStartTime);

            if (result.isSuccess()) {
              // 새 게임으로 교체
              player = result.getPlayer();
              gameState = result.getGameState();
              saveGameController.setCurrentSaveSlot(result.getSlotNumber());
              updateControllersWithNewGameState();
            }
            break;
          case 10:
            returnToMainMenu();
            break;
          case 11:
            showHelp();
            break;
          case 99:
            // 디버그 메뉴 진입 (DEBUG_MODE가 true일 때만)
            if (SystemConstants.DEBUG_MODE && debugController != null) {
              debugController.showDebugMenu();
            } else {
              System.out.println("디버그 모드가 비활성화되어 있습니다.");
            }
            break;

          default:
            System.out.println("잘못된 선택입니다.");
        }


      } catch (Exception e) {
        logger.error("게임 루프 중 오류", e);
        System.out.println("게임 진행 중 오류가 발생했습니다. 계속 진행합니다.");
      }
    }

    if (!player.isAlive()) {
      handleGameOver();
    }

    // 인게임 루프 종료 후 메인 메뉴로 복귀
    inGameLoop = false;
  }

  /**
   * 확장된 메인 메뉴를 표시합니다.
   */
  private void showInGameMenu() {
    System.out.println("\n" + ConsoleColors.BOLD + ConsoleColors.BRIGHT_CYAN + "=== 🎯 게임 메뉴 ===" + ConsoleColors.RESET);

    // 탐험 관련
    System.out.println(ConsoleColors.colorize("1. 🗡️ 탐험하기", ConsoleColors.BRIGHT_RED));
    System.out.println(ConsoleColors.colorize("2. 📊 상태 확인", ConsoleColors.BRIGHT_BLUE));

    // 관리 메뉴
    System.out.println(ConsoleColors.colorize("3. 🎒 인벤토리", ConsoleColors.BRIGHT_GREEN));
    System.out.println(ConsoleColors.colorize("4. ⚡ 스킬 관리", ConsoleColors.GOLD_FALLBACK));
    System.out.println(ConsoleColors.colorize("5. 📋 퀘스트", ConsoleColors.BRIGHT_PURPLE));
    System.out.println(ConsoleColors.colorize("6. 🏪 상점", ConsoleColors.BRIGHT_CYAN));

    // 정보 메뉴
    System.out.println(ConsoleColors.colorize("7. 🗺️ 지역 정보", ConsoleColors.GREEN));
    System.out.println(ConsoleColors.colorize("8. 📚 몬스터 도감", ConsoleColors.YELLOW));

    // 시스템 메뉴
    System.out.println(ConsoleColors.colorize("9. 📁 저장 관리", ConsoleColors.PURPLE));
    System.out.println(ConsoleColors.colorize("10. 🚪 게임 종료", ConsoleColors.RED));
    System.out.println(ConsoleColors.colorize("11. ❓ 도움말", ConsoleColors.WHITE));

    // 디버그 모드가 활성화된 경우에만 디버그 메뉴 표시
    if (SystemConstants.DEBUG_MODE) {
      System.out.println(ConsoleColors.colorize("99. 🔧 디버그 메뉴", ConsoleColors.GOLD_FALLBACK));
    }
    System.out.println(ConsoleColors.CYAN + "==================" + ConsoleColors.RESET);
  }


  /**
   * 지역별 몬스터를 표시합니다.
   */
  private void showMonstersByLocation() {
    String location = InputValidator.getStringInput("지역명을 입력하세요: ", 1, 20);

    var monsters = MonsterDataLoader.getMonstersByLocation(location);

    if (monsters.isEmpty()) {
      System.out.println("해당 지역에는 몬스터가 없거나 존재하지 않는 지역입니다.");
      return;
    }

    System.out.println("\n🏞️ " + location + "의 몬스터들:");

    monsters.forEach(monster -> {
      String rarity = getRarityIcon(monster.getRarity());
      int level = estimateMonsterLevel(monster);

      System.out.printf("%s %s (레벨 %d, 출현율 %.0f%%)%n", rarity, monster.getName(), level, monster.getSpawnRate() * 100);

      System.out.printf("   📝 %s%n", monster.getDescription());

      if (!monster.getAbilities().isEmpty()) {
        System.out.printf("   💫 특수능력: %s%n", String.join(", ", monster.getAbilities()));
      }

      System.out.printf("   💎 보상: 경험치 %d, 골드 %d%n", monster.getRewards().getExp(), monster.getRewards().getGold());
      System.out.println();
    });
  }

  /**
   * 몬스터를 검색합니다.
   */
  private void searchMonster() {
    String keyword = InputValidator.getStringInput("몬스터 이름을 입력하세요: ", 1, 20);

    var allMonsters = MonsterDataLoader.loadAllMonsters();

    var searchResults =
        allMonsters.values().stream().filter(monster -> monster.getName().toLowerCase().contains(keyword.toLowerCase())).collect(Collectors.toList());

    if (searchResults.isEmpty()) {
      System.out.println("'" + keyword + "'와 일치하는 몬스터를 찾을 수 없습니다.");
      return;
    }

    System.out.println("\n🔍 검색 결과: " + searchResults.size() + "종");

    for (MonsterData monster : searchResults) {
      showDetailedMonsterInfo(monster);
    }
  }

  /**
   * 몬스터의 상세 정보를 표시합니다.
   */
  private void showDetailedMonsterInfo(MonsterData monster) {
    String rarity = getRarityIcon(monster.getRarity());
    int level = estimateMonsterLevel(monster);

    System.out.println("\n" + "=".repeat(50));
    System.out.printf("%s %s (레벨 %d)%n", rarity, monster.getName(), level);
    System.out.println("📝 " + monster.getDescription());
    System.out.println("🏷️ 등급: " + monster.getRarity());

    // 능력치
    var stats = monster.getStats();
    System.out.printf("⚔️ 능력치: HP %d, 공격 %d, 방어 %d, 속도 %d%n", stats.getHp(), stats.getAttack(), stats.getDefense(), stats.getSpeed());

    // 보상
    var rewards = monster.getRewards();
    System.out.printf("💎 보상: 경험치 %d, 골드 %d%n", rewards.getExp(), rewards.getGold());

    // 출현 지역
    if (!monster.getLocations().isEmpty()) {
      System.out.println("🗺️ 출현 지역: " + String.join(", ", monster.getLocations()));
    }

    // 출현 레벨 범위
    System.out.printf("📊 출현 레벨: %d ~ %d (확률 %.0f%%)%n", monster.getMinLevel(), monster.getMaxLevel(), monster.getSpawnRate() * 100);

    // 특수 능력
    if (!monster.getAbilities().isEmpty()) {
      System.out.println("💫 특수능력: " + String.join(", ", monster.getAbilities()));
    }

    // 드롭 아이템
    if (!rewards.getDropItems().isEmpty()) {
      System.out.println("🎁 드롭 아이템:");
      for (var dropItem : rewards.getDropItems()) {
        System.out.printf("   • %s (확률 %.1f%%, 수량 %d~%d)%n", dropItem.getItemId(), dropItem.getDropRate() * 100, dropItem.getMinQuantity(),
            dropItem.getMaxQuantity());
      }
    }

    System.out.println("=".repeat(50));
  }

  // === 헬퍼 메서드들 ===

  private String getDangerLevel(String location) {
    return switch (location) {
      case "숲속 깊은 곳" -> "🟢 낮음";
      case "어두운 동굴", "험준한 산길" -> "🟡 보통";
      case "마법의 숲", "신비한 호수" -> "🟠 높음";
      case "폐허가 된 성", "고대 유적" -> "🔴 매우 높음";
      case "용암 동굴" -> "💀 극도로 높음";
      default -> "❓ 알 수 없음";
    };
  }

  private String getLocationRecommendation(String location, int playerLevel, int monsterCount) {
    return monsterCount + "종의 몬스터 (레벨 " + playerLevel + " 적합)";
  }

  private String getUnsuitableReason(String location, int playerLevel) {
    var allLocationMonsters = MonsterDataLoader.getMonstersByLocation(location);

    if (allLocationMonsters.isEmpty()) {
      return "몬스터 정보 없음";
    }

    int minLevel = allLocationMonsters.stream().mapToInt(MonsterData::getMinLevel).min().orElse(1);
    int maxLevel = allLocationMonsters.stream().mapToInt(MonsterData::getMaxLevel).max().orElse(99);

    if (playerLevel < minLevel) {
      return "레벨이 너무 낮음 (최소 " + minLevel + " 필요)";
    } else {
      return "레벨이 너무 높음 (최대 " + maxLevel + " 권장)";
    }
  }

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

  private int estimateMonsterLevel(MonsterData monsterData) {
    return Math.max(1, (monsterData.getStats().getHp() + monsterData.getStats().getAttack() * 2) / 15);
  }

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

  private void showLocationStatistics(String location) {
    var monsters = MonsterDataLoader.getMonstersByLocation(location);

    if (monsters.isEmpty())
      return;

    System.out.println("\n📊 지역 통계:");
    System.out.println("   총 몬스터 종류: " + monsters.size() + "종");

    int minLevel = monsters.stream().mapToInt(MonsterData::getMinLevel).min().orElse(1);
    int maxLevel = monsters.stream().mapToInt(MonsterData::getMaxLevel).max().orElse(99);
    System.out.println("   레벨 범위: " + minLevel + " ~ " + maxLevel);

    double avgSpawnRate = monsters.stream().mapToDouble(MonsterData::getSpawnRate).average().orElse(0.0);
    System.out.printf("   평균 출현율: %.1f%%%n", avgSpawnRate * 100);
  }



  /**
   * 게임 알림을 표시합니다.
   */
  private void showNotifications() {
    if (questController.hasClaimableRewards()) {
      System.out.println("🎁 수령 가능한 퀘스트 보상이 있습니다!");
    }

    double inventoryUsage = inventoryController.getInventoryUsageRate(player);
    if (inventoryUsage > 0.8) {
      System.out.println("💼 인벤토리가 거의 가득 찼습니다! (" + String.format("%.0f%%", inventoryUsage * 100) + ")");
    }
  }

  /**
   * 메인 메뉴로 돌아갑니다.
   */
  private void returnToMainMenu() {
    boolean shouldSave = InputValidator.getConfirmation("게임을 저장하고 메인 메뉴로 돌아가시겠습니까?");

    if (shouldSave) {
      saveGameController.saveGame(player, gameState, gameStartTime);
    }

    inGameLoop = false;
    System.out.println("🏠 메인 메뉴로 돌아갑니다.");
  }

  /**
   * 탐험을 처리합니다. (개선된 버전)
   */
  private void handleExploration() {
    while (true) {
      showExplorationMenu();

      List<LocationData> availableLocations = LocationManager.getAvailableLocations(player.getLevel());
      int maxChoice = availableLocations.size();

      int choice = InputValidator.getIntInput("선택: ", 0, maxChoice);

      if (choice == 0) {
        System.out.println("🏠 마을로 돌아갑니다.");
        break;
      }

      if (choice > 0 && choice <= availableLocations.size()) {
        LocationData selectedLocation = availableLocations.get(choice - 1);
        exploreSpecificLocation(selectedLocation);
      }
    }
  }

  /**
   * 탐험 메뉴를 표시합니다.
   */
  private void showExplorationMenu() {
    System.out.println("\n" + ConsoleColors.BOLD + ConsoleColors.BRIGHT_CYAN + "=== 🗡️ 탐험 메뉴 ===" + ConsoleColors.RESET);
    System.out.println(ConsoleColors.CYAN + "(갈 수 있는 지역이 레벨별 추천에 맞춰서 정렬되어 보임)" + ConsoleColors.RESET);
    System.out.println(ConsoleColors.YELLOW + "현재 레벨: " + player.getLevel() + ConsoleColors.RESET);

    List<LocationData> availableLocations = LocationManager.getAvailableLocations(player.getLevel());

    for (int i = 0; i < availableLocations.size(); i++) {
      LocationData location = availableLocations.get(i);
      String difficultyColor = getDifficultyColor(location.getDangerLevel());
      String recommendationText = getRecommendationText(location, player.getLevel());

      System.out.printf("%s%d. %s %s%s %s%n", difficultyColor, i + 1, location.getIcon(), location.getNameKo(), ConsoleColors.RESET,
          recommendationText);
    }

    System.out.println(ConsoleColors.WHITE + "0. 🏠 마을로 돌아가기" + ConsoleColors.RESET);
    System.out.println(ConsoleColors.CYAN + "==================" + ConsoleColors.RESET);

  }



  /**
   * 특정 지역으로 탐험을 진행합니다. (LocationData 기반)
   */
  private void exploreSpecificLocation(LocationData location) {
    System.out.println("\n🚀 " + location.getNameKo() + "(으)로 향합니다!");

    // 현재 위치 설정 (한글명으로 설정, 호환성 유지)
    gameState.setCurrentLocation(location.getNameKo());

    // 지역 설명 표시
    showLocationDescription(location);

    // 해당 지역에서의 탐험 진행 (LocationID 사용)
    ExploreResult result = exploreController.exploreLocation(player, location.getId());

    // 탐험 결과 처리
    handleExplorationResult(result);

    // 탐험 후 잠시 대기
    InputValidator.waitForAnyKey("\n계속하려면 Enter를 누르세요...");
  }

  /**
   * 탐험 결과 처리
   */
  private void handleExplorationResult(ExploreResult result) {
    switch (result.getType()) {
      case BATTLE_DEFEAT -> {
        if (!player.isAlive()) {
          System.out.println("💀 전투에서 패배했습니다...");
          gameRunning = false;
        }
      }
      case TREASURE -> {
        // 보물 관련 퀘스트 진행도 업데이트
        questController.updateProgress("treasure", 1);
        logger.debug("보물 발견 이벤트 완료");
      }
      case KNOWLEDGE -> {
        // 지식 획득 관련 퀘스트 진행도 업데이트
        questController.updateLevelProgress(player);
      }
      case MERCHANT -> {
        // 상인 조우 관련 퀘스트 진행도 업데이트
        questController.updateProgress("merchant", 1);
      }
      case REST -> {
        // 휴식 관련 효과는 이미 ExploreEngine에서 처리됨
        logger.debug("휴식 이벤트 완료");
      }
      default -> {
        // 기타 결과 처리
      }
    }
  }

  /**
   * 지역 설명을 표시합니다. (LocationData 기반)
   */
  private void showLocationDescription(LocationData location) {
    System.out.println(location.getDescription());

    // 지역 특성 표시
    Map<String, Object> properties = location.properties();
    if (properties != null && !properties.isEmpty()) {
      showLocationProperties(properties);
    }
  }

  /**
   * 지역 특성 표시
   */
  private void showLocationProperties(Map<String, Object> properties) {
    List<String> traits = new ArrayList<>();

    if (Boolean.TRUE.equals(properties.get("magical"))) {
      traits.add("🌟 마법의 기운");
    }
    if (Boolean.TRUE.equals(properties.get("hazardous"))) {
      traits.add("⚠️ 위험");
    }
    if (Boolean.TRUE.equals(properties.get("healing"))) {
      traits.add("💚 치유");
    }
    if (Boolean.TRUE.equals(properties.get("shelter"))) {
      traits.add("🏠 은신처");
    }
    if (Boolean.TRUE.equals(properties.get("water"))) {
      traits.add("💧 수중");
    }

    if (!traits.isEmpty()) {
      System.out.println("특성: " + String.join(", ", traits));
    }
  }

  /**
   * 난이도에 따른 색상을 반환합니다. (DangerLevel 기반)
   */
  private String getDifficultyColor(DangerLevel dangerLevel) {
    return switch (dangerLevel) {
      case EASY -> ConsoleColors.GREEN;
      case NORMAL -> ConsoleColors.YELLOW;
      case HARD -> ConsoleColors.BRIGHT_RED;
      case VERY_HARD -> ConsoleColors.RED;
      case EXTREME -> ConsoleColors.PURPLE;
      case NIGHTMARE -> ConsoleColors.BLACK;
      case DIVINE -> ConsoleColors.WHITE;
      case IMPOSSIBLE -> ConsoleColors.RED + ConsoleColors.BOLD;
    };
  }

  /**
   * 추천 텍스트를 생성합니다. (LocationData 기반)
   */
  private String getRecommendationText(LocationData location, int playerLevel) {
    if (playerLevel >= location.getMinLevel() && playerLevel <= location.getMaxLevel()) {
      return ConsoleColors.GREEN + "[추천]" + ConsoleColors.RESET;
    } else if (playerLevel < location.getMaxLevel() + 3) {
      return ConsoleColors.GOLD_FALLBACK + "[적정]" + ConsoleColors.RESET;
    } else if (playerLevel > location.getMaxLevel()) {
      return ConsoleColors.BRIGHT_BLACK + "[쉬움]" + ConsoleColors.RESET;
    } else {
      return ConsoleColors.RED + "[위험]" + ConsoleColors.RESET;
    }
  }


  /**
   * 플레이어 상태를 표시합니다.
   */
  private void displayPlayerStatus() {
    player.displayStats();

    // 추가 정보 표시
    System.out.println("\n=== 추가 정보 ===");
    System.out.println("📍 현재 위치: " + gameState.getCurrentLocation());
    System.out.println("⚔️ 처치한 몬스터: " + gameState.getMonstersKilled() + "마리");
    System.out.println("📋 완료한 퀘스트: " + gameState.getQuestsCompleted() + "개");
    System.out.println("⏰ 총 플레이 시간: " + gameState.getTotalPlayTime() + "분");

    if (currentSaveSlot > 0) {
      System.out.println("💾 현재 저장 슬롯: " + currentSaveSlot);
    }
  }


  /**
   * 지역 정보를 표시합니다. (LocationManager 기반)
   */
  private void showLocationInfo() {
    while (true) {
      System.out.println("\n🗺️ === 지역 정보 ===");
      System.out.println("1. 현재 위치 상세 정보");
      System.out.println("2. 모든 지역 개요");
      System.out.println("3. 레벨별 추천 지역");
      System.out.println("4. 지역 통계");
      System.out.println("5. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

      switch (choice) {
        case 1 -> showCurrentLocationDetail();
        case 2 -> showAllLocationsOverview();
        case 3 -> showLocationRecommendations();
        case 4 -> LocationManager.printLocationStatistics();
        case 5 -> {
          return;
        }
      }

      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
    }
  }

  /**
   * 현재 위치의 상세 정보를 표시합니다. (LocationManager 기반)
   */
  private void showCurrentLocationDetail() {
    String currentLocation = gameState.getCurrentLocation();
    String locationId = LocationManager.getLocationIdByKoreanName(currentLocation);

    if (locationId == null) {
      System.out.println("❌ 현재 위치 정보를 찾을 수 없습니다.");
      return;
    }

    LocationData location = LocationManager.getLocation(locationId);
    if (location == null) {
      System.out.println("❌ 지역 데이터를 찾을 수 없습니다.");
      return;
    }

    System.out.println("\n📍 현재 위치: " + location.getNameKo() + " (" + location.getNameEn() + ")");
    System.out.println("🎯 추천 레벨: " + location.getMinLevel() + " ~ " + location.getMaxLevel());
    System.out.println("⚡ 난이도: " + location.getDangerLevel().getEmoji() + " " + location.getDangerLevel().getDisplayName());
    System.out.println("🎲 이벤트 확률: " + location.getEventChance() + "%");

    // 지역 설명
    System.out.println("\n📋 설명:");
    System.out.println("   " + location.getDescription());

    // 지역 특성
    showLocationProperties(location.properties());

    // 현재 위치의 몬스터 정보
    exploreController.showCurrentLocationMonsters(player.getLevel());

    // 지역 통계
    showLocationStatistics(location, locationId);
  }

  /**
   * 모든 지역의 개요를 표시합니다. (LocationManager 기반)
   */
  private void showAllLocationsOverview() {
    System.out.println("\n🌍 === 모든 지역 개요 ===");

    List<LocationData> allLocations = LocationManager.getAllLocations();

    // 레벨 순으로 정렬
    allLocations.sort((l1, l2) -> Integer.compare(l1.getMinLevel(), l2.getMinLevel()));

    for (LocationData location : allLocations) {
      System.out.println("\n" + location.getIcon() + " " + location.getNameKo());
      System.out.println("   레벨: " + location.getMinLevel() + "-" + location.getMaxLevel() + " | 난이도: " + location.getDangerLevel().getDisplayName());

      // 해당 지역의 몬스터 수
      List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(location.getId());
      System.out.println("   몬스터: " + locationMonsters.size() + "종");

      // 접근 가능 여부
      if (player.getLevel() >= location.getMinLevel()) {
        System.out.println("   상태: ✅ 접근 가능");
      } else {
        System.out.println("   상태: 🔒 레벨 " + location.getMinLevel() + " 필요");
      }
    }
  }

  /**
   * 지역별 추천 정보를 표시합니다. (LocationManager 기반)
   */
  private void showLocationRecommendations() {
    int playerLevel = player.getLevel();
    System.out.println("\n🎯 === 레벨 " + playerLevel + " 추천 지역 ===");

    List<LocationData> availableLocations = LocationManager.getAvailableLocations(playerLevel);

    if (availableLocations.isEmpty()) {
      System.out.println("❌ 현재 레벨에서 갈 수 있는 지역이 없습니다.");
      return;
    }

    System.out.println("\n✅ 접근 가능한 지역:");
    for (LocationData location : availableLocations) {
      List<MonsterData> suitableMonsters = MonsterDataLoader.getMonstersByLocationAndLevel(location.getId(), playerLevel);

      String recommendation = getLocationRecommendationDetail(location, playerLevel, suitableMonsters.size());
      System.out.printf("• %s %s - %s%n", location.getIcon(), location.getNameKo(), recommendation);
    }

    // 미래에 접근 가능한 지역도 표시
    showFutureLocations(playerLevel);
  }

  /**
   * 미래 접근 가능 지역 표시
   */
  private void showFutureLocations(int playerLevel) {
    List<LocationData> futureLocations = LocationManager.getAllLocations().stream().filter(location -> location.getMinLevel() > playerLevel)
        .filter(location -> location.getMinLevel() <= playerLevel + 5) // 5레벨 이내
        .sorted((l1, l2) -> Integer.compare(l1.getMinLevel(), l2.getMinLevel())).collect(Collectors.toList());

    if (!futureLocations.isEmpty()) {
      System.out.println("\n🔮 곧 접근 가능한 지역:");
      for (LocationData location : futureLocations) {
        int levelsNeeded = location.getMinLevel() - playerLevel;
        System.out.printf("• %s %s - %d레벨 후 접근 가능%n", location.getIcon(), location.getNameKo(), levelsNeeded);
      }
    }
  }

  /**
   * 지역 추천 상세 정보 생성
   */
  private String getLocationRecommendationDetail(LocationData location, int playerLevel, int suitableMonsters) {
    StringBuilder recommendation = new StringBuilder();

    // 난이도 평가
    if (playerLevel >= location.getMinLevel() && playerLevel <= location.getMaxLevel()) {
      recommendation.append("🎯 적정 레벨");
    } else if (playerLevel < location.getMaxLevel() + 3) {
      recommendation.append("⚡ 도전적");
    } else {
      recommendation.append("😌 여유로움");
    }

    // 몬스터 정보
    if (suitableMonsters > 0) {
      recommendation.append(" (몬스터 ").append(suitableMonsters).append("종)");
    } else {
      recommendation.append(" (적합한 몬스터 없음)");
    }

    return recommendation.toString();
  }

  /**
   * 지역 통계 표시
   */
  private void showLocationStatistics(LocationData location, String locationId) {
    System.out.println("\n📊 === 지역 통계 ===");

    List<MonsterData> allMonsters = MonsterDataLoader.getMonstersByLocation(locationId);
    List<MonsterData> suitableMonsters = MonsterDataLoader.getMonstersByLocationAndLevel(locationId, player.getLevel());

    System.out.println("총 몬스터 종류: " + allMonsters.size() + "종");
    System.out.println("현재 레벨 적합 몬스터: " + suitableMonsters.size() + "종");

    if (!allMonsters.isEmpty()) {
      // 레벨 범위
      int minMonsterLevel = allMonsters.stream().mapToInt(MonsterData::getMinLevel).min().orElse(0);
      int maxMonsterLevel = allMonsters.stream().mapToInt(MonsterData::getMaxLevel).max().orElse(0);
      System.out.println("몬스터 레벨 범위: " + minMonsterLevel + " ~ " + maxMonsterLevel);

      // 희귀도 분포
      Map<String, Long> rarityDistribution = allMonsters.stream().collect(Collectors.groupingBy(MonsterData::getRarity, Collectors.counting()));

      System.out.println("희귀도 분포:");
      rarityDistribution.forEach((rarity, count) -> System.out.println("  " + rarity + ": " + count + "종"));
    }
  }



  /**
   * 스킬을 관리합니다.
   */
  private void manageSkills() {
    while (true) {
      player.getSkillManager().displaySkills(player);
      System.out.println("\n1. 스킬 정보 보기");
      System.out.println("2. 돌아가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 2);

      if (choice == 1) {
        showSkillInfo();
      } else {
        break;
      }
    }
  }

  /**
   * 스킬 정보를 표시합니다.
   */
  private void showSkillInfo() {
    var skills = player.getSkillManager().getLearnedSkills();
    if (skills.isEmpty()) {
      System.out.println("학습한 스킬이 없습니다.");
      return;
    }

    int skillIndex = InputValidator.getIntInput("정보를 볼 스킬 번호 (0: 취소): ", 0, skills.size()) - 1;
    if (skillIndex < 0)
      return;

    Skill skill = skills.get(skillIndex);
    System.out.println("\n" + skill.getSkillInfo());
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 게임 오버를 처리합니다.
   */
  private void handleGameOver() {
    System.out.println("\n💀 게임 오버!");
    System.out.println("모험가 " + player.getName() + "님의 모험이 끝났습니다.");
    System.out.printf("최종 레벨: %d, 획득한 골드: %d%n", player.getLevel(), player.getGold());

    // 게임 통계 표시
    gameState.displayGameStats();

    logger.info("게임 오버: {} (레벨: {}, 골드: {})", player.getName(), player.getLevel(), player.getGold());

    if (currentSaveSlot > 0 && InputValidator.getConfirmation("현재 슬롯의 저장 파일을 삭제하시겠습니까?")) {
      GameDataRepository.deleteSaveSlot(currentSaveSlot);
    }
  }



  /**
   * 완전한 도움말을 표시합니다. (기존 메서드를 대체)
   */
  private void showHelp() {

    System.out.println("\n📖 === 게임 도움말 (v" + SystemConstants.GAME_VERSION + ") ===");

    // 기본 게임 기능 설명
    System.out.println("\n🎮 === 기본 기능 ===");
    System.out.println("🗡️ 탐험하기: JSON 기반 지역별 몬스터와 다양한 랜덤 이벤트 경험");
    System.out.println("📊 상태 확인: 캐릭터 스탯, 장비, 현재 위치 정보 확인");
    System.out.println("🎒 인벤토리: 통합 아이템 관리 및 장비 착용/해제 시스템");
    System.out.println("⚡ 스킬 관리: 레벨업으로 학습한 스킬 확인 및 전투 활용");
    System.out.println("📋 퀘스트: JSON 기반 퀘스트 시스템으로 목표 달성 및 보상 획득");
    System.out.println("🏪 상점: 카테고리별 아이템 구매/판매 및 특별 상점 이벤트");
    System.out.println("🗺️ 지역 정보: 현재 위치 몬스터 정보 및 지역별 추천 레벨");
    System.out.println("📚 몬스터 도감: 조우한 몬스터 목록 및 상세 정보");
    System.out.println("💾 게임 저장: 다중 슬롯 지원 (최대 5개 캐릭터 동시 관리)");
    System.out.println("📁 저장 관리: 슬롯별 캐릭터 정보 확인 및 삭제");

    // 새로운 기능 및 업데이트 사항
    System.out.println("\n🆕 === v" + SystemConstants.GAME_VERSION + " 주요 업데이트 ===");
    System.out.println("• 🎯 JSON 기반 퀘스트 템플릿 시스템 (외부 파일로 퀘스트 관리)");
    System.out.println("• 🎲 랜덤 아이템 생성 시스템 (희귀도별, 레벨별, 상황별)");
    System.out.println("• 🏭 Factory 패턴 적용 (QuestFactory, GameItemFactory 통합)");
    System.out.println("• 📦 ItemDataLoader와 GameItemFactory 연동으로 일관된 아이템 관리");
    System.out.println("• 🔧 GameDataLoader 기본 아이템 생성 시스템 개선");
    System.out.println("• 🎯 ExploreController 아이템 생성 로직 통합 및 안정성 강화");

    // 게임 팁
    System.out.println("\n💡 === 게임 팁 ===");
    System.out.println("• 초보자: 슬라임 사냥 퀘스트부터 시작하여 기본 장비를 획득하세요");
    System.out.println("• 레벨업: 경험치는 몬스터 처치와 퀘스트 완료로 획득할 수 있습니다");
    System.out.println("• 아이템: 희귀한 아이템은 보물 상자나 강한 몬스터에서 획득 가능합니다");
    System.out.println("• 퀘스트: 일일 퀘스트는 매일 새롭게 생성되므로 꾸준히 확인하세요");
    System.out.println("• 상점: 레벨이 높아질수록 더 좋은 아이템을 판매합니다");
    System.out.println("• 저장: 중요한 순간에는 꼭 저장하여 진행 상황을 보존하세요");

    // 시스템 정보
    showSystemInfo();

    try {
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.printf("• 퀘스트 템플릿: 메인 %d개, 사이드 %d개, 일일 %d개\n", questFactory.getQuestCount("MAIN"), questFactory.getQuestCount("SIDE"),
          questFactory.getQuestCount("DAILY"));
    } catch (Exception e) {
      System.out.println("• 퀘스트 시스템: 초기화 중 오류 발생");
    }

  }

  /**
   * 시스템 정보 표시 (분리된 메서드)
   */
  private void showSystemInfo() {
    System.out.println("\n⚙️ === 시스템 정보 ===");
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.printf("• 로드된 아이템 수: %d개\n", itemFactory.getItemCount());
      System.out.printf("• 초기화 상태: %s\n", itemFactory.isInitialized() ? "정상" : "오류");

      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.printf("• 퀘스트 템플릿: 메인 %d개, 사이드 %d개, 일일 %d개\n", questFactory.getQuestCount("MAIN"), questFactory.getQuestCount("SIDE"),
          questFactory.getQuestCount("DAILY"));
    } catch (Exception e) {
      System.out.println("• 시스템 상태: 일부 오류 발생 (" + e.getMessage() + ")");
    }
  }


  /**
   * 몬스터 도감을 표시합니다. (LocationManager 기반)
   */
  private void showMonsterEncyclopedia() {
    while (true) {
      System.out.println("\n📚 === 몬스터 도감 ===");
      System.out.println("1. 지역별 몬스터");
      System.out.println("2. 레벨별 몬스터");
      System.out.println("3. 희귀도별 몬스터");
      System.out.println("4. 몬스터 검색");
      System.out.println("5. 몬스터 통계");
      System.out.println("6. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 6);

      switch (choice) {
        case 1 -> showMonstersByLocation();
        case 2 -> showMonstersByLevel();
        case 3 -> showMonstersByRarity();
        case 4 -> searchMonsters();
        case 5 -> MonsterDataLoader.printMonsterStatistics();
        case 6 -> {
          return;
        }
      }

      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
    }
  }

  /**
   * 레벨별 몬스터 표시
   */
  private void showMonstersByLevel() {
    int targetLevel = InputValidator.getIntInput("확인할 레벨 (현재: " + player.getLevel() + "): ", 1, 50);

    System.out.println("\n📈 === 레벨 " + targetLevel + " 적합 몬스터 ===");

    List<MonsterData> suitableMonsters = MonsterDataLoader.getMonstersByLevel(targetLevel);

    if (suitableMonsters.isEmpty()) {
      System.out.println("❌ 해당 레벨에 적합한 몬스터가 없습니다.");
      return;
    }

    // 지역별로 그룹화
    Map<String, List<MonsterData>> monstersByLocation = suitableMonsters.stream()
        .flatMap(monster -> monster.getLocations().stream().map(locationId -> new AbstractMap.SimpleEntry<>(locationId, monster)))
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    for (Map.Entry<String, List<MonsterData>> entry : monstersByLocation.entrySet()) {
      String locationId = entry.getKey();
      LocationData location = LocationManager.getLocation(locationId);
      String locationName = location != null ? location.getNameKo() : locationId;

      System.out.println("\n📍 " + locationName + ":");
      for (MonsterData monster : entry.getValue()) {
        System.out.printf("  • %s (레벨 %d-%d) - %s%n", monster.getName(), monster.getMinLevel(), monster.getMaxLevel(), monster.getRarity());
      }
    }
  }

  /**
   * 희귀도별 몬스터 표시
   */
  private void showMonstersByRarity() {
    System.out.println("\n✨ === 희귀도별 몬스터 ===");

    Map<String, List<MonsterData>> monstersByRarity =
        MonsterDataLoader.loadAllMonsters().values().stream().collect(Collectors.groupingBy(MonsterData::getRarity));

    String[] rarityOrder = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHICAL"};

    for (String rarity : rarityOrder) {
      List<MonsterData> monsters = monstersByRarity.get(rarity);
      if (monsters != null && !monsters.isEmpty()) {
        System.out.println("\n" + getRarityEmoji(rarity) + " " + rarity + ":");

        for (MonsterData monster : monsters) {
          System.out.printf("  • %s (레벨 %d-%d)%n", monster.getName(), monster.getMinLevel(), monster.getMaxLevel());
        }
      }
    }
  }

  /**
   * 희귀도 이모지 반환
   */
  private String getRarityEmoji(String rarity) {
    return switch (rarity) {
      case "COMMON" -> "⚪";
      case "UNCOMMON" -> "🟢";
      case "RARE" -> "🔵";
      case "EPIC" -> "🟣";
      case "LEGENDARY" -> "🟡";
      case "MYTHICAL" -> "🔴";
      default -> "❓";
    };
  }

  /**
   * 몬스터 검색
   */
  private void searchMonsters() {
    String searchTerm = InputValidator.getStringInput("몬스터 이름 검색: ", 1, 20);

    if (searchTerm.trim().isEmpty()) {
      System.out.println("❌ 검색어를 입력해주세요.");
      return;
    }

    System.out.println("\n🔍 === '" + searchTerm + "' 검색 결과 ===");

    List<MonsterData> allMonsters = new ArrayList<>(MonsterDataLoader.loadAllMonsters().values());
    List<MonsterData> searchResults = allMonsters.stream()
        .filter(monster -> monster.getName().contains(searchTerm) || monster.getDescription().contains(searchTerm)).collect(Collectors.toList());

    if (searchResults.isEmpty()) {
      System.out.println("❌ 검색 결과가 없습니다.");
      return;
    }

    for (MonsterData monster : searchResults) {
      System.out.printf("\n👹 %s%n", monster.getName());
      System.out.printf("   📝 %s%n", monster.getDescription());
      System.out.printf("   📊 레벨: %d-%d | 희귀도: %s%n", monster.getMinLevel(), monster.getMaxLevel(), monster.getRarity());

      // 서식지 표시
      List<String> locationNames = monster.getLocations().stream().map(LocationManager::getLocationName).collect(Collectors.toList());
      System.out.printf("   🗺️ 서식지: %s%n", String.join(", ", locationNames));
    }
  }
}
