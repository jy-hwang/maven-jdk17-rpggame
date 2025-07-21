package rpg.core.engine;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import rpg.core.exploration.ExploreResultData;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameItem;
import rpg.domain.location.LocationData;
import rpg.domain.monster.MonsterData;
import rpg.domain.player.Player;
import rpg.domain.skill.Skill;
import rpg.infrastructure.data.loader.MonsterDataLoader;
import rpg.infrastructure.persistence.GameDataRepository;
import rpg.presentation.controller.InventoryController;
import rpg.presentation.controller.QuestController;
import rpg.presentation.controller.ShopController;
import rpg.presentation.menu.GameMenu;
import rpg.presentation.menu.MainMenu;
import rpg.shared.constant.GameConstants;
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
  
  private final Random random;
  
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

  private MainMenu mainMenu;
  private GameMenu gameMenu;

  public GameEngine() {
    this.gameRunning = true;
    this.inGameLoop = false;
    this.gameState = new GameState();
    this.gameStartTime = System.currentTimeMillis();
    this.currentSaveSlot = 0;
    this.random = new Random();

    // 단계별 초기화
    initializeBasicControllers();
    logger.info("게임 엔진 기본 초기화 완료 (v{})", SystemConstants.GAME_VERSION);
  }

  /**
   * 플레이어 독립적인 기본 컨트롤러들을 초기화합니다.
   */
  private void initializeBasicControllers() {
    try {
      // 1단계: 플레이어 독립적인 컨트롤러들
      mainMenu = new MainMenu();
      gameMenu = new GameMenu();

      inventoryController = new InventoryController();
      saveGameController = new SaveGameController();

      // 2단계: 나머지는 플레이어 생성 후 초기화
      logger.debug("기본 컨트롤러 초기화 완료");

    } catch (Exception e) {
      logger.error("기본 컨트롤러 초기화 실패", e);
      throw new RuntimeException("게임 초기화 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 플레이어 종속적인 컨트롤러들을 초기화합니다.
   */
  private void initializePlayerDependentControllers() {
    if (player == null) {
      throw new IllegalStateException("플레이어가 없으면 컨트롤러를 초기화할 수 없습니다.");
    }

    try {
      QuestManager questManager = player.getQuestManager();

      // 1단계: QuestController 먼저 초기화
      questController = new QuestController(questManager, gameState, player);
      logger.debug("QuestController 초기화 완료");

      // 2단계: 나머지 컨트롤러들 초기화
      battleController = new BattleEngine(questManager, gameState);
      shopController = new ShopController(inventoryController);
      exploreController = new ExploreEngine(battleController, questController, inventoryController, gameState);

      // 3단계: 디버그 컨트롤러 (선택적)
      if (SystemConstants.DEBUG_MODE) {
        debugController = new DebugController(player);
      }

      logger.debug("플레이어 종속 컨트롤러 초기화 완료");

    } catch (Exception e) {
      logger.error("플레이어 종속 컨트롤러 초기화 실패", e);
      throw new RuntimeException("컨트롤러 초기화 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 게임을 시작합니다.
   */
  public void start() {
    try {
      logger.info("게임 시작 (v" + SystemConstants.GAME_VERSION + ")");
      mainMenu.showWelcomeMessage();

      // 메인 메뉴 루프
      while (gameRunning) {
        mainMenu.showMainMenu();
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
   * 새 게임을 시작합니다.
   */
  private void startNewGame() {
    try {
      String name = InputValidator.getStringInput("캐릭터 이름: ", 2, 20);

      // 1단계: 플레이어 생성
      player = new Player(name);
      logger.info("새 플레이어 생성: {}", name);

      gameStartTime = System.currentTimeMillis();

      // 2단계: 플레이어 종속 컨트롤러들 초기화
      initializePlayerDependentControllers();

      // 3단계: 게임 초기화
      giveStartingItems();
      player.displayStats();

      // 4단계: 일일 퀘스트 생성
      // player.getQuestManager().generateDailyQuests(player);

      logger.info("새 게임 초기화 완료");
      System.out.println("\n💡 퀘스트 메뉴에서 첫 번째 퀘스트를 수락해보세요!");

      // 5단계: 게임 시작
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
      // 1단계: 플레이어와 게임 상태 복원
      player = result.getPlayer();
      gameState = result.getGameState();
      currentSaveSlot = result.getSlotNumber();
      gameStartTime = System.currentTimeMillis();

      // 2단계: 컨트롤러 재초기화
      initializePlayerDependentControllers();

      // 3단계: 게임 시작
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
   * 컨트롤러 상태 검증
   */
  private void validateControllers() {
    if (player != null) {
      // 플레이어가 있을 때는 모든 컨트롤러가 초기화되어야 함
      if (questController == null) {
        throw new IllegalStateException("QuestController가 초기화되지 않았습니다.");
      }
      if (battleController == null) {
        throw new IllegalStateException("BattleController가 초기화되지 않았습니다.");
      }
      if (exploreController == null) {
        throw new IllegalStateException("ExploreController가 초기화되지 않았습니다.");
      }
    }

    // 기본 컨트롤러들은 항상 초기화되어야 함
    if (inventoryController == null) {
      throw new IllegalStateException("InventoryController가 초기화되지 않았습니다.");
    }
    if (saveGameController == null) {
      throw new IllegalStateException("SaveGameController가 초기화되지 않았습니다.");
    }
  }

  /**
   * 새로운 게임 상태로 컨트롤러들을 업데이트합니다.
   */
  private void updateControllersWithNewGameState() {
    if (player == null) {
      logger.warn("플레이어가 null인 상태에서 컨트롤러 업데이트 시도");
      return;
    }

    // 플레이어 종속 컨트롤러들 재초기화
    initializePlayerDependentControllers();
    logger.debug("컨트롤러 업데이트 완료");
  }

  /**
   * 메인 게임 루프를 실행합니다.
   */
  private void startGameLoop() {
    validateControllers();

    inGameLoop = true;

    while (inGameLoop && player.isAlive()) {
      try {
        gameMenu.showInGameMenu();
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
            if (questController == null) {
              System.out.println("❌ 퀘스트 시스템이 초기화되지 않았습니다.");
              break;
            }
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
    if (exploreController == null) {
      System.out.println("❌ 탐험 시스템이 초기화되지 않았습니다.");
      return;
    }

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
    System.out.println("\n=== 🗡️ 탐험 메뉴 ===");
    System.out.println("(갈 수 있는 지역이 레벨별 추천에 맞춰서 정렬되어 보임)");
    System.out.println("현재 레벨: " + player.getLevel());

    List<LocationData> availableLocations = LocationManager.getAvailableLocations(player.getLevel());

    for (int i = 0; i < availableLocations.size(); i++) {
      LocationData location = availableLocations.get(i);
      String difficultyColor = "(" + location.getDangerLevel().toString() + ")";
      String recommendationText = getRecommendationText(location, player.getLevel());

      System.out.printf("%d. %s %s%s\t%s %n", i + 1, location.getIcon(), location.getNameKo(), recommendationText, difficultyColor);
    }

    System.out.println("0. 🏠 마을로 돌아가기");
    System.out.println("==================");
  }

  /**
   * 특정 지역으로 탐험을 진행합니다. (LocationData 기반)
   */
  private void exploreSpecificLocation(LocationData location) {
    displayLocationEntry(location);

    // 지역 정보 표시
    showLocationInfo(location);

    // 탐험 실행
    ExploreResultData resultData = exploreController.exploreLocation(player, location.getId());

    // 결과 처리 및 표시
    processExplorationResult(resultData);

    // 후처리
    handlePostExplorationActions(resultData);

    // 탐험 후 잠시 대기
    InputValidator.waitForAnyKey("\n계속하려면 Enter를 누르세요...");
  }

  /**
   * 🌟 지역 진입 시 연출 개선
   */
  private void displayLocationEntry(LocationData location) {
    System.out.println("\n" + "=".repeat(50));
    System.out.println("🚀 " + location.getNameKo() + "(으)로 향합니다!");
    System.out.println("=".repeat(50));

    // 현재 위치 설정
    gameState.setCurrentLocation(location.getNameKo());

    // 지역별 분위기 연출
    displayLocationAtmosphere(location);
  }

  /**
   * 🎭 지역별 분위기 연출
   */
  private void displayLocationAtmosphere(LocationData location) {
    String atmosphere = switch (location.getId()) {
      case "forest" -> "🌲 바람에 나뭇잎이 바스락거리며 신비로운 기운이 감돕니다...";
      case "cave" -> "🕳️ 동굴 깊숙한 곳에서 차가운 바람이 불어옵니다...";
      case "mountain" -> "⛰️ 높은 산 정상에서 구름이 발밑으로 흘러갑니다...";
      case "lake" -> "🏞️ 맑은 호수면에 달빛이 반짝이며 잔물결이 일고 있습니다...";
      case "ruins" -> "🏛️ 오래된 유적에서 고대의 신비로운 힘이 느껴집니다...";
      case "volcano" -> "🌋 뜨거운 용암의 열기가 피부를 태우듯 뜨겁습니다...";
      default -> "🗺️ 새로운 모험이 기다리고 있습니다...";
    };

    System.out.println(atmosphere);
    System.out.println();

    // 잠시 대기 (몰입감 증대)
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 📊 지역 정보 표시 개선
   */
  private void showLocationInfo(LocationData location) {
    System.out.println("📍 지역 정보:");
    System.out.printf("   %s %s (위험도: %s)%n", location.getIcon(), location.getNameKo(), location.getDangerLevel());
    System.out.printf("   권장 레벨: %d-%d (현재: %d)%n", location.getMinLevel(), location.getMaxLevel(), player.getLevel());
    System.out.printf("   이벤트 확률: %d%% ", // | 입장료: %d골드%n",
        location.getEventChance());// , location.getEntryCost());

    // 난이도 표시
    String difficultyText = calculateLocationDifficulty(location);
    System.out.println("   예상 난이도: " + difficultyText);
    System.out.println();
  }

  /**
   * 🎯 대폭 개선된 탐험 결과 처리
   * - ExploreResultData 기반
   * - 결과별 맞춤 처리
   * - 향상된 피드백
   */
  private void processExplorationResult(ExploreResultData resultData) {
    ExploreResult result = resultData.getResult();
    String message = resultData.getMessage();

    // 결과 타입별 아이콘과 색상 적용
    String resultIcon = getResultIcon(result);
    //String coloredMessage = applyResultColor(message, result);

    System.out.println("\n" + "=".repeat(40));
    System.out.println(resultIcon + " 탐험 결과");
    System.out.println("=".repeat(40));
    System.out.println(message);

    // 세부 결과 처리
    handleSpecificResult(result, resultData);

    // 공통 후처리
    handleCommonPostProcessing(resultData);

    System.out.println("=".repeat(40));
  }

  /**
   * 🎨 결과별 아이콘 반환
   */
  private String getResultIcon(ExploreResult result) {
    return switch (result) {
      case BATTLE_VICTORY -> "🏆";
      case BATTLE_DEFEAT -> "💀";
      case BATTLE_ESCAPED -> "🏃‍♂️";
      case TREASURE -> "💎";
      case KNOWLEDGE -> "📚";
      case REST -> "😴";
      case HEALING_SPRING -> "💧";
      case MAGIC_CRYSTAL -> "🔮";
      case SHRINE_BLESSING -> "⛩️";
      case ERROR -> "❌";
    };
  }

  /**
   * 🌈 결과별 색상 적용
   */
  private String applyResultColor(String message, ExploreResult result) {
    return switch (result) {
      case BATTLE_VICTORY, TREASURE, KNOWLEDGE, HEALING_SPRING, MAGIC_CRYSTAL, SHRINE_BLESSING -> ConsoleColors.GREEN + message + ConsoleColors.RESET;
      case BATTLE_DEFEAT, ERROR -> ConsoleColors.RED + message + ConsoleColors.RESET;
      case BATTLE_ESCAPED -> ConsoleColors.YELLOW + message + ConsoleColors.RESET;
      case REST -> ConsoleColors.CYAN + message + ConsoleColors.RESET;
    };
  }

  /**
   * 🎯 결과별 세부 처리
   */
  private void handleSpecificResult(ExploreResult result, ExploreResultData resultData) {
    switch (result) {
      case BATTLE_VICTORY -> handleBattleVictory();
      case BATTLE_DEFEAT -> handleBattleDefeat();
      case BATTLE_ESCAPED -> handleBattleEscape();
      case TREASURE -> handleTreasureFound();
      case KNOWLEDGE -> handleKnowledgeGained();
      case REST -> handleRestEvent();
      case HEALING_SPRING -> handleHealingSpring();
      case MAGIC_CRYSTAL -> handleMagicCrystal();
      case SHRINE_BLESSING -> handleShrineBlessing();
      case ERROR -> handleError(resultData.getMessage());
    }
  }

  /**
   * ⚔️ 전투 승리 처리
   */
  private void handleBattleVictory() {
    System.out.println("🎉 전투에서 승리했습니다!");

    // 레벨업 체크
    if (checkAndHandleLevelUp()) {
      return; // 레벨업 시 추가 처리 스킵
    }

    // 현재 상태 표시
    showPlayerStatusBrief();

    // 퀘스트 진행도 체크
    checkQuestProgress();
  }

  /**
   * 💀 전투 패배 처리
   */
  private void handleBattleDefeat() {
    System.out.println("💔 전투에서 패배했습니다...");

    if (!player.isAlive()) {
      handlePlayerDeath();
    } else {
      handleDefeatPenalty();
    }
  }

  /**
   * 🏃‍♂️ 전투 도망 처리
   */
  private void handleBattleEscape() {
    System.out.println("💨 위험을 피해 안전하게 도망쳤습니다.");
    // 도망에 대한 특별한 처리는 없음
  }

  /**
   * 💎 보물 발견 처리
   */
  private void handleTreasureFound() {
    System.out.println("✨ 귀중한 보물을 발견했습니다!");
    playTreasureEffect();
    updateTreasureStatistics();
  }

  /**
   * 📚 지식 습득 처리
   */
  private void handleKnowledgeGained() {
    System.out.println("🧠 고대의 지혜를 터득했습니다!");

    if (checkAndHandleLevelUp()) {
      System.out.println("💡 지식의 힘으로 한층 더 성장했습니다!");
    }
  }

  /**
   * 😴 휴식 처리
   */
  private void handleRestEvent() {
    System.out.println("💤 편안한 휴식을 취했습니다.");
    showRecoveryStatus();
  }

  /**
   * 💧 치유의 샘 처리
   */
  private void handleHealingSpring() {
    System.out.println("🌟 신비한 치유의 힘을 받았습니다!");
    System.out.println("💝 체력이 완전히 회복되었습니다!");
    showRecoveryStatus();
  }

  /**
   * 🔮 마법 크리스탈 처리
   */
  private void handleMagicCrystal() {
    System.out.println("✨ 마법의 힘이 온몸에 흘러들어옵니다!");
    System.out.println("🌙 마나가 완전히 회복되었습니다!");
    showRecoveryStatus();
  }

  /**
   * ⛩️ 제단 축복 처리
   */
  private void handleShrineBlessing() {
    System.out.println("🙏 신성한 축복을 받았습니다!");
    System.out.println("🌟 신비한 힘이 당신을 보호할 것입니다.");
    showBlessingEffect();
  }

  /**
   * ❌ 오류 처리
   */
  private void handleError(String errorMessage) {
    System.out.println("⚠️ 예상치 못한 일이 발생했습니다: " + errorMessage);
    logger.warn("탐험 중 오류 발생: {}", errorMessage);
  }

  /**
   * 🎯 공통 후처리
   */
  private void handleCommonPostProcessing(ExploreResultData resultData) {
    // 긍정적 결과에 대한 추가 보상
    if (resultData.isPositive()) {
      // 통계 업데이트
      updateGameStatistics(resultData.getResult());

      // 행운의 보너스 체크 (낮은 확률)
      checkLuckyBonus();
    }

    // 퀘스트 상태 확인
    questController.checkQuestCompletion();

  }

  /**
   * 🎯 탐험 후 액션 처리
   */
  private void handlePostExplorationActions(ExploreResultData resultData) {
    // 인벤토리 가득참 경고
    checkInventoryWarning();

    // 체력/마나 부족 경고
    checkHealthWarning();

    // 장비 내구도 경고 (향후 구현)
    // checkEquipmentDurability();

    // 추천 액션 제안
    suggestNextActions(resultData);
  }

  // === 유틸리티 메서드들 ===

  /**
   * 레벨업 체크 및 처리
   */
  private boolean checkAndHandleLevelUp() {
    // 이미 gainExperience()에서 레벨업이 처리되었고 결과를 받았다면
    // 여기서는 GameEngine 차원의 추가 처리만 수행
    
    // 예: 새로운 지역 해금 알림, 특별 이벤트 등
    // System.out.println("🗺️ 새로운 지역이 해금될 수 있습니다!");
    
    return true; // gainExperience()의 반환값을 그대로 전달받아 사용
  }

  /**
   * 패배 페널티 처리
   */
  private void handleDefeatPenalty() {
    // 체력을 1로 설정 (완전 사망 방지)
    player.setHp(1);

    // 경험치 약간 감소
    int expLoss = Math.max(1, player.getLevel() * 3);
    player.gainExp(expLoss);

    System.out.println("💔 패배로 인해 경험치 " + expLoss + "를 잃었습니다.");
    System.out.println("🏥 안전한 곳으로 이동되었습니다.");

    // 시작 지역으로 이동
    gameState.setCurrentLocation("마을");
  }

  /**
   * 플레이어 사망 처리
   */
  private void handlePlayerDeath() {
    System.out.println("\n💀 " + player.getName() + "이(가) 쓰러졌습니다...");
    System.out.println("게임 오버! 메인 메뉴로 돌아갑니다.");

    // 게임 종료
    gameRunning = false;
    inGameLoop = false;
  }

  /**
   * 지역 난이도 계산
   */
  private String calculateLocationDifficulty(LocationData location) {
    int playerLevel = player.getLevel();
    int avgLocationLevel = (location.getMinLevel() + location.getMaxLevel()) / 2;
    int levelDiff = avgLocationLevel - playerLevel;

    if (levelDiff <= -3) {
      return "🟢 쉬움";
    } else if (levelDiff <= 0) {
      return "🟡 적정";
    } else if (levelDiff <= 3) {
      return "🔴 어려움";
    } else {
      return "💀 매우 어려움";
    }
  }

  /**
   * 플레이어 상태 간단 표시
   */
  private void showPlayerStatusBrief() {
    System.out.printf("📊 상태: Lv.%d | HP: %d/%d | MP: %d/%d | EXP: %d%n", player.getLevel(), player.getHp(), player.getMaxHp(), player.getMp(),
        player.getMaxMp(), player.getExp());
  }

  /**
   * 회복 상태 표시
   */
  private void showRecoveryStatus() {
    System.out.printf("💝 회복 완료 - HP: %d/%d, MP: %d/%d%n", player.getHp(), player.getMaxHp(), player.getMp(), player.getMaxMp());
  }

  /**
   * 보물 효과 연출
   */
  private void playTreasureEffect() {
    System.out.println("♪♫♪ 짜잔~ ♪♫♪");
  }

  /**
   * 축복 효과 표시
   */
  private void showBlessingEffect() {
    System.out.println("🌟 ✨ 🌟 ✨ 🌟");
    System.out.println("신성한 기운이 당신을 감쌉니다...");
  }

  /**
   * 인벤토리 경고 체크
   */
  private void checkInventoryWarning() {
    double usage = inventoryController.getInventoryUsageRate(player);
    if (usage > 0.9) {
      System.out.println("⚠️ 인벤토리가 거의 가득 찼습니다! (" + String.format("%.0f%%", usage * 100) + ")");
    }
  }

  /**
   * 체력 경고 체크
   */
  private void checkHealthWarning() {
    double hpRate = (double) player.getHp() / player.getMaxHp();
    double mpRate = (double) player.getMp() / player.getMaxMp();

    if (hpRate < 0.3) {
      System.out.println("⚠️ 체력이 부족합니다! 휴식을 권장합니다.");
    }
    if (mpRate < 0.3) {
      System.out.println("⚠️ 마나가 부족합니다! 회복을 권장합니다.");
    }
  }

  /**
   * 행운의 보너스 체크
   */
  private void checkLuckyBonus() {
    if (random.nextInt(GameConstants.NUMBER_HUNDRED) < GameConstants.NUMBER_TEN) { // 10% 확률
      int bonusGold = player.getLevel() * GameConstants.NUMBER_TEN;
      player.addGold(bonusGold);
      System.out.println("🍀 행운의 보너스! 골드 +" + bonusGold);
    }
  }

  /**
   * 다음 액션 추천
   */
  private void suggestNextActions(ExploreResultData resultData) {
    System.out.println("\n💡 추천 액션:");

    if (player.getHp() < player.getMaxHp() * 0.5) {
      System.out.println("   🏥 체력 회복 (휴식 또는 아이템 사용)");
    }

    if (inventoryController.getInventoryUsageRate(player) > 0.8) {
      System.out.println("   🛒 상점에서 아이템 판매");
    }

    if (questController.hasCompletableQuests()) {
      System.out.println("   🎯 퀘스트 완료 및 보상 수령");
    }

    if (player.getLevel() >= getRecommendedLevelForNextArea()) {
      System.out.println("   🗺️ 더 높은 레벨 지역 탐험");
    }
  }

  // 기타 필요한 메서드들...
  private void updateTreasureStatistics() { /* 구현 */ }

  private void updateGameStatistics(ExploreResult result) { /* 구현 */ }

  private void checkQuestProgress() { /* 구현 */ }

    private int getRecommendedLevelForNextArea() {
    return player.getLevel() + 5;
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
   * 추천 텍스트를 생성합니다. (LocationData 기반)
   */
  private String getRecommendationText(LocationData location, int playerLevel) {
    if (playerLevel >= location.getMinLevel() && playerLevel <= location.getMaxLevel()) {
      return "[추천]";
    } else if (playerLevel < location.getMaxLevel() + 3) {
      return "[적정]";
    } else if (playerLevel > location.getMaxLevel()) {
      return "[쉬움]";
    } else {
      return "[위험]";
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
    // showLocationStatistics(location, locationId);
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
      // List<MonsterData> locationMonsters = MonsterDataLoader.getMonstersByLocation(location.getId());
      // System.out.println(" 몬스터: " + locationMonsters.size() + "종");

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

    // 🆕 추가: 도움말에서만 표시되는 통계 정보
    System.out.println("\n📊 === 게임 세계 통계 ===");

    // 지역 통계
    showHelpLocationStatistics();

    // 몬스터 통계
    showHelpMonsterStatistics();

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

      // JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      // System.out.printf("• 퀘스트 템플릿: 메인 %d개, 사이드 %d개, 일일 %d개\n", questFactory.getQuestCount("MAIN"),
      // questFactory.getQuestCount("SIDE"), questFactory.getQuestCount("DAILY"));
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
      // System.out.println("5. 몬스터 통계");
      System.out.println("5. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 5);

      switch (choice) {
        case 1 -> showMonstersByLocation();
        case 2 -> showMonstersByLevel();
        case 3 -> showMonstersByRarity();
        case 4 -> searchMonsters();
        // case 5 -> MonsterDataLoader.printMonsterStatistics();
        case 5 -> {
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

  /**
   * 컨트롤러 초기화 상태 열거형
   */
  public enum ControllerInitializationState {
    BASIC_ONLY, // 기본 컨트롤러만 초기화
    PLAYER_DEPENDENT, // 플레이어 종속 컨트롤러까지 초기화
    FULLY_INITIALIZED // 모든 컨트롤러 초기화 완료
  }

  /**
   * 초기화 상태 추적
   */
  private ControllerInitializationState initializationState = ControllerInitializationState.BASIC_ONLY;

  /**
   * 현재 초기화 상태 반환
   */
  public ControllerInitializationState getInitializationState() {
    return initializationState;
  }

  /**
   * 게임 상태 디버그 정보 출력
   */
  public void printGameEngineStatus() {
    System.out.println("\n=== 🎮 GameEngine 상태 ===");
    System.out.printf("초기화 상태: %s\n", initializationState);
    System.out.printf("플레이어: %s\n", player != null ? player.getName() : "없음");
    System.out.printf("게임 실행 중: %s\n", gameRunning ? "예" : "아니오");
    System.out.printf("인게임 루프: %s\n", inGameLoop ? "예" : "아니오");

    System.out.println("\n컨트롤러 상태:");
    System.out.printf("  InventoryController: %s\n", inventoryController != null ? "✅" : "❌");
    System.out.printf("  QuestController: %s\n", questController != null ? "✅" : "❌");
    System.out.printf("  BattleEngine: %s\n", battleController != null ? "✅" : "❌");
    System.out.printf("  ExploreEngine: %s\n", exploreController != null ? "✅" : "❌");
    System.out.printf("  ShopController: %s\n", shopController != null ? "✅" : "❌");
    System.out.printf("  DebugController: %s\n", debugController != null ? "✅" : "❌");

    System.out.println("========================");
  }

  // 5. 도움말 전용 지역 통계 메서드 추가
  private void showHelpLocationStatistics() {
    System.out.println("\n🗺️ 지역 통계:");

    List<LocationData> allLocations = LocationManager.getAllLocations();
    System.out.println("• 총 지역 수: " + allLocations.size() + "개");

    // 난이도별 분포
    Map<String, Long> dangerLevelStats =
        allLocations.stream().collect(Collectors.groupingBy(location -> location.getDangerLevel().getDisplayName(), Collectors.counting()));

    System.out.println("• 난이도별 분포:");
    dangerLevelStats.forEach((level, count) -> System.out.println("  " + level + ": " + count + "개"));

    // 레벨 범위
    int minLocationLevel = allLocations.stream().mapToInt(LocationData::getMinLevel).min().orElse(1);
    int maxLocationLevel = allLocations.stream().mapToInt(LocationData::getMaxLevel).max().orElse(50);

    System.out.println("• 레벨 범위: " + minLocationLevel + " ~ " + maxLocationLevel);
  }

  // 6. 도움말 전용 몬스터 통계 메서드 추가
  private void showHelpMonsterStatistics() {
    System.out.println("\n👹 몬스터 통계:");

    // MonsterDataLoader의 printMonsterStatistics() 내용을 여기로 이동
    List<MonsterData> allMonsters = MonsterDataLoader.getAllMonsters();
    System.out.println("• 총 몬스터 종류: " + allMonsters.size() + "종");

    // 희귀도별 통계
    Map<String, Long> rarityStats = allMonsters.stream().collect(Collectors.groupingBy(MonsterData::getRarity, Collectors.counting()));

    System.out.println("• 희귀도별 분포:");
    rarityStats.forEach((rarity, count) -> System.out.println("  " + rarity + ": " + count + "종"));

    // 지역별 통계 (상위 5개만)
    Map<String, Long> locationStats = allMonsters.stream().flatMap(monster -> monster.getLocations().stream())
        .collect(Collectors.groupingBy(location -> location, Collectors.counting()));

    System.out.println("• 주요 지역별 분포:");
    locationStats.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5) // 상위 5개만 표시
        .forEach(entry -> {
          String locationName = LocationManager.getLocationName(entry.getKey());
          System.out.println("  " + locationName + ": " + entry.getValue() + "종");
        });

    // 레벨 분포
    IntSummaryStatistics levelStats =
        allMonsters.stream().mapToInt(monster -> (monster.getMinLevel() + monster.getMaxLevel()) / 2).summaryStatistics();

    System.out
        .println("• 레벨 분포: 최소 " + levelStats.getMin() + " | 최대 " + levelStats.getMax() + " | 평균 " + String.format("%.1f", levelStats.getAverage()));
  }

}
