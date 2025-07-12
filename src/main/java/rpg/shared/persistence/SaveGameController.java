package rpg.shared.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.validator.InputValidator;
import rpg.core.engine.GameState;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.player.Player;
import rpg.infrastructure.persistence.GameDataRepository;
import rpg.infrastructure.persistence.SaveSlotInfo;

/**
 * 게임 저장/로드 기능을 전담하는 컨트롤러
 * GameEngine에서 분리된 모든 저장/로드 관련 메서드들을 포함
 */
public class SaveGameController {
  private static final Logger logger = LoggerFactory.getLogger(SaveGameController.class);
  
  private int currentSaveSlot = 0; // 현재 사용 중인 저장 슬롯
  
  public SaveGameController() {
    logger.debug("SaveGameController 초기화 완료");
  }

  /**
   * 메인 저장/로드 메뉴 실행
   */
  public SaveLoadResult showSaveLoadMenu(Player player, GameState gameState) {
    while (true) {
      System.out.println("\n=== 💾 저장/로드 관리 ===");
      System.out.println("1. 💾 게임 저장");
      System.out.println("2. 📁 게임 불러오기");
      System.out.println("3. 🗂️ 저장 슬롯 관리");
      System.out.println("4. 📊 저장 파일 정보");
      System.out.println("5. 🔙 돌아가기");

      int choice = InputValidator.getIntInput("선택 (1-5): ", 1, 5);

      switch (choice) {
        case 1:
          saveGame(player, gameState);
          break;
        case 2:
          SaveLoadResult loadResult = loadGame();
          if (loadResult.isSuccess()) {
            return loadResult; // 로드 성공 시 결과 반환
          }
          break;
        case 3:
          manageSaveSlots(player, gameState);
          break;
        case 4:
          showSaveFileInfo();
          break;
        case 5:
          return new SaveLoadResult(SaveLoadResult.ResultType.CANCELLED, null, null, 0);
      }
    }
  }

  /**
   * 게임을 저장합니다.
   */
  public void saveGame(Player player, GameState gameState) {
    try {
      // 현재 슬롯이 있으면 그 슬롯에 저장, 없으면 슬롯 선택
      if (currentSaveSlot > 0) {
        boolean useSameSlot = InputValidator.getConfirmation("현재 슬롯 " + currentSaveSlot + "에 저장하시겠습니까?");

        if (useSameSlot) {
          GameDataRepository.saveGame(player, gameState, currentSaveSlot);
          System.out.println("✅ 슬롯 " + currentSaveSlot + "에 게임이 저장되었습니다!");
          logger.info("슬롯 {} 게임 저장 완료: {}", currentSaveSlot, player.getName());
          InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
          return;
        }
      }

      // 슬롯 선택해서 저장
      chooseSlotAndSave(player, gameState);

    } catch (GameDataRepository.GameDataException e) {
      logger.error("게임 저장 실패", e);
      System.out.println("❌ 게임 저장 실패: " + e.getMessage());
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
    }
  }

  /**
   * 슬롯을 선택해서 저장합니다.
   */
  public void chooseSlotAndSave(Player player, GameState gameState) {
    try {
      // 저장 슬롯 목록 표시
      System.out.println("\n=== 💾 저장 슬롯 선택 ===");
      GameDataRepository.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("저장할 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0) {
        System.out.println("저장이 취소되었습니다.");
        return;
      }

      // 슬롯이 이미 사용 중인지 확인
      var slots = GameDataRepository.getAllSaveSlots();
      var targetSlot = slots.stream()
          .filter(slot -> slot.getSlotNumber() == slotNumber)
          .findFirst()
          .orElse(null);

      if (targetSlot != null && targetSlot.isOccupied()) {
        System.out.printf("\n⚠️ 슬롯 %d에 이미 저장된 캐릭터:\n", slotNumber);
        System.out.printf("   캐릭터: %s (레벨 %d)\n", targetSlot.getCharacterName(), targetSlot.getCharacterLevel());
        System.out.printf("   저장 시간: %s\n", targetSlot.getSaveTime());
        
        boolean overwrite = InputValidator.getConfirmation("이 슬롯을 덮어쓰시겠습니까?");

        if (!overwrite) {
          System.out.println("저장이 취소되었습니다.");
          return;
        }
      }

      // 저장 실행
      GameDataRepository.saveGame(player, gameState, slotNumber);
      currentSaveSlot = slotNumber; // 현재 슬롯 업데이트
      
      System.out.println("✅ 슬롯 " + slotNumber + "에 게임이 저장되었습니다!");
      System.out.printf("   캐릭터: %s (레벨 %d)\n", player.getName(), player.getLevel());
      System.out.printf("   위치: %s\n", gameState.getCurrentLocation());
      
      logger.info("슬롯 {} 게임 저장 완료: {}", slotNumber, player.getName());

    } catch (GameDataRepository.GameDataException e) {
      logger.error("슬롯 선택 저장 실패", e);
      System.out.println("❌ 게임 저장 실패: " + e.getMessage());
    }
    
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 게임을 불러옵니다.
   */
  public SaveLoadResult loadGame() {
    try {
      // 저장 슬롯 목록 표시
      System.out.println("\n=== 📁 게임 불러오기 ===");
      GameDataRepository.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0) {
        return new SaveLoadResult(SaveLoadResult.ResultType.CANCELLED, null, null, 0);
      }

      GameDataRepository.SaveData saveData = GameDataRepository.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("❌ 슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        return new SaveLoadResult(SaveLoadResult.ResultType.FAILED, null, null, 0);
      }

      // 로드된 데이터 정보 표시
      Player loadedPlayer = saveData.getCharacter();
      GameState loadedGameState = saveData.getGameState();
      
      System.out.println("\n📋 로드할 게임 정보:");
      System.out.printf("   캐릭터: %s (레벨 %d)\n", loadedPlayer.getName(), loadedPlayer.getLevel());
      System.out.printf("   골드: %d G\n", loadedPlayer.getGold());
      System.out.printf("   위치: %s\n", loadedGameState.getCurrentLocation());
      System.out.printf("   저장 시간: %s\n", saveData.getSaveTime());
      
      boolean confirmLoad = InputValidator.getConfirmation("이 게임을 불러오시겠습니까?");
      
      if (confirmLoad) {
        currentSaveSlot = slotNumber;
        
        // 로드된 데이터 검증
        loadedPlayer.validateLoadedData();
        
        // 퀘스트 진행 상황 확인
        var questManager = loadedPlayer.getQuestManager();
        int activeCount = questManager.getActiveQuests().size();
        int completedCount = questManager.getCompletedQuests().size();
        
        // 착용 장비 상태 확인
        PlayerInventory inventory = loadedPlayer.getInventory();
        int equippedCount = 0;
        if (inventory.getEquippedWeapon() != null) equippedCount++;
        if (inventory.getEquippedArmor() != null) equippedCount++;
        if (inventory.getEquippedAccessory() != null) equippedCount++;
        
        System.out.println("\n🎮 게임을 성공적으로 불러왔습니다!");
        System.out.println("어서오세요, " + loadedPlayer.getName() + "님!");
        
        if (activeCount > 0 || completedCount > 0) {
          System.out.printf("📋 퀘스트 진행 상황: 활성 %d개, 완료 %d개\n", activeCount, completedCount);
        }
        
        if (equippedCount > 0) {
          System.out.printf("⚔️ 착용 장비 %d개가 복원되었습니다.\n", equippedCount);
        }
        
        loadedPlayer.displayStats();
        loadedGameState.displayGameStats();
        
        logger.info("슬롯 {} 게임 로드 완료: {}", slotNumber, loadedPlayer.getName());
        
        return new SaveLoadResult(SaveLoadResult.ResultType.SUCCESS, loadedPlayer, loadedGameState, slotNumber);
      } else {
        return new SaveLoadResult(SaveLoadResult.ResultType.CANCELLED, null, null, 0);
      }

    } catch (GameDataRepository.GameDataException e) {
      logger.error("게임 로드 실패", e);
      System.out.println("❌ 게임 로드 실패: " + e.getMessage());
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return new SaveLoadResult(SaveLoadResult.ResultType.FAILED, null, null, 0);
    }
  }

  /**
   * 저장 슬롯을 관리합니다.
   */
  public void manageSaveSlots(Player currentPlayer, GameState currentGameState) {
    while (true) {
      System.out.println("\n=== 🗂️ 저장 슬롯 관리 ===");
      GameDataRepository.displaySaveSlots();
      
      System.out.println("\n1. 📁 다른 슬롯에서 불러오기");
      System.out.println("2. 💾 다른 슬롯에 저장");
      System.out.println("3. 🗑️ 슬롯 삭제");
      System.out.println("4. 📊 슬롯 상세 정보");
      System.out.println("5. 🔙 돌아가기");

      int choice = InputValidator.getIntInput("선택 (1-5): ", 1, 5);

      switch (choice) {
        case 1:
          SaveLoadResult result = loadFromSlot();
          if (result.isSuccess()) {
            System.out.println("게임이 로드되었습니다. 메뉴로 돌아갑니다.");
            return; // 메인 메뉴로 돌아가서 새 게임 시작
          }
          break;
        case 2:
          chooseSlotAndSave(currentPlayer, currentGameState);
          break;
        case 3:
          deleteSlot();
          break;
        case 4:
          showSlotDetails();
          break;
        case 5:
          return;
      }
    }
  }

  /**
   * 다른 슬롯에서 게임을 불러옵니다.
   */
  public SaveLoadResult loadFromSlot() {
    try {
      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

      if (slotNumber == 0) {
        return new SaveLoadResult(SaveLoadResult.ResultType.CANCELLED, null, null, 0);
      }

      GameDataRepository.SaveData saveData = GameDataRepository.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("❌ 슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        return new SaveLoadResult(SaveLoadResult.ResultType.FAILED, null, null, 0);
      }

      System.out.printf("\n📋 불러올 게임: %s (레벨 %d)\n", 
          saveData.getCharacter().getName(), saveData.getCharacter().getLevel());
      
      boolean confirmLoad = InputValidator.getConfirmation(
          "현재 게임을 이 캐릭터로 교체하시겠습니까? (현재 진행사항은 저장되지 않습니다)");

      if (confirmLoad) {
        currentSaveSlot = slotNumber;
        System.out.println("🎮 게임을 불러왔습니다!");
        
        logger.info("슬롯 {} 게임 교체 로드: {}", slotNumber, saveData.getCharacter().getName());
        
        return new SaveLoadResult(SaveLoadResult.ResultType.SUCCESS, 
            saveData.getCharacter(), saveData.getGameState(), slotNumber);
      } else {
        return new SaveLoadResult(SaveLoadResult.ResultType.CANCELLED, null, null, 0);
      }

    } catch (GameDataRepository.GameDataException e) {
      System.out.println("❌ 게임 로드 실패: " + e.getMessage());
      logger.error("슬롯 교체 로드 실패", e);
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return new SaveLoadResult(SaveLoadResult.ResultType.FAILED, null, null, 0);
    }
  }

  /**
   * 슬롯을 삭제합니다.
   */
  public void deleteSlot() {
    System.out.println("\n=== 🗑️ 슬롯 삭제 ===");
    GameDataRepository.displaySaveSlots();
    
    int slotNumber = InputValidator.getIntInput("삭제할 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

    if (slotNumber == 0) {
      return;
    }

    // 현재 사용 중인 슬롯인지 확인
    if (slotNumber == currentSaveSlot) {
      System.out.println("⚠️ 현재 사용 중인 슬롯은 삭제할 수 없습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    var slots = GameDataRepository.getAllSaveSlots();
    var targetSlot = slots.stream()
        .filter(slot -> slot.getSlotNumber() == slotNumber)
        .findFirst()
        .orElse(null);

    if (targetSlot == null || !targetSlot.isOccupied()) {
      System.out.println("❌ 슬롯 " + slotNumber + "는 이미 비어있습니다.");
      InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
      return;
    }

    // 삭제 확인 정보 표시
    System.out.printf("\n🗑️ 삭제할 슬롯 정보:\n");
    System.out.printf("   캐릭터: %s (레벨 %d)\n", targetSlot.getCharacterName(), targetSlot.getCharacterLevel());
    System.out.printf("   저장 시간: %s\n", targetSlot.getSaveTime());
    System.out.printf("   플레이 시간: %d분\n", targetSlot.getPlayTime());
    
    boolean confirmDelete = InputValidator.getConfirmation("정말로 이 슬롯을 삭제하시겠습니까?");

    if (confirmDelete) {
      if (GameDataRepository.deleteSaveSlot(slotNumber)) {
        System.out.println("✅ 슬롯 " + slotNumber + "이 삭제되었습니다.");
        logger.info("슬롯 {} 삭제 완료: {}", slotNumber, targetSlot.getCharacterName());
      } else {
        System.out.println("❌ 슬롯 삭제에 실패했습니다.");
      }
    } else {
      System.out.println("삭제가 취소되었습니다.");
    }
    
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 슬롯 상세 정보를 표시합니다.
   */
  public void showSlotDetails() {
    System.out.println("\n=== 📊 슬롯 상세 정보 ===");
    GameDataRepository.displaySaveSlots();
    
    int slotNumber = InputValidator.getIntInput("상세 정보를 볼 슬롯 번호 (0: 취소): ", 0, GameDataRepository.getMaxSaveSlots());

    if (slotNumber == 0) {
      return;
    }

    try {
      GameDataRepository.SaveData saveData = GameDataRepository.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("❌ 슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
        return;
      }

      Player player = saveData.getCharacter();
      GameState gameState = saveData.getGameState();
      
      System.out.println("\n" + "=".repeat(50));
      System.out.printf("📋 슬롯 %d 상세 정보\n", slotNumber);
      System.out.println("=".repeat(50));
      
      // 기본 정보
      System.out.printf("👤 캐릭터 이름: %s\n", player.getName());
      System.out.printf("⭐ 레벨: %d\n", player.getLevel());
      System.out.printf("🆙 경험치: %d\n", player.getExp());
      System.out.printf("💰 골드: %d G\n", player.getGold());
      System.out.printf("🗺️ 현재 위치: %s\n", gameState.getCurrentLocation());
      
      // 스탯 정보
      System.out.println("\n📊 스탯 정보:");
      System.out.printf("   ❤️ 체력: %d/%d\n", player.getHp(), player.getMaxHp());
      System.out.printf("   💙 마나: %d/%d\n", player.getMana(), player.getMaxMana());
      System.out.printf("   ⚔️ 공격력: %d\n", player.getAttack());
      System.out.printf("   🛡️ 방어력: %d\n", player.getTotalDefense());
      
      // 장비 정보
      PlayerInventory inventory = player.getInventory();
      System.out.println("\n⚔️ 장착 장비:");
      System.out.printf("   무기: %s\n", 
          inventory.getEquippedWeapon() != null ? inventory.getEquippedWeapon().getName() : "없음");
      System.out.printf("   방어구: %s\n", 
          inventory.getEquippedArmor() != null ? inventory.getEquippedArmor().getName() : "없음");
      System.out.printf("   액세서리: %s\n", 
          inventory.getEquippedAccessory() != null ? inventory.getEquippedAccessory().getName() : "없음");
      
      // 인벤토리 정보
      System.out.printf("\n🎒 인벤토리: %d/%d 슬롯 사용 중\n", 
          inventory.getCurrentSize(), inventory.getMaxSize());
      
      // 퀘스트 정보
      var questManager = player.getQuestManager();
      System.out.printf("\n📋 퀘스트 진행 상황:\n");
      System.out.printf("   진행 중: %d개\n", questManager.getActiveQuests().size());
      System.out.printf("   완료: %d개\n", questManager.getCompletedQuests().size());
      
      // 게임 통계
      System.out.println("\n📈 게임 통계:");
      System.out.printf("   플레이 시간: %d분\n", gameState.getTotalPlayTime());
      System.out.printf("   처치한 몬스터: %d마리\n", gameState.getMonstersKilled());
      System.out.printf("   완료한 퀘스트: %d개\n", gameState.getQuestsCompleted());
      
      // 저장 메타데이터
      System.out.println("\n💾 저장 정보:");
      System.out.printf("   저장 시간: %s\n", saveData.getSaveTime());
      System.out.printf("   게임 버전: %s\n", saveData.getVersion());
      System.out.printf("   현재 슬롯: %s\n", slotNumber == currentSaveSlot ? "예 (사용 중)" : "아니오");
      
      System.out.println("=".repeat(50));

    } catch (GameDataRepository.GameDataException e) {
      System.out.println("❌ 슬롯 정보 로드 실패: " + e.getMessage());
      logger.error("슬롯 상세 정보 로드 실패", e);
    }
    
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 저장 파일 정보를 표시합니다.
   */
  public void showSaveFileInfo() {
    System.out.println("\n=== 📊 저장 파일 정보 ===");
    
    var slots = GameDataRepository.getAllSaveSlots();
    int occupiedSlots = (int) slots.stream().filter(SaveSlotInfo::isOccupied).count();
    int totalSlots = GameDataRepository.getMaxSaveSlots();
    
    System.out.printf("💾 저장 슬롯 사용 현황: %d/%d (%.1f%%)\n", 
        occupiedSlots, totalSlots, (occupiedSlots * 100.0) / totalSlots);
    
    if (occupiedSlots > 0) {
      System.out.println("\n📋 저장된 캐릭터 목록:");
      
      for (SaveSlotInfo slot : slots) {
        if (slot.isOccupied()) {
          String currentMarker = (slot.getSlotNumber() == currentSaveSlot) ? " [현재]" : "";
          System.out.printf("   슬롯 %d: %s (레벨 %d) - %d분 플레이%s\n",
              slot.getSlotNumber(),
              slot.getCharacterName(),
              slot.getCharacterLevel(),
              slot.getPlayTime(),
              currentMarker);
        }
      }
      
      // 총 플레이 시간
      int totalPlayTime = slots.stream()
          .filter(SaveSlotInfo::isOccupied)
          .mapToInt(SaveSlotInfo::getPlayTime)
          .sum();
      
      System.out.printf("\n⏱️ 총 플레이 시간: %d분 (%.1f시간)\n", 
          totalPlayTime, totalPlayTime / 60.0);
    } else {
      System.out.println("\n📭 저장된 게임이 없습니다.");
    }
    
    System.out.printf("\n🔧 시스템 정보:\n");
    System.out.printf("   최대 저장 슬롯: %d개\n", totalSlots);
    System.out.printf("   현재 사용 슬롯: %s\n", 
        currentSaveSlot > 0 ? "슬롯 " + currentSaveSlot : "없음");
    
    InputValidator.waitForAnyKey("계속하려면 Enter를 누르세요...");
  }

  /**
   * 게임 오버 시 슬롯 삭제 처리
   */
  public void handleGameOverSlot(Player player) {
    if (currentSaveSlot > 0) {
      System.out.printf("\n💀 게임 오버 - %s님의 모험이 끝났습니다.\n", player.getName());
      System.out.printf("최종 레벨: %d, 획득한 골드: %d G\n", player.getLevel(), player.getGold());
      
      boolean deleteSlot = InputValidator.getConfirmation(
          "슬롯 " + currentSaveSlot + "의 저장 파일을 삭제하시겠습니까?");
      
      if (deleteSlot) {
        if (GameDataRepository.deleteSaveSlot(currentSaveSlot)) {
          System.out.println("💾 저장 파일이 삭제되었습니다.");
          logger.info("게임 오버로 슬롯 {} 삭제: {}", currentSaveSlot, player.getName());
        }
        currentSaveSlot = 0;
      }
    }
  }

  /**
   * 새 게임 시작 시 슬롯 초기화
   */
  public void resetForNewGame() {
    currentSaveSlot = 0;
    logger.debug("새 게임으로 슬롯 정보 초기화");
  }

  // Getters and Setters
  public int getCurrentSaveSlot() {
    return currentSaveSlot;
  }

  public void setCurrentSaveSlot(int currentSaveSlot) {
    this.currentSaveSlot = currentSaveSlot;
    logger.debug("현재 저장 슬롯 변경: {}", currentSaveSlot);
  }

  /**
   * 저장/로드 결과를 나타내는 클래스
   */
  public static class SaveLoadResult {
    public enum ResultType {
      SUCCESS,    // 성공
      FAILED,     // 실패
      CANCELLED   // 취소
    }
    
    private final ResultType resultType;
    private final Player player;
    private final GameState gameState;
    private final int slotNumber;
    
    public SaveLoadResult(ResultType resultType, Player player, GameState gameState, int slotNumber) {
      this.resultType = resultType;
      this.player = player;
      this.gameState = gameState;
      this.slotNumber = slotNumber;
    }
    
    public boolean isSuccess() {
      return resultType == ResultType.SUCCESS;
    }
    
    public boolean isFailed() {
      return resultType == ResultType.FAILED;
    }
    
    public boolean isCancelled() {
      return resultType == ResultType.CANCELLED;
    }
    
    // Getters
    public ResultType getResultType() { return resultType; }
    public Player getPlayer() { return player; }
    public GameState getGameState() { return gameState; }
    public int getSlotNumber() { return slotNumber; }
  }
}