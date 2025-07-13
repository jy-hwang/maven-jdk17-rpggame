package rpg.application.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rpg.domain.location.DangerLevel;
import rpg.domain.location.LocationData;
import rpg.shared.constant.SystemConstants;

public class LocationManager {
  private static final Logger logger = LoggerFactory.getLogger(LocationManager.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Map<String, LocationData> locations = new HashMap<>();
  private static boolean initialized = false;

  /**
   * 지역 데이터 초기화
   */
  public static synchronized void initialize() {
    if (initialized) {
      logger.debug("LocationManager 이미 초기화됨");
      return;
    }

    try {
      loadLocationData();
      initialized = true;
      logger.info("LocationManager 초기화 완료: {}개 지역 로드", locations.size());
    } catch (Exception e) {
      logger.error("LocationManager 초기화 실패", e);
      createDefaultLocations();
      initialized = true;
    }
  }

  /**
   * JSON 파일에서 지역 데이터 로드
   */
  private static void loadLocationData() throws IOException {
    try (InputStream inputStream = LocationManager.class.getResourceAsStream(SystemConstants.LOCATIONS_CONFIG)) {
      if (inputStream == null) {
        throw new IOException("지역 설정 파일을 찾을 수 없습니다: " + SystemConstants.LOCATIONS_CONFIG);
      }

      JsonNode rootNode = objectMapper.readTree(inputStream);
      JsonNode locationsNode = rootNode.get("locations");

      if (locationsNode == null || !locationsNode.isArray()) {
        throw new IOException("지역 설정 파일 형식이 올바르지 않습니다");
      }

      for (JsonNode locationNode : locationsNode) {
        LocationData locationData = objectMapper.treeToValue(locationNode, LocationData.class);
        locations.put(locationData.getId(), locationData);
        logger.debug("지역 로드: {} ({})", locationData.getId(), locationData.getNameKo());
      }
    }
  }

  /**
   * 기본 지역 데이터 생성 (JSON 로드 실패 시)
   */
  private static void createDefaultLocations() {
    logger.warn("기본 지역 데이터 생성 중...");

    // 하드코딩된 기본 지역 몇 개 생성
    Map<String, Object> emptyProps = new HashMap<>();

    locations.put("deep_forest", new LocationData("deep_forest", "숲속 깊은 곳", "Deep Forest", "🌲", "울창한 숲입니다.", 1, 3, "EASY", 40, emptyProps));

    locations.put("dark_cave", new LocationData("dark_cave", "어두운 동굴", "Dark Cave", "🕳️", "어두운 동굴입니다.", 2, 5, "NORMAL", 25, emptyProps));

    logger.info("기본 지역 {}개 생성 완료", locations.size());
  }

  /**
   * 지역 ID로 지역 데이터 조회
   */
  public static LocationData getLocation(String locationId) {
    if (!initialized) {
      initialize();
    }
    return locations.get(locationId);
  }

  /**
   * 지역 ID로 한글 이름 조회
   */
  public static String getLocationName(String locationId) {
    LocationData location = getLocation(locationId);
    return location != null ? location.getNameKo() : locationId;
  }

  /**
   * 지역 ID로 영문 이름 조회
   */
  public static String getLocationNameEn(String locationId) {
    LocationData location = getLocation(locationId);
    return location != null ? location.getNameEn() : locationId;
  }

  /**
   * 플레이어 레벨에 따라 접근 가능한 지역 목록 반환
   */
  public static List<LocationData> getAvailableLocations(int playerLevel) {
    if (!initialized) {
      initialize();
    }

    return locations.values().stream().filter(location -> playerLevel >= location.getMinLevel()).sorted((l1, l2) -> {
      int priority1 = getLocationPriority(l1, playerLevel);
      int priority2 = getLocationPriority(l2, playerLevel);
      return Integer.compare(priority2, priority1); // 내림차순
    }).collect(Collectors.toList());
  }

  /**
   * 지역의 우선순위 계산 (플레이어 레벨 기준)
   */
  private static int getLocationPriority(LocationData location, int playerLevel) {
    if (playerLevel >= location.getMinLevel() && playerLevel <= location.getMaxLevel()) {
      return 100; // 적정 레벨 - 최우선
    } else if (playerLevel < location.getMaxLevel() + 3) {
      return 50; // 약간 높은 레벨 - 도전적
    } else {
      return 10; // 너무 낮은 레벨 - 낮은 우선순위
    }
  }

  /**
   * 모든 지역 목록 반환
   */
  public static List<LocationData> getAllLocations() {
    if (!initialized) {
      initialize();
    }
    return new ArrayList<>(locations.values());
  }

  /**
   * 특정 난이도의 지역들 반환
   */
  public static List<LocationData> getLocationsByDanger(DangerLevel dangerLevel) {
    if (!initialized) {
      initialize();
    }

    return locations.values().stream().filter(location -> location.getDangerLevel() == dangerLevel).collect(Collectors.toList());
  }

  /**
   * 레벨 범위에 맞는 지역들 반환
   */
  public static List<LocationData> getLocationsByLevelRange(int minLevel, int maxLevel) {
    if (!initialized) {
      initialize();
    }

    return locations.values().stream().filter(location -> location.getMinLevel() <= maxLevel && location.getMaxLevel() >= minLevel)
        .collect(Collectors.toList());
  }

  /**
   * 지역 통계 출력
   */
  public static void printLocationStatistics() {
    if (!initialized) {
      initialize();
    }

    System.out.println("\n🗺️ === 지역 통계 ===");
    System.out.println("총 지역 수: " + locations.size() + "개");

    // 난이도별 통계
    Map<DangerLevel, Long> dangerStats =
        locations.values().stream().collect(Collectors.groupingBy(LocationData::getDangerLevel, Collectors.counting()));

    System.out.println("\n📊 난이도별 분포:");
    dangerStats.forEach((danger, count) -> System.out.printf("   %s %s: %d개%n", danger.getEmoji(), danger.getDisplayName(), count));

    System.out.println("==================");
  }

  /**
   * 한글 지역명으로 지역 ID 찾기 (호환성을 위해)
   */
  public static String getLocationIdByKoreanName(String koreanName) {
    if (!initialized) {
      initialize();
    }

    return locations.entrySet().stream().filter(entry -> entry.getValue().getNameKo().equals(koreanName)).map(Map.Entry::getKey).findFirst()
        .orElse(null);
  }

  /**
   * 초기화 상태 확인
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * 데이터 리로드
   */
  public static synchronized void reload() {
    locations.clear();
    initialized = false;
    initialize();
    logger.info("LocationManager 리로드 완료");
  }
}
