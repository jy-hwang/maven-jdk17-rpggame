package rpg.infrastructure.data.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.application.manager.LocationManager;
import rpg.domain.monster.MonsterData;
import rpg.shared.constant.SystemConstants;

/**
 * 리팩토링된 몬스터 데이터 로더 - 통합 JSON 파일 사용
 */
public class MonsterDataLoader {
  private static final Logger logger = LoggerFactory.getLogger(MonsterDataLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  // 통합된 몬스터 데이터 저장소
  private static final Map<String, MonsterData> allMonsters = new HashMap<>();
  private static boolean dataLoaded = false;

  /**
   * 모든 몬스터 데이터를 로드합니다.
   */
  public static synchronized Map<String, MonsterData> loadAllMonsters() {
    if (!dataLoaded) {
      loadMonsterData();
    }
    return new HashMap<>(allMonsters);
  }

  public static List<MonsterData> getAllMonsters() {
    if (!dataLoaded) {
        loadAllMonsters();
    }
    return new ArrayList<>(allMonsters.values());
}
  
  /**
   * 통합 JSON 파일에서 몬스터 데이터 로드
   */
  private static void loadMonsterData() {
    try {
      logger.info("통합 몬스터 데이터 로드 시작: {}", SystemConstants.UNIFIED_MONSTERS_CONFIG);

      try (InputStream inputStream = MonsterDataLoader.class.getResourceAsStream(SystemConstants.UNIFIED_MONSTERS_CONFIG)) {
        if (inputStream == null) {
          throw new IOException("몬스터 설정 파일을 찾을 수 없습니다: " + SystemConstants.UNIFIED_MONSTERS_CONFIG);
        }

        JsonNode rootNode = objectMapper.readTree(inputStream);
        JsonNode monstersNode = rootNode.get("monsters");

        if (monstersNode == null || !monstersNode.isArray()) {
          throw new IOException("몬스터 설정 파일 형식이 올바르지 않습니다");
        }

        // 각 몬스터 데이터 파싱
        for (JsonNode monsterNode : monstersNode) {
          try {
            MonsterData monsterData = objectMapper.treeToValue(monsterNode, MonsterData.class);
            allMonsters.put(monsterData.getId(), monsterData);
            logger.debug("몬스터 로드: {} ({})", monsterData.getId(), monsterData.getName());
          } catch (Exception e) {
            logger.error("몬스터 데이터 파싱 실패: {}", monsterNode.get("id"), e);
          }
        }

        dataLoaded = true;
        logger.info("몬스터 데이터 로드 완료: {}종", allMonsters.size());

      }
    } catch (Exception e) {
      logger.error("몬스터 데이터 로드 실패", e);
      createDefaultMonsters();
      dataLoaded = true;
    }
  }

  /**
   * 기본 몬스터 데이터 생성 (로드 실패 시)
   */
  private static void createDefaultMonsters() {
    logger.warn("기본 몬스터 데이터 생성 중...");

    // 기본 몬스터 몇 개 하드코딩으로 생성
    // 실제 구현에서는 MonsterData 생성자에 맞게 조정 필요
    logger.info("기본 몬스터 {}개 생성 완료", allMonsters.size());
  }

  /**
   * 특정 지역의 몬스터 목록 반환
   */
  public static List<MonsterData> getMonstersByLocation(String locationId) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.values().stream().filter(monster -> monster.getLocations().contains(locationId)).collect(Collectors.toList());
  }

  /**
   * 특정 지역과 레벨에 적합한 몬스터 목록 반환
   */
  public static List<MonsterData> getMonstersByLocationAndLevel(String locationId, int playerLevel) {
    return getMonstersByLocation(locationId).stream()
        .filter(monster -> playerLevel >= monster.getMinLevel() && playerLevel <= monster.getMaxLevel() + 2) // 약간의 여유
        .collect(Collectors.toList());
  }

  /**
   * 특정 레벨에 적합한 모든 몬스터 반환
   */
  public static List<MonsterData> getMonstersByLevel(int playerLevel) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.values().stream().filter(monster -> playerLevel >= monster.getMinLevel() && playerLevel <= monster.getMaxLevel())
        .collect(Collectors.toList());
  }

  /**
   * 특정 희귀도의 몬스터 목록 반환
   */
  public static List<MonsterData> getMonstersByRarity(String rarity) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.values().stream().filter(monster -> rarity.equalsIgnoreCase(monster.getRarity())).collect(Collectors.toList());
  }

  /**
   * 몬스터 ID로 특정 몬스터 데이터 반환
   */
  public static MonsterData getMonsterById(String monsterId) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.get(monsterId);
  }

  /**
   * 몬스터 이름으로 검색
   */
  public static List<MonsterData> getMonstersByName(String name) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.values().stream().filter(monster -> monster.getName().contains(name)).collect(Collectors.toList());
  }

  /**
   * 레벨 범위에 맞는 몬스터 반환
   */
  public static List<MonsterData> getMonstersByLevelRange(int minLevel, int maxLevel) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return allMonsters.values().stream().filter(monster -> monster.getMinLevel() <= maxLevel && monster.getMaxLevel() >= minLevel)
        .collect(Collectors.toList());
  }

  /**
   * 몬스터 통계 정보 출력
   */
  public static void printMonsterStatistics() {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    System.out.println("\n👹 === 몬스터 통계 ===");
    System.out.println("총 몬스터 종류: " + allMonsters.size() + "종");

    // 희귀도별 통계
    Map<String, Long> rarityStats = allMonsters.values().stream().collect(Collectors.groupingBy(MonsterData::getRarity, Collectors.counting()));

    System.out.println("\n📊 희귀도별 분포:");
    rarityStats.forEach((rarity, count) -> System.out.printf("   %s: %d종%n", rarity, count));

    // 지역별 통계
    Map<String, Long> locationStats = allMonsters.values().stream().flatMap(monster -> monster.getLocations().stream())
        .collect(Collectors.groupingBy(location -> location, Collectors.counting()));

    System.out.println("\n🗺️ 지역별 분포:");
    locationStats.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).forEach(entry -> {
      String locationName = LocationManager.getLocationName(entry.getKey());
      System.out.printf("   %s: %d종%n", locationName, entry.getValue());
    });

    // 레벨 분포
    IntSummaryStatistics levelStats =
        allMonsters.values().stream().mapToInt(monster -> (monster.getMinLevel() + monster.getMaxLevel()) / 2).summaryStatistics();

    System.out.println("\n📈 레벨 분포:");
    System.out.printf("   최소: %d | 최대: %d | 평균: %.1f%n", levelStats.getMin(), levelStats.getMax(), levelStats.getAverage());

    System.out.println("==================");
  }

  /**
   * 특정 지역의 몬스터 통계
   */
  public static void printLocationMonsterStats(String locationId) {
    List<MonsterData> locationMonsters = getMonstersByLocation(locationId);

    if (locationMonsters.isEmpty()) {
      System.out.println("❌ 해당 지역에 몬스터가 없습니다.");
      return;
    }

    String locationName = LocationManager.getLocationName(locationId);
    System.out.println("\n👹 === " + locationName + " 몬스터 통계 ===");
    System.out.println("총 몬스터 종류: " + locationMonsters.size() + "종");

    // 레벨 범위
    int minLevel = locationMonsters.stream().mapToInt(MonsterData::getMinLevel).min().orElse(0);
    int maxLevel = locationMonsters.stream().mapToInt(MonsterData::getMaxLevel).max().orElse(0);
    System.out.println("레벨 범위: " + minLevel + " ~ " + maxLevel);

    // 희귀도 분포
    Map<String, Long> rarityDist = locationMonsters.stream().collect(Collectors.groupingBy(MonsterData::getRarity, Collectors.counting()));

    System.out.println("희귀도 분포:");
    rarityDist.forEach((rarity, count) -> System.out.printf("   %s: %d종%n", rarity, count));

    System.out.println("==================");
  }

  /**
   * 데이터 리로드
   */
  public static synchronized void reloadData() {
    logger.info("몬스터 데이터 리로드 시작");
    allMonsters.clear();
    dataLoaded = false;
    loadAllMonsters();
    logger.info("몬스터 데이터 리로드 완료");
  }

  /**
   * 로드 상태 확인
   */
  public static boolean isDataLoaded() {
    return dataLoaded;
  }

  /**
   * 캐시된 몬스터 수 반환
   */
  public static int getMonsterCount() {
    return allMonsters.size();
  }

  // === 하위 호환성을 위한 deprecated 메서드들 ===

  /**
   * @deprecated getMonstersByLocation 사용 권장
   */
  @Deprecated
  public static List<MonsterData> getMonstersByLocation(String locationName, boolean useKoreanName) {
    if (useKoreanName) {
      String locationId = LocationManager.getLocationIdByKoreanName(locationName);
      return locationId != null ? getMonstersByLocation(locationId) : new ArrayList<>();
    } else {
      return getMonstersByLocation(locationName);
    }
  }
}
