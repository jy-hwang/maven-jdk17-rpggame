package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import model.GameCharacter;
import model.Skill;
import model.factory.GameItemFactory;
import model.item.GameConsumable;
import model.item.GameItem;
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
    System.out.println("• 🌟 향상된 탐험 시스템");
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
        int choice = InputValidator.getIntInput("선택: ", 1, 10);

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
            saveGame();
            break;
          case 8:
            manageSaveSlots();
            break;
          case 9:
            returnToMainMenu();
            break;
          case 10:
            showHelp();
            break;
          default:
            System.out.println("잘못된 선택입니다.");
        }

        if (inGameLoop && choice != 2 && choice != 10) {
          InputValidator.waitForAnyKey("\n계속하려면 Enter를 누르세요...");
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
   * 메인 메뉴를 표시합니다.
   */
  private void showInGameMenu() {
    System.out.println("\n=== 메인 메뉴 ===");
    System.out.println("1. 🗡️ 탐험하기");
    System.out.println("2. 📊 상태 확인");
    System.out.println("3. 🎒 인벤토리");
    System.out.println("4. ⚡ 스킬 관리");
    System.out.println("5. 📋 퀘스트");
    System.out.println("6. 🏪 상점");
    System.out.println("7. 💾 게임 저장");
    System.out.println("8. 📁 저장 관리");
    System.out.println("9. 🚪 게임 종료");
    System.out.println("10. ❓ 도움말");

    // 알림 표시
    showNotifications();
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
    System.out.println("\n📖 === 게임 도움말 ===");
    System.out.println("🗡️ 탐험하기: 몬스터와 싸우고 다양한 이벤트를 경험하세요");
    System.out.println("📊 상태 확인: 캐릭터의 현재 상태와 게임 진행도를 확인하세요");
    System.out.println("🎒 인벤토리: 아이템을 관리하고 장비를 착용하세요");
    System.out.println("⚡ 스킬 관리: 학습한 스킬을 확인하고 전투에서 사용하세요");
    System.out.println("📋 퀘스트: 퀘스트를 수락하고 완료하여 보상을 받으세요");
    System.out.println("🏪 상점: 골드로 유용한 아이템과 장비를 구매하세요");
    System.out.println("💾 게임 저장: 현재 진행 상황을 저장하세요 (5개 슬롯 지원)");
    System.out.println("📁 저장 관리: 다중 저장 슬롯을 관리하세요");
    System.out.println("\n💡 새로운 기능:");
    System.out.println("• 탐험 중 다양한 랜덤 이벤트 (보물, 치유의 샘, 상인 등)");
    System.out.println("• 향상된 전투 시스템 (스킬과 아이템 활용)");
    System.out.println("• 확장된 상점 시스템 (카테고리별 아이템 구매와 판매)");
    System.out.println("• 고도화된 퀘스트 관리 (진행도 추적 및 보상 시스템)");
    System.out.println("• 다중 저장 슬롯 (최대 5개 캐릭터 동시 관리)");
    System.out.println("====================");
  }
}
