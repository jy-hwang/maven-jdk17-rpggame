package model.monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;

/**
 * 게임 몬스터를 표현하는 클래스
 */
public class Monster {
  private static final Logger logger = LoggerFactory.getLogger(Monster.class);

  private String name;
  private int hp;
  private int maxHp; // 원본 HP 보존을 위해 추가
  private int attack;
  private int expReward;
  private int goldReward;


  // === 새로운 JSON 확장 필드들 ===
  private String id; // JSON에서의 고유 ID
  private String description; // 몬스터 설명
  private int defense; // 방어력
  private int speed; // 속도
  private double criticalRate; // 크리티컬 확률
  private String rarity; // 등급 (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)
  private List<String> abilities; // 특수 능력
  private List<String> locations; // 출현 지역
  private int minLevel; // 최소 출현 레벨
  private int maxLevel; // 최대 출현 레벨
  private double spawnRate; // 출현 확률
  private Map<String, Object> properties; // 확장 속성
  private MonsterData sourceData; // 원본 JSON 데이터 참조

  /**
   * 몬스터 생성자
   * 
   * @param name 몬스터 이름
   * @param hp 체력
   * @param attack 공격력
   * @param expReward 경험치 보상
   * @param goldReward 골드 보상
   * @throws IllegalArgumentException 유효하지 않은 값인 경우
   */
  public Monster(String name, int hp, int attack, int expReward, int goldReward) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("몬스터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("몬스터 이름은 비어있을 수 없습니다.");
    }

    if (hp < BaseConstant.NUMBER_ONE) {
      logger.error("몬스터 HP가 유효하지 않음: {}", hp);
      throw new IllegalArgumentException("몬스터 HP는 " + BaseConstant.NUMBER_ONE + " 이상이어야 합니다.");
    }

    if (attack < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 공격력이 유효하지 않음: {}", attack);
      throw new IllegalArgumentException("몬스터 공격력은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    if (expReward < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 경험치 보상이 유효하지 않음: {}", expReward);
      throw new IllegalArgumentException("경험치 보상은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    if (goldReward < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 골드 보상이 유효하지 않음: {}", goldReward);
      throw new IllegalArgumentException("골드 보상은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    this.name = name.trim();
    this.hp = hp;
    this.maxHp = hp;
    this.attack = attack;
    this.expReward = expReward;
    this.goldReward = goldReward;

    logger.debug("몬스터 생성: {} (HP: {}, 공격력: {}, 경험치: {}, 골드: {})", this.name, hp, attack, expReward, goldReward);
  }

  /**
   * 확장된 몬스터 생성자 (MonsterData 포함)
   */
  public Monster(String name, int hp, int attack, int expReward, int goldReward, MonsterData sourceData) {
    // 기본 필드 검증 (기존 로직 유지)
    validateBasicFields(name, hp, attack, expReward, goldReward);

    // 기본 필드 설정
    this.name = name.trim();
    this.hp = hp;
    this.maxHp = hp;
    this.attack = attack;
    this.expReward = expReward;
    this.goldReward = goldReward;

    // JSON 데이터가 있으면 확장 필드 설정
    if (sourceData != null) {
      initializeFromMonsterData(sourceData);
    } else {
      initializeDefaultExtendedFields();
    }

    logger.debug("몬스터 생성: {} (HP: {}, 공격력: {}, 확장: {})", this.name, hp, attack, sourceData != null ? "JSON" : "기본");
  }

  // === JSON 기반 팩토리 메서드 ===
  /**
   * MonsterData로부터 Monster 인스턴스를 생성합니다.
   */
  public static Monster fromMonsterData(MonsterData data) {
    Monster monster = new Monster(data.getName(), data.getStats().getHp(), data.getStats().getAttack(), data.getRewards().getExp(),
        data.getRewards().getGold(), data);

    return monster;
  }

  /**
   * 기본 몬스터를 생성합니다 (레거시 지원)
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

    if (hp < BaseConstant.NUMBER_ONE) {
      logger.error("몬스터 HP가 유효하지 않음: {}", hp);
      throw new IllegalArgumentException("몬스터 HP는 " + BaseConstant.NUMBER_ONE + " 이상이어야 합니다.");
    }

    if (attack < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 공격력이 유효하지 않음: {}", attack);
      throw new IllegalArgumentException("몬스터 공격력은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    if (expReward < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 경험치 보상이 유효하지 않음: {}", expReward);
      throw new IllegalArgumentException("경험치 보상은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    if (goldReward < BaseConstant.NUMBER_ZERO) {
      logger.error("몬스터 골드 보상이 유효하지 않음: {}", goldReward);
      throw new IllegalArgumentException("골드 보상은 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }
  }

  private void initializeFromMonsterData(MonsterData data) {
    this.sourceData = data;
    this.id = data.getId();
    this.description = data.getDescription();
    this.defense = data.getStats().getDefense();
    this.speed = data.getStats().getSpeed();
    this.criticalRate = data.getStats().getCriticalRate();
    this.rarity = data.getRarity();
    this.abilities = new ArrayList<>(data.getAbilities());
    this.locations = new ArrayList<>(data.getLocations());
    this.minLevel = data.getMinLevel();
    this.maxLevel = data.getMaxLevel();
    this.spawnRate = data.getSpawnRate();
    this.properties = new HashMap<>(data.getProperties());
  }

  private void initializeDefaultExtendedFields() {
    this.id = name.toUpperCase().replace(" ", "_");
    this.description = name + "입니다.";
    this.defense = 2; // 기본 방어력
    this.speed = 5; // 기본 속도
    this.criticalRate = 0.1; // 기본 크리티컬 확률
    this.rarity = "COMMON";
    this.abilities = new ArrayList<>();
    this.locations = new ArrayList<>();
    this.minLevel = 1;
    this.maxLevel = 99;
    this.spawnRate = 0.5;
    this.properties = new HashMap<>();
  }


  /**
   * 몬스터가 데미지를 받습니다.
   * 
   * @param damage 받을 데미지
   * @throws IllegalArgumentException 음수 데미지인 경우
   */
  public void takeDamage(int damage) {
    if (damage < BaseConstant.NUMBER_ZERO) {
      logger.warn("음수 데미지 시도: {}", damage);
      throw new IllegalArgumentException("데미지는 " + BaseConstant.NUMBER_ZERO + " 이상이어야 합니다.");
    }
    // 방어력 적용 (새로운 기능)
    int actualDamage = Math.max(1, damage - defense); // 최소 1 데미지

    int oldHp = this.hp;
    this.hp -= damage;
    if (this.hp < BaseConstant.NUMBER_ZERO) {
      this.hp = BaseConstant.NUMBER_ZERO;
    }

    logger.debug("{} 데미지 받음: {} -> {} (-{})", name, oldHp, this.hp, actualDamage);

    if (!isAlive()) {
      logger.debug("{} 처치됨", name);
    }
  }

  /**
   * 몬스터의 생존 여부를 확인합니다.
   * 
   * @return 생존 시 true
   */
  public boolean isAlive() {
    return hp > BaseConstant.NUMBER_ZERO;
  }

  /**
   * 몬스터의 상태를 표시합니다.
   */
  public void displayStatus() {
    try {
      System.out.println("=== " + name + " ===");
      System.out.println("체력: " + hp + "/" + maxHp);
      System.out.println("공격력: " + attack);

      // 확장 정보 표시 (JSON 데이터가 있을 때)
      if (sourceData != null) {
        System.out.println("방어력: " + defense);
        System.out.println("속도: " + speed);
        if (!abilities.isEmpty()) {
          System.out.println("특수능력: " + String.join(", ", abilities));
        }
      }

      System.out.println("===============");
    } catch (Exception e) {
      logger.error("몬스터 상태 표시 중 오류", e);
      System.out.println("몬스터 정보 표시 중 오류가 발생했습니다.");
    }
  }

  /**
   * 몬스터의 현재 체력 비율을 반환합니다.
   */
  public double getHealthRatio() {
    if (maxHp <= BaseConstant.NUMBER_ZERO)
      return BaseConstant.NUMBER_ZERO_DOT_ZERO;
    return (double) hp / maxHp;
  }

  /**
   * 몬스터가 중상인지 확인합니다.
   */
  public boolean isCriticallyWounded() {
    return getHealthRatio() <= BaseConstant.MONSTER_CRITICAL && isAlive();
  }

  // === 새로운 확장 메서드들 ===

  /**
   * 특수 능력을 가지고 있는지 확인합니다.
   */
  public boolean hasAbility(String abilityName) {
    return abilities.contains(abilityName);
  }

  /**
   * 특정 지역에 출현하는지 확인합니다.
   */
  public boolean canAppearInLocation(String location) {
    return locations.isEmpty() || locations.contains(location);
  }

  /**
   * 특정 레벨 범위에 출현하는지 확인합니다.
   */
  public boolean canAppearAtLevel(int level) {
    return level >= minLevel && level <= maxLevel;
  }

  /**
   * 몬스터의 추정 레벨을 계산합니다.
   */
  public int getEstimatedLevel() {
    return Math.max(1, (maxHp + attack * 2) / 15);
  }

  /**
   * 크리티컬 공격 여부를 확인합니다.
   */
  public boolean rollCritical() {
    return Math.random() < criticalRate;
  }

  /**
   * 크리티컬 데미지를 계산합니다.
   */
  public int getCriticalDamage() {
    return (int) (attack * BaseConstant.CRITICAL_DAMAGE_MULTIPLIER);
  }

  /**
   * 속성 저항을 확인합니다.
   */
  public boolean isResistantTo(String element) {
    if (properties.containsKey("resistances")) {
      @SuppressWarnings("unchecked")
      List<String> resistances = (List<String>) properties.get("resistances");
      return resistances.contains(element);
    }
    return false;
  }

  /**
   * 속성 약점을 확인합니다.
   */
  public boolean isWeakTo(String element) {
    return properties.containsKey("weakness") && element.equals(properties.get("weakness"));
  }

  /**
   * 보스 몬스터인지 확인합니다.
   */
  public boolean isBoss() {
    return properties.containsKey("boss") && Boolean.TRUE.equals(properties.get("boss"));
  }

  /**
   * 재생 능력이 있는지 확인합니다.
   */
  public boolean hasRegeneration() {
    return properties.containsKey("regeneration");
  }

  /**
   * 재생 회복량을 반환합니다.
   */
  public int getRegenerationAmount() {
    if (hasRegeneration()) {
      return (Integer) properties.getOrDefault("regeneration", 0);
    }
    return 0;
  }

  /**
   * 턴 종료 시 처리 (재생 등)
   */
  public void endTurn() {
    if (hasRegeneration() && isAlive()) {
      int regenAmount = getRegenerationAmount();
      if (regenAmount > 0) {
        int oldHp = hp;
        hp = Math.min(maxHp, hp + regenAmount);

        if (hp > oldHp) {
          System.out.println("🔄 " + name + "이(가) " + (hp - oldHp) + " 체력을 회복했습니다!");
          logger.debug("{} 재생: {} -> {} (+{})", name, oldHp, hp, hp - oldHp);
        }
      }
    }
  }


  // === toString, equals, hashCode (업데이트) ===

  @Override
  public String toString() {
    if (sourceData != null) {
      return String.format("Monster{id='%s', name='%s', hp=%d/%d, attack=%d, defense=%d, rarity='%s'}", id, name, hp, maxHp, attack, defense, rarity);
    } else {
      return String.format("Monster{name='%s', hp=%d/%d, attack=%d, expReward=%d, goldReward=%d}", name, hp, maxHp, attack, expReward, goldReward);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;

    Monster monster = (Monster) obj;

    // ID가 있으면 ID로 비교, 없으면 기존 방식
    if (id != null && monster.id != null) {
      return id.equals(monster.id);
    }

    return maxHp == monster.maxHp && attack == monster.attack && expReward == monster.expReward && goldReward == monster.goldReward
        && name.equals(monster.name);
  }

  @Override
  public int hashCode() {
    if (id != null) {
      return id.hashCode();
    }

    int result = name.hashCode();
    result = BaseConstant.NUMBER_THIRTY_ONE * result + maxHp;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + attack;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + expReward;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + goldReward;
    return result;
  }

  // === Getters (기존 + 새로운) ===

  // 기존 Getters
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

  // 새로운 Getters
  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public int getDefense() {
    return defense;
  }

  public int getSpeed() {
    return speed;
  }

  public double getCriticalRate() {
    return criticalRate;
  }

  public String getRarity() {
    return rarity;
  }

  public List<String> getAbilities() {
    return new ArrayList<>(abilities);
  }

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

  public MonsterData getSourceData() {
    return sourceData;
  }

  // === 확장성을 위한 Setters (필요시) ===

  public void setDefense(int defense) {
    this.defense = defense;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public void setCriticalRate(double criticalRate) {
    this.criticalRate = criticalRate;
  }

  /**
   * JSON에서 로드된 몬스터인지 확인합니다.
   */
  public boolean isJsonBased() {
    return sourceData != null;
  }

  /**
   * 기본 몬스터인지 확인합니다 (레거시).
   */
  public boolean isLegacy() {
    return sourceData == null;
  }
}
