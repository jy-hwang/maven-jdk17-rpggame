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
  public static final String BASIC_POTIONS_CONFIG = "/config/items/basic_potions.json";
  public static final String BASIC_WEAPONS_CONFIG = "/config/items/basic_weapons.json";
  public static final String BASIC_ARMORS_CONFIG = "/config/items/basic_armors.json";
  public static final String BASIC_ACCESSORIES_CONFIG = "/config/items/basic_accessories.json";

  // === 탐험 아이템 JSON 설정파일 경로 ===
  public static final String EXPLORE_TREASURE = "/config/items/treasure_items.json";
  public static final String EXPLORE_EQUIPMENT = "/config/items/equipment_items.json";
  public static final String EXPLORE_DROP = "/config/items/drop_items.json";

  // === 몬스터 JSON 설정 파일 경로 ===
  public static final String MONSTERS_CONFIG = "/config/monsters/monsters.json";
  public static final String FOREST_MONSTERS_CONFIG = "/config/monsters/forest_monsters.json";
  public static final String CAVE_MONSTERS_CONFIG = "/config/monsters/cave_monsters.json";
  public static final String MOUNTAIN_MONSTERS_CONFIG = "/config/monsters/mountain_monsters.json";
  public static final String SPECIAL_MONSTERS_CONFIG = "/config/monsters/special_monsters.json";
  public static final String WATER_MONSTERS_CONFIG = "/config/monsters/water_monsters.json";
  public static final String RUINS_MONSTERS_CONFIG = "/config/monsters/ruins_monsters.json";
  public static final String LAVA_MONSTERS_CONFIG = "/config/monsters/lava_monsters.json";

  // === 퀘스트 JSON 설정 파일 경로 ===
  public static final String MAIN_QUESTS_CONFIG = "/config/quests/main-quests.json";
  public static final String SIDE_QUESTS_CONFIG = "/config/quests/side-quests.json";
  public static final String DAILY_QUESTS_CONFIG = "/config/quests/daily-quests.json";
  public static final String WEEKLY_QUESTS_CONFIG = "/config/quests/weekly-quests.json";
  public static final String EVENT_QUESTS_CONFIG = "/config/quests/event-quests.json";
  public static final String TUTORIAL_QUESTS_CONFIG = "/config/quests/tutorial-quests.json";

  // === 퀘스트 시스템 설정 ===
  public static final int MAX_ACTIVE_QUESTS = 10; // 동시 진행 가능한 퀘스트 수
  public static final int MAX_DAILY_QUESTS = 3; // 일일 퀘스트 최대 개수
  public static final int MAX_WEEKLY_QUESTS = 2; // 주간 퀘스트 최대 개수
  public static final boolean ENABLE_QUEST_CHAINS = true; // 연쇄 퀘스트 활성화
  public static final boolean ENABLE_DYNAMIC_QUESTS = true; // 동적 퀘스트 활성화
  public static final boolean QUEST_AUTO_ACCEPT = false; // 퀘스트 자동 수락

  // === 퀘스트 시간 설정 (초 단위) ===
  public static final int DAILY_QUEST_RESET_TIME = 86400; // 24시간
  public static final int WEEKLY_QUEST_RESET_TIME = 604800; // 7일
  public static final int DEFAULT_QUEST_TIMEOUT = 0; // 무제한 (0 = 제한 없음)

  // === JSON 파일 검증 관련 ===
  public static final boolean ENABLE_JSON_VALIDATION = true; // JSON 데이터 검증 활성화
  public static final boolean STRICT_VALIDATION = false; // 엄격한 검증 모드
  public static final boolean LOG_VALIDATION_ERRORS = true; // 검증 오류 로깅

//=== 게임 플레이 개선 관련 ===
 public static final boolean SHOW_MONSTER_HINTS = true; // 몬스터 힌트 표시
 public static final boolean DETAILED_COMBAT_LOG = false; // 상세 전투 로그
 public static final boolean ENABLE_MONSTER_ABILITIES = true; // 몬스터 특수 능력 활성화
 public static final boolean SHOW_QUEST_HINTS = true; // 퀘스트 힌트 표시
 public static final boolean DETAILED_QUEST_LOG = true; // 상세 퀘스트 로그

 // === 퀘스트 UI 관련 ===
 public static final boolean SHOW_QUEST_PROGRESS_BAR = true; // 진행률 바 표시
 public static final boolean SHOW_QUEST_REWARDS_PREVIEW = true; // 보상 미리보기
 public static final boolean ENABLE_QUEST_NOTIFICATIONS = true; // 퀘스트 알림
 public static final boolean SHOW_COMPLETION_EFFECTS = true; // 완료 효과 표시

 // === 개발/디버그 관련 ===
 public static final boolean DEBUG_MODE = true; // 디버그 모드
 public static final boolean ENABLE_MONSTER_STATS = true; // 몬스터 통계 표시
 public static final boolean AUTO_RELOAD_JSON = false; // JSON 자동 리로드 (개발용)
 public static final boolean DEBUG_QUEST_SYSTEM = false; // 퀘스트 시스템 디버그
 public static final boolean QUEST_TEMPLATE_VALIDATION = true; // 퀘스트 템플릿 검증

 // === 퀘스트 밸런싱 관련 ===
 public static final double QUEST_EXP_MULTIPLIER = 1.0; // 퀘스트 경험치 배율
 public static final double QUEST_GOLD_MULTIPLIER = 1.0; // 퀘스트 골드 배율
 public static final double DAILY_QUEST_BONUS = 1.2; // 일일 퀘스트 보너스 배율
 public static final double WEEKLY_QUEST_BONUS = 1.5; // 주간 퀘스트 보너스 배율

 // === 퀘스트 생성 관련 ===
 public static final int MIN_DYNAMIC_QUEST_LEVEL = 5; // 동적 퀘스트 최소 레벨
 public static final int MAX_QUEST_OBJECTIVES = 5; // 최대 퀘스트 목표 개수
 public static final int MIN_QUEST_REWARD_GOLD = 10; // 최소 퀘스트 골드 보상
 public static final int MIN_QUEST_REWARD_EXP = 20; // 최소 퀘스트 경험치 보상
}
