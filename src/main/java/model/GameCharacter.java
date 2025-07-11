package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.effect.GameStatusCondition;

/**
 * 향상된 게임 캐릭터 클래스 (인벤토리, 스킬, 마나 시스템 포함)
 */
public class GameCharacter {
  private static final Logger logger = LoggerFactory.getLogger(Character.class);

  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int mana;
  private int maxMana;
  private int exp;
  private int baseAttack;
  private int baseDefense;
  private int gold;
  private GameInventory inventory;
  private SkillManager skillManager;
  private GameStatusCondition playerStatusCondition;

  // 상수 정의
  private static final int INITIAL_LEVEL = 1;
  private static final int INITIAL_MAX_HP = 100;
  private static final int INITIAL_MAX_MANA = 50;
  private static final int INITIAL_EXP = 0;
  private static final int INITIAL_ATTACK = 10;
  private static final int INITIAL_DEFENSE = 5;
  private static final int INITIAL_GOLD = 100;

  private static final int LEVEL_UP_HP_BONUS = 20;
  private static final int LEVEL_UP_MANA_BONUS = 15;
  private static final int LEVEL_UP_ATTACK_BONUS = 5;
  private static final int LEVEL_UP_DEFENSE_BONUS = 3;

  /**
   * 새 캐릭터 생성자
   */
  public GameCharacter(String name) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    this.name = name.trim();
    this.level = INITIAL_LEVEL;
    this.maxHp = INITIAL_MAX_HP;
    this.hp = maxHp;
    this.maxMana = INITIAL_MAX_MANA;
    this.mana = maxMana;
    this.exp = INITIAL_EXP;
    this.baseAttack = INITIAL_ATTACK;
    this.baseDefense = INITIAL_DEFENSE;
    this.gold = INITIAL_GOLD;
    this.inventory = new GameInventory(20);
    this.skillManager = new SkillManager();
    this.playerStatusCondition = GameStatusCondition.NORMAL;

    logger.info("새 캐릭터 생성: {}", this.name);
  }

  /**
   * 저장된 데이터로 캐릭터 생성자
   */
  @JsonCreator
  public GameCharacter(
  //@formatter:off
      @JsonProperty("name") String name,
      @JsonProperty("level") int level,
      @JsonProperty("hp") int hp,
      @JsonProperty("maxHp") int maxHp,
      @JsonProperty("mana") int mana,
      @JsonProperty("maxMana") int maxMana,
      @JsonProperty("exp") int exp,
      @JsonProperty("baseAttack") int baseAttack,
      @JsonProperty("baseDefense") int baseDefense,
      @JsonProperty("gold") int gold,
      @JsonProperty("inventory") GameInventory inventory,
      @JsonProperty("skillManager") SkillManager skillManager,
      @JsonProperty("playerStatusCondition") GameStatusCondition playerStatusCondition
      //@formatter:on
  ) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("저장된 캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    validateStats(level, hp, maxHp, exp, baseAttack, baseDefense, gold);

    this.name = name.trim();
    this.level = level;
    this.hp = Math.max(0, hp);
    this.maxHp = Math.max(1, maxHp);
    this.mana = Math.max(0, mana);
    this.maxMana = Math.max(0, maxMana);
    this.exp = Math.max(0, exp);
    this.baseAttack = Math.max(1, baseAttack);
    this.baseDefense = Math.max(0, baseDefense);
    this.gold = Math.max(0, gold);
    this.inventory = inventory != null ? inventory : new GameInventory(20);
    this.skillManager = skillManager != null ? skillManager : new SkillManager();
    this.playerStatusCondition = playerStatusCondition != null ? playerStatusCondition : GameStatusCondition.NORMAL;
    logger.info("저장된 캐릭터 로드: {} (레벨: {})", this.name, this.level);
  }

  /**
   * 스탯 값들의 유효성을 검증합니다.
   */
  private void validateStats(int level, int hp, int maxHp, int exp, int attack, int defense, int gold) {
    if (level < 1 || level > 1000) {
      logger.warn("비정상적인 레벨 값: {}", level);
      throw new IllegalArgumentException("레벨은 1~1000 사이여야 합니다.");
    }
    if (maxHp < 1) {
      logger.warn("비정상적인 최대 HP 값: {}", maxHp);
      throw new IllegalArgumentException("최대 HP는 1 이상이어야 합니다.");
    }
    if (attack < 1) {
      logger.warn("비정상적인 공격력 값: {}", attack);
      throw new IllegalArgumentException("공격력은 1 이상이어야 합니다.");
    }
  }

  /**
   * 경험치를 획득하고 레벨업 여부를 반환합니다.
   */
  public boolean gainExp(int expGained) {
    if (expGained < 0) {
      logger.warn("음수 경험치 획득 시도: {}", expGained);
      throw new IllegalArgumentException("경험치는 0 이상이어야 합니다.");
    }

    int oldLevel = this.level;
    this.exp += expGained;
    logger.debug("{} 경험치 획득: {} (현재: {})", name, expGained, this.exp);

    boolean leveledUp = false;
    while (this.exp >= getExpRequiredForNextLevel()) {
      levelUp();
      leveledUp = true;
    }

    if (leveledUp) {
      logger.info("{} 레벨업: {} -> {}", name, oldLevel, this.level);
    }

    return leveledUp;
  }

  /**
   * 다음 레벨까지 필요한 경험치를 반환합니다.
   */
  public int getExpRequiredForNextLevel() {
    return level * 100;
  }

  /**
   * 레벨업을 처리합니다.
   */
  private void levelUp() {
    try {
      this.exp -= getExpRequiredForNextLevel();
      level++;

      // 스탯 증가
      maxHp += LEVEL_UP_HP_BONUS;
      maxMana += LEVEL_UP_MANA_BONUS;
      baseAttack += LEVEL_UP_ATTACK_BONUS;
      baseDefense += LEVEL_UP_DEFENSE_BONUS;

      // 체력과 마나 완전 회복
      hp = maxHp;
      mana = maxMana;

      System.out.println("🎉 레벨업! 새로운 레벨: " + level);
      System.out.println("💚 체력과 마나가 완전 회복되었습니다!");

      // 새로운 스킬 학습 확인
      var newSkills = skillManager.checkAndLearnNewSkills(level);
      if (!newSkills.isEmpty()) {
        System.out.println("✨ 새로운 스킬을 학습했습니다!");
        for (Skill skill : newSkills) {
          System.out.println("- " + skill.getName());
        }
      }

      logger.info("{} 레벨업 완료 - 레벨: {}, 최대HP: {}, 최대마나: {}, 공격력: {}, 방어력: {}", name, level, maxHp, maxMana, baseAttack, baseDefense);

    } catch (Exception e) {
      logger.error("레벨업 처리 중 오류 발생", e);
      throw new RuntimeException("레벨업 처리 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 체력을 회복합니다.
   */
  public void heal(int amount) {
    if (amount < 0) {
      logger.warn("음수 체력 회복 시도: {}", amount);
      throw new IllegalArgumentException("회복량은 0 이상이어야 합니다.");
    }

    int oldHp = this.hp;
    this.hp = Math.min(hp + amount, maxHp);

    logger.debug("{} 체력 회복: {} -> {} (+{})", name, oldHp, this.hp, amount);
  }

  /**
   * 마나를 회복합니다.
   */
  public void restoreMana(int amount) {
    if (amount < 0) {
      logger.warn("음수 마나 회복 시도: {}", amount);
      throw new IllegalArgumentException("마나 회복량은 0 이상이어야 합니다.");
    }

    int oldMana = this.mana;
    this.mana = Math.min(mana + amount, maxMana);

    logger.debug("{} 마나 회복: {} -> {} (+{})", name, oldMana, this.mana, amount);
  }

  /**
   * 마나를 소모합니다.
   */
  public boolean useMana(int amount) {
    if (amount < 0) {
      logger.warn("음수 마나 소모 시도: {}", amount);
      return false;
    }

    if (mana >= amount) {
      mana -= amount;
      logger.debug("{} 마나 소모: {} (-{})", name, mana, amount);
      return true;
    }

    return false;
  }

  /**
   * 데미지를 받습니다.
   */
  public void takeDamage(int damage) {
    if (damage < 0) {
      logger.warn("음수 데미지 시도: {}", damage);
      throw new IllegalArgumentException("데미지는 0 이상이어야 합니다.");
    }

    int totalDefense = getTotalDefense();
    int actualDamage = Math.max(damage - totalDefense, 1);
    int oldHp = this.hp;
    this.hp -= actualDamage;
    if (this.hp < 0)
      this.hp = 0;

    logger.debug("{} 데미지 받음: {} -> {} (-{}, 방어력: {})", name, oldHp, this.hp, actualDamage, totalDefense);

    if (!isAlive()) {
      logger.info("{} 사망", name);
    }
  }

  /**
   * 캐릭터의 생존 여부를 확인합니다.
   */
  public boolean isAlive() {
    return hp > 0;
  }

  /**
   * 총 공격력을 반환합니다 (기본 공격력 + 장비 보너스).
   */
  public int getAttack() {
    GameInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return baseAttack + bonus.getAttackBonus();
  }

  /**
   * 총 방어력을 반환합니다 (기본 방어력 + 장비 보너스).
   */
  public int getTotalDefense() {
    GameInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return baseDefense + bonus.getDefenseBonus();
  }

  /**
   * 총 최대 체력을 반환합니다 (기본 최대 체력 + 장비 보너스).
   */
  public int getTotalMaxHp() {
    GameInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return maxHp + bonus.getHpBonus();
  }

  /**
   * 캐릭터 상태를 표시합니다.
   */
  public void displayStats() {
    try {
      GameInventory.EquipmentBonus bonus = inventory.getTotalBonus();

      System.out.println("========== 캐릭터 정보 ==========");
      System.out.println("이름: " + name);
      System.out.println("레벨: " + level);
      System.out.printf("체력: %d/%d", hp, getTotalMaxHp());
      if (bonus.getHpBonus() > 0) {
        System.out.printf(" (%d+%d)", maxHp, bonus.getHpBonus());
      }
      System.out.println();

      System.out.printf("마나: %d/%d%n", mana, maxMana);
      System.out.printf("경험치: %d/%d%n", exp, getExpRequiredForNextLevel());

      System.out.printf("공격력: %d", getAttack());
      if (bonus.getAttackBonus() > 0) {
        System.out.printf(" (%d+%d)", baseAttack, bonus.getAttackBonus());
      }
      System.out.println();

      System.out.printf("방어력: %d", getTotalDefense());
      if (bonus.getDefenseBonus() > 0) {
        System.out.printf(" (%d+%d)", baseDefense, bonus.getDefenseBonus());
      }
      System.out.println();
      System.out.println("상태: " + playerStatusCondition);
      System.out.println("골드: " + gold);
      System.out.println("=========================");

      logger.debug("{} 상태 표시 완료", name);
    } catch (Exception e) {
      logger.error("캐릭터 상태 표시 중 오류", e);
      System.out.println("캐릭터 정보 표시 중 오류가 발생했습니다.");
    }
  }

  /**
   * 턴 종료 처리 (스킬 쿨다운 감소, 마나 자연 회복 등)
   */
  public void endTurn() {
    // 스킬 쿨다운 감소
    skillManager.reduceCooldowns();

    // 마나 자연 회복 (최대 마나의 10%)
    int manaRegenAmount = Math.max(1, maxMana / 10);
    restoreMana(manaRegenAmount);

    logger.debug("{} 턴 종료 처리 완료", name);
  }

  // Getters and Setters
  public String getName() {
    return name;
  }

  public int getLevel() {
    return level;
  }

  public int getHp() {
    return hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public int getMana() {
    return mana;
  }

  public int getMaxMana() {
    return maxMana;
  }

  public int getExp() {
    return exp;
  }

  public int getBaseAttack() {
    return baseAttack;
  }

  public int getBaseDefense() {
    return baseDefense;
  }

  public int getGold() {
    return gold;
  }

  public GameInventory getInventory() {
    return inventory;
  }

  public SkillManager getSkillManager() {
    return skillManager;
  }

  /**
   * 골드를 설정합니다.
   */
  public void setGold(int gold) {
    if (gold < 0) {
      logger.warn("음수 골드 설정 시도: {}", gold);
      throw new IllegalArgumentException("골드는 0 이상이어야 합니다.");
    }

    int oldGold = this.gold;
    this.gold = gold;
    logger.debug("{} 골드 변경: {} -> {}", name, oldGold, gold);
  }

  /**
   * CSV 형식으로 캐릭터 데이터를 변환합니다.
   * 
   * @return CSV 형식 문자열
   */
  public String toCsv() {
    String csvData = String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d", name, level, hp, maxHp, mana, maxMana, exp, baseAttack, baseDefense, gold);
    logger.debug("캐릭터 CSV 변환: {}", csvData);
    return csvData;
  }

  /**
   * CSV 데이터로부터 캐릭터를 생성합니다.
   * 
   * @param csvLine CSV 형식 문자열
   * @return GameCharacter 객체
   * @throws IllegalArgumentException CSV 형식이 잘못된 경우
   */
  public static GameCharacter fromCsv(String csvLine) {
    try {
      if (csvLine == null || csvLine.trim().isEmpty()) {
        logger.error("빈 CSV 라인");
        throw new IllegalArgumentException("CSV 데이터가 비어있습니다.");
      }

      String[] parts = csvLine.split(",");
      if (parts.length != 10) {
        logger.error("잘못된 CSV 형식: 예상 10개, 실제 {}개", parts.length);
        throw new IllegalArgumentException("CSV 형식이 올바르지 않습니다. 10개의 필드가 필요합니다.");
      }

      // 기본 스탯만으로 캐릭터 생성
      GameCharacter character = new GameCharacter(parts[0].trim(), // name
          Integer.parseInt(parts[1].trim()), // level
          Integer.parseInt(parts[2].trim()), // hp
          Integer.parseInt(parts[3].trim()), // maxHp
          Integer.parseInt(parts[4].trim()), // mana
          Integer.parseInt(parts[5].trim()), // maxMana
          Integer.parseInt(parts[6].trim()), // exp
          Integer.parseInt(parts[7].trim()), // baseAttack
          Integer.parseInt(parts[8].trim()), // baseDefense
          Integer.parseInt(parts[9].trim()), // gold
          new GameInventory(20), // 새 인벤토리 생성
          new SkillManager(), // 새 스킬 매니저 생성
          GameStatusCondition.NORMAL);

      // 레벨에 맞는 스킬들을 다시 학습시킴
      character.getSkillManager().checkAndLearnNewSkills(character.getLevel());

      logger.debug("CSV에서 캐릭터 로드: {}", character.getName());
      return character;

    } catch (NumberFormatException e) {
      logger.error("CSV 숫자 변환 오류: {}", csvLine, e);
      throw new IllegalArgumentException("CSV 데이터의 숫자 형식이 올바르지 않습니다.", e);
    } catch (Exception e) {
      logger.error("CSV 파싱 오류: {}", csvLine, e);
      throw new IllegalArgumentException("CSV 데이터 파싱 중 오류가 발생했습니다.", e);
    }
  }


}
