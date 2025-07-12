package config;

public class BaseConstant {
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

  // === 전투 관련 상수 ===
  public static final int ESCAPE_CHANCE = 50;

  // === 탐험 관련 상수 ===
  public static final int RANDOM_EVENT_CHANCE = 15;
  public static final int ITEM_DROP_CHANCE = 20;

  // === 상점 이벤트 관련 상수 ===
  public static final int EVENT_CHANCE = 20;
  public static final int BEGINNER_LEVEL = 3;
  public static final int INTERMEDIATE_LEVEL = 7;
  public static final int HIGH_LEVEL = 15;
  public static final int ULTRA_HIGH_LEVEL = 25;

  // === 아이템 등급별 확률 ===
  public static final double COMMON_RATE = 50.0;
  public static final double UNCOMMON_RATE = 25.0;
  public static final double RARE_RATE = 10.0;
  public static final double EPIC_RATE = 4.0;
  public static final double LEGENDARY_RATE = 1.0;
  // === 아이템 등급별 가격 배율 ===
  public static final double COMMON_MULTIPL = 1.0;
  public static final double UNCOMMON_MULTIPL = 1.2;
  public static final double RARE_MULTIPL = 1.5;
  public static final double EPIC_MULTIPL = 2.0;
  public static final double LEGENDARY_MULTIPL = 3.0;

  // === 케릭터 관련 상수 ===
  public static final int INITIAL_LEVEL = 1;
  public static final int INITIAL_MAX_HP = 100;
  public static final int INITIAL_MAX_MANA = 50;
  public static final int INITIAL_EXP = 0;
  public static final int INITIAL_ATTACK = 10;
  public static final int INITIAL_DEFENSE = 5;
  public static final int INITIAL_GOLD = 100;
  public static final double RESTORE_HP = 1.0;
  public static final double RESTORE_MANA = 1.0;

  public static final int LEVEL_UP_HP_BONUS = 20;
  public static final int LEVEL_UP_MANA_BONUS = 15;
  public static final int LEVEL_UP_ATTACK_BONUS = 5;
  public static final int LEVEL_UP_DEFENSE_BONUS = 3;
  public static final double LEVEL_UP_RESTORE_HP = 0.3;
  public static final double LEVEL_UP_RESTORE_MANA = 0.2;

  // === 인벤토리 관련 ===
  public static final int DEFAULT_INVENTORY = 20;
  public static final double INVENTORY_DANGER_ALERT = 0.9;
  public static final double INVENTORY_WARNING_ALERT = 0.4;

  // 기타 숫자 관련
  public static final int NUMBER_ZERO = 0;
  public static final int NUMBER_ONE = 1;
  public static final int NUMBER_TWO = 2;
  public static final int NUMBER_FIVE = 5;
  public static final int NUMBER_TEN = 10;
  public static final int NUMBER_TWENTY = 20;
  public static final int NUMBER_THIRTY = 30;
  public static final int NUMBER_THIRTY_ONE = 31;
  public static final int NUMBER_FIFTY = 50;
  public static final int NUMBER_EIGHTY_ONE = 81;
  public static final int NUMBER_HUNDRED = 100;
  public static final int NUMBER_THOUSAND = 1000;

  // === 몬스터 관련 상수 ===
  public static final double NUMBER_ZERO_DOT_ZERO = 0.0;
  public static final double MONSTER_CRITICAL = 0.3;
  public static final double DEFAULT_SPAWN_RATE = 0.5; // 기본 출현율
  public static final int MIN_MONSTER_LEVEL = 1;
  public static final int MAX_MONSTER_LEVEL = 99;
  public static final double RARE_MONSTER_CHANCE = 0.1; // 희귀 몬스터 출현 확률
  public static final double BOSS_MONSTER_CHANCE = 0.05; // 보스 몬스터 출현 확률

  // === 몬스터 능력치 관련 ===
  public static final double MONSTER_STAT_VARIANCE = 0.1; // 능력치 변동폭 (±10%)
  public static final int MONSTER_CRITICAL_BASE = 10; // 기본 크리티컬 확률 (%)
  public static final double MONSTER_LEVEL_SCALING = 1.2; // 레벨당 능력치 증가율

  // === 지역별 몬스터 출현 가중치 ===
  public static final double FOREST_WEIGHT = 1.0;
  public static final double CAVE_WEIGHT = 0.8;
  public static final double MOUNTAIN_WEIGHT = 0.6;
  public static final double WATER_WEIGHT = 0.4;
  public static final double RUINS_WEIGHT = 0.3;
  public static final double LAVA_WEIGHT = 0.2;

  // === 몬스터 드롭 관련 ===
  public static final double BASE_DROP_RATE = 0.3; // 기본 드롭 확률
  public static final double RARE_DROP_RATE = 0.1; // 희귀 아이템 드롭 확률
  public static final double LEGENDARY_DROP_RATE = 0.01; // 전설 아이템 드롭 확률
  public static final int MAX_DROP_QUANTITY = 5; // 최대 드롭 수량

  // === 몬스터 특수 능력 관련 ===
  public static final double ABILITY_TRIGGER_CHANCE = 0.25; // 특수 능력 발동 확률
  public static final int REGENERATION_AMOUNT = 5; // 재생 회복량
  public static final double CRITICAL_DAMAGE_MULTIPLIER = 1.5; // 크리티컬 데미지 배율

  // === 몬스터 JSON 캐싱 관련 ===
  public static final boolean ENABLE_MONSTER_CACHE = true; // 몬스터 데이터 캐싱 활성화
  public static final int MONSTER_CACHE_SIZE = 1000; // 캐시 최대 크기
  public static final long MONSTER_CACHE_EXPIRE_TIME = 3600000; // 캐시 만료 시간 (1시간)

  // === 몬스터 밸런싱 관련 ===
  public static final double PLAYER_LEVEL_BONUS = 0.1; // 플레이어 레벨당 보너스
  public static final double LOCATION_DIFFICULTY_MULTIPLIER = 1.0; // 지역별 난이도 배율
  public static final int MONSTER_SCALING_THRESHOLD = 10; // 몬스터 스케일링 임계점

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
