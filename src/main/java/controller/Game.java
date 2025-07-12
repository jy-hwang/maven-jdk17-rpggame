package controller;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import loader.ItemDataLoader;
import loader.MonsterDataLoader;
import model.GameCharacter;
import model.Skill;
import model.factory.GameItemFactory;
import model.item.GameConsumable;
import model.item.GameItem;
import model.monster.Monster;
import model.monster.MonsterData;
import service.GameDataService;
import service.QuestManager;
import util.InputValidator;

/**
 * 리팩토링된 메인 게임 컨트롤러 각 기능별 Controller들을 조율하는 역할
 */
public class Game {
  private static final Logger logger = LoggerFactory.getLogger(Game.class);

  // 게임 상태
  private GameCharacter player;
  private QuestManager questManager;
  private GameDataService.GameState gameState;
  private boolean gameRunning;
  private boolean inGameLoop;
  private long gameStartTime;
  private int currentSaveSlot;

  // 컨트롤러들
  private BattleController battleController;
  private InventoryController inventoryController;
  private QuestController questController;
  private ShopController shopController;
  private ExploreController exploreController;

  public Game() {
    this.gameRunning = true;
    this.inGameLoop = false;
    this.questManager = new QuestManager();
    this.gameState = new GameDataService.GameState();
    this.gameStartTime = System.currentTimeMillis();
    this.currentSaveSlot = 0;

    initializeControllers();
    logger.info("게임 인스턴스 생성 완료 (v" + BaseConstant.GAME_VERSION + "- 상점판매기능 추가)");
  }

  /**
   * 모든 컨트롤러를 초기화합니다.
   */
  private void initializeControllers() {
    try {
      // 순서 중요: 의존성이 있는 컨트롤러들을 순서대로 초기화
      inventoryController = new InventoryController();
      questController = new QuestController(questManager, gameState);
      battleController = new BattleController(questManager, gameState);
      shopController = new ShopController(inventoryController);
      exploreController = new ExploreController(battleController, questController, inventoryController, gameState);

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
      logger.info("게임 시작 (v" + BaseConstant.GAME_VERSION + ")");
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
    System.out.println("   🎮 RPG 게임 v" + BaseConstant.GAME_VERSION + " 🎮   ");
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
    var saveSlots = GameDataService.getAllSaveSlots();
    long occupiedSlots = saveSlots.stream().filter(GameDataService.SaveSlotInfo::isOccupied).count();

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
      player = new GameCharacter(name);

      // 게임 상태 초기화
      gameState = new GameDataService.GameState();
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
      GameDataService.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

      if (slotNumber == 0)
        return;

      GameDataService.SaveData saveData = GameDataService.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        return;
      }

      player = saveData.getCharacter();
      gameState = saveData.getGameState();
      currentSaveSlot = slotNumber;
      gameStartTime = System.currentTimeMillis(); // 플레이 시간을 새로 시작

      // 컨트롤러들에 새로운 gameState 적용
      updateControllersWithNewGameState();

      System.out.println("🎮 슬롯 " + slotNumber + "에서 게임을 불러왔습니다!");
      System.out.println("어서오세요, " + player.getName() + "님!");
      player.displayStats();
      gameState.displayGameStats();

      logger.info("슬롯 {} 기존 캐릭터 로드: {}", slotNumber, player.getName());

      // 인게임 루프 시작
      startGameLoop();

    } catch (GameDataService.GameDataException e) {
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
    questController = new QuestController(questManager, gameState);
    battleController = new BattleController(questManager, gameState);
    exploreController = new ExploreController(battleController, questController, inventoryController, gameState);
  }

  /**
   * 메인 게임 루프를 실행합니다.
   */
  private void startGameLoop() {
    inGameLoop = true;

    while (inGameLoop && player.isAlive()) {
      try {
        showInGameMenu();
        int choice = InputValidator.getIntInput("선택: ", 1, 12);

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
    if (BaseConstant.DEBUG_MODE) {
      System.out.println("\n=== 개발자 메뉴 ===");
      System.out.println("90. 🔧 전체 데이터 리로드");
      System.out.println("91. 📈 몬스터 통계");
      System.out.println("92. 🎁 아이템 통계");
      System.out.println("93. 🧪 테스트 몬스터 생성");
      System.out.println("94. 🎒 테스트 아이템 생성");
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
    if (!BaseConstant.DEBUG_MODE) {
      System.out.println("개발자 모드가 활성화되지 않았습니다.");
      return;
    }

    System.out.println("전체 게임 데이터를 다시 로드합니다...");
    exploreController.reloadAllData();
    System.out.println("완료!");
  }

  /**
   * 개발자 전용 - 아이템 통계 표시
   */
  private void showItemStatistics() {
    if (!BaseConstant.DEBUG_MODE) {
      System.out.println("개발자 모드가 활성화되지 않았습니다.");
      return;
    }

    ItemDataLoader.printItemStatistics();
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
    if (!BaseConstant.DEBUG_MODE) {
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
    if (!BaseConstant.DEBUG_MODE) {
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
    ExploreController.ExploreResult result = exploreController.startExploration(player);

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
          GameDataService.saveGame(player, gameState, currentSaveSlot);
          logger.info("슬롯 {} 게임 저장 완료: {}", currentSaveSlot, player.getName());
          return;
        }
      }

      // 슬롯 선택해서 저장
      chooseSlotAndSave();

    } catch (GameDataService.GameDataException e) {
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
      GameDataService.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("저장할 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

      if (slotNumber == 0) {
        System.out.println("저장이 취소되었습니다.");
        return;
      }

      // 슬롯이 이미 사용 중인지 확인
      var slots = GameDataService.getAllSaveSlots();
      var targetSlot = slots.stream().filter(slot -> slot.getSlotNumber() == slotNumber).findFirst().orElse(null);

      if (targetSlot != null && targetSlot.isOccupied()) {
        boolean overwrite =
            InputValidator.getConfirmation("슬롯 " + slotNumber + "에 이미 '" + targetSlot.getCharacterName() + "' 캐릭터가 저장되어 있습니다. 덮어쓰시겠습니까?");

        if (!overwrite) {
          System.out.println("저장이 취소되었습니다.");
          return;
        }
      }

      GameDataService.saveGame(player, gameState, slotNumber);
      currentSaveSlot = slotNumber; // 현재 슬롯 업데이트
      logger.info("슬롯 {} 게임 저장 완료: {}", slotNumber, player.getName());

    } catch (GameDataService.GameDataException e) {
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
      GameDataService.displaySaveSlots();
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
      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

      if (slotNumber == 0)
        return;

      GameDataService.SaveData saveData = GameDataService.loadGame(slotNumber);

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

    } catch (GameDataService.GameDataException e) {
      System.out.println("게임 로드 실패: " + e.getMessage());
    }
  }

  /**
   * 슬롯을 삭제합니다.
   */
  private void deleteSlot() {
    int slotNumber = InputValidator.getIntInput("삭제할 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

    if (slotNumber == 0)
      return;

    if (slotNumber == currentSaveSlot) {
      System.out.println("현재 사용 중인 슬롯은 삭제할 수 없습니다.");
      return;
    }

    boolean confirmDelete = InputValidator.getConfirmation("정말로 슬롯 " + slotNumber + "를 삭제하시겠습니까? (복구할 수 없습니다)");

    if (confirmDelete) {
      GameDataService.deleteSaveSlot(slotNumber);
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
      GameDataService.deleteSaveSlot(currentSaveSlot);
    }
  }

  /**
   * 도움말을 표시합니다.
   */
  private void showHelp() {
    System.out.println("\n📖 === 게임 도움말 (v" + BaseConstant.GAME_VERSION + ") ===");
    System.out.println("🗡️ 탐험하기: JSON 기반 지역별 몬스터와 다양한 랜덤 이벤트 경험");
    System.out.println("📊 상태 확인: 캐릭터 스탯, 장비, 현재 위치 정보 확인");
    System.out.println("🎒 인벤토리: 통합 아이템 관리 및 장비 착용/해제 시스템");
    System.out.println("⚡ 스킬 관리: 레벨업으로 학습한 스킬 확인 및 전투 활용");
    System.out.println("📋 퀘스트: JSON 기반 퀘스트 시스템으로 목표 달성 및 보상 획득");
    System.out.println("🏪 상점: 카테고리별 아이템 구매/판매 및 특별 상점 이벤트");
    System.out.println("💾 게임 저장: 다중 슬롯 지원 (최대 5개 캐릭터 동시 관리)");
    System.out.println("\n🆕 v" + BaseConstant.GAME_VERSION + " 주요 업데이트:");
    System.out.println("• 🔧 GameDataLoader 기본 아이템 생성 시스템 개선 (JSON 연동)");
    System.out.println("• 🎯 ExploreController 아이템 생성 로직 통합 및 안정성 강화");
    System.out.println("• 📦 ItemDataLoader와 GameItemFactory 연동으로 일관된 아이템 관리");
    System.out.println("====================");
  }

}
