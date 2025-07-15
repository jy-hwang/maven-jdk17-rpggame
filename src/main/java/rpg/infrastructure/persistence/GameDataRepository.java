package rpg.infrastructure.persistence;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.core.engine.GameState;
import rpg.domain.player.Player;
import rpg.shared.constant.SystemConstants;

/**
 * 게임 데이터 저장소 - 최적화된 시스템으로 업데이트 기존 DTO/Mapper 시스템을 OptimizedGameRepository로 위임
 */
public class GameDataRepository {

  private static final Logger logger = LoggerFactory.getLogger(GameDataRepository.class);

  /**
   * 게임 저장 (최적화된 방식으로 위임)
   */
  public static void saveGame(Player character, GameState gameState, int slotNumber) throws GameDataException {
    try {
      logger.info("최적화된 저장 시스템 사용: 슬롯 {}", slotNumber);
      OptimizedGameRepository.saveGame(character, gameState, slotNumber);

    } catch (IOException e) {
      logger.error("게임 저장 실패: 슬롯 {}", slotNumber, e);
      throw new GameDataException("게임 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 게임 로드 (최적화된 방식으로 위임)
   */
  public static SaveData loadGame(int slotNumber) throws GameDataException {
    try {
      logger.info("최적화된 로드 시스템 사용: 슬롯 {}", slotNumber);
      SaveData optimizedData = OptimizedGameRepository.loadGame(slotNumber);

      if (optimizedData == null) {
        return null;
      }

      // 기존 SaveData 형식으로 변환
      SaveData saveData = new SaveData(optimizedData.getCharacter(), optimizedData.getGameState(), optimizedData.getSlotNumber());
      saveData.setSaveTime(optimizedData.getSaveTime());
      saveData.setVersion(optimizedData.getVersion());

      return saveData;

    } catch (IOException e) {
      logger.error("게임 로드 실패: 슬롯 {}", slotNumber, e);
      throw new GameDataException("저장 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 모든 저장 슬롯 정보 조회 (최적화된 방식으로 위임)
   */
  public static List<SaveSlotInfo> getAllSaveSlots() {
    List<SaveSlotInfo> optimizedSlots = OptimizedGameRepository.getAllSaveSlots();

    // 기존 형식으로 변환
    return optimizedSlots.stream().map(slot -> new SaveSlotInfo(slot.getSlotNumber(), slot.isOccupied(), slot.getCharacterName(), slot.getCharacterLevel(), slot.getSaveTime(), slot.getPlayTime()))
        .toList();
  }

  /**
   * 저장 슬롯 표시
   */
  public static void displaySaveSlots() {
    List<SaveSlotInfo> slots = getAllSaveSlots();

    System.out.println("\n=== 📁 저장 슬롯 목록 ===");
    for (SaveSlotInfo slot : slots) {
      if (slot.isOccupied()) {
        System.out.printf("%d. %s (레벨 %d) - %s (플레이 시간: %d분)%n", slot.getSlotNumber(), slot.getCharacterName(), slot.getCharacterLevel(), slot.getSaveTime(), slot.getPlayTime());
      } else {
        System.out.printf("%d. [빈 슬롯]%n", slot.getSlotNumber());
      }
    }
    System.out.println("========================");
  }

  /**
   * 저장 슬롯 삭제 (최적화된 방식으로 위임)
   */
  public static boolean deleteSaveSlot(int slotNumber) {
    return OptimizedGameRepository.deleteSaveSlot(slotNumber);
  }

  /**
   * 최대 저장 슬롯 수 반환
   */
  public static int getMaxSaveSlots() {
    return SystemConstants.MAX_SAVE_SLOTS;
  }

  // === 기존 호환성을 위한 클래스들 ===



  /**
   * 저장 데이터 래핑 클래스
   */
  public static class SaveData {
    private Player character;
    private GameState gameState;
    private String saveTime;
    private String version;
    private int slotNumber;

    public SaveData() {}

    public SaveData(Player character, GameState gameState, int slotNumber) {
      this.character = character;
      this.gameState = gameState != null ? gameState : new GameState();
      this.slotNumber = slotNumber;
    }

    // Getters and Setters
    public Player getCharacter() {
      return character;
    }

    public void setCharacter(Player character) {
      this.character = character;
    }

    public GameState getGameState() {
      return gameState;
    }

    public void setGameState(GameState gameState) {
      this.gameState = gameState;
    }

    public String getSaveTime() {
      return saveTime;
    }

    public void setSaveTime(String saveTime) {
      this.saveTime = saveTime;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public int getSlotNumber() {
      return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
      this.slotNumber = slotNumber;
    }
  }


  /**
   * 게임 데이터 예외 클래스
   */
  public static class GameDataException extends Exception {
    public GameDataException(String message) {
      super(message);
    }

    public GameDataException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
