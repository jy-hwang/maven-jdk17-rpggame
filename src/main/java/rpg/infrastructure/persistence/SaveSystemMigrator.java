package rpg.infrastructure.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import rpg.core.engine.GameState;
import rpg.domain.player.Player;
import rpg.infrastructure.data.mapper.SaveGameMapper;
import rpg.shared.constant.SystemConstants;
import rpg.shared.dto.save.SaveGameDto;

/**
 * 기존 DTO 기반 저장 파일을 새로운 SimpleSaveData 형식으로 마이그레이션하는 유틸리티
 */
public class SaveSystemMigrator {

  private static final Logger logger = LoggerFactory.getLogger(SaveSystemMigrator.class);
  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  /**
   * 모든 저장 슬롯을 새로운 형식으로 마이그레이션
   */
  public static void migrateAllSlots() {
    logger.info("===== 저장 시스템 마이그레이션 시작 =====");

    int migratedCount = 0;
    int errorCount = 0;

    for (int i = 1; i <= SystemConstants.MAX_SAVE_SLOTS; i++) {
      try {
        if (migrateSlot(i)) {
          migratedCount++;
          logger.info("슬롯 {} 마이그레이션 완료", i);
        }
      } catch (Exception e) {
        errorCount++;
        logger.error("슬롯 {} 마이그레이션 실패", i, e);
      }
    }

    logger.info("===== 마이그레이션 완료: 성공 {}개, 실패 {}개 =====", migratedCount, errorCount);

    if (migratedCount > 0) {
      System.out.println("🎉 " + migratedCount + "개 슬롯이 새로운 형식으로 변환되었습니다!");
      System.out.println("💾 파일 크기가 대폭 줄어들었습니다.");
    }

    if (errorCount > 0) {
      System.out.println("⚠️  " + errorCount + "개 슬롯 변환에 실패했습니다. 로그를 확인해주세요.");
    }
  }

  /**
   * 특정 슬롯을 새로운 형식으로 마이그레이션
   */
  public static boolean migrateSlot(int slotNumber) throws IOException {
    String fileName = SystemConstants.SAVE_FILE_PREFIX + slotNumber + ".json";
    Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

    if (!Files.exists(saveFilePath)) {
      logger.debug("슬롯 {} 파일이 존재하지 않음 - 건너뜀", slotNumber);
      return false;
    }

    // 기존 파일 크기 확인
    long originalSize = Files.size(saveFilePath);

    try {
      // 1. 기존 DTO 형식으로 읽기 시도
      SaveGameDto oldDto = objectMapper.readValue(saveFilePath.toFile(), SaveGameDto.class);

      // 2. 이미 새로운 형식인지 확인 (간단한 휴리스틱)
      if (isAlreadyMigrated(saveFilePath)) {
        logger.debug("슬롯 {}는 이미 새로운 형식 - 건너뜀", slotNumber);
        return false;
      }

      // 3. DTO를 도메인 모델로 변환
      GameDataRepository.SaveData oldSaveData = SaveGameMapper.fromDto(oldDto);
      Player player = oldSaveData.getCharacter();
      GameState gameState = oldSaveData.getGameState();

      // 4. 백업 생성
      createMigrationBackup(saveFilePath, slotNumber);

      // 5. 새로운 형식으로 저장
      SimpleSaveData newSaveData = SimpleSaveData.from(player, gameState, slotNumber);
      objectMapper.writeValue(saveFilePath.toFile(), newSaveData);

      // 6. 결과 확인
      long newSize = Files.size(saveFilePath);
      double reduction = ((double) (originalSize - newSize) / originalSize) * 100;

      logger.info("슬롯 {} 마이그레이션 성공: {} bytes → {} bytes ({:.1f}% 감소)", slotNumber, originalSize, newSize, reduction);

      return true;

    } catch (Exception e) {
      logger.error("슬롯 {} 마이그레이션 중 오류", slotNumber, e);
      throw new IOException("마이그레이션 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 파일이 이미 새로운 형식인지 확인
   */
  private static boolean isAlreadyMigrated(Path filePath) {
    try {
      // 새로운 형식인지 확인하기 위해 SimpleSaveData로 읽기 시도
      SimpleSaveData saveData = objectMapper.readValue(filePath.toFile(), SimpleSaveData.class);
      return saveData.getPlayerName() != null; // 성공하면 새로운 형식
    } catch (Exception e) {
      return false; // 실패하면 구 형식
    }
  }

  /**
   * 마이그레이션 백업 파일 생성
   */
  private static void createMigrationBackup(Path originalFile, int slotNumber) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String backupFileName = "migration_backup_slot" + slotNumber + "_" + timestamp + ".json";
      Path backupPath = Paths.get(SystemConstants.SAVE_DIRECTORY, backupFileName);

      Files.copy(originalFile, backupPath);
      logger.debug("마이그레이션 백업 생성: {}", backupPath);

    } catch (IOException e) {
      logger.warn("마이그레이션 백업 생성 실패: 슬롯 {}", slotNumber, e);
    }
  }

  /**
   * 특정 슬롯의 파일 크기 비교
   */
  public static void compareFileSizes() {
    logger.info("===== 저장 파일 크기 비교 =====");

    long totalOriginal = 0;
    long totalNew = 0;
    int fileCount = 0;

    for (int i = 1; i <= SystemConstants.MAX_SAVE_SLOTS; i++) {
      String fileName = SystemConstants.SAVE_FILE_PREFIX + i + ".json";
      Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

      if (Files.exists(saveFilePath)) {
        try {
          long currentSize = Files.size(saveFilePath);

          // 백업 파일에서 원본 크기 추정
          String backupPattern = "migration_backup_slot" + i + "_";
          Path saveDir = Paths.get(SystemConstants.SAVE_DIRECTORY);

          long originalSize = Files.list(saveDir).filter(path -> path.getFileName().toString().startsWith(backupPattern)).mapToLong(path -> {
            try {
              return Files.size(path);
            } catch (IOException e) {
              return 0;
            }
          }).findFirst().orElse(currentSize);

          if (originalSize > currentSize) {
            double reduction = ((double) (originalSize - currentSize) / originalSize) * 100;
            logger.info("슬롯 {}: {} bytes → {} bytes ({:.1f}% 감소)", i, originalSize, currentSize, reduction);

            totalOriginal += originalSize;
            totalNew += currentSize;
            fileCount++;
          }

        } catch (IOException e) {
          logger.warn("슬롯 {} 크기 확인 실패", i, e);
        }
      }
    }

    if (fileCount > 0) {
      double totalReduction = ((double) (totalOriginal - totalNew) / totalOriginal) * 100;
      logger.info("===== 전체 요약: {} bytes → {} bytes ({:.1f}% 감소) =====", totalOriginal, totalNew, totalReduction);
    }
  }

  /**
   * 마이그레이션 롤백 (백업에서 복원)
   */
  public static boolean rollbackSlot(int slotNumber) {
    try {
      Path saveDir = Paths.get(SystemConstants.SAVE_DIRECTORY);
      String backupPattern = "migration_backup_slot" + slotNumber + "_";

      Path backupFile = Files.list(saveDir).filter(path -> path.getFileName().toString().startsWith(backupPattern))
          .max((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString())).orElse(null);

      if (backupFile == null) {
        logger.warn("슬롯 {} 백업 파일을 찾을 수 없음", slotNumber);
        return false;
      }

      String fileName = SystemConstants.SAVE_FILE_PREFIX + slotNumber + ".json";
      Path saveFilePath = Paths.get(SystemConstants.SAVE_DIRECTORY, fileName);

      Files.copy(backupFile, saveFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      logger.info("슬롯 {} 롤백 완료: {}", slotNumber, backupFile.getFileName());

      return true;

    } catch (IOException e) {
      logger.error("슬롯 {} 롤백 실패", slotNumber, e);
      return false;
    }
  }
}
