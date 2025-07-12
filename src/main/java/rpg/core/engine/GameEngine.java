package rpg.core.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameEffectFactory;
import rpg.application.factory.GameItemFactory;
import rpg.application.factory.JsonBasedQuestFactory;
import rpg.application.service.QuestManager;
import rpg.application.validator.InputValidator;
import rpg.core.battle.BattleEngine;
import rpg.core.exploration.ExploreEngine;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameConsumable;
import rpg.domain.item.GameItem;
import rpg.domain.item.GameItemData;
import rpg.domain.item.ItemRarity;
import rpg.domain.item.effect.GameEffect;
import rpg.domain.monster.Monster;
import rpg.domain.monster.MonsterData;
import rpg.domain.player.Player;
import rpg.domain.quest.Quest;
import rpg.domain.quest.Quest.QuestType;
import rpg.domain.skill.Skill;
import rpg.infrastructure.data.loader.ConfigDataLoader;
import rpg.infrastructure.data.loader.MonsterDataLoader;
import rpg.infrastructure.data.loader.QuestTemplateLoader;
import rpg.infrastructure.persistence.GameDataRepository;
import rpg.infrastructure.persistence.SaveSlotInfo;
import rpg.presentation.controller.InventoryController;
import rpg.presentation.controller.QuestController;
import rpg.presentation.controller.ShopController;
import rpg.shared.constant.SystemConstants;

/**
 * 리팩토링된 메인 게임 컨트롤러 각 기능별 Controller들을 조율하는 역할
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

  public GameEngine() {
    this.gameRunning = true;
    this.inGameLoop = false;
    this.gameState = new GameState();
    this.gameStartTime = System.currentTimeMillis();
    this.currentSaveSlot = 0;

    initializeControllers();
    logger.info("게임 인스턴스 생성 완료 (v" + SystemConstants.GAME_VERSION + "- 상점판매기능 추가)");
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
    System.out.println("====================================");
    System.out.println("   🎮 RPG 게임 v" + SystemConstants.GAME_VERSION + " 🎮   ");
    System.out.println("====================================");
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
    System.out.println("\n=== 🎮 메인 메뉴 ===");
    System.out.println("1. 🆕 새로하기");
    System.out.println("2. 📁 불러오기");
    System.out.println("3. 🚪 종료하기");
    System.out.println("==================");

    // 저장된 게임이 있는지 확인해서 표시
    showSaveFileInfo();
  }

  /**
   * 저장 파일 정보를 표시합니다.
   */
  private void showSaveFileInfo() {
    var saveSlots = GameDataRepository.getAllSaveSlots();
    long occupiedSlots = saveSlots.stream().filter(SaveSlotInfo::isOccupied).count();

    if (occupiedSlots > 0) {
      System.out.println("💾 저장된 게임: " + occupiedSlots + "개");
    } else {
      System.out.println("💾 저장된 게임이 없습니다.");
    }
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

  /**
   * 게임을 불러옵니다.
   */
  private void loadGame() {
    try {
      // 저장 슬롯 목록 표시
      GameDataRepository.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0)
        return;

      GameDataRepository.SaveData saveData = GameDataRepository.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        return;
      }

      player = saveData.getCharacter();
      gameState = saveData.getGameState();
      currentSaveSlot = slotNumber;
      gameStartTime = System.currentTimeMillis(); // 플레이 시간을 새로 시작

      // 🔥 로드 후 데이터 검증 (추가된 부분)
      player.validateLoadedData();

      // 컨트롤러들에 새로운 gameState 적용
      updateControllersWithNewGameState();

      QuestManager questManager = player.getQuestManager();
      int activeCount = questManager.getActiveQuests().size();
      int completedCount = questManager.getCompletedQuests().size();

      if (activeCount > 0 || completedCount > 0) {
        System.out.println("📋 퀘스트 진행 상황이 복원되었습니다: 활성 " + activeCount + "개, 완료 " + completedCount + "개");
      }

      System.out.println("🎮 슬롯 " + slotNumber + "에서 게임을 불러왔습니다!");
      System.out.println("어서오세요, " + player.getName() + "님!");
      player.displayStats();
      gameState.displayGameStats();

      // 🔥 착용 장비 상태 확인 메시지 (추가된 부분)
      PlayerInventory inventory = player.getInventory();
      int equippedCount = 0;
      if (inventory.getEquippedWeapon() != null)
        equippedCount++;
      if (inventory.getEquippedArmor() != null)
        equippedCount++;
      if (inventory.getEquippedAccessory() != null)
        equippedCount++;

      if (equippedCount > 0) {
        System.out.println("⚔️ 착용 장비 " + equippedCount + "개가 복원되었습니다.");
      }

      logger.info("슬롯 {} 기존 캐릭터 로드: {}", slotNumber, player.getName());

      // 인게임 루프 시작
      startGameLoop();

    } catch (GameDataRepository.GameDataException e) {
      logger.error("게임 로드 실패", e);
      System.out.println("게임 로드 실패: " + e.getMessage());
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
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

    questController = new QuestController(questManager, gameState);
    battleController = new BattleEngine(questManager, gameState);
    exploreController = new ExploreEngine(battleController, questController, inventoryController, gameState);

  }

  /**
   * 메인 게임 루프를 실행합니다.
   */
  private void startGameLoop() {
    inGameLoop = true;

    while (inGameLoop && player.isAlive()) {
      try {
        showInGameMenu();
        int choice = 0;
        if (!SystemConstants.DEBUG_MODE) {
          choice = InputValidator.getIntInput("선택: ", 1, 12);
        } else {
          choice = InputValidator.getIntInput("선택: ", 1, 99);
        }
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
            saveGame();
            break;
          case 10:
            manageSaveSlots();
            break;
          case 11:
            returnToMainMenu();
            break;
          case 12:
            showHelp();
            break;

          case 21:
            if (SystemConstants.DEBUG_MODE) {
              testFactories();
            }
            break;
          case 22:
            if (SystemConstants.DEBUG_MODE) {
              testRandomItemGeneration();
            }
            break;
          case 23:
            if (SystemConstants.DEBUG_MODE) {
              testQuestTemplates();
            }
            break;
          case 24:
            if (SystemConstants.DEBUG_MODE) {
              testQuestSystem();
            }
            break;
          case 25:
            if (SystemConstants.DEBUG_MODE) {
              reloadAllGameData();
            }
            break;
          case 26:
            if (SystemConstants.DEBUG_MODE) {
              MonsterDataLoader.printMonsterStatistics();
            }
            break;
          case 27:
            if (SystemConstants.DEBUG_MODE) {
              showItemStatistics();
            }
            break;
          case 28:
            if (SystemConstants.DEBUG_MODE) {
              createTestMonster();
            }
          case 29:
            if (SystemConstants.DEBUG_MODE) {
              createTestMonster();
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
    System.out.println("\n=== 메인 메뉴 ===");
    System.out.println("1. 🗡️ 탐험하기");
    System.out.println("2. 📊 상태 확인");
    System.out.println("3. 🎒 인벤토리");
    System.out.println("4. ⚡ 스킬 관리");
    System.out.println("5. 📋 퀘스트");
    System.out.println("6. 🏪 상점");
    System.out.println("7. 🗺️ 지역 정보"); // 새로운 기능
    System.out.println("8. 📚 몬스터 도감"); // 새로운 기능
    System.out.println("9. 💾 게임 저장");
    System.out.println("10. 📁 저장 관리");
    System.out.println("11. 🚪 게임 종료");
    System.out.println("12. ❓ 도움말");

    // 개발자 모드 메뉴 (디버그용)
    if (SystemConstants.DEBUG_MODE) {
      System.out.println("\n=== 🔧 디버그 메뉴 ===");
      System.out.println("21. 🧪 팩토리 테스트");
      System.out.println("22. 🎲 랜덤 아이템 테스트");
      System.out.println("23. 📋 퀘스트 템플릿 테스트");
      System.out.println("24. 🎯 퀘스트 시스템 테스트");
      System.out.println("25. 🔧 전체 데이터 리로드");
      System.out.println("26. 📈 몬스터 통계");
      System.out.println("27. 🎁 아이템 통계");
      System.out.println("28. 🧪 테스트 몬스터 생성");
      System.out.println("29. 🎒 테스트 아이템 생성");
      System.out.println("==================");
    }

    // 알림 표시
    showNotifications();
  }

  /**
   * 지역 정보를 표시합니다.
   */
  private void showLocationInfo() {
    while (true) {
      System.out.println("\n🗺️ === 지역 정보 ===");
      System.out.println("1. 현재 위치 몬스터 정보");
      System.out.println("2. 모든 지역 개요");
      System.out.println("3. 지역별 추천 레벨");
      System.out.println("4. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 4);

      switch (choice) {
        case 1:
          showCurrentLocationDetail();
          break;
        case 2:
          showAllLocationsOverview();
          break;
        case 3:
          showLocationRecommendations();
          break;
        case 4:
          return;
      }
    }
  }

  /**
   * 개발자 전용 - 전체 데이터 리로드
   */
  private void reloadAllGameData() {
    if (!SystemConstants.DEBUG_MODE) {
      System.out.println("개발자 모드가 활성화되지 않았습니다.");
      return;
    }

    System.out.println("전체 게임 데이터를 다시 로드합니다...");
    exploreController.reloadAllData();
    System.out.println("완료!");
  }

  /**
   * 현재 위치의 상세 정보를 표시합니다.
   */
  private void showCurrentLocationDetail() {
    String currentLocation = gameState.getCurrentLocation();

    System.out.println("\n📍 현재 위치: " + currentLocation);

    // 지역 설명
    showLocationDescription(currentLocation);

    // 현재 위치의 몬스터 정보
    exploreController.showCurrentLocationMonsters(player.getLevel());

    // 지역 통계
    showLocationStatistics(currentLocation);
  }

  /**
   * 모든 지역의 개요를 표시합니다.
   */
  private void showAllLocationsOverview() {
    String[] locations = {"숲속 깊은 곳", "고대 유적", "어두운 동굴", "험준한 산길", "신비한 호수", "폐허가 된 성", "마법의 숲", "용암 동굴"};

    System.out.println("\n🌍 === 모든 지역 개요 ===");

    for (String location : locations) {
      System.out.println("\n📍 " + location);

      // 지역별 몬스터 수
      var locationMonsters = MonsterDataLoader.getMonstersByLocation(location);
      System.out.println("   몬스터 종류: " + locationMonsters.size() + "종");

      // 추천 레벨 범위
      if (!locationMonsters.isEmpty()) {
        int minLevel = locationMonsters.stream().mapToInt(MonsterData::getMinLevel).min().orElse(1);
        int maxLevel = locationMonsters.stream().mapToInt(MonsterData::getMaxLevel).max().orElse(99);
        System.out.println("   추천 레벨: " + minLevel + " ~ " + maxLevel);
      }

      // 위험도
      String dangerLevel = getDangerLevel(location);
      System.out.println("   위험도: " + dangerLevel);
    }
  }

  /**
   * 지역별 추천 정보를 표시합니다.
   */
  private void showLocationRecommendations() {
    int playerLevel = player.getLevel();

    System.out.println("\n🎯 === 레벨 " + playerLevel + " 추천 지역 ===");

    String[] locations = {"숲속 깊은 곳", "고대 유적", "어두운 동굴", "험준한 산길", "신비한 호수", "폐허가 된 성", "마법의 숲", "용암 동굴"};

    for (String location : locations) {
      var suitableMonsters = MonsterDataLoader.getMonstersByLocationAndLevel(location, playerLevel);

      if (!suitableMonsters.isEmpty()) {
        String recommendation = getLocationRecommendation(location, playerLevel, suitableMonsters.size());
        System.out.println("✅ " + location + " - " + recommendation);
      } else {
        String reason = getUnsuitableReason(location, playerLevel);
        System.out.println("❌ " + location + " - " + reason);
      }
    }
  }

  /**
   * 몬스터 도감을 표시합니다.
   */
  private void showMonsterEncyclopedia() {
    while (true) {
      System.out.println("\n📚 === 몬스터 도감 ===");
      System.out.println("1. 조우한 몬스터 목록");
      System.out.println("2. 지역별 몬스터");
      System.out.println("3. 등급별 몬스터");
      System.out.println("4. 몬스터 검색");
      System.out.println("5. 몬스터 통계");
      System.out.println("6. 나가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 6);

      switch (choice) {
        case 1:
          showEncounteredMonsters();
          break;
        case 2:
          showMonstersByLocation();
          break;
        case 3:
          showMonstersByRarity();
          break;
        case 4:
          searchMonster();
          break;
        case 5:
          MonsterDataLoader.printMonsterStatistics();
          break;
        case 6:
          return;
      }
    }
  }

  /**
   * 조우한 몬스터 목록을 표시합니다.
   */
  private void showEncounteredMonsters() {
    // 실제 구현에서는 GameState에 조우한 몬스터 목록을 저장
    var allMonsters = MonsterDataLoader.loadAllMonsters();

    System.out.println("\n👹 === 조우한 몬스터 ===");
    System.out.println("총 " + allMonsters.size() + "종의 몬스터가 발견되었습니다.");

    // 몬스터를 레벨 순으로 정렬하여 표시
    allMonsters.values().stream().sorted((m1, m2) -> Integer.compare(m1.getMinLevel(), m2.getMinLevel())).forEach(monster -> {
      String rarity = getRarityIcon(monster.getRarity());
      int level = estimateMonsterLevel(monster);

      System.out.printf("%s %s (레벨 %d) - %s%n", rarity, monster.getName(), level, monster.getDescription());
    });
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
   * 등급별 몬스터를 표시합니다.
   */
  private void showMonstersByRarity() {
    System.out.println("\n⭐ === 등급별 몬스터 ===");

    var allMonsters = MonsterDataLoader.loadAllMonsters();

    // 등급별로 그룹화
    var monstersByRarity = allMonsters.values().stream().collect(Collectors.groupingBy(MonsterData::getRarity));

    String[] rarities = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"};

    for (String rarity : rarities) {
      var monsters = monstersByRarity.get(rarity);
      if (monsters == null || monsters.isEmpty())
        continue;

      String icon = getRarityIcon(rarity);
      System.out.println("\n" + icon + " " + rarity + " (" + monsters.size() + "종)");

      monsters.stream().sorted((m1, m2) -> Integer.compare(m1.getMinLevel(), m2.getMinLevel())).forEach(monster -> {
        int level = estimateMonsterLevel(monster);
        System.out.printf("   • %s (레벨 %d)%n", monster.getName(), level);
      });
    }
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

  /**
   * 개발자 전용 - 몬스터 데이터 리로드
   */
  private void reloadMonsterData() {
    if (!SystemConstants.DEBUG_MODE) {
      System.out.println("개발자 모드가 활성화되지 않았습니다.");
      return;
    }

    System.out.println("몬스터 데이터를 다시 로드합니다...");
    exploreController.reloadMonsterData();
    System.out.println("완료!");
  }

  /**
   * 개발자 전용 - 테스트 몬스터 생성
   */
  private void createTestMonster() {
    if (!SystemConstants.DEBUG_MODE) {
      System.out.println("개발자 모드가 활성화되지 않았습니다.");
      return;
    }

    System.out.println("테스트 몬스터를 생성합니다...");
    Monster testMonster = exploreController.getRandomMonster(player.getLevel());

    System.out.println("생성된 몬스터: " + testMonster.getName());
    System.out.printf("능력치: HP %d, 공격 %d%n", testMonster.getHp(), testMonster.getAttack());
    System.out.printf("보상: 경험치 %d, 골드 %d%n", testMonster.getExpReward(), testMonster.getGoldReward());
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
      saveGame();
    }

    // updatePlayTime(); // 플레이 시간 업데이트
    inGameLoop = false;
    System.out.println("🏠 메인 메뉴로 돌아갑니다.");

  }

  /**
   * 탐험을 처리합니다.
   */
  private void handleExploration() {
    ExploreEngine.ExploreResult result = exploreController.startExploration(player);

    // 탐험 결과에 따른 추가 처리
    switch (result.getType()) {
      case BATTLE_DEFEAT -> {
        if (!player.isAlive()) {
          System.out.println("💀 전투에서 패배했습니다...");
          gameRunning = false;
        }
      }
      case TREASURE -> {
        // 보물 관련 퀘스트 진행도 업데이트 가능
        logger.debug("보물 발견 이벤트 완료");
      }
      case KNOWLEDGE -> {
        // 지식 획득 관련 퀘스트 진행도 업데이트
        questController.updateLevelProgress(player);
      }
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
   * 게임 저장을 처리합니다.
   */
  private void saveGame() {
    try {
      updatePlayTime(); // 플레이 시간 업데이트

      // 현재 슬롯이 있으면 그 슬롯에 저장, 없으면 슬롯 선택
      if (currentSaveSlot > 0) {
        boolean useSameSlot = InputValidator.getConfirmation("현재 슬롯 " + currentSaveSlot + "에 저장하시겠습니까?");

        if (useSameSlot) {
          GameDataRepository.saveGame(player, gameState, currentSaveSlot);
          logger.info("슬롯 {} 게임 저장 완료: {}", currentSaveSlot, player.getName());
          return;
        }
      }

      // 슬롯 선택해서 저장
      chooseSlotAndSave();

    } catch (GameDataRepository.GameDataException e) {
      logger.error("게임 저장 실패", e);
      System.out.println("게임 저장 실패: " + e.getMessage());
    }
  }

  /**
   * 슬롯을 선택해서 저장합니다.
   */
  private void chooseSlotAndSave() {
    try {
      // 저장 슬롯 목록 표시
      GameDataRepository.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("저장할 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0) {
        System.out.println("저장이 취소되었습니다.");
        return;
      }

      // 슬롯이 이미 사용 중인지 확인
      var slots = GameDataRepository.getAllSaveSlots();
      var targetSlot = slots.stream().filter(slot -> slot.getSlotNumber() == slotNumber).findFirst().orElse(null);

      if (targetSlot != null && targetSlot.isOccupied()) {
        boolean overwrite =
            InputValidator.getConfirmation("슬롯 " + slotNumber + "에 이미 '" + targetSlot.getCharacterName() + "' 캐릭터가 저장되어 있습니다. 덮어쓰시겠습니까?");

        if (!overwrite) {
          System.out.println("저장이 취소되었습니다.");
          return;
        }
      }

      GameDataRepository.saveGame(player, gameState, slotNumber);
      currentSaveSlot = slotNumber; // 현재 슬롯 업데이트
      logger.info("슬롯 {} 게임 저장 완료: {}", slotNumber, player.getName());

    } catch (GameDataRepository.GameDataException e) {
      logger.error("슬롯 선택 저장 실패", e);
      System.out.println("게임 저장 실패: " + e.getMessage());
    }
  }

  /**
   * 저장 슬롯을 관리합니다.
   */
  private void manageSaveSlots() {
    while (true) {
      System.out.println("\n=== 저장 관리 ===");
      GameDataRepository.displaySaveSlots();
      System.out.println("\n1. 게임 불러오기");
      System.out.println("2. 다른 슬롯에 저장");
      System.out.println("3. 슬롯 삭제");
      System.out.println("4. 돌아가기");

      int choice = InputValidator.getIntInput("선택: ", 1, 4);

      switch (choice) {
        case 1:
          loadFromSlot();
          break;
        case 2:
          chooseSlotAndSave();
          break;
        case 3:
          deleteSlot();
          break;
        case 4:
          return;
      }
    }
  }

  /**
   * 다른 슬롯에서 게임을 불러옵니다.
   */
  private void loadFromSlot() {
    try {
      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0)
        return;

      GameDataRepository.SaveData saveData = GameDataRepository.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        return;
      }

      boolean confirmLoad = InputValidator.getConfirmation("현재 게임을 '" + saveData.getCharacter().getName() + "' 캐릭터로 교체하시겠습니까? (현재 진행사항은 저장되지 않습니다)");

      if (confirmLoad) {
        player = saveData.getCharacter();
        gameState = saveData.getGameState();
        currentSaveSlot = slotNumber;

        // 컨트롤러들에 새로운 데이터 적용
        updateControllersWithNewGameState();

        System.out.println("🎮 게임을 불러왔습니다!");
        player.displayStats();
      }

    } catch (GameDataRepository.GameDataException e) {
      System.out.println("게임 로드 실패: " + e.getMessage());
    }
  }

  /**
   * 슬롯을 삭제합니다.
   */
  private void deleteSlot() {
    int slotNumber = InputValidator.getIntInput("삭제할 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

    if (slotNumber == 0)
      return;

    if (slotNumber == currentSaveSlot) {
      System.out.println("현재 사용 중인 슬롯은 삭제할 수 없습니다.");
      return;
    }

    boolean confirmDelete = InputValidator.getConfirmation("정말로 슬롯 " + slotNumber + "를 삭제하시겠습니까? (복구할 수 없습니다)");

    if (confirmDelete) {
      GameDataRepository.deleteSaveSlot(slotNumber);
    }
  }

  /**
   * 플레이 시간을 업데이트합니다.
   */
  private void updatePlayTime() {
    long playTimeMs = System.currentTimeMillis() - gameStartTime;
    int playTimeMinutes = (int) (playTimeMs / 60000);
    gameState.addPlayTime(playTimeMinutes);
  }

  /**
   * 게임 종료 확인을 처리합니다.
   */
  private boolean confirmExit() {
    boolean shouldSave = InputValidator.getConfirmation("게임을 저장하고 종료하시겠습니까?");

    if (shouldSave) {
      saveGame();
    }

    return InputValidator.getConfirmation("정말로 게임을 종료하시겠습니까?");
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
   * 팩토리들을 테스트합니다.
   */
  private void testFactories() {
    System.out.println("\n=== 🏭 팩토리 테스트 ===");

    try {
      // GameItemFactory 테스트
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.println("✅ GameItemFactory 초기화 상태: " + itemFactory.isInitialized());
      System.out.println("📦 로드된 아이템 수: " + itemFactory.getItemCount());

      // JsonBasedQuestFactory 테스트
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      questFactory.printFactoryStatus();

      // 간단한 생성 테스트
      GameItem testItem = itemFactory.createItem("HEALTH_POTION");
      if (testItem != null) {
        System.out.println("✅ 아이템 생성 테스트 성공: " + testItem.getName());
      } else {
        System.out.println("❌ 아이템 생성 테스트 실패");
      }

      Quest testQuest = questFactory.createQuest("quest_001");
      if (testQuest != null) {
        System.out.println("✅ 퀘스트 생성 테스트 성공: " + testQuest.getTitle());
      } else {
        System.out.println("❌ 퀘스트 생성 테스트 실패");
      }

    } catch (Exception e) {
      System.out.println("❌ 팩토리 테스트 중 오류: " + e.getMessage());
      logger.error("팩토리 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 랜덤 아이템 생성을 테스트합니다.
   */
  private void testRandomItemGeneration() {
    System.out.println("\n=== 🎲 랜덤 아이템 생성 테스트 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      // 기본 테스트 실행
      factory.testRandomGeneration();

      // 인터랙티브 테스트
      System.out.println("\n🎯 인터랙티브 테스트:");
      System.out.println("1. 특정 희귀도 아이템 생성");
      System.out.println("2. 레벨별 아이템 생성");
      System.out.println("3. 특수 상황별 아이템 생성");
      System.out.println("4. 통계 보기");

      int choice = InputValidator.getIntInput("선택 (1-4): ", 1, 4);

      switch (choice) {
        case 1:
          testRarityBasedGeneration(factory);
          break;
        case 2:
          testLevelBasedGeneration(factory);
          break;
        case 3:
          testSpecialGeneration(factory);
          break;
        case 4:
          factory.printRandomGenerationStats();
          break;
      }

    } catch (Exception e) {
      System.out.println("❌ 랜덤 아이템 테스트 중 오류: " + e.getMessage());
      logger.error("랜덤 아이템 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 희귀도별 아이템 생성 테스트
   */
  private void testRarityBasedGeneration(GameItemFactory factory) {
    System.out.println("\n💎 희귀도별 생성 테스트:");

    for (ItemRarity rarity : ItemRarity.values()) {
      System.out.printf("🎲 %s 아이템 생성 중...\n", rarity.getDisplayName());

      for (int i = 0; i < 3; i++) {
        GameItem item = factory.createRandomItemByRarity(rarity);
        if (item != null) {
          System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
        } else {
          System.out.printf("   %d. 생성 실패\n", i + 1);
        }
      }
      System.out.println();
    }
  }

  /**
   * 레벨별 아이템 생성 테스트
   */
  private void testLevelBasedGeneration(GameItemFactory factory) {
    System.out.println("\n📈 레벨별 생성 테스트:");

    int[] testLevels = {1, 3, 5, 8, 12, 18, 25};

    for (int level : testLevels) {
      System.out.printf("🎯 레벨 %d 아이템:\n", level);

      for (int i = 0; i < 3; i++) {
        GameItem item = factory.createRandomItemForLevel(level);
        if (item != null) {
          System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
        }
      }
      System.out.println();
    }
  }

  /**
   * 특수 상황별 아이템 생성 테스트
   */
  private void testSpecialGeneration(GameItemFactory factory) {
    System.out.println("\n🎁 특수 상황별 생성 테스트:");

    // 보물 상자 아이템
    System.out.println("📦 보물 상자 아이템 (5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createTreasureChestItem();
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }

    // 몬스터 드롭 아이템
    System.out.println("\n⚔️ 몬스터 드롭 아이템 (레벨 7 몬스터, 5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createMonsterDropItem(7);
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }

    // 상점 아이템
    System.out.println("\n🏪 상점 아이템 (레벨 5 상점, 5개):");
    for (int i = 0; i < 5; i++) {
      GameItem item = factory.createShopItem(5);
      if (item != null) {
        System.out.printf("   %d. %s (%s)\n", i + 1, item.getName(), item.getRarity().getDisplayName());
      }
    }
  }

  /**
   * 퀘스트 템플릿을 테스트합니다.
   */
  private void testQuestTemplates() {
    System.out.println("\n=== 📋 퀘스트 템플릿 테스트 ===");

    try {
      // QuestTemplateLoader 테스트
      QuestTemplateLoader.printLoaderStatus();

      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();

      // 전체 퀘스트 목록
      factory.printAllQuests();

      // 특정 퀘스트 상세 정보
      System.out.println("\n🔍 특정 퀘스트 상세 정보:");
      String questId = InputValidator.getStringInput("퀘스트 ID 입력 (예: quest_001): ", 1, 50);
      factory.printQuestTemplateDetails(questId);

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 템플릿 테스트 중 오류: " + e.getMessage());
      logger.error("퀘스트 템플릿 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 퀘스트 시스템을 테스트합니다.
   */
  private void testQuestSystem() {
    System.out.println("\n=== 🎯 퀘스트 시스템 테스트 ===");

    try {
      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();

      System.out.println("1. 기본 퀘스트 생성 테스트");
      System.out.println("2. 동적 퀘스트 생성 테스트");
      System.out.println("3. 플레이어 레벨별 퀘스트 테스트");
      System.out.println("4. 일일/주간 퀘스트 테스트");

      int choice = InputValidator.getIntInput("선택 (1-4): ", 1, 4);

      switch (choice) {
        case 1:
          testBasicQuestGeneration(factory);
          break;
        case 2:
          testDynamicQuestGeneration(factory);
          break;
        case 3:
          testLevelBasedQuestGeneration(factory);
          break;
        case 4:
          testDailyWeeklyQuests(factory);
          break;
      }

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 시스템 테스트 중 오류: " + e.getMessage());
      logger.error("퀘스트 시스템 테스트 실패", e);
    }

    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 기본 퀘스트 생성 테스트
   */
  private void testBasicQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n📜 기본 퀘스트 생성 테스트:");

    String[] basicQuests = {"quest_001", "quest_002", "quest_003", "quest_004", "quest_005"};

    for (String questId : basicQuests) {
      Quest quest = factory.createQuest(questId);
      if (quest != null) {
        System.out.printf("✅ %s: %s (레벨 %d)\n", questId, quest.getTitle(), quest.getRequiredLevel());
        System.out.printf("   설명: %s\n", quest.getDescription());
        System.out.printf("   보상: 경험치 %d, 골드 %d\n", quest.getReward().getExpReward(), quest.getReward().getGoldReward());

        if (quest.getReward().getItemRewards() != null && !quest.getReward().getItemRewards().isEmpty()) {
          System.out.println("   아이템 보상:");
          quest.getReward().getItemRewards().forEach((item, quantity) -> System.out.printf("     - %s x%d\n", item.getName(), quantity));
        }
        System.out.println();
      } else {
        System.out.printf("❌ %s: 생성 실패\n", questId);
      }
    }
  }

  /**
   * 동적 퀘스트 생성 테스트
   */
  private void testDynamicQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n🎲 동적 퀘스트 생성 테스트:");

    int testLevel = InputValidator.getIntInput("테스트할 플레이어 레벨 (1-20): ", 1, 20);

    System.out.printf("🎯 레벨 %d 동적 퀘스트 생성:\n", testLevel);

    for (int i = 0; i < 3; i++) {
      Quest quest = factory.createLevelAppropriateQuest(testLevel);
      if (quest != null) {
        System.out.printf("%d. %s\n", i + 1, quest.getTitle());
        System.out.printf("   타입: %s, 필요 레벨: %d\n", quest.getType(), quest.getRequiredLevel());
        System.out.printf("   목표: %s\n", quest.getObjectives());

        if (quest.getReward().getItemRewards() != null && !quest.getReward().getItemRewards().isEmpty()) {
          quest.getReward().getItemRewards().forEach(
              (item, quantity) -> System.out.printf("   보상 아이템: %s x%d (%s)\n", item.getName(), quantity, item.getRarity().getDisplayName()));
        }
        System.out.println();
      }
    }
  }

  /**
   * 레벨별 퀘스트 생성 테스트
   */
  private void testLevelBasedQuestGeneration(JsonBasedQuestFactory factory) {
    System.out.println("\n📈 레벨별 퀘스트 생성 테스트:");

    int[] testLevels = {1, 3, 5, 8, 12, 16, 20};

    for (int level : testLevels) {
      System.out.printf("🎯 레벨 %d 퀘스트:\n", level);

      // 각 타입별로 하나씩 생성
      for (Quest.QuestType type : Quest.QuestType.values()) {
        Quest quest = factory.createRandomQuest(type, level);
        if (quest != null) {
          System.out.printf("   %s: %s\n", type.name(), quest.getTitle());
        }
      }
      System.out.println();
    }
  }

  /**
   * 일일/주간 퀘스트 테스트
   */
  private void testDailyWeeklyQuests(JsonBasedQuestFactory factory) {
    System.out.println("\n⏰ 일일/주간 퀘스트 생성 테스트:");

    // 일일 퀘스트
    System.out.println("📅 일일 퀘스트:");
    for (QuestType type : new QuestType[] {QuestType.KILL, QuestType.COLLECT}) {
      Quest dailyQuest = factory.createDailyQuest(type);
      if (dailyQuest != null) {
        System.out.printf("   %s: %s\n", type.name(), dailyQuest.getTitle());
        System.out.printf("   목표: %s\n", dailyQuest.getObjectives());
      }
    }

    // 주간 퀘스트
    System.out.println("\n📆 주간 퀘스트:");
    for (Quest.QuestType type : new Quest.QuestType[] {Quest.QuestType.KILL, Quest.QuestType.COLLECT}) {
      Quest weeklyQuest = factory.createWeeklyQuest(type);
      if (weeklyQuest != null) {
        System.out.printf("   %s: %s\n", type.name(), weeklyQuest.getTitle());
        System.out.printf("   목표: %s\n", weeklyQuest.getObjectives());
      }
    }
  }

  /**
   * 완전한 도움말을 표시합니다. (기존 메서드를 대체)
   */
  private void showHelp() {
    boolean inHelpMenu = true;
    while (inHelpMenu) {
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

      System.out.println("\n⚙️ === 시스템 정보 ===");
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.printf("• 로드된 아이템 수: %d개\n", itemFactory.getItemCount());
      System.out.printf("• 초기화 상태: %s\n", itemFactory.isInitialized() ? "정상" : "오류");

      try {
        JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
        System.out.printf("• 퀘스트 템플릿: 메인 %d개, 사이드 %d개, 일일 %d개\n", questFactory.getQuestCount("MAIN"), questFactory.getQuestCount("SIDE"),
            questFactory.getQuestCount("DAILY"));
      } catch (Exception e) {
        System.out.println("• 퀘스트 시스템: 초기화 중 오류 발생");
      }

      // 디버그 모드 정보
      if (SystemConstants.DEBUG_MODE) {
        showDebugHelp();
      }

      // 하단 메뉴 (루프용)
      System.out.println("\n🔧 === 도움말 메뉴 ===");
      System.out.println("1. 🧪 간단한 팩토리 테스트 실행");
      System.out.println("2. 📊 아이템 통계 보기");
      System.out.println("3. 🎯 퀘스트 시스템 상태 확인");
      System.out.println("4. 📋 게임 데이터 검증");
      System.out.println("5. 🎲 랜덤 생성 테스트");
      System.out.println("6. 🔍 상세 진단 도구");
      System.out.println("7. 📝 로그 파일 확인");
      System.out.println("0. 📖 도움말 종료");

      int choice = InputValidator.getIntInput("선택 (0-7): ", 0, 7);

      switch (choice) {
        case 1:
          quickFactoryTest();
          break;
        case 2:
          showItemStatistics();
          break;
        case 3:
          showQuestSystemStatus();
          break;
        case 4:
          validateGameData();
          break;
        case 5:
          runInteractiveRandomTest();
          break;
        case 6:
          runDetailedDiagnostics();
          break;
        case 7:
          showLogFileInfo();
          break;
        case 0:
          inHelpMenu = false;
          System.out.println("📖 도움말을 종료합니다.");
          break;
        default:
          System.out.println("잘못된 선택입니다.");
      }
      // 0번이 아닌 경우 계속 진행 확인
      if (inHelpMenu && choice != 0) {
        System.out.println("\n" + "=".repeat(50));
        if (!InputValidator.getConfirmation("도움말 메뉴를 계속 사용하시겠습니까?")) {
          inHelpMenu = false;
          System.out.println("📖 도움말을 종료합니다.");
        }
      }
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
   * 인터랙티브 랜덤 테스트 실행
   */
  private void runInteractiveRandomTest() {
    boolean inRandomTest = true;

    while (inRandomTest) {
      System.out.println("\n=== 🎲 인터랙티브 랜덤 테스트 ===");
      System.out.println("1. 🎯 특정 희귀도 아이템 생성");
      System.out.println("2. 📈 레벨별 아이템 생성");
      System.out.println("3. 🎁 특수 상황별 아이템 생성");
      System.out.println("4. 📊 랜덤 생성 통계");
      System.out.println("5. 🔄 가중치 시뮬레이션");
      System.out.println("6. 🧪 종합 랜덤 테스트");
      System.out.println("0. 🔙 이전 메뉴로");

      int choice = InputValidator.getIntInput("선택 (0-6): ", 0, 6);

      try {
        GameItemFactory factory = GameItemFactory.getInstance();

        switch (choice) {
          case 1:
            testRarityBasedGeneration(factory);
            break;
          case 2:
            testLevelBasedGeneration(factory);
            break;
          case 3:
            testSpecialGeneration(factory);
            break;
          case 4:
            factory.printRandomGenerationStats();
            break;
          case 5:
            runWeightSimulation(factory);
            break;
          case 6:
            factory.testRandomGeneration();
            break;
          case 0:
            inRandomTest = false;
            break;
          default:
            System.out.println("잘못된 선택입니다.");
        }

        if (inRandomTest && choice != 0) {
          InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        }

      } catch (Exception e) {
        System.out.println("❌ 랜덤 테스트 중 오류: " + e.getMessage());
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      }
    }
  }

  /**
   * 상세 진단 도구 실행
   */
  private void runDetailedDiagnostics() {
    boolean inDiagnostics = true;

    while (inDiagnostics) {
      System.out.println("\n=== 🔍 상세 진단 도구 ===");
      System.out.println("1. 🏭 모든 팩토리 상태 검사");
      System.out.println("2. 📁 JSON 파일 무결성 검사");
      System.out.println("3. 🔗 의존성 연결 상태 검사");
      System.out.println("4. 💾 메모리 사용량 확인");
      System.out.println("5. ⚡ 성능 벤치마크");
      System.out.println("6. 🛠️ 전체 시스템 진단");
      System.out.println("0. 🔙 이전 메뉴로");

      int choice = InputValidator.getIntInput("선택 (0-6): ", 0, 6);

      switch (choice) {
        case 1:
          checkAllFactories();
          break;
        case 2:
          checkJsonFileIntegrity();
          break;
        case 3:
          checkDependencyConnections();
          break;
        case 4:
          checkMemoryUsage();
          break;
        case 5:
          runPerformanceBenchmark();
          break;
        case 6:
          runFullSystemDiagnostics();
          break;
        case 0:
          inDiagnostics = false;
          break;
        default:
          System.out.println("잘못된 선택입니다.");
      }

      if (inDiagnostics && choice != 0) {
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      }
    }
  }

  /**
   * 로그 파일 정보 표시
   */
  private void showLogFileInfo() {
    System.out.println("\n=== 📝 로그 파일 정보 ===");

    String[] logFiles = {"logs/rpg-game.log", "logs/rpg-game-error.log"};

    for (String logFile : logFiles) {
      try {
        File file = new File(logFile);
        if (file.exists()) {
          System.out.printf("📄 %s:\n", logFile);
          System.out.printf("   크기: %.2f KB\n", file.length() / 1024.0);
          System.out.printf("   마지막 수정: %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())));

          // 마지막 몇 줄 미리보기
          System.out.println("   최근 로그 (마지막 3줄):");
          showLastLines(file, 3);
        } else {
          System.out.printf("❌ %s: 파일이 존재하지 않습니다.\n", logFile);
        }
        System.out.println();
      } catch (Exception e) {
        System.out.printf("❌ %s 확인 중 오류: %s\n", logFile, e.getMessage());
      }
    }

    System.out.println("💡 전체 로그를 보려면 텍스트 에디터로 파일을 직접 열어보세요.");
  }

  /**
   * 파일의 마지막 N줄 표시
   */
  private void showLastLines(File file, int lineCount) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      List<String> lines = new ArrayList<>();
      String line;

      while ((line = reader.readLine()) != null) {
        lines.add(line);
        if (lines.size() > lineCount) {
          lines.remove(0);
        }
      }

      for (String lastLine : lines) {
        System.out.println("     " + lastLine);
      }

    } catch (Exception e) {
      System.out.println("     (로그 미리보기 실패: " + e.getMessage() + ")");
    }
  }

  /**
   * 가중치 시뮬레이션 실행
   */
  private void runWeightSimulation(GameItemFactory factory) {
    System.out.println("\n=== ⚖️ 가중치 시뮬레이션 ===");

    int simCount = InputValidator.getIntInput("시뮬레이션 횟수 (10-1000): ", 10, 1000);

    Map<ItemRarity, Integer> results = new HashMap<>();
    for (ItemRarity rarity : ItemRarity.values()) {
      results.put(rarity, 0);
    }

    System.out.printf("🎲 %d번의 가중치 기반 아이템 생성 시뮬레이션 중...\n", simCount);

    for (int i = 0; i < simCount; i++) {
      try {
        GameItem item = factory.createWeightedRandomItem();
        if (item != null) {
          results.merge(item.getRarity(), 1, Integer::sum);
        }

        // 진행률 표시 (10% 단위)
        if ((i + 1) % (simCount / 10) == 0) {
          System.out.printf("진행률: %d%%\n", ((i + 1) * 100) / simCount);
        }
      } catch (Exception e) {
        System.out.printf("시뮬레이션 %d회차 오류: %s\n", i + 1, e.getMessage());
      }
    }

    System.out.println("\n📊 시뮬레이션 결과:");
    for (Map.Entry<ItemRarity, Integer> entry : results.entrySet()) {
      double percentage = (entry.getValue() * 100.0) / simCount;
      System.out.printf("   %s: %d회 (%.1f%%)\n", entry.getKey().getDisplayName(), entry.getValue(), percentage);
    }

    // 이론값과 비교
    System.out.println("\n📈 이론값과의 비교:");
    System.out.println("   일반: 50% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.COMMON) * 100.0) / simCount) + " (실제)");
    System.out.println("   고급: 25% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.UNCOMMON) * 100.0) / simCount) + " (실제)");
    System.out.println("   희귀: 15% (이론) vs " + String.format("%.1f%%", (results.get(ItemRarity.RARE) * 100.0) / simCount) + " (실제)");
  }

  /**
   * 모든 팩토리 상태 검사
   */
  private void checkAllFactories() {
    System.out.println("\n=== 🏭 모든 팩토리 상태 검사 ===");

    // GameItemFactory 검사
    System.out.println("🔍 GameItemFactory 검사:");
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.printf("   ✅ 인스턴스: %s\n", itemFactory != null ? "정상" : "NULL");
      System.out.printf("   ✅ 초기화: %s\n", itemFactory.isInitialized() ? "완료" : "실패");
      System.out.printf("   ✅ 아이템 수: %d개\n", itemFactory.getItemCount());

      // 메서드 존재 확인
      GameItem testItem = itemFactory.createRandomItemByRarity(ItemRarity.COMMON);
      System.out.printf("   ✅ createRandomItemByRarity: %s\n", testItem != null ? "정상" : "오류");

    } catch (Exception e) {
      System.out.printf("   ❌ GameItemFactory 오류: %s\n", e.getMessage());
    }

    // JsonBasedQuestFactory 검사
    System.out.println("\n🔍 JsonBasedQuestFactory 검사:");
    try {
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.printf("   ✅ 인스턴스: %s\n", questFactory != null ? "정상" : "NULL");

      Quest testQuest = questFactory.createQuest("quest_001");
      System.out.printf("   ✅ createQuest: %s\n", testQuest != null ? "정상" : "오류");

      boolean validation = questFactory.validateTemplates();
      System.out.printf("   ✅ 템플릿 검증: %s\n", validation ? "통과" : "실패");

    } catch (Exception e) {
      System.out.printf("   ❌ JsonBasedQuestFactory 오류: %s\n", e.getMessage());
    }

    // GameEffectFactory 검사 (있다면)
    System.out.println("\n🔍 GameEffectFactory 검사:");
    try {
      GameEffect testEffect = GameEffectFactory.createHealHpEffect(50);
      System.out.printf("   ✅ createHealHpEffect: %s\n", testEffect != null ? "정상" : "오류");
    } catch (Exception e) {
      System.out.printf("   ❌ GameEffectFactory 오류: %s\n", e.getMessage());
    }
  }

  /**
   * JSON 파일 무결성 검사
   */
  private void checkJsonFileIntegrity() {
    System.out.println("\n=== 📁 JSON 파일 무결성 검사 ===");

    String[] requiredFiles = {SystemConstants.MAIN_QUESTS_CONFIG, SystemConstants.SIDE_QUESTS_CONFIG, SystemConstants.DAILY_QUESTS_CONFIG,
        SystemConstants.BASIC_POTIONS_CONFIG, SystemConstants.BASIC_WEAPONS_CONFIG, SystemConstants.BASIC_ARMORS_CONFIG};

    int existingFiles = 0;
    int totalFiles = requiredFiles.length;

    for (String filePath : requiredFiles) {
      System.out.printf("🔍 검사 중: %s\n", filePath);

      try (InputStream is = GameEngine.class.getResourceAsStream(filePath)) {
        if (is != null) {
          // 파일 크기 확인
          int size = is.available();
          System.out.printf("   ✅ 존재함 (크기: %d bytes)\n", size);

          if (size == 0) {
            System.out.println("   ⚠️ 경고: 파일이 비어있습니다");
          } else if (size < 50) {
            System.out.println("   ⚠️ 경고: 파일이 너무 작습니다");
          }

          existingFiles++;
        } else {
          System.out.println("   ❌ 파일 없음");
        }
      } catch (Exception e) {
        System.out.printf("   ❌ 오류: %s\n", e.getMessage());
      }
    }

    System.out.printf("\n📊 결과: %d/%d 파일 존재 (%.1f%%)\n", existingFiles, totalFiles, (existingFiles * 100.0) / totalFiles);
  }

  /**
   * 전체 시스템 진단
   */
  private void runFullSystemDiagnostics() {
    System.out.println("\n=== 🛠️ 전체 시스템 진단 ===");
    System.out.println("종합적인 시스템 상태를 확인합니다...\n");

    checkAllFactories();
    System.out.println("\n" + "=".repeat(50));

    checkJsonFileIntegrity();
    System.out.println("\n" + "=".repeat(50));

    validateGameData();
    System.out.println("\n" + "=".repeat(50));

    showSystemInfo();

    System.out.println("\n🎉 전체 시스템 진단 완료!");
  }

  /**
   * 디버그 모드 도움말 표시
   */
  private void showDebugHelp() {
    System.out.println("\n🔧 === 디버그 모드 활성화 ===");
    System.out.println("디버그 기능이 활성화되어 있습니다!");
    System.out.println("메인 메뉴에서 다음 기능들을 사용할 수 있습니다:");

    if (SystemConstants.DEBUG_MODE) {
      System.out.println("• 21. 🧪 팩토리 테스트 - 모든 팩토리 기능 종합 테스트");
      System.out.println("• 22. 🎲 랜덤 아이템 테스트 - 희귀도별, 레벨별 랜덤 생성 테스트");
      System.out.println("• 23. 📋 퀘스트 템플릿 테스트 - JSON 템플릿 로드 및 검증");
      System.out.println("• 24. 🎯 퀘스트 시스템 테스트 - 동적 퀘스트 생성 테스트");

      // 개발자 메뉴도 표시
      System.out.println("\n개발자 전용 메뉴:");
      System.out.println("• 25. 🔧 전체 데이터 리로드");
      System.out.println("• 26. 📈 몬스터 통계");
      System.out.println("• 27. 🎁 아이템 통계");
      System.out.println("• 28. 🧪 테스트 몬스터 생성");
      System.out.println("• 29. 🎒 테스트 아이템 생성");
    }

    System.out.println("\n디버그 로그 위치: logs/rpg-game.log");
    System.out.println("오류 로그 위치: logs/rpg-game-error.log");
  }

  /**
   * 간단한 팩토리 테스트 (도움말에서 호출)
   */
  private void quickFactoryTest() {
    System.out.println("\n🚀 간단한 팩토리 테스트 실행 중...");

    try {
      // GameItemFactory 테스트
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      System.out.println("✅ GameItemFactory 초기화: " + itemFactory.isInitialized());

      // 기본 아이템 생성 테스트
      GameItem basicItem = itemFactory.createItem("HEALTH_POTION");
      System.out.println("✅ 기본 아이템 생성: " + (basicItem != null ? basicItem.getName() : "실패"));

      // 랜덤 아이템 생성 테스트
      GameItem randomItem = itemFactory.createRandomItemByRarity(ItemRarity.RARE);
      System.out.println("✅ 랜덤 레어 아이템: " + (randomItem != null ? randomItem.getName() : "실패"));

      // JsonBasedQuestFactory 테스트
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      System.out.println("✅ JsonBasedQuestFactory 초기화: 완료");

      Quest testQuest = questFactory.createQuest("quest_001");
      System.out.println("✅ 기본 퀘스트 생성: " + (testQuest != null ? testQuest.getTitle() : "실패"));

      // 동적 퀘스트 테스트
      int testLevel = player != null ? player.getLevel() : 5;
      Quest dynamicQuest = questFactory.createLevelAppropriateQuest(testLevel);
      System.out.println("✅ 동적 퀘스트 생성: " + (dynamicQuest != null ? dynamicQuest.getTitle() : "실패"));

      // 일일 퀘스트 테스트
      Quest dailyQuest = questFactory.createDailyQuest(Quest.QuestType.KILL);
      System.out.println("✅ 일일 퀘스트 생성: " + (dailyQuest != null ? dailyQuest.getTitle() : "실패"));

      System.out.println("🎉 간단한 테스트 완료! 모든 기능이 정상 작동합니다.");

    } catch (Exception e) {
      System.out.println("❌ 테스트 중 오류 발생: " + e.getMessage());
      logger.error("간단한 팩토리 테스트 실패", e);

      // 오류 해결 제안
      System.out.println("\n🔧 문제 해결 제안:");
      System.out.println("1. GameItemFactory에 createRandomItemByRarity 메서드가 추가되었는지 확인");
      System.out.println("2. JSON 퀘스트 파일들이 resources/config/quests/ 에 있는지 확인");
      System.out.println("3. 프로젝트를 다시 컴파일해보세요");
    }
  }

  /**
   * 아이템 통계 표시
   */
  private void showItemStatistics() {
    System.out.println("\n📊 === 아이템 시스템 통계 ===");

    try {
      GameItemFactory factory = GameItemFactory.getInstance();

      // 기본 정보
      System.out.printf("총 아이템 수: %d개\n", factory.getItemCount());
      System.out.printf("초기화 상태: %s\n", factory.isInitialized() ? "정상" : "오류");

      // 희귀도별 분포
      Map<ItemRarity, Integer> rarityDist = factory.getRarityDistribution();
      System.out.println("\n💎 희귀도별 분포:");
      for (Map.Entry<ItemRarity, Integer> entry : rarityDist.entrySet()) {
        if (entry.getValue() > 0) {
          System.out.printf("   %s: %d개 (%.1f%%)\n", entry.getKey().getDisplayName(), entry.getValue(),
              (entry.getValue() * 100.0) / factory.getItemCount());
        }
      }

      // 타입별 분포
      Map<String, Integer> typeDist = factory.getTypeDistribution();
      System.out.println("\n🔧 타입별 분포:");
      for (Map.Entry<String, Integer> entry : typeDist.entrySet()) {
        System.out.printf("   %s: %d개\n", entry.getKey(), entry.getValue());
      }

      // 랜덤 생성 테스트
      System.out.println("\n🎲 랜덤 생성 능력 테스트:");
      for (ItemRarity rarity : ItemRarity.values()) {
        GameItem testItem = factory.createRandomItemByRarity(rarity);
        System.out.printf("   %s: %s\n", rarity.getDisplayName(), testItem != null ? "✅ 가능" : "❌ 불가능");
      }

    } catch (Exception e) {
      System.out.println("❌ 아이템 통계 조회 실패: " + e.getMessage());
    }
  }

  /**
   * 퀘스트 시스템 상태 확인
   */
  private void showQuestSystemStatus() {
    System.out.println("\n🎯 === 퀘스트 시스템 상태 ===");

    try {
      // QuestTemplateLoader 상태
      System.out.println("📋 템플릿 로더 상태:");
      QuestTemplateLoader.printLoaderStatus();

      // JsonBasedQuestFactory 상태
      JsonBasedQuestFactory factory = JsonBasedQuestFactory.getInstance();
      factory.printFactoryStatus();

      // 플레이어의 퀘스트 매니저 상태 (있는 경우)
      if (player != null && player.getQuestManager() != null) {
        System.out.println("\n👤 플레이어 퀘스트 상태:");
        player.getQuestManager().printQuestSystemStatus();
      }

      // 퀘스트 생성 테스트
      System.out.println("\n🧪 퀘스트 생성 테스트:");
      String[] testQuests = {"quest_001", "quest_002", "quest_005"};
      for (String questId : testQuests) {
        Quest quest = factory.createQuest(questId);
        System.out.printf("   %s: %s\n", questId, quest != null ? "✅ " + quest.getTitle() : "❌ 생성 실패");
      }

    } catch (Exception e) {
      System.out.println("❌ 퀘스트 시스템 상태 확인 실패: " + e.getMessage());
    }
  }

  /**
   * 게임 데이터 검증
   */
  private void validateGameData() {
    System.out.println("\n📋 === 게임 데이터 검증 ===");

    int totalChecks = 0;
    int passedChecks = 0;

    try {
      // GameItemFactory 검증
      System.out.println("🏭 GameItemFactory 검증 중...");
      totalChecks++;
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      if (itemFactory.isInitialized() && itemFactory.getItemCount() > 0) {
        System.out.println("   ✅ GameItemFactory: 정상");
        passedChecks++;
      } else {
        System.out.println("   ❌ GameItemFactory: 초기화 오류 또는 아이템 없음");
      }

      // JsonBasedQuestFactory 검증
      System.out.println("🎯 JsonBasedQuestFactory 검증 중...");
      totalChecks++;
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      boolean questValidation = questFactory.validateTemplates();
      if (questValidation) {
        System.out.println("   ✅ JsonBasedQuestFactory: 모든 템플릿 유효");
        passedChecks++;
      } else {
        System.out.println("   ❌ JsonBasedQuestFactory: 일부 템플릿 오류");
      }

      // 랜덤 생성 기능 검증
      System.out.println("🎲 랜덤 생성 기능 검증 중...");
      totalChecks++;
      boolean randomTestPassed = true;
      for (ItemRarity rarity : ItemRarity.values()) {
        GameItem testItem = itemFactory.createRandomItemByRarity(rarity);
        if (testItem == null) {
          randomTestPassed = false;
          break;
        }
      }
      if (randomTestPassed) {
        System.out.println("   ✅ 랜덤 아이템 생성: 모든 희귀도 생성 가능");
        passedChecks++;
      } else {
        System.out.println("   ❌ 랜덤 아이템 생성: 일부 희귀도 생성 불가");
      }

      // JSON 파일 존재 확인
      System.out.println("📁 JSON 설정 파일 확인 중...");
      totalChecks++;
      String[] requiredFiles = {SystemConstants.MAIN_QUESTS_CONFIG, SystemConstants.SIDE_QUESTS_CONFIG, SystemConstants.DAILY_QUESTS_CONFIG};

      boolean allFilesExist = true;
      for (String filePath : requiredFiles) {
        try (InputStream is = GameEngine.class.getResourceAsStream(filePath)) {
          if (is == null) {
            System.out.println("   ❌ 파일 없음: " + filePath);
            allFilesExist = false;
          }
        } catch (Exception e) {
          allFilesExist = false;
        }
      }

      if (allFilesExist) {
        System.out.println("   ✅ JSON 설정 파일: 모두 존재");
        passedChecks++;
      } else {
        System.out.println("   ❌ JSON 설정 파일: 일부 누락");
      }

      // 검증 결과 요약
      System.out.printf("\n📊 검증 결과: %d/%d 통과 (%.1f%%)\n", passedChecks, totalChecks, (passedChecks * 100.0) / totalChecks);

      if (passedChecks == totalChecks) {
        System.out.println("🎉 모든 검증 통과! 게임 시스템이 정상 작동합니다.");
      } else {
        System.out.println("⚠️ 일부 검증 실패. 문제가 있는 기능을 확인하세요.");
      }

    } catch (Exception e) {
      System.out.println("❌ 데이터 검증 중 오류: " + e.getMessage());
    }
  }

  /**
   * 게임 객체들의 메모리 사용량 추정 (수정된 버전)
   */
  private void estimateGameObjectMemory() {
    System.out.println("\n📊 게임 객체 메모리 추정:");

    try {
      // GameItemFactory 메모리 추정
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      int itemCount = itemFactory.getItemCount();
      // 아이템 하나당 대략 200바이트로 추정
      double itemMemoryKB = (itemCount * 200) / 1024.0;
      System.out.printf("   🎒 GameItemFactory: ~%.2f KB (%d개 아이템)\n", itemMemoryKB, itemCount);

      // Player 객체 메모리 추정 (있는 경우)
      if (player != null) {
        // 플레이어 기본 데이터
        double playerMemoryKB = 2.0; // 기본 2KB로 추정

        if (player.getInventory() != null) {
          // 인벤토리 현재 크기 기반으로 계산
          int inventorySize = player.getInventory().getCurrentSize();
          playerMemoryKB += inventorySize * 0.1; // 아이템 슬롯당 100바이트

          System.out.printf("   👤 Player 객체: ~%.2f KB (인벤토리 %d슬롯)\n", playerMemoryKB, inventorySize);
        } else {
          System.out.printf("   👤 Player 객체: ~%.2f KB\n", playerMemoryKB);
        }
      } else {
        System.out.println("   👤 Player 객체: 없음 (게임 시작 전)");
      }

      // JsonBasedQuestFactory 메모리 추정
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      int totalQuests = questFactory.getQuestCount("MAIN") + questFactory.getQuestCount("SIDE") + questFactory.getQuestCount("DAILY");
      double questMemoryKB = (totalQuests * 150) / 1024.0; // 퀘스트당 150바이트로 추정
      System.out.printf("   🎯 QuestFactory: ~%.2f KB (%d개 퀘스트)\n", questMemoryKB, totalQuests);

      // QuestManager 메모리 추정 (플레이어가 있는 경우)
      if (player != null && player.getQuestManager() != null) {
        // 활성/완료 퀘스트 수 계산
        int activeQuests = player.getQuestManager().getActiveQuests().size();
        int completedQuests = player.getQuestManager().getCompletedQuests().size();
        double questManagerKB = ((activeQuests + completedQuests) * 100) / 1024.0;
        System.out.printf("   📋 QuestManager: ~%.2f KB (활성 %d개, 완료 %d개)\n", questManagerKB, activeQuests, completedQuests);
      }

      // 총 추정 메모리
      double totalEstimatedKB = itemMemoryKB + questMemoryKB + (player != null ? 4.0 : 0);
      System.out.printf("   📊 총 게임 데이터: ~%.2f KB\n", totalEstimatedKB);

      // 메모리 효율성 평가
      if (totalEstimatedKB < 50) {
        System.out.println("   ✅ 메모리 사용량: 매우 효율적");
      } else if (totalEstimatedKB < 100) {
        System.out.println("   ✅ 메모리 사용량: 효율적");
      } else if (totalEstimatedKB < 200) {
        System.out.println("   ⚠️ 메모리 사용량: 보통");
      } else {
        System.out.println("   🔴 메모리 사용량: 최적화 필요");
      }

    } catch (Exception e) {
      System.out.printf("   ❌ 메모리 추정 실패: %s\n", e.getMessage());
    }
  }

  /**
   * 인벤토리 상세 메모리 분석 (추가 메서드)
   */
  private void analyzeInventoryMemory() {
    if (player == null || player.getInventory() == null) {
      System.out.println("   📦 인벤토리: 분석 불가 (플레이어 없음)");
      return;
    }

    PlayerInventory inventory = player.getInventory();

    System.out.println("\n📦 인벤토리 상세 메모리 분석:");

    // 기본 인벤토리 구조 메모리
    double baseMemoryKB = 1.0; // 기본 구조체
    System.out.printf("   기본 구조: ~%.2f KB\n", baseMemoryKB);

    // 아이템 슬롯별 메모리
    int currentSize = inventory.getCurrentSize();
    int maxSize = inventory.getMaxSize();
    double slotsMemoryKB = (currentSize * 0.1); // 사용 중인 슬롯
    double emptyMemoryKB = ((maxSize - currentSize) * 0.02); // 빈 슬롯 (더 적음)

    System.out.printf("   사용 슬롯: %d개 (~%.2f KB)\n", currentSize, slotsMemoryKB);
    System.out.printf("   빈 슬롯: %d개 (~%.2f KB)\n", maxSize - currentSize, emptyMemoryKB);

    // 장착 장비 메모리
    int equippedCount = 0;
    if (inventory.getEquippedWeapon() != null)
      equippedCount++;
    if (inventory.getEquippedArmor() != null)
      equippedCount++;
    if (inventory.getEquippedAccessory() != null)
      equippedCount++;

    double equippedMemoryKB = equippedCount * 0.15; // 장착 장비당 150바이트
    System.out.printf("   장착 장비: %d개 (~%.2f KB)\n", equippedCount, equippedMemoryKB);

    // 총 인벤토리 메모리
    double totalInventoryKB = baseMemoryKB + slotsMemoryKB + emptyMemoryKB + equippedMemoryKB;
    System.out.printf("   📊 인벤토리 총계: ~%.2f KB\n", totalInventoryKB);

    // 효율성 분석
    double efficiency = (currentSize * 100.0) / maxSize;
    System.out.printf("   📈 공간 효율성: %.1f%% (%d/%d)\n", efficiency, currentSize, maxSize);

    if (efficiency > 80) {
      System.out.println("   ⚠️ 권장: 인벤토리 정리 또는 확장 필요");
    } else if (efficiency > 60) {
      System.out.println("   💡 권장: 불필요한 아이템 정리 고려");
    } else {
      System.out.println("   ✅ 인벤토리 공간 충분");
    }
  }

  /**
   * 메모리 사용량 확인 (전체 메서드 - 위의 estimateGameObjectMemory를 교체)
   */
  private void checkMemoryUsage() {
    System.out.println("\n=== 💾 메모리 사용량 확인 ===");

    // JVM 메모리 정보 가져오기
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;

    // MB 단위로 변환
    double maxMB = maxMemory / (1024.0 * 1024.0);
    double totalMB = totalMemory / (1024.0 * 1024.0);
    double usedMB = usedMemory / (1024.0 * 1024.0);
    double freeMB = freeMemory / (1024.0 * 1024.0);

    System.out.println("🖥️ JVM 메모리 현황:");
    System.out.printf("   최대 메모리: %.2f MB\n", maxMB);
    System.out.printf("   할당된 메모리: %.2f MB\n", totalMB);
    System.out.printf("   사용 중인 메모리: %.2f MB (%.1f%%)\n", usedMB, (usedMB / totalMB) * 100);
    System.out.printf("   여유 메모리: %.2f MB\n", freeMB);

    // 메모리 사용률에 따른 상태 평가
    double usagePercentage = (usedMB / totalMB) * 100;
    if (usagePercentage < 50) {
      System.out.println("   ✅ 메모리 상태: 양호");
    } else if (usagePercentage < 80) {
      System.out.println("   ⚠️ 메모리 상태: 보통");
    } else {
      System.out.println("   🔴 메모리 상태: 주의 필요");
    }

    // 가비지 컬렉션 실행 전후 비교
    System.out.println("\n🧹 가비지 컬렉션 테스트:");
    long beforeGC = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("   GC 전 사용량: %.2f MB\n", beforeGC / (1024.0 * 1024.0));

    System.gc(); // 가비지 컬렉션 실행
    try {
      Thread.sleep(100); // GC 완료 대기
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long afterGC = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("   GC 후 사용량: %.2f MB\n", afterGC / (1024.0 * 1024.0));
    System.out.printf("   정리된 메모리: %.2f MB\n", (beforeGC - afterGC) / (1024.0 * 1024.0));

    // 시스템 정보
    System.out.println("\n🖱️ 시스템 정보:");
    System.out.printf("   사용 가능한 프로세서: %d개\n", runtime.availableProcessors());

    // 게임 객체들의 대략적인 메모리 사용량 추정
    estimateGameObjectMemory();

    // 인벤토리 상세 분석 (플레이어가 있는 경우)
    if (player != null) {
      analyzeInventoryMemory();
    }

    // 메모리 최적화 제안
    provideMemoryOptimizationSuggestions(usagePercentage);
  }

  /**
   * 메모리 최적화 제안
   */
  private void provideMemoryOptimizationSuggestions(double usagePercentage) {
    System.out.println("\n💡 메모리 최적화 제안:");

    if (usagePercentage > 80) {
      System.out.println("   🔴 높은 메모리 사용률 감지!");
      System.out.println("   • 게임을 재시작하여 메모리 정리");
      System.out.println("   • 불필요한 아이템 판매 또는 버리기");
      System.out.println("   • 완료된 퀘스트 정리");
    } else if (usagePercentage > 60) {
      System.out.println("   ⚠️ 보통 수준의 메모리 사용");
      System.out.println("   • 주기적인 인벤토리 정리 권장");
      System.out.println("   • 가비지 컬렉션이 정상 작동 중");
    } else {
      System.out.println("   ✅ 메모리 사용량이 적정 수준입니다");
      System.out.println("   • 현재 상태가 양호함");
      System.out.println("   • 추가 최적화 불필요");
    }

    // 게임별 특화 제안
    if (player != null && player.getInventory() != null) {
      double inventoryUsage = player.getInventory().getUsageRate() * 100;
      if (inventoryUsage > 80) {
        System.out.println("   📦 인벤토리 관련:");
        System.out.println("   • 인벤토리가 거의 가득참 - 아이템 정리 필요");
        System.out.println("   • 불필요한 아이템 판매 또는 사용");
      }
    }
  }

  /**
   * 성능 벤치마크 실행
   */
  private void runPerformanceBenchmark() {
    System.out.println("\n=== ⚡ 성능 벤치마크 ===");

    // 벤치마크 설정
    int warmupRounds = 100;
    int testRounds = 1000;

    System.out.printf("🏃 벤치마크 설정: 워밍업 %d회, 테스트 %d회\n", warmupRounds, testRounds);
    System.out.println("⏱️ 각 기능별 성능을 측정합니다...\n");

    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();

      // 1. 기본 아이템 생성 성능
      benchmarkItemCreation(itemFactory, "기본 아이템 생성", warmupRounds, testRounds);

      // 2. 랜덤 아이템 생성 성능
      benchmarkRandomItemCreation(itemFactory, "랜덤 아이템 생성", warmupRounds, testRounds);

      // 3. 퀘스트 생성 성능
      benchmarkQuestCreation(questFactory, "퀘스트 생성", warmupRounds, testRounds);

      // 4. 동적 퀘스트 생성 성능
      benchmarkDynamicQuestCreation(questFactory, "동적 퀘스트 생성", warmupRounds, testRounds);

      // 5. 종합 성능 점수 계산
      calculateOverallPerformanceScore();

    } catch (Exception e) {
      System.out.printf("❌ 벤치마크 실행 실패: %s\n", e.getMessage());
    }
  }

  /**
   * 기본 아이템 생성 벤치마크
   */
  private void benchmarkItemCreation(GameItemFactory factory, String testName, int warmup, int test) {
    System.out.printf("🔧 %s 벤치마크:\n", testName);

    String[] testItems = {"HEALTH_POTION", "MANA_POTION", "IRON_SWORD"};

    // 워밍업
    for (int i = 0; i < warmup; i++) {
      factory.createItem(testItems[i % testItems.length]);
    }

    // 실제 테스트
    long startTime = System.nanoTime();
    int successCount = 0;

    for (int i = 0; i < test; i++) {
      GameItem item = factory.createItem(testItems[i % testItems.length]);
      if (item != null)
        successCount++;
    }

    long endTime = System.nanoTime();
    double elapsedMs = (endTime - startTime) / 1_000_000.0;
    double avgTimeMs = elapsedMs / test;
    double itemsPerSecond = test / (elapsedMs / 1000.0);

    System.out.printf("   총 시간: %.2f ms\n", elapsedMs);
    System.out.printf("   평균 시간: %.4f ms/개\n", avgTimeMs);
    System.out.printf("   처리량: %.0f개/초\n", itemsPerSecond);
    System.out.printf("   성공률: %.1f%% (%d/%d)\n", (successCount * 100.0) / test, successCount, test);

    // 성능 등급 평가
    if (avgTimeMs < 0.1) {
      System.out.println("   🟢 성능 등급: 우수");
    } else if (avgTimeMs < 1.0) {
      System.out.println("   🟡 성능 등급: 보통");
    } else {
      System.out.println("   🔴 성능 등급: 개선 필요");
    }
    System.out.println();
  }

  /**
   * 랜덤 아이템 생성 벤치마크
   */
  private void benchmarkRandomItemCreation(GameItemFactory factory, String testName, int warmup, int test) {
    System.out.printf("🎲 %s 벤치마크:\n", testName);

    ItemRarity[] rarities = ItemRarity.values();

    // 워밍업
    for (int i = 0; i < warmup; i++) {
      factory.createRandomItemByRarity(rarities[i % rarities.length]);
    }

    // 실제 테스트
    long startTime = System.nanoTime();
    int successCount = 0;

    for (int i = 0; i < test; i++) {
      GameItem item = factory.createRandomItemByRarity(rarities[i % rarities.length]);
      if (item != null)
        successCount++;
    }

    long endTime = System.nanoTime();
    double elapsedMs = (endTime - startTime) / 1_000_000.0;
    double avgTimeMs = elapsedMs / test;

    System.out.printf("   총 시간: %.2f ms\n", elapsedMs);
    System.out.printf("   평균 시간: %.4f ms/개\n", avgTimeMs);
    System.out.printf("   성공률: %.1f%% (%d/%d)\n", (successCount * 100.0) / test, successCount, test);

    if (avgTimeMs < 0.5) {
      System.out.println("   🟢 성능 등급: 우수");
    } else if (avgTimeMs < 2.0) {
      System.out.println("   🟡 성능 등급: 보통");
    } else {
      System.out.println("   🔴 성능 등급: 개선 필요");
    }
    System.out.println();
  }

  /**
   * 퀘스트 생성 벤치마크
   */
  private void benchmarkQuestCreation(JsonBasedQuestFactory factory, String testName, int warmup, int test) {
    System.out.printf("📋 %s 벤치마크:\n", testName);

    String[] testQuests = {"quest_001", "quest_002", "quest_005"};

    // 워밍업
    for (int i = 0; i < warmup; i++) {
      factory.createQuest(testQuests[i % testQuests.length]);
    }

    // 실제 테스트
    long startTime = System.nanoTime();
    int successCount = 0;

    for (int i = 0; i < test; i++) {
      Quest quest = factory.createQuest(testQuests[i % testQuests.length]);
      if (quest != null)
        successCount++;
    }

    long endTime = System.nanoTime();
    double elapsedMs = (endTime - startTime) / 1_000_000.0;
    double avgTimeMs = elapsedMs / test;

    System.out.printf("   총 시간: %.2f ms\n", elapsedMs);
    System.out.printf("   평균 시간: %.4f ms/개\n", avgTimeMs);
    System.out.printf("   성공률: %.1f%% (%d/%d)\n", (successCount * 100.0) / test, successCount, test);
    System.out.println();
  }

  /**
   * 동적 퀘스트 생성 벤치마크
   */
  private void benchmarkDynamicQuestCreation(JsonBasedQuestFactory factory, String testName, int warmup, int test) {
    System.out.printf("🎯 %s 벤치마크:\n", testName);

    // 워밍업
    for (int i = 0; i < warmup; i++) {
      factory.createLevelAppropriateQuest(5 + (i % 10));
    }

    // 실제 테스트
    long startTime = System.nanoTime();
    int successCount = 0;

    for (int i = 0; i < test; i++) {
      Quest quest = factory.createLevelAppropriateQuest(5 + (i % 10));
      if (quest != null)
        successCount++;
    }

    long endTime = System.nanoTime();
    double elapsedMs = (endTime - startTime) / 1_000_000.0;
    double avgTimeMs = elapsedMs / test;

    System.out.printf("   총 시간: %.2f ms\n", elapsedMs);
    System.out.printf("   평균 시간: %.4f ms/개\n", avgTimeMs);
    System.out.printf("   성공률: %.1f%% (%d/%d)\n", (successCount * 100.0) / test, successCount, test);
    System.out.println();
  }

  /**
   * 종합 성능 점수 계산
   */
  private void calculateOverallPerformanceScore() {
    System.out.println("📊 === 종합 성능 평가 ===");

    // 간단한 종합 테스트
    long startTime = System.currentTimeMillis();

    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();

      // 다양한 작업 수행
      for (int i = 0; i < 100; i++) {
        itemFactory.createItem("HEALTH_POTION");
        itemFactory.createRandomItemByRarity(ItemRarity.COMMON);
        questFactory.createQuest("quest_001");
        if (i % 10 == 0) {
          questFactory.createLevelAppropriateQuest(i / 10 + 1);
        }
      }

      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;

      System.out.printf("⏱️ 종합 테스트 시간: %d ms\n", totalTime);

      // 성능 점수 계산 (낮을수록 좋음)
      int score;
      String grade;

      if (totalTime < 50) {
        score = 95;
        grade = "A+";
      } else if (totalTime < 100) {
        score = 85;
        grade = "A";
      } else if (totalTime < 200) {
        score = 75;
        grade = "B";
      } else if (totalTime < 500) {
        score = 65;
        grade = "C";
      } else {
        score = 50;
        grade = "D";
      }

      System.out.printf("🏆 성능 점수: %d점 (%s등급)\n", score, grade);

      // 권장사항
      if (totalTime > 200) {
        System.out.println("💡 개선 권장사항:");
        System.out.println("   - JSON 파일 크기 최적화");
        System.out.println("   - 객체 캐싱 구현");
        System.out.println("   - 메모리 사용량 최적화");
      } else {
        System.out.println("✅ 시스템 성능이 양호합니다!");
      }

    } catch (Exception e) {
      System.out.printf("❌ 종합 성능 테스트 실패: %s\n", e.getMessage());
    }
  }

  /**
   * 의존성 연결 상태 검사
   */
  private void checkDependencyConnections() {
    System.out.println("\n=== 🔗 의존성 연결 상태 검사 ===");

    int totalConnections = 0;
    int successfulConnections = 0;

    // GameItemFactory 의존성 확인
    System.out.println("🔍 GameItemFactory 의존성:");
    totalConnections++;
    try {
      GameItemFactory itemFactory = GameItemFactory.getInstance();
      if (itemFactory != null && itemFactory.isInitialized()) {
        System.out.println("   ✅ GameItemFactory 인스턴스");
        successfulConnections++;
      } else {
        System.out.println("   ❌ GameItemFactory 초기화 실패");
      }
    } catch (Exception e) {
      System.out.printf("   ❌ GameItemFactory 오류: %s\n", e.getMessage());
    }

    // JsonBasedQuestFactory 의존성 확인
    System.out.println("\n🔍 JsonBasedQuestFactory 의존성:");
    totalConnections++;
    try {
      JsonBasedQuestFactory questFactory = JsonBasedQuestFactory.getInstance();
      if (questFactory != null) {
        System.out.println("   ✅ JsonBasedQuestFactory 인스턴스");
        successfulConnections++;

        // QuestFactory와 ItemFactory 연결 확인
        totalConnections++;
        Quest testQuest = questFactory.createLevelAppropriateQuest(5);
        if (testQuest != null && testQuest.getReward() != null) {
          System.out.println("   ✅ QuestFactory ↔ ItemFactory 연결");
          successfulConnections++;
        } else {
          System.out.println("   ❌ QuestFactory ↔ ItemFactory 연결 실패");
        }
      } else {
        System.out.println("   ❌ JsonBasedQuestFactory 인스턴스 없음");
      }
    } catch (Exception e) {
      System.out.printf("   ❌ JsonBasedQuestFactory 오류: %s\n", e.getMessage());
    }

    // GameEffectFactory 의존성 확인
    System.out.println("\n🔍 GameEffectFactory 의존성:");
    totalConnections++;
    try {
      GameEffect testEffect = GameEffectFactory.createHealHpEffect(50);
      if (testEffect != null) {
        System.out.println("   ✅ GameEffectFactory 정상");
        successfulConnections++;
      } else {
        System.out.println("   ❌ GameEffectFactory 생성 실패");
      }
    } catch (Exception e) {
      System.out.printf("   ❌ GameEffectFactory 오류: %s\n", e.getMessage());
    }

    // Player 객체 의존성 확인 (있는 경우)
    System.out.println("\n🔍 Player 객체 의존성:");
    if (player != null) {
      totalConnections++;
      try {
        if (player.getInventory() != null && player.getQuestManager() != null) {
          System.out.println("   ✅ Player ↔ Inventory/QuestManager 연결");
          successfulConnections++;
        } else {
          System.out.println("   ❌ Player 하위 객체 연결 실패");
        }
      } catch (Exception e) {
        System.out.printf("   ❌ Player 의존성 오류: %s\n", e.getMessage());
      }
    } else {
      System.out.println("   ℹ️ Player 객체 없음 (게임 시작 전)");
    }

    // ConfigDataLoader 의존성 확인
    System.out.println("\n🔍 ConfigDataLoader 의존성:");
    totalConnections++;
    try {
      Map<String, GameItemData> testData = ConfigDataLoader.loadAllItems();
      if (testData != null && !testData.isEmpty()) {
        System.out.printf("   ✅ ConfigDataLoader 정상 (%d개 아이템)\n", testData.size());
        successfulConnections++;
      } else {
        System.out.println("   ❌ ConfigDataLoader 데이터 없음");
      }
    } catch (Exception e) {
      System.out.printf("   ❌ ConfigDataLoader 오류: %s\n", e.getMessage());
    }

    // 의존성 검사 결과
    System.out.printf("\n📊 의존성 연결 결과: %d/%d (%.1f%%)\n", successfulConnections, totalConnections,
        (successfulConnections * 100.0) / totalConnections);

    if (successfulConnections == totalConnections) {
      System.out.println("🎉 모든 의존성이 정상적으로 연결되었습니다!");
    } else {
      System.out.println("⚠️ 일부 의존성에 문제가 있습니다. 시스템 불안정이 예상됩니다.");
      System.out.println("💡 권장사항: 문제가 있는 컴포넌트를 재초기화하거나 재시작하세요.");
    }
  }

}
