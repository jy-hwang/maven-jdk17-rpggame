package rpg.shared.constant;

public class SystemConstants {
  // === 게임 버전 ===
  public static final double GAME_VERSION = 1.6; // 몬스터 JSON 시스템 추가로 버전 업

  // === 저장관련 ===
  public static final int MAX_SAVE_SLOTS = 5;
  public static final String SAVE_DIRECTORY = "save";
  public static final String SAVE_FILE_PREFIX = "rpg_save_slot";
  public static final String BACKUP_PREFIX = "rpg_save_backup_";

  // === 설정 파일 경로 ===
  public static final String BASIC_POTIONS_CONFIG = "/config/basic_potions.json";
  public static final String BASIC_WEAPONS_CONFIG = "/config/basic_weapons.json";
  public static final String BASIC_ARMORS_CONFIG = "/config/basic_armors.json";
  public static final String BASIC_ACCESSORIES_CONFIG = "/config/basic_accessories.json";

  // === 탐험 아이템 JSON 설정파일 경로 ===
  public static final String EXPLORE_TREASURE = "/config/treasure_items.json";
  public static final String EXPLORE_EQUIPMENT = "/config/equipment_items.json";
  public static final String EXPLORE_DROP = "/config/drop_items.json";

  // === 몬스터 JSON 설정 파일 경로 ===
  public static final String MONSTERS_CONFIG = "/config/monsters.json";
  public static final String FOREST_MONSTERS_CONFIG = "/config/forest_monsters.json";
  public static final String CAVE_MONSTERS_CONFIG = "/config/cave_monsters.json";
  public static final String MOUNTAIN_MONSTERS_CONFIG = "/config/mountain_monsters.json";
  public static final String SPECIAL_MONSTERS_CONFIG = "/config/special_monsters.json";
  public static final String WATER_MONSTERS_CONFIG = "/config/water_monsters.json";
  public static final String RUINS_MONSTERS_CONFIG = "/config/ruins_monsters.json";
  public static final String LAVA_MONSTERS_CONFIG = "/config/lava_monsters.json";

  
  // === JSON 파일 검증 관련 ===
  public static final boolean ENABLE_JSON_VALIDATION = true; // JSON 데이터 검증 활성화
  public static final boolean STRICT_VALIDATION = false; // 엄격한 검증 모드
  public static final boolean LOG_VALIDATION_ERRORS = true; // 검증 오류 로깅

  // === 게임 플레이 개선 관련 ===
  public static final boolean SHOW_MONSTER_HINTS = true; // 몬스터 힌트 표시
  public static final boolean DETAILED_COMBAT_LOG = false; // 상세 전투 로그
  public static final boolean ENABLE_MONSTER_ABILITIES = true; // 몬스터 특수 능력 활성화

  // === 개발/디버그 관련 ===
  public static final boolean DEBUG_MODE = true; // 디버그 모드
  public static final boolean ENABLE_MONSTER_STATS = true; // 몬스터 통계 표시
  public static final boolean AUTO_RELOAD_JSON = false; // JSON 자동 리로드 (개발용)

}
