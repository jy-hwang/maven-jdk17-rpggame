package config;

public class BaseConstant {
  // 게임 버전
  public static final double GAME_VERSION = 1.4;

  // 저장관련
  public static final int MAX_SAVE_SLOTS = 5;
  public static final String SAVE_DIRECTORY = "save";
  public static final String SAVE_FILE_PREFIX = "rpg_save_slot";
  public static final String BACKUP_PREFIX = "rpg_save_backup_";

  // 설정 파일 경로
  public static final String BASIC_POTIONS_CONFIG = "/config/basic_potions.json";

  // 전투 관련 상수
  public static final int ESCAPE_CHANCE = 50;

  // 탐험 관련 상수
  public static final int RANDOM_EVENT_CHANCE = 15;
  public static final int ITEM_DROP_CHANCE = 20;

  // 상점 이벤트 관련 상수
  public static final int EVENT_CHANCE = 20;
  public static final int BEGINNER_LEVEL = 3;
  public static final int INTERMEDIATE_LEVEL = 7;
  public static final int HIGH_LEVEL = 15;
  public static final int ULTRA_HIGH_LEVEL = 25;

  // 아이템 등급별 확률
  public static final double COMMON_RATE = 50.0;
  public static final double UNCOMMON_RATE = 25.0;
  public static final double RARE_RATE = 10.0;
  public static final double EPIC_RATE = 4.0;
  public static final double LEGENDARY_RATE = 1.0;
  // 아이템 등급별 가격 배율
  public static final double COMMON_MULTIPL = 1.0;
  public static final double UNCOMMON_MULTIPL = 1.2;
  public static final double RARE_MULTIPL = 1.5;
  public static final double EPIC_MULTIPL = 2.0;
  public static final double LEGENDARY_MULTIPL = 3.0;

  // 케릭터 관련 상수
  public static final int INITIAL_LEVEL = 1;
  public static final int INITIAL_MAX_HP = 100;
  public static final int INITIAL_MAX_MANA = 50;
  public static final int INITIAL_EXP = 0;
  public static final int INITIAL_ATTACK = 10;
  public static final int INITIAL_DEFENSE = 5;
  public static final int INITIAL_GOLD = 100;
  public static final double RESTORE_HP = 1.0;
  public static final double RESTORE_MANA = 2.0;

  public static final int LEVEL_UP_HP_BONUS = 20;
  public static final int LEVEL_UP_MANA_BONUS = 15;
  public static final int LEVEL_UP_ATTACK_BONUS = 5;
  public static final int LEVEL_UP_DEFENSE_BONUS = 3;
  public static final double LEVEL_UP_RESTORE_HP = 0.2;
  public static final double LEVEL_UP_RESTORE_MANA = 0.4;

  // 인벤토리 관련
  public static final int DEFAULT_INVENTORY = 20;
  public static final double INVENTORY_DANGER_ALERT = 0.9;
  public static final double INVENTORY_WARNING_ALERT = 0.4;

  // 몬스터 관련
  public static final double NUMBER_ZERO_DOT_ZERO = 0.0;
  public static final double MONSTER_CRITICAL = 0.3;

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


}
