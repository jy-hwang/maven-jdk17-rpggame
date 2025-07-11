package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.GameCharacter;
import service.GameDataService;
import util.InputValidator;

public class FileController {
  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  private int currentSaveSlot = 0; // 현재 사용 중인 저장 슬롯

  /**
   * 게임을 저장합니다.
   */
  public void saveGame(GameCharacter player, GameDataService.GameState gameState) {
    try {
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
      chooseSlotAndSave(player, gameState);

    } catch (GameDataService.GameDataException e) {
      logger.error("게임 저장 실패", e);
      System.out.println("게임 저장 실패: " + e.getMessage());
    }
  }

  /**
   * 슬롯을 선택해서 저장합니다.
   */
  public void chooseSlotAndSave(GameCharacter player, GameDataService.GameState gameState) {
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
   * 게임을 불러옵니다.
   */
  public LoadResult loadGame() {
    try {
      // 저장 슬롯 목록 표시
      GameDataService.displaySaveSlots();

      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

      if (slotNumber == 0) {
        return new LoadResult(false, null, null, 0);
      }

      GameDataService.SaveData saveData = GameDataService.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        return new LoadResult(false, null, null, 0);
      } else {
        currentSaveSlot = slotNumber; // 현재 슬롯 업데이트

        System.out.println("🎮 슬롯 " + slotNumber + "에서 게임을 불러왔습니다!");
        System.out.println("어서오세요, " + saveData.getCharacter().getName() + "님!");

        logger.info("슬롯 {} 기존 캐릭터 로드: {}", slotNumber, saveData.getCharacter().getName());
        return new LoadResult(true, saveData.getCharacter(), saveData.getGameState(), slotNumber);
      }

    } catch (GameDataService.GameDataException e) {
      logger.error("게임 로드 실패", e);
      System.out.println("게임 로드 실패: " + e.getMessage());
      return new LoadResult(false, null, null, 0);
    }
  }

  /**
   * 저장 슬롯을 관리합니다.
   */
  public void manageSaveSlots(GameCharacter currentPlayer, GameDataService.GameState currentGameState) {
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
          LoadResult result = loadFromSlot();
          if (result.isSuccess()) {
            // 로드 성공 시 현재 플레이어 정보 업데이트는 Game에서 처리
            System.out.println("게임이 로드되었습니다. 메뉴로 돌아갑니다.");
            return;
          }
          break;
        case 2:
          chooseSlotAndSave(currentPlayer, currentGameState);
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
  public LoadResult loadFromSlot() {
    try {
      int slotNumber = InputValidator.getIntInput("불러올 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

      if (slotNumber == 0) {
        return new LoadResult(false, null, null, 0);
      }

      GameDataService.SaveData saveData = GameDataService.loadGame(slotNumber);

      if (saveData == null) {
        System.out.println("슬롯 " + slotNumber + "에 저장된 게임이 없습니다.");
        return new LoadResult(false, null, null, 0);
      }

      boolean confirmLoad = InputValidator.getConfirmation("현재 게임을 '" + saveData.getCharacter().getName() + "' 캐릭터로 교체하시겠습니까? (현재 진행사항은 저장되지 않습니다)");

      if (confirmLoad) {
        currentSaveSlot = slotNumber;
        System.out.println("🎮 게임을 불러왔습니다!");
        return new LoadResult(true, saveData.getCharacter(), saveData.getGameState(), slotNumber);
      } else {
        return new LoadResult(false, null, null, 0);
      }

    } catch (GameDataService.GameDataException e) {
      System.out.println("게임 로드 실패: " + e.getMessage());
      return new LoadResult(false, null, null, 0);
    }
  }

  /**
   * 슬롯을 삭제합니다.
   */
  public void deleteSlot() {
    int slotNumber = InputValidator.getIntInput("삭제할 슬롯 번호 (0: 취소): ", 0, GameDataService.getMaxSaveSlots());

    if (slotNumber == 0)
      return;

    var slots = GameDataService.getAllSaveSlots();
    var targetSlot = slots.stream().filter(slot -> slot.getSlotNumber() == slotNumber).findFirst().orElse(null);

    if (targetSlot == null || !targetSlot.isOccupied()) {
      System.out.println("슬롯 " + slotNumber + "는 이미 비어있습니다.");
      return;
    }

    boolean confirmDelete = InputValidator.getConfirmation("슬롯 " + slotNumber + "의 '" + targetSlot.getCharacterName() + "' 캐릭터를 정말 삭제하시겠습니까?");

    if (confirmDelete) {
      if (GameDataService.deleteSaveSlot(slotNumber)) {
        // 현재 사용 중인 슬롯이 삭제된 경우
        if (currentSaveSlot == slotNumber) {
          currentSaveSlot = 0;
          System.out.println("현재 게임의 저장 슬롯이 삭제되었습니다. 새로 저장할 때 슬롯을 다시 선택해주세요.");
        }
      }
    }
  }

  /**
   * 게임 오버 시 슬롯 삭제 처리
   */
  public void handleGameOverSlot(GameCharacter player) {
    if (currentSaveSlot > 0) {
      boolean deleteSlot = InputValidator.getConfirmation("슬롯 " + currentSaveSlot + "의 저장 파일을 삭제하시겠습니까?");
      if (deleteSlot) {
        GameDataService.deleteSaveSlot(currentSaveSlot);
        currentSaveSlot = 0;
      }
    }
  }

  // Getters and Setters
  public int getCurrentSaveSlot() {
    return currentSaveSlot;
  }

  public void setCurrentSaveSlot(int currentSaveSlot) {
    this.currentSaveSlot = currentSaveSlot;
  }

  /**
   * 로드 결과를 담는 클래스
   */
  public static class LoadResult {
    private final boolean success;
    private final GameCharacter character;
    private final GameDataService.GameState gameState;
    private final int slotNumber;

    public LoadResult(boolean success, GameCharacter character, GameDataService.GameState gameState, int slotNumber) {
      this.success = success;
      this.character = character;
      this.gameState = gameState;
      this.slotNumber = slotNumber;
    }

    public boolean isSuccess() {
      return success;
    }

    public GameCharacter getCharacter() {
      return character;
    }

    public GameDataService.GameState getGameState() {
      return gameState;
    }

    public int getSlotNumber() {
      return slotNumber;
    }
  }
}
