package loader;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.BaseConstant;
import model.item.GameEffectData;
import model.item.GameItemData;

public class GameDataLoader {
  private static final Logger logger = LoggerFactory.getLogger(GameDataLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();



  /**
   * 기본 물약 데이터 로드
   */
  public static Map<String, GameItemData> loadBasicPotions() {
    try {
      logger.info("기본 물약 데이터 로드 시작...");

      InputStream inputStream = GameDataLoader.class.getResourceAsStream(BaseConstant.BASIC_POTIONS_CONFIG);

      if (inputStream == null) {
        logger.error("물약 설정 파일을 찾을 수 없습니다: {}", BaseConstant.BASIC_POTIONS_CONFIG);
        return createDefaultPotions();
      }

      List<GameItemData> potionList = objectMapper.readValue(inputStream, new TypeReference<List<GameItemData>>() {});

      Map<String, GameItemData> potionMap = potionList.stream().collect(Collectors.toMap(GameItemData::getId, item -> item));

      logger.info("기본 물약 데이터 로드 완료: {}개", potionList.size());

      // 로드된 데이터 검증
      validateLoadedData(potionList);

      return potionMap;

    } catch (Exception e) {
      logger.error("기본 물약 데이터 로드 실패", e);
      return createDefaultPotions();
    }
  }

  /**
   * 로드된 데이터 검증
   */
  private static void validateLoadedData(List<GameItemData> potionList) {
    int validCount = 0;
    int invalidCount = 0;

    for (GameItemData potion : potionList) {
      boolean isValid = true;

      // 기본 필드 검증
      if (potion.getId() == null || potion.getId().trim().isEmpty()) {
        logger.warn("물약 ID가 비어있음: {}", potion.getName());
        isValid = false;
      }

      if (potion.getName() == null || potion.getName().trim().isEmpty()) {
        logger.warn("물약 이름이 비어있음: ID {}", potion.getId());
        isValid = false;
      }

      if (potion.getValue() < 0) {
        logger.warn("물약 가격이 음수: {} ({}골드)", potion.getName(), potion.getValue());
        isValid = false;
      }

      // 효과 검증
      if (potion.getEffects().isEmpty()) {
        logger.warn("효과가 없는 물약: {}", potion.getName());
        isValid = false;
      } else {
        for (GameEffectData effect : potion.getEffects()) {
          if (effect.getType() == null || effect.getValue() <= 0) {
            logger.warn("잘못된 효과 데이터: {} - 타입: {}, 값: {}", potion.getName(), effect.getType(), effect.getValue());
            isValid = false;
          }
        }
      }

      if (isValid) {
        validCount++;
        logger.debug("유효한 물약: {} (효과 {}개)", potion.getName(), potion.getEffects().size());
      } else {
        invalidCount++;
      }
    }

    logger.info("데이터 검증 완료: 유효 {}개, 무효 {}개", validCount, invalidCount);

    if (invalidCount > 0) {
      logger.warn("일부 물약 데이터에 문제가 있습니다. 로그를 확인하세요.");
    }
  }

  /**
   * 기본 물약 생성 (JSON 파일이 없을 때)
   */
  private static Map<String, GameItemData> createDefaultPotions() {
    logger.info("기본 물약 데이터를 코드로 생성 중...");

    try {
      // HP 회복 물약
      List<GameEffectData> hpEffect = List.of(new GameEffectData("HEAL_HP", 50));

      // MP 회복 물약
      List<GameEffectData> mpEffect = List.of(new GameEffectData("HEAL_MP", 40));

      // 큰 HP 회복 물약
      List<GameEffectData> largeHpEffect = List.of(new GameEffectData("HEAL_HP", 100));

      Map<String, GameItemData> defaultPotions = Map.of("HEALTH_POTION",
          new GameItemData("HEALTH_POTION", "체력 물약", "HP를 50 회복합니다", "CONSUMABLE", 50, "COMMON", true, hpEffect), "MANA_POTION",
          new GameItemData("MANA_POTION", "마나 물약", "MP를 40 회복합니다", "CONSUMABLE", 60, "COMMON", true, mpEffect), "LARGE_HEALTH_POTION",
          new GameItemData("LARGE_HEALTH_POTION", "큰 체력 물약", "HP를 100 회복합니다", "CONSUMABLE", 120, "UNCOMMON", true, largeHpEffect));

      logger.info("기본 물약 생성 완료: {}개", defaultPotions.size());

      // 생성된 기본 데이터도 검증
      validateLoadedData(List.copyOf(defaultPotions.values()));

      return defaultPotions;

    } catch (Exception e) {
      logger.error("기본 물약 생성 실패", e);
      return Map.of(); // 빈 맵 반환
    }
  }

  /**
   * 설정 파일 존재 여부 확인
   */
  public static boolean isConfigFileExists() {
    InputStream inputStream = GameDataLoader.class.getResourceAsStream(BaseConstant.BASIC_POTIONS_CONFIG);
    boolean exists = inputStream != null;

    if (exists) {
      try {
        inputStream.close();
      } catch (Exception e) {
        logger.debug("InputStream 닫기 실패", e);
      }
    }

    logger.debug("설정 파일 존재 여부: {} ({})", exists, BaseConstant.BASIC_POTIONS_CONFIG);
    return exists;
  }

  /**
   * 게임 데이터 전체 로드 (확장 가능)
   */
  public static void loadAllGameData() {
    logger.info("전체 게임 데이터 로드 시작...");

    // 현재는 물약만 로드하지만, 나중에 확장 가능
    Map<String, GameItemData> potions = loadBasicPotions();

    // 추후 추가할 데이터들
    // Map<String, MonsterData> monsters = loadMonsters();
    // Map<String, SkillData> skills = loadSkills();
    // Map<String, QuestData> quests = loadQuests();

    logger.info("전체 게임 데이터 로드 완료");
    logger.info("로드된 데이터: 물약 {}개", potions.size());

    // 통계 정보 출력
    if (logger.isInfoEnabled()) {
      printDataStatistics(potions);
    }
  }

  /**
   * 데이터 통계 정보 출력
   */
  private static void printDataStatistics(Map<String, GameItemData> potions) {
    if (potions.isEmpty()) {
      logger.info("데이터 통계: 로드된 아이템이 없습니다.");
      return;
    }

    // 타입별 통계
    Map<String, Long> typeStats = potions.values().stream().collect(Collectors.groupingBy(GameItemData::getType, Collectors.counting()));

    // 등급별 통계
    Map<String, Long> rarityStats =
        potions.values().stream().collect(Collectors.groupingBy(item -> item.getRarity().getDisplayName(), Collectors.counting()));

    // 중첩 가능 통계
    long stackableCount = potions.values().stream().mapToLong(item -> item.isStackable() ? 1 : 0).sum();

    // 효과별 통계
    Map<String, Long> effectStats = potions.values().stream().flatMap(item -> item.getEffects().stream())
        .collect(Collectors.groupingBy(GameEffectData::getType, Collectors.counting()));

    logger.info("=== 데이터 통계 ===");
    logger.info("총 아이템: {}개", potions.size());
    logger.info("타입별: {}", typeStats);
    logger.info("등급별: {}", rarityStats);
    logger.info("중첩 가능: {}개", stackableCount);
    logger.info("효과별: {}", effectStats);
    logger.info("================");
  }

  /**
   * 특정 타입의 아이템만 로드 (확장용)
   */
  public static Map<String, GameItemData> loadItemsByType(String itemType) {
    Map<String, GameItemData> allItems = loadBasicPotions();

    return allItems.entrySet().stream().filter(entry -> entry.getValue().getType().equalsIgnoreCase(itemType))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 데이터 다시 로드 (런타임 리로드용)
   */
  public static void reloadGameData() {
    logger.info("게임 데이터 리로드 중...");

    // 캐시 무효화 (구현 시)
    // clearCache();

    // 데이터 다시 로드
    loadAllGameData();

    logger.info("게임 데이터 리로드 완료");
  }
}
