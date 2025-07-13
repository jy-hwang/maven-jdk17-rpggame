package rpg.domain.monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.shared.constant.GameConstants;

/**
 * 개선된 게임 몬스터 클래스 - JSON 데이터 완전 지원
 */
public class Monster {
  private static final Logger logger = LoggerFactory.getLogger(Monster.class);

  // === 기본 필드들 (기존 유지) ===
  private String name;
  private int hp;
  private int maxHp;
  private int attack;
  private int expReward;
  private int goldReward;

  // === JSON 확장 필드들 (새로 추가/개선) ===
  private String id; // JSON에서의 고유 ID
  private String description; // 몬스터 설명
  private int defense; // ⭐ 누락되어 있던 방어력
  private int speed; // ⭐ 누락되어 있던 속도
  private double criticalRate; // ⭐ 누락되어 있던 크리티컬 확률
  private String rarity; // 등급
  private List<String> abilities; // ⭐ 누락되어 있던 특수 능력
  private List<String> locations; // 출현 지역
  private int minLevel; // 최소 출현 레벨
  private int maxLevel; // 최대 출현 레벨
  private double spawnRate; // 출현 확률
  private Map<String, Object> properties; // 확장 속성
  private MonsterData sourceData; // ⭐ 원본 JSON 데이터 참조 개선

  /**
   * 기본 몬스터 생성자 (레거시 호환성 유지)
   */
  public Monster(String name, int hp, int attack, int expReward, int goldReward) {
    validateBasicFields(name, hp, attack, expReward, goldReward);

    this.name = name.trim();
    this.hp = hp;
    this.maxHp = hp;
    this.attack = attack;
    this.expReward = expReward;
    this.goldReward = goldReward;

    // 기본값으로 확장 필드 초기화
    initializeDefaultExtendedFields();

    logger.debug("기본 몬스터 생성: {} (HP: {}, 공격력: {})", this.name, hp, attack);
  }

  /**
   * JSON 데이터를 포함한 확장 몬스터 생성자
   */
  public Monster(String name, int hp, int attack, int expReward, int goldReward, MonsterData sourceData) {
    validateBasicFields(name, hp, attack, expReward, goldReward);

    this.name = name.trim();
    this.hp = hp;
    this.maxHp = hp;
    this.attack = attack;
    this.expReward = expReward;
    this.goldReward = goldReward;
    this.sourceData = sourceData;

    // JSON 데이터로부터 확장 필드 초기화
    if (sourceData != null) {
      initializeFromMonsterData(sourceData);
    } else {
      initializeDefaultExtendedFields();
    }

    logger.debug("확장 몬스터 생성: {} (JSON: {})", this.name, sourceData != null);
  }

  // === 팩토리 메서드들 ===

  /**
   * ⭐ 개선된 MonsterData 기반 팩토리 메서드
   */
  public static Monster fromMonsterData(MonsterData data) {
    if (data == null) {
      throw new IllegalArgumentException("MonsterData는 null일 수 없습니다.");
    }

    // MonsterData의 stats와 rewards 정보 사용
    MonsterStats stats = data.getStats();
    MonsterRewards rewards = data.getRewards();

    Monster monster = new Monster(data.getName(), stats.getHp(), stats.getAttack(), rewards.getExp(), rewards.getGold(), data // ⭐ sourceData로 전달하여 참조
                                                                                                                              // 유지
    );

    return monster;
  }

  /**
   * 기본 몬스터 생성 (레거시 지원)
   */
  public static Monster createBasic(String name, int hp, int attack, int expReward, int goldReward) {
    return new Monster(name, hp, attack, expReward, goldReward);
  }

  // === 초기화 메서드들 ===

  private void validateBasicFields(String name, int hp, int attack, int expReward, int goldReward) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("몬스터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("몬스터 이름은 비어있을 수 없습니다.");
    }

    if (hp < GameConstants.NUMBER_ONE) {
      logger.error("몬스터 HP가 유효하지 않음: {}", hp);
      throw new IllegalArgumentException("몬스터 HP는 1 이상이어야 합니다.");
    }

    if (attack < GameConstants.NUMBER_ZERO) {
      logger.error("몬스터 공격력이 유효하지 않음: {}", attack);
      throw new IllegalArgumentException("몬스터 공격력은 0 이상이어야 합니다.");
    }

    if (expReward < GameConstants.NUMBER_ZERO) {
      logger.error("몬스터 경험치 보상이 유효하지 않음: {}", expReward);
      throw new IllegalArgumentException("경험치 보상은 0 이상이어야 합니다.");
    }

    if (goldReward < GameConstants.NUMBER_ZERO) {
      logger.error("몬스터 골드 보상이 유효하지 않음: {}", goldReward);
      throw new IllegalArgumentException("골드 보상은 0 이상이어야 합니다.");
    }
  }

  /**
   * ⭐ MonsterData로부터 확장 필드 초기화 (개선됨)
   */
  private void initializeFromMonsterData(MonsterData data) {
    this.id = data.getId();
    this.description = data.getDescription();

    // ⭐ 누락되어 있던 stats 필드들 추가
    MonsterStats stats = data.getStats();
    this.defense = stats.getDefense();
    this.speed = stats.getSpeed();
    this.criticalRate = stats.getCriticalRate();

    this.rarity = data.getRarity();
    this.abilities = new ArrayList<>(data.getAbilities()); // ⭐ 추가
    this.locations = new ArrayList<>(data.getLocations());
    this.minLevel = data.getMinLevel();
    this.maxLevel = data.getMaxLevel();
    this.spawnRate = data.getSpawnRate();
    this.properties = new HashMap<>(data.getProperties());

    logger.debug("MonsterData로부터 확장 필드 초기화 완료: {}", this.name);
  }

  /**
   * 기본값으로 확장 필드 초기화
   */
  private void initializeDefaultExtendedFields() {
    this.id = "UNKNOWN_" + name.toUpperCase().replace(" ", "_");
    this.description = name + "에 대한 설명이 없습니다.";
    this.defense = 1; // ⭐ 기본 방어력
    this.speed = 5; // ⭐ 기본 속도
    this.criticalRate = 0.05; // ⭐ 기본 크리티컬 확률 5%
    this.rarity = "COMMON";
    this.abilities = new ArrayList<>(); // ⭐ 빈 능력 리스트
    this.locations = new ArrayList<>();
    this.minLevel = 1;
    this.maxLevel = 99;
    this.spawnRate = 1.0;
    this.properties = new HashMap<>();

    logger.debug("기본값으로 확장 필드 초기화: {}", this.name);
  }

  // === 편의 메서드들 ===

  /**
   * ⭐ 몬스터의 드롭 아이템 정보를 쉽게 가져오는 메서드
   */
  public List<DropItem> getDropItems() {
    if (sourceData != null && sourceData.getRewards() != null) {
      return sourceData.getRewards().getDropItems();
    }
    return new ArrayList<>(); // 빈 리스트 반환
  }

  /**
   * ⭐ MonsterData 참조가 있는지 확인
   */
  public boolean hasMonsterData() {
    return sourceData != null;
  }

  /**
   * ⭐ 특정 능력을 가지고 있는지 확인
   */
  public boolean hasAbility(String abilityName) {
    return abilities.contains(abilityName);
  }

  /**
   * ⭐ 특정 지역에 출현하는지 확인
   */
  public boolean canSpawnInLocation(String location) {
    return locations.contains(location);
  }

  /**
   * ⭐ 플레이어 레벨에 적합한 몬스터인지 확인
   */
  public boolean isSuitableForLevel(int playerLevel) {
    return playerLevel >= minLevel && playerLevel <= maxLevel;
  }

  // === Getters (기존 + 새로 추가) ===

  // 기존 getters
  public String getName() {
    return name;
  }

  public int getHp() {
    return hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public int getAttack() {
    return attack;
  }

  public int getExpReward() {
    return expReward;
  }

  public int getGoldReward() {
    return goldReward;
  }

  // ⭐ 새로 추가된 getters
  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public int getDefense() {
    return defense;
  } // ⭐ 추가

  public int getSpeed() {
    return speed;
  } // ⭐ 추가

  public double getCriticalRate() {
    return criticalRate;
  } // ⭐ 추가

  public String getRarity() {
    return rarity;
  }

  public List<String> getAbilities() {
    return new ArrayList<>(abilities);
  } // ⭐ 추가

  public List<String> getLocations() {
    return new ArrayList<>(locations);
  }

  public int getMinLevel() {
    return minLevel;
  }

  public int getMaxLevel() {
    return maxLevel;
  }

  public double getSpawnRate() {
    return spawnRate;
  }

  public Map<String, Object> getProperties() {
    return new HashMap<>(properties);
  }

  public MonsterData getMonsterData() {
    return sourceData;
  } // ⭐ 개선된 getter

  // === Setters (상태 변경용) ===

  public void setHp(int hp) {
    this.hp = Math.max(0, hp);
    logger.debug("몬스터 {} HP 변경: {}/{}", name, this.hp, maxHp);
  }

  public void takeDamage(int damage) {
    int actualDamage = Math.max(1, damage - defense); // ⭐ 방어력 적용
    setHp(hp - actualDamage);
    logger.debug("몬스터 {} 피해 입음: {} (방어력 {} 적용, 실제 피해: {})", name, damage, defense, actualDamage);
  }

  public boolean isAlive() {
    return hp > 0;
  }

  public boolean isDead() {
    return hp <= 0;
  }

  // === 전투 관련 메서드들 ===

  /**
   * ⭐ 크리티컬 공격 확인
   */
  public boolean rollCriticalHit() {
    return Math.random() < criticalRate;
  }

  /**
   * ⭐ 실제 공격력 계산 (크리티컬 포함)
   */
  public int calculateAttackDamage() {
    int baseDamage = attack;
    if (rollCriticalHit()) {
      baseDamage = (int) (baseDamage * 1.5); // 크리티컬 시 1.5배
      logger.debug("몬스터 {} 크리티컬 공격! 피해: {}", name, baseDamage);
    }
    return baseDamage;
  }

  // === Object 메서드 오버라이드 ===

  @Override
  public String toString() {
    return String.format("Monster{name='%s', hp=%d/%d, attack=%d, defense=%d, level=%d-%d}", name, hp, maxHp, attack, defense, minLevel, maxLevel);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    Monster monster = (Monster) obj;
    return id != null ? id.equals(monster.id) : name.equals(monster.name);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : name.hashCode();
  }
}
