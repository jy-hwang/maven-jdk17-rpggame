package rpg.infrastructure.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import rpg.core.engine.GameState;
import rpg.domain.player.Player;
import rpg.infrastructure.persistence.GameDataRepository.SaveData;
import rpg.shared.constant.GameConstants;
import rpg.shared.constant.SystemConstants;

public class OptimizedGameRepository {

  private static final Logger logger = LoggerFactory.getLogger(OptimizedGameRepository.class);
  private static final ObjectMapper objectMapper;

  static {
    // ObjectMapper 설정
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    createSaveDirectory();
  }

  /**
   * 저장 디렉토리 생성
   */
  private static void createSaveDirectory() {
    try {
      Path saveDir = Paths.get(SystemConstants.SAVE_DIRECTORY);
      if (!Files.exists(saveDir)) {
        Files.createDirectories(saveDir);
        logger.info("저장 디렉토리 생성: {}", saveDir.toAbsolutePath());
      }
    } catch (Exception e) {
      logger.error("저장 디렉토리 생성 실패", e);
    }
  }

  /**
   * 최적화된 게임 저장
   */
  public static void saveGame(Player player, GameState gameState, int slotNumber) throws IOException {

    if (player == null) {
      throw new IllegalArgumentException("저장할 플레이어 데이터가 없습니다.");
    }

    if (slotNumber < GameConstants.NUMBER_ONE || slotNumber > SystemConstants.MAX_SAVE_SLOTS) {
      throw new IllegalArgumentException("저장 슬롯 번호는 1~" + SystemConstants.MAX_SAVE_SLOTS + " 사이여야 합니다.");
    }

    // 파일 경로
    String fileName = SystemConstants.SAVE_FILE_PREFIX + slotNumber + ".json";
    Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

    try {
      // 백업 생성
      if (Files.exists(saveFilePath)) {
        createBackup(saveFilePath, slotNumber);
      }

      // SimpleSaveData로 변환 (DTO/Mapper 없이)
      SimpleSaveData saveData = SimpleSaveData.from(player, gameState, slotNumber);

      // JSON 저장
      objectMapper.writeValue(saveFilePath.toFile(), saveData);

      logger.info("최적화된 게임 저장 완료: 슬롯 {} (캐릭터: {})", slotNumber, player.getName());
      System.out.println("🎮 게임이 슬롯 " + slotNumber + "에 저장되었습니다!");

      // 파일 크기 로그
      long fileSize = Files.size(saveFilePath);
      logger.debug("저장 파일 크기: {} bytes ({} KB)", fileSize, fileSize / 1024);

    } catch (Exception e) {
      logger.error("게임 저장 실패: 슬롯 {}", slotNumber, e);
      throw new IOException("게임 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 최적화된 게임 로드
   */
  public static SaveData loadGame(int slotNumber) throws IOException {

    if (slotNumber < GameConstants.NUMBER_ONE || slotNumber > SystemConstants.MAX_SAVE_SLOTS) {
      throw new IllegalArgumentException("저장 슬롯 번호는 1~" + SystemConstants.MAX_SAVE_SLOTS + " 사이여야 합니다.");
    }

    String fileName = SystemConstants.SAVE_FILE_PREFIX + slotNumber + ".json";
    Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

    if (!Files.exists(saveFilePath)) {
      logger.info("저장 파일이 존재하지 않음: 슬롯 {}", slotNumber);
      return null;
    }

    try {
      // 파일 크기 로그
      long fileSize = Files.size(saveFilePath);
      logger.debug("로드할 파일 크기: {} bytes ({} KB)", fileSize, fileSize / 1024);

      // JSON에서 SimpleSaveData로 로드
      SimpleSaveData saveData = objectMapper.readValue(saveFilePath.toFile(), SimpleSaveData.class);

      // Player로 변환
      Player player = saveData.toPlayer();

      // GameState 복원
      GameState gameState = new GameState();
      gameState.setTotalPlayTime(saveData.getTotalPlayTime());
      gameState.setMonstersKilled(saveData.getMonstersKilled());
      gameState.setQuestsCompleted(saveData.getQuestsCompleted());
      gameState.setCurrentLocation(saveData.getCurrentLocation());

      // SaveData 생성
      SaveData result = new SaveData(player, gameState, slotNumber);
      result.setSaveTime(saveData.getSaveTime());
      result.setVersion(saveData.getVersion());

      logger.info("최적화된 게임 로드 완료: 슬롯 {} (캐릭터: {})", slotNumber, player.getName());
      return result;

    } catch (Exception e) {
      logger.error("게임 로드 실패: 슬롯 {}", slotNumber, e);
      throw new IOException("저장 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 모든 저장 슬롯 정보 조회 (최적화)
   */
  public static List<SaveSlotInfo> getAllSaveSlots() {
    List<SaveSlotInfo> slots = new ArrayList<>();

    for (int i = GameConstants.NUMBER_ONE; i <= SystemConstants.MAX_SAVE_SLOTS; i++) {
      String fileName = SystemConstants.SAVE_FILE_PREFIX + i + ".json";
      Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

      if (Files.exists(saveFilePath)) {
        try {
          // 최적화된 방식으로 메타데이터만 읽기
          SimpleSaveData saveData = objectMapper.readValue(saveFilePath.toFile(), SimpleSaveData.class);

          SaveSlotInfo slotInfo =
              new SaveSlotInfo(i, true, saveData.getPlayerName(), saveData.getLevel(), saveData.getSaveTime(), saveData.getTotalPlayTime());
          slots.add(slotInfo);

        } catch (IOException e) {
          logger.warn("슬롯 {} 정보 읽기 실패: {}", i, e.getMessage());
          slots.add(new SaveSlotInfo(i, false, "오류", 0, "알 수 없음", 0));
        }
      } else {
        slots.add(new SaveSlotInfo(i, false, null, 0, null, 0));
      }
    }

    return slots;
  }

  /**
   * 저장 슬롯 삭제
   */
  public static boolean deleteSaveSlot(int slotNumber) {
    if (slotNumber < GameConstants.NUMBER_ONE || slotNumber > SystemConstants.MAX_SAVE_SLOTS) {
      System.out.println("잘못된 슬롯 번호입니다.");
      return false;
    }

    String fileName = SystemConstants.SAVE_FILE_PREFIX + slotNumber + ".json";
    Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

    try {
      if (Files.exists(saveFilePath)) {
        // 삭제 전 백업
        createBackup(saveFilePath, slotNumber);

        boolean deleted = Files.deleteIfExists(saveFilePath);
        if (deleted) {
          logger.info("슬롯 {} 삭제 완료", slotNumber);
          System.out.println("슬롯 " + slotNumber + "가 삭제되었습니다.");
        }
        return deleted;
      } else {
        System.out.println("슬롯 " + slotNumber + "는 이미 비어있습니다.");
        return true;
      }
    } catch (IOException e) {
      logger.error("슬롯 {} 삭제 중 오류", slotNumber, e);
      System.out.println("저장 파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
      return false;
    }
  }

  /**
   * 백업 파일 생성
   */
  private static void createBackup(Path originalFile, int slotNumber) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String backupFileName = SystemConstants.BACKUP_PREFIX + "slot" + slotNumber + "_" + timestamp + ".json";
      Path backupPath = Paths.get(SystemConstants.SAVE_DIRECTORY, backupFileName);

      Files.copy(originalFile, backupPath);
      logger.debug("백업 파일 생성: {}", backupPath);

      // 오래된 백업 정리
      cleanupOldBackups();

    } catch (IOException e) {
      logger.warn("백업 파일 생성 실패: 슬롯 {}", slotNumber, e);
    }
  }

  /**
   * 오래된 백업 파일 정리
   */
  private static void cleanupOldBackups() {
    try {
      Path saveDir = Paths.get(SystemConstants.SAVE_DIRECTORY);
      Files.list(saveDir).filter(path -> path.getFileName().toString().startsWith(SystemConstants.BACKUP_PREFIX))
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString())).skip(10) // 최신 10개는 유지
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
              logger.debug("오래된 백업 파일 삭제: {}", path);
            } catch (IOException e) {
              logger.warn("백업 파일 삭제 실패: {}", path, e);
            }
          });
    } catch (IOException e) {
      logger.warn("백업 파일 정리 중 오류", e);
    }
  }

}
