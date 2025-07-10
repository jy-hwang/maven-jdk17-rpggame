package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 게임 캐릭터를 표현하는 클래스
 */
public class Character {
  private static final Logger logger = LoggerFactory.getLogger(Character.class);

  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int exp;
  private int attack;
  private int defense;
  private int gold;

  // 상수 정의
  private static final int INITIAL_LEVEL = 1;
  private static final int INITIAL_MAX_HP = 100;
  private static final int INITIAL_EXP = 0;
  private static final int INITIAL_ATTACK = 10;
  private static final int INITIAL_DEFENSE = 5;
  private static final int INITIAL_GOLD = 50;

  private static final int LEVEL_UP_HP_BONUS = 20;
  private static final int LEVEL_UP_ATTACK_BONUS = 5;
  private static final int LEVEL_UP_DEFENSE_BONUS = 3;

  /**
   * 새 캐릭터 생성자
   * 
   * @param name 캐릭터 이름
   * @throws IllegalArgumentException 이름이 null이거나 비어있는 경우
   */
  public Character(String name) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    this.name = name.trim();
    this.level = INITIAL_LEVEL;
    this.maxHp = INITIAL_MAX_HP;
    this.hp = maxHp;
    this.exp = INITIAL_EXP;
    this.attack = INITIAL_ATTACK;
    this.defense = INITIAL_DEFENSE;
    this.gold = INITIAL_GOLD;

    logger.info("새 캐릭터 생성: {}", this.name);
  }

  /**
   * 저장된 데이터로 캐릭터 생성자
   */
  public Character(String name, int level, int hp, int maxHp, int exp, int attack, int defense,
      int gold) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("저장된 캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    // 값 검증
    validateStats(level, hp, maxHp, exp, attack, defense, gold);

    this.name = name.trim();
    this.level = level;
    this.hp = Math.max(0, hp); // HP는 0 이상
    this.maxHp = Math.max(1, maxHp); // 최대 HP는 1 이상
    this.exp = Math.max(0, exp);
    this.attack = Math.max(1, attack);
    this.defense = Math.max(0, defense);
    this.gold = Math.max(0, gold);

    logger.info("저장된 캐릭터 로드: {} (레벨: {})", this.name, this.level);
  }

  /**
   * 스탯 값들의 유효성을 검증합니다.
   */
  private void validateStats(int level, int hp, int maxHp, int exp, int attack, int defense,
      int gold) {
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
   * 
   * @param expGained 획득할 경험치
   * @return 레벨업 발생 시 true
   * @throws IllegalArgumentException 음수 경험치인 경우
   */
  public boolean gainExp(int expGained) {
    if (expGained < 0) {
      logger.warn("음수 경험치 획득 시도: {}", expGained);
      throw new IllegalArgumentException("경험치는 0 이상이어야 합니다.");
    }

    int oldLevel = this.level;
    this.exp += expGained;
    logger.debug("{} 경험치 획득: {} (현재: {})", name, expGained, this.exp);

    if (this.exp >= level * 100) {
      levelUp();
      logger.info("{} 레벨업: {} -> {}", name, oldLevel, this.level);
      return true;
    }
    return false;
  }

  /**
   * 레벨업을 처리합니다.
   */
  private void levelUp() {
    try {
      level++;
      exp = 0;
      maxHp += LEVEL_UP_HP_BONUS;
      attack += LEVEL_UP_ATTACK_BONUS;
      defense += LEVEL_UP_DEFENSE_BONUS;
      hp = maxHp; // 레벨업 시 체력 완전 회복

      System.out.println("레벨업! 새로운 레벨: " + level);
      System.out.println("체력이 완전 회복되었습니다!");

      logger.info("{} 레벨업 완료 - 레벨: {}, 최대HP: {}, 공격력: {}, 방어력: {}", name, level, maxHp, attack,
          defense);
    } catch (Exception e) {
      logger.error("레벨업 처리 중 오류 발생", e);
      throw new RuntimeException("레벨업 처리 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 체력을 회복합니다.
   * 
   * @param amount 회복할 체력
   * @throws IllegalArgumentException 음수 회복량인 경우
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
   * 데미지를 받습니다.
   * 
   * @param damage 받을 데미지
   * @throws IllegalArgumentException 음수 데미지인 경우
   */
  public void takeDamage(int damage) {
    if (damage < 0) {
      logger.warn("음수 데미지 시도: {}", damage);
      throw new IllegalArgumentException("데미지는 0 이상이어야 합니다.");
    }

    int actualDamage = Math.max(damage - defense, 1);
    int oldHp = this.hp;
    this.hp -= actualDamage;
    if (this.hp < 0)
      this.hp = 0;

    logger.debug("{} 데미지 받음: {} -> {} (-{})", name, oldHp, this.hp, actualDamage);

    if (!isAlive()) {
      logger.info("{} 사망", name);
    }
  }

  /**
   * 캐릭터의 생존 여부를 확인합니다.
   * 
   * @return 생존 시 true
   */
  public boolean isAlive() {
    return hp > 0;
  }

  /**
   * CSV 형식으로 캐릭터 데이터를 변환합니다.
   * 
   * @return CSV 형식 문자열
   */
  public String toCsv() {
    String csvData = String.format("%s,%d,%d,%d,%d,%d,%d,%d", name, level, hp, maxHp, exp, attack,
        defense, gold);
    logger.debug("캐릭터 CSV 변환: {}", csvData);
    return csvData;
  }

  /**
   * CSV 데이터로부터 캐릭터를 생성합니다.
   * 
   * @param csvLine CSV 형식 문자열
   * @return Character 객체
   * @throws IllegalArgumentException CSV 형식이 잘못된 경우
   */
  public static Character fromCsv(String csvLine) {
    try {
      if (csvLine == null || csvLine.trim().isEmpty()) {
        logger.error("빈 CSV 라인");
        throw new IllegalArgumentException("CSV 데이터가 비어있습니다.");
      }

      String[] parts = csvLine.split(",");
      if (parts.length != 8) {
        logger.error("잘못된 CSV 형식: 예상 8개, 실제 {}개", parts.length);
        throw new IllegalArgumentException("CSV 형식이 올바르지 않습니다. 8개의 필드가 필요합니다.");
      }

      return new Character(parts[0].trim(), Integer.parseInt(parts[1].trim()),
          Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()),
          Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()),
          Integer.parseInt(parts[6].trim()), Integer.parseInt(parts[7].trim()));

    } catch (NumberFormatException e) {
      logger.error("CSV 숫자 변환 오류: {}", csvLine, e);
      throw new IllegalArgumentException("CSV 데이터의 숫자 형식이 올바르지 않습니다.", e);
    } catch (Exception e) {
      logger.error("CSV 파싱 오류: {}", csvLine, e);
      throw new IllegalArgumentException("CSV 데이터 파싱 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 캐릭터 상태를 표시합니다.
   */
  public void displayStats() {
    try {
      System.out.println("========== 캐릭터 정보 ==========");
      System.out.println("이름: " + name);
      System.out.println("레벨: " + level);
      System.out.println("체력: " + hp + "/" + maxHp);
      System.out.println("경험치: " + exp + "/" + (level * 100));
      System.out.println("공격력: " + attack);
      System.out.println("방어력: " + defense);
      System.out.println("골드: " + gold);
      System.out.println("=========================");

      logger.debug("{} 상태 표시 완료", name);
    } catch (Exception e) {
      logger.error("캐릭터 상태 표시 중 오류", e);
      System.out.println("캐릭터 정보 표시 중 오류가 발생했습니다.");
    }
  }

  // Getters and Setters with validation
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

  public int getExp() {
    return exp;
  }

  public int getAttack() {
    return attack;
  }

  public int getDefense() {
    return defense;
  }

  public int getGold() {
    return gold;
  }

  /**
   * 골드를 설정합니다.
   * 
   * @param gold 설정할 골드 (0 이상)
   * @throws IllegalArgumentException 음수 골드인 경우
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
}
