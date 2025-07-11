package model;

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

    int oldHp = this.hp;
    this.hp -= damage;
    if (this.hp < BaseConstant.NUMBER_ZERO) {
      this.hp = BaseConstant.NUMBER_ZERO;
    }

    logger.debug("{} 데미지 받음: {} -> {} (-{})", name, oldHp, this.hp, damage);

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
      System.out.println("===============");
    } catch (Exception e) {
      logger.error("몬스터 상태 표시 중 오류", e);
      System.out.println("몬스터 정보 표시 중 오류가 발생했습니다.");
    }
  }

  /**
   * 몬스터의 현재 체력 비율을 반환합니다.
   * 
   * @return 체력 비율 (0.0 ~ 1.0)
   */
  public double getHealthRatio() {
    if (maxHp <= BaseConstant.NUMBER_ZERO)
      return BaseConstant.NUMBER_ZERO_DOT_ZERO;
    return (double) hp / maxHp;
  }

  /**
   * 몬스터가 중상인지 확인합니다.
   * 
   * @return 체력이 30% 이하일 때 true
   */
  public boolean isCriticallyWounded() {
    return getHealthRatio() <= BaseConstant.MONSTER_CRITICAL && isAlive();
  }

  /**
   * 몬스터 정보를 문자열로 반환합니다.
   */
  @Override
  public String toString() {
    return String.format("Monster{name='%s', hp=%d/%d, attack=%d, expReward=%d, goldReward=%d}", name, hp, maxHp, attack, expReward, goldReward);
  }

  /**
   * 두 몬스터가 같은지 비교합니다.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;

    Monster monster = (Monster) obj;
    return maxHp == monster.maxHp && attack == monster.attack && expReward == monster.expReward && goldReward == monster.goldReward
        && name.equals(monster.name);
  }

  /**
   * 몬스터의 해시코드를 반환합니다.
   */
  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = BaseConstant.NUMBER_THIRTY_ONE * result + maxHp;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + attack;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + expReward;
    result = BaseConstant.NUMBER_THIRTY_ONE * result + goldReward;
    return result;
  }

  // Getters
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
}
