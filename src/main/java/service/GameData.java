package service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.Character;

/**
 * 게임 데이터 저장/로드를 담당하는 서비스 클래스
 */
public class GameData {
  private static final Logger logger = LoggerFactory.getLogger(GameData.class);

  private static final String SAVE_DIRECTORY = "data";
  private static final String SAVE_FILE = "rpg_save.csv";
  private static final String BACKUP_PREFIX = "rpg_save_backup_";
  private static final String CSV_HEADER = "name,level,hp,maxHp,exp,attack,defense,gold";

  static {
    // 저장 디렉토리 생성
    createSaveDirectory();
  }

  /**
   * 저장 디렉토리를 생성합니다.
   */
  private static void createSaveDirectory() {
    try {
      Path saveDir = Paths.get(SAVE_DIRECTORY);
      if (!Files.exists(saveDir)) {
        Files.createDirectories(saveDir);
        logger.info("저장 디렉토리 생성: {}", saveDir.toAbsolutePath());
      }
    } catch (Exception e) {
      logger.error("저장 디렉토리 생성 실패", e);
    }
  }

  /**
   * 게임을 저장합니다.
   * 
   * @param character 저장할 캐릭터
   * @throws GameDataException 저장 실패 시
   */
  public static void saveGame(Character character) throws GameDataException {
    if (character == null) {
      logger.error("저장할 캐릭터가 null입니다.");
      throw new GameDataException("저장할 캐릭터 데이터가 없습니다.");
    }

    Path saveFilePath = Paths.get(SAVE_DIRECTORY, SAVE_FILE);

    try {
      // 기존 파일이 있으면 백업 생성
      if (Files.exists(saveFilePath)) {
        createBackup(saveFilePath);
      }

      // 새 파일 저장
      try (FileWriter writer = new FileWriter(saveFilePath.toFile(), false)) {
        writer.write(CSV_HEADER + "\n");
        writer.write(character.toCsv() + "\n");
        writer.flush();
      }

      logger.info("게임 저장 완료: {} (캐릭터: {})", saveFilePath.toAbsolutePath(), character.getName());
      System.out.println("게임이 저장되었습니다!");

    } catch (IOException e) {
      logger.error("게임 저장 실패: {}", saveFilePath, e);
      throw new GameDataException("게임 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
    } catch (Exception e) {
      logger.error("예상치 못한 저장 오류", e);
      throw new GameDataException("게임 저장 중 예상치 못한 오류가 발생했습니다.", e);
    }
  }

  /**
   * 기존 저장 파일의 백업을 생성합니다.
   * 
   * @param originalFile 원본 파일 경로
   */
  private static void createBackup(Path originalFile) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String backupFileName = BACKUP_PREFIX + timestamp + ".csv";
      Path backupPath = Paths.get(SAVE_DIRECTORY, backupFileName);

      Files.copy(originalFile, backupPath);
      logger.debug("백업 파일 생성: {}", backupPath);

      // 오래된 백업 파일들 정리 (최대 5개까지만 유지)
      cleanupOldBackups();

    } catch (IOException e) {
      logger.warn("백업 파일 생성 실패: {}", originalFile, e);
    }
  }

  /**
   * 오래된 백업 파일들을 정리합니다. (최대 5개까지만 유지)
   */
  private static void cleanupOldBackups() {
    try {
      Path saveDir = Paths.get(SAVE_DIRECTORY);
      Files.list(saveDir).filter(path -> path.getFileName().toString().startsWith(BACKUP_PREFIX))
          .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
          .skip(5) // 최신 5개는 유지
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

  /**
   * 게임을 불러옵니다.
   * 
   * @return 불러온 캐릭터, 실패 시 null
   * @throws GameDataException 로드 실패 시
   */
  public static Character loadGame() throws GameDataException {
    Path saveFilePath = Paths.get(SAVE_DIRECTORY, SAVE_FILE);

    if (!Files.exists(saveFilePath)) {
      logger.info("저장 파일이 존재하지 않음: {}", saveFilePath);
      System.out.println("저장 파일을 찾을 수 없습니다.");
      return null;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(saveFilePath.toFile()))) {
      String header = reader.readLine();

      // 헤더 검증
      if (header == null || !header.equals(CSV_HEADER)) {
        logger.warn("잘못된 CSV 헤더: {}", header);
        throw new GameDataException("저장 파일 형식이 올바르지 않습니다.");
      }

      String data = reader.readLine();
      if (data == null || data.trim().isEmpty()) {
        logger.warn("빈 데이터 라인");
        throw new GameDataException("저장 파일에 캐릭터 데이터가 없습니다.");
      }

      Character character = Character.fromCsv(data);
      logger.info("게임 로드 완료: {} (캐릭터: {})", saveFilePath, character.getName());
      return character;

    } catch (FileNotFoundException e) {
      logger.error("저장 파일을 찾을 수 없음: {}", saveFilePath, e);
      throw new GameDataException("저장 파일을 찾을 수 없습니다.", e);
    } catch (IOException e) {
      logger.error("파일 읽기 오류: {}", saveFilePath, e);
      throw new GameDataException("저장 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      logger.error("캐릭터 데이터 파싱 오류", e);
      throw new GameDataException("저장 파일의 캐릭터 데이터가 손상되었습니다: " + e.getMessage(), e);
    } catch (Exception e) {
      logger.error("예상치 못한 로드 오류", e);
      throw new GameDataException("게임 로드 중 예상치 못한 오류가 발생했습니다.", e);
    }
  }

  /**
   * 저장 파일이 존재하는지 확인합니다.
   * 
   * @return 저장 파일 존재 시 true
   */
  public static boolean saveFileExists() {
    Path saveFilePath = Paths.get(SAVE_DIRECTORY, SAVE_FILE);
    boolean exists = Files.exists(saveFilePath);
    logger.debug("저장 파일 존재 여부: {} ({})", exists, saveFilePath);
    return exists;
  }

  /**
   * 저장 파일을 삭제합니다.
   * 
   * @return 삭제 성공 시 true
   */
  public static boolean deleteSaveFile() {
    Path saveFilePath = Paths.get(SAVE_DIRECTORY, SAVE_FILE);

    try {
      if (Files.exists(saveFilePath)) {
        // 삭제 전 백업 생성
        createBackup(saveFilePath);

        boolean deleted = Files.deleteIfExists(saveFilePath);
        if (deleted) {
          logger.info("저장 파일 삭제 완료: {}", saveFilePath);
          System.out.println("저장 파일이 삭제되었습니다.");
        } else {
          logger.warn("저장 파일 삭제 실패: {}", saveFilePath);
        }
        return deleted;
      } else {
        logger.info("삭제할 저장 파일이 없음: {}", saveFilePath);
        System.out.println("삭제할 저장 파일이 없습니다.");
        return true;
      }
    } catch (IOException e) {
      logger.error("저장 파일 삭제 중 오류: {}", saveFilePath, e);
      System.out.println("저장 파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
      return false;
    }
  }

  /**
   * 게임 데이터 관련 예외 클래스
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
