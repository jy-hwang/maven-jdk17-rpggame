package rpg.infrastructure.data.loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.domain.monster.MonsterData;
import rpg.domain.monster.MonsterRewards;
import rpg.domain.monster.MonsterStats;
import rpg.shared.constant.SystemConstants;

public class MonsterDataLoader {
  private static final Logger logger = LoggerFactory.getLogger(MonsterDataLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static Map<String, MonsterData> monsterCache = new HashMap<>();
  private static Map<String, List<MonsterData>> locationCache = new HashMap<>();
  private static boolean dataLoaded = false;

  /**
   * 모든 몬스터 데이터를 로드합니다.
   */
  public static Map<String, MonsterData> loadAllMonsters() {
    if (dataLoaded && !monsterCache.isEmpty()) {
      return new HashMap<>(monsterCache);
    }

    try {
      // 기본 몬스터 데이터 로드
      Map<String, MonsterData> monsters = loadMonstersFromFile(SystemConstants.MONSTERS_CONFIG);

      // 지역별 몬스터 데이터 로드
      monsters.putAll(loadMonstersFromFile(SystemConstants.FOREST_MONSTERS_CONFIG));
      monsters.putAll(loadMonstersFromFile(SystemConstants.CAVE_MONSTERS_CONFIG));
      monsters.putAll(loadMonstersFromFile(SystemConstants.MOUNTAIN_MONSTERS_CONFIG));
      monsters.putAll(loadMonstersFromFile(SystemConstants.SPECIAL_MONSTERS_CONFIG));

      // 데이터 검증
      validateMonsterData(monsters.values());

      // 캐시 업데이트
      monsterCache = monsters;
      updateLocationCache(monsters);
      dataLoaded = true;

      logger.info("몬스터 데이터 로드 완료: {}종류", monsters.size());
      return new HashMap<>(monsters);

    } catch (Exception e) {
      logger.error("몬스터 데이터 로드 실패", e);
      return createDefaultMonsters();
    }
  }

  /**
   * 특정 파일에서 몬스터 데이터를 로드합니다.
   */
  private static Map<String, MonsterData> loadMonstersFromFile(String filePath) {
    try (InputStream inputStream = MonsterDataLoader.class.getResourceAsStream(filePath)) {
      if (inputStream == null) {
        logger.warn("몬스터 파일 없음: {}", filePath);
        return Map.of();
      }

      List<MonsterData> monsterList = objectMapper.readValue(inputStream, new TypeReference<List<MonsterData>>() {});

      return monsterList.stream().collect(Collectors.toMap(MonsterData::getId, monster -> monster));

    } catch (Exception e) {
      logger.error("몬스터 파일 로드 실패: {}", filePath, e);
      return Map.of();
    }
  }

  /**
   * 지역별 몬스터를 반환합니다.
   */
  public static List<MonsterData> getMonstersByLocation(String location) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return locationCache.getOrDefault(location, List.of());
  }

  /**
   * 플레이어 레벨에 적합한 몬스터를 반환합니다.
   */
  public static List<MonsterData> getMonstersByLevel(int playerLevel) {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    return monsterCache.values().stream().filter(monster -> playerLevel >= monster.getMinLevel() && playerLevel <= monster.getMaxLevel())
        .collect(Collectors.toList());
  }

  /**
   * 지역과 레벨에 적합한 몬스터를 반환합니다.
   */
  public static List<MonsterData> getMonstersByLocationAndLevel(String location, int playerLevel) {
    return getMonstersByLocation(location).stream().filter(monster -> playerLevel >= monster.getMinLevel() && playerLevel <= monster.getMaxLevel())
        .collect(Collectors.toList());
  }

  /**
   * 지역별 캐시를 업데이트합니다.
   */
  private static void updateLocationCache(Map<String, MonsterData> monsters) {
    locationCache.clear();

    for (MonsterData monster : monsters.values()) {
      for (String location : monster.getLocations()) {
        locationCache.computeIfAbsent(location, k -> new ArrayList<>()).add(monster);
      }
    }

    logger.debug("지역별 캐시 업데이트 완료: {}개 지역", locationCache.size());
  }

  /**
   * 몬스터 데이터를 검증합니다.
   */
  private static void validateMonsterData(Collection<MonsterData> monsters) {
    for (MonsterData monster : monsters) {
      if (monster.getId() == null || monster.getId().trim().isEmpty()) {
        throw new IllegalArgumentException("몬스터 ID가 비어있습니다: " + monster.getName());
      }

      if (monster.getName() == null || monster.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("몬스터 이름이 비어있습니다: " + monster.getId());
      }

      if (monster.getStats() == null) {
        throw new IllegalArgumentException("몬스터 스탯이 없습니다: " + monster.getName());
      }

      if (monster.getStats().getHp() <= 0) {
        throw new IllegalArgumentException("몬스터 HP가 0 이하입니다: " + monster.getName());
      }

      if (monster.getMinLevel() > monster.getMaxLevel()) {
        throw new IllegalArgumentException("최소 레벨이 최대 레벨보다 큽니다: " + monster.getName());
      }
    }

    logger.debug("몬스터 데이터 검증 완료: {}종류", monsters.size());
  }

  /**
   * 기본 몬스터 데이터를 생성합니다. (JSON 파일이 없을 때)
   */
  private static Map<String, MonsterData> createDefaultMonsters() {
    logger.warn("기본 몬스터 데이터 생성 중...");

    Map<String, MonsterData> defaultMonsters = new HashMap<>();

    // 기본 몬스터들
    defaultMonsters.put("SLIME", new MonsterData("SLIME", "슬라임", "젤리 같은 몬스터", new MonsterStats(20, 5, 2, 3, 0.1),
        new MonsterRewards(10, 5, List.of()), List.of("숲속 깊은 곳"), 1, 3, 0.8, "COMMON", List.of(), Map.of()));

    defaultMonsters.put("GOBLIN", new MonsterData("GOBLIN", "고블린", "작고 교활한 몬스터", new MonsterStats(30, 8, 3, 5, 0.15),
        new MonsterRewards(15, 10, List.of()), List.of("숲속 깊은 곳", "어두운 동굴"), 2, 5, 0.7, "COMMON", List.of(), Map.of()));

    logger.info("기본 몬스터 생성 완료: {}종류", defaultMonsters.size());
    return defaultMonsters;
  }

  /**
   * 몬스터 데이터를 다시 로드합니다.
   */
  public static void reloadMonsterData() {
    logger.info("몬스터 데이터 리로드 중...");

    monsterCache.clear();
    locationCache.clear();
    dataLoaded = false;

    loadAllMonsters();

    logger.info("몬스터 데이터 리로드 완료");
  }

  /**
   * 몬스터 통계를 출력합니다.
   */
  public static void printMonsterStatistics() {
    if (!dataLoaded) {
      loadAllMonsters();
    }

    System.out.println("\n🐾 === 몬스터 통계 ===");
    System.out.println("총 몬스터 종류: " + monsterCache.size() + "개");
    System.out.println("총 출현 지역: " + locationCache.size() + "개");

    // 등급별 통계
    Map<String, Long> rarityStats = monsterCache.values().stream().collect(Collectors.groupingBy(MonsterData::getRarity, Collectors.counting()));

    System.out.println("\n📊 등급별 분포:");
    rarityStats.forEach((rarity, count) -> System.out.printf("   %s: %d개%n", rarity, count));

    // 지역별 통계
    System.out.println("\n🗺️ 지역별 분포:");
    locationCache.forEach((location, monsters) -> System.out.printf("   %s: %d종류%n", location, monsters.size()));

    System.out.println("==================");
  }
}
