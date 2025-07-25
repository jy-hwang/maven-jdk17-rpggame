package rpg.domain.player;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.application.service.QuestManager;
import rpg.application.service.SkillService;
import rpg.domain.inventory.PlayerInventory;
import rpg.shared.constant.GameConstants;

/**
 * 향상된 게임 캐릭터 클래스 (인벤토리, 스킬, 마나 시스템 포함)
 */
public class Player {
  private static final Logger logger = LoggerFactory.getLogger(Character.class);

  private String name;
  private int level;
  private int hp;
  private int maxHp;
  private int mp;
  private int maxMp;
  private int exp;
  private int baseAttack;
  private int baseDefense;
  private int gold;
  private double restoreHp;
  private double restoreMana;
  private PlayerInventory inventory;
  private SkillService skillManager;
  private PlayerStatusCondition playerStatusCondition;
  private QuestManager questManager;

  /**
   * 새 캐릭터 생성자
   */
  public Player(String name) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    this.name = name.trim();
    this.level = GameConstants.INITIAL_LEVEL;
    this.maxHp = GameConstants.INITIAL_MAX_HP;
    this.hp = maxHp;
    this.maxMp = GameConstants.INITIAL_MAX_MP;
    this.mp = maxMp;
    this.exp = GameConstants.INITIAL_EXP;
    this.baseAttack = GameConstants.INITIAL_ATTACK;
    this.baseDefense = GameConstants.INITIAL_DEFENSE;
    this.gold = GameConstants.INITIAL_GOLD;
    this.inventory = new PlayerInventory(GameConstants.DEFAULT_INVENTORY);
    this.restoreHp = GameConstants.RESTORE_HP;
    this.restoreMana = GameConstants.RESTORE_MANA;
    this.skillManager = new SkillService();
    this.playerStatusCondition = PlayerStatusCondition.NORMAL;
    this.questManager = new QuestManager();

    logger.info("새 캐릭터 생성: {}", this.name);
  }

  /**
   * 저장된 데이터로 캐릭터 생성자
   */
  @JsonCreator
  public Player(
//@formatter:off
  @JsonProperty("name") String name
, @JsonProperty("level") int level
, @JsonProperty("hp") int hp
, @JsonProperty("maxHp") int maxHp
, @JsonProperty("mp") int mp
, @JsonProperty("maxMp") int maxMp
, @JsonProperty("restoreHp") double restoreHp
, @JsonProperty("restoreMana") double restoreMana
, @JsonProperty("exp") int exp
, @JsonProperty("baseAttack") int baseAttack
, @JsonProperty("baseDefense") int baseDefense
, @JsonProperty("gold") int gold
, @JsonProperty("inventory") PlayerInventory inventory
, @JsonProperty("skillManager") SkillService skillManager
, @JsonProperty("playerStatusCondition") PlayerStatusCondition playerStatusCondition
, @JsonProperty("questManager") QuestManager questManager
//@formatter:on
  ) {
    if (name == null || name.trim().isEmpty()) {
      logger.error("저장된 캐릭터 이름이 유효하지 않음: {}", name);
      throw new IllegalArgumentException("캐릭터 이름은 비어있을 수 없습니다.");
    }

    validateStats(level, hp, maxHp, exp, baseAttack, baseDefense, gold);

    this.name = name.trim();
    this.level = level;
    this.hp = Math.max(GameConstants.NUMBER_ZERO, hp);
    this.maxHp = Math.max(GameConstants.NUMBER_ONE, maxHp);
    this.mp = Math.max(GameConstants.NUMBER_ZERO, mp);
    this.maxMp = Math.max(GameConstants.NUMBER_ZERO, maxMp);
    this.restoreHp = Math.max(GameConstants.NUMBER_ONE, restoreHp);
    this.restoreMana = Math.max(GameConstants.NUMBER_ONE, restoreMana);
    this.exp = Math.max(GameConstants.NUMBER_ZERO, exp);
    this.baseAttack = Math.max(GameConstants.NUMBER_ONE, baseAttack);
    this.baseDefense = Math.max(GameConstants.NUMBER_ZERO, baseDefense);
    this.gold = Math.max(GameConstants.NUMBER_ZERO, gold);
    this.inventory = inventory != null ? inventory : new PlayerInventory(GameConstants.DEFAULT_INVENTORY);
    this.skillManager = skillManager != null ? skillManager : new SkillService();
    this.playerStatusCondition = playerStatusCondition != null ? playerStatusCondition : PlayerStatusCondition.NORMAL;
    this.questManager = questManager != null ? questManager : new QuestManager();

    logger.info("저장된 캐릭터 로드: {} (레벨: {})", this.name, this.level);
  }

  /**
   * 스탯 값들의 유효성을 검증합니다.
   */
  private void validateStats(int level, int hp, int maxHp, int exp, int attack, int defense, int gold) {
    if (level < GameConstants.NUMBER_ONE || level > GameConstants.NUMBER_THOUSAND) {
      logger.warn("비정상적인 레벨 값: {}", level);
      throw new IllegalArgumentException("레벨은 " + GameConstants.NUMBER_ONE + "~" + GameConstants.NUMBER_THOUSAND + "사이여야 합니다.");
    }
    if (maxHp < GameConstants.NUMBER_ONE) {
      logger.warn("비정상적인 최대 HP 값: {}", maxHp);
      throw new IllegalArgumentException("최대 HP는 " + GameConstants.NUMBER_ONE + " 이상이어야 합니다.");
    }
    if (attack < GameConstants.NUMBER_ONE) {
      logger.warn("비정상적인 공격력 값: {}", attack);
      throw new IllegalArgumentException("공격력은 " + GameConstants.NUMBER_ONE + " 이상이어야 합니다.");
    }
  }


  /**
   * 경험치 획득 메서드 (기존 gainExp가 있다면 이것을 수정)
   */
  public boolean gainExp(int expGained) {
    return gainExperience(expGained); // 단순 위임
  }

  /**
   * 경험치 획득 및 레벨업 처리 (메인 메서드)
   */
  public boolean gainExperience(int expGained) {
    if (expGained <= 0) {
      logger.warn("잘못된 경험치 획득 시도: {}", expGained);
      return false;
    }

    int oldLevel = this.level;
    this.exp += expGained;
    logger.debug("{} 경험치 획득: {} (현재: {})", name, expGained, this.exp);

    boolean leveledUp = false;
    while (this.exp >= getExpRequiredForNextLevel()) {
      levelUp();
      leveledUp = true;
    }

    // 레벨업 시 퀘스트 진행도 업데이트
    if (leveledUp && questManager != null) {
      logger.debug("레벨업 감지: {} -> {} - 퀘스트 진행도 업데이트", oldLevel, this.level);
      questManager.updateLevelProgress(this);
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
    return level * GameConstants.NUMBER_FIFTY;
  }

  /**
   * 레벨업을 처리합니다.
   */
  private void levelUp() {
    try {
      this.exp -= getExpRequiredForNextLevel();
      level++;

      // 스탯 증가
      maxHp += GameConstants.LEVEL_UP_HP_BONUS;
      maxMp += GameConstants.LEVEL_UP_MANA_BONUS;
      baseAttack += GameConstants.LEVEL_UP_ATTACK_BONUS;
      baseDefense += GameConstants.LEVEL_UP_DEFENSE_BONUS;

      // 자체 회복량 증가
      restoreHp += GameConstants.LEVEL_UP_RESTORE_HP;
      restoreMana += GameConstants.LEVEL_UP_RESTORE_MP;

      // 체력과 마나 완전 회복
      hp = getTotalMaxHp();
      mp = getTotalMaxMp();

      System.out.println("🎉 레벨업! 새로운 레벨: " + level);
      System.out.println("💚 체력과 마나가 완전 회복되었습니다!");

      // 새로운 스킬 학습 확인
      List<String> newSkills = skillManager.checkAndLearnNewSkills(level);
      if (!newSkills.isEmpty()) {
        System.out.println("✨ 새로운 스킬을 학습했습니다!");
        for (String skill : newSkills) {
          System.out.println("- " + skill);
        }
      }

      // 🆕 레벨업 시 새로운 일일 퀘스트 확인
      if (level % 5 == 0) { // 5레벨마다
        System.out.println("🎉 레벨업으로 새로운 일일 퀘스트가 해금되었을 수 있습니다!");
      }

      logger.info("{} 레벨업 완료 - 레벨: {}, 최대HP: {}, 최대MP: {}, 공격력: {}, 방어력: {}", name, level, maxHp, maxMp, baseAttack, baseDefense);

    } catch (Exception e) {
      logger.error("레벨업 처리 중 오류 발생", e);
      throw new RuntimeException("레벨업 처리 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 체력을 회복합니다.
   */
  public void heal(int amount) {
    if (amount < GameConstants.NUMBER_ZERO) {
      logger.warn("음수 체력 회복 시도: {}", amount);
      throw new IllegalArgumentException("회복량은 " + GameConstants.NUMBER_ZERO + " 이상이어야 합니다.");
    }
    int oldHp = this.hp;
    this.hp = Math.min(hp + amount, getTotalMaxHp());

    logger.debug("{} 체력 회복: {} -> {} (+{}), 최대HP: {}", name, oldHp, this.hp, amount, getTotalMaxHp());
  }

  /**
   * 마나를 회복합니다.
   */
  public void restoreMp(int amount) {
    if (amount < GameConstants.NUMBER_ZERO) {
      logger.warn("음수 마나 회복 시도: {}", amount);
      throw new IllegalArgumentException("마나 회복량은 " + GameConstants.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    int oldMp = this.mp;
    this.mp = Math.min(oldMp + amount, maxMp);

    logger.debug("{} 마나 회복: {} -> {} (+{}), 최대MP: {}", name, oldMp, this.mp, amount, getTotalMaxMp());
  }

  /**
   * 완전 회복 메서드도 추가 (필요시)
   */
  public void fullHeal() {
    int oldHp = this.hp;
    int oldMp = this.mp;
    this.hp = getTotalMaxHp(); // 장비 보너스 포함한 최대 체력으로 회복
    this.mp = getTotalMaxMp();

    logger.info("{} 완전 회복: HP {} -> {}, MP {} -> {}", name, oldHp, this.hp, oldMp, maxMp);
    System.out.println("💚 체력과 마나가 완전 회복되었습니다!");
  }

  /**
   * 마나를 소모합니다.
   */
  public boolean useMp(int amount) {
    if (amount < GameConstants.NUMBER_ZERO) {
      logger.warn("음수 MP 소모 시도: {}", amount);
      return false;
    }

    if (mp >= amount) {
      mp -= amount;
      logger.debug("{} MP 소모: {} (-{})", name, mp, amount);
      return true;
    }

    return false;
  }

  /**
   * 데미지를 받습니다.
   */
  public int takeDamage(int damage) {
    if (damage < GameConstants.NUMBER_ZERO) {
      logger.warn("음수 데미지 시도: {}", damage);
      throw new IllegalArgumentException("데미지는 " + GameConstants.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    int totalDefense = getTotalDefense();
    int actualDamage = Math.max(damage - totalDefense, 1);
    int oldHp = this.hp;
    this.hp -= actualDamage;
    if (this.hp < GameConstants.NUMBER_ZERO)
      this.hp = GameConstants.NUMBER_ZERO;

    logger.debug("{} 데미지 받음: {} -> {} (-{}, 방어력: {})", name, oldHp, this.hp, actualDamage, totalDefense);

    if (!isAlive()) {
      logger.info("{} 사망", name);
    }

    return actualDamage;
  }

  /**
   * 캐릭터의 생존 여부를 확인합니다.
   */
  public boolean isAlive() {
    return hp > GameConstants.NUMBER_ZERO;
  }

  /**
   * 총 공격력을 반환합니다 (기본 공격력 + 장비 보너스).
   */
  public int getAttack() {
    PlayerInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return baseAttack + bonus.getAttackBonus();
  }

  /**
   * 총 방어력을 반환합니다 (기본 방어력 + 장비 보너스).
   */
  public int getTotalDefense() {
    PlayerInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return baseDefense + bonus.getDefenseBonus();
  }

  /**
   * 총 최대 HP을 반환합니다 (기본 최대 HP + 장비 보너스).
   */
  public int getTotalMaxHp() {
    PlayerInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return maxHp + bonus.getHpBonus();
  }
  
  /**
   * 총 최대 MP을 반환합니다 (기본 최대 MP + 장비 보너스).
   */
  public int getTotalMaxMp() {
    PlayerInventory.EquipmentBonus bonus = inventory.getTotalBonus();
    return maxMp + bonus.getMpBonus();
  }

  /**
   * 캐릭터 상태를 표시합니다.
   */
  public void displayStats() {
    try {
      PlayerInventory.EquipmentBonus bonus = inventory.getTotalBonus();

      System.out.println("========== 캐릭터 정보 ==========");
      System.out.println("이름: " + name);
      System.out.println("레벨: " + level);
      System.out.printf("HP: %d/%d", hp, getTotalMaxHp());
      if (bonus.getHpBonus() > GameConstants.NUMBER_ZERO) {
        System.out.printf(" (%d+%d)", maxHp, bonus.getHpBonus());
      }
      System.out.println();

      System.out.printf("MP: %d/%d%n", mp, maxMp);
      if (bonus.getMpBonus() > GameConstants.NUMBER_ZERO) {
        System.out.printf(" (%d+%d)", maxMp, bonus.getMpBonus());
      }
      System.out.printf("체력회복량: %.1f, 마나회복량: %.1f%n", restoreHp, restoreMana);
      System.out.printf("경험치: %d/%d%n", exp, getExpRequiredForNextLevel());

      System.out.printf("공격력: %d", getAttack());
      if (bonus.getAttackBonus() > GameConstants.NUMBER_ZERO) {
        System.out.printf(" (%d+%d)", baseAttack, bonus.getAttackBonus());
      }
      System.out.println();

      System.out.printf("방어력: %d", getTotalDefense());
      if (bonus.getDefenseBonus() > GameConstants.NUMBER_ZERO) {
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
   * 턴 종료 처리 (스킬 쿨다운 감소 등)
   */
  public void endTurn() {
    // 스킬 쿨다운 감소
    skillManager.reduceCooldowns();

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

  public int setHp(int hp) {
    return this.hp = hp;
  }

  public int getMaxHp() {
    return maxHp;
  }

  public int getMp() {
    return mp;
  }

  public int getMaxMp() {
    return maxMp;
  }

  public double getRestoreHp() {
    return restoreHp;
  }

  public void setRestoreHp(double restoreHp) {
    this.restoreHp = restoreHp;
  }

  public double getRestoreMana() {
    return restoreMana;
  }

  public void setRestoreMana(double restoreMana) {
    this.restoreMana = restoreMana;
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

  public int addGold(int gold) {
    return this.getGold() + gold;
  }

  public PlayerInventory getInventory() {
    return inventory;
  }

  public void setInventory(PlayerInventory inventory) {
    this.inventory = inventory;
  }

  public SkillService getSkillManager() {
    return skillManager;
  }

  public void setSkillManager(SkillService skillManager) {
    this.skillManager = skillManager;
  }

  public PlayerStatusCondition getPlayerStatusCondition() {
    return playerStatusCondition;
  }

  public void setPlayerStatusCondition(PlayerStatusCondition playerStatusCondition) {
    this.playerStatusCondition = playerStatusCondition;
  }

  public QuestManager getQuestManager() {
    return questManager;
  }

  public void setQuestManager(QuestManager questManager) {
    this.questManager = questManager != null ? questManager : new QuestManager();
  }

  /**
   * 골드를 설정합니다.
   */
  public void setGold(int gold) {
    if (gold < GameConstants.NUMBER_ZERO) {
      logger.warn("음수 골드 설정 시도: {}", gold);
      throw new IllegalArgumentException("골드는 " + GameConstants.NUMBER_ZERO + " 이상이어야 합니다.");
    }

    int oldGold = this.gold;
    this.gold = gold;
    logger.debug("{} 골드 변경: {} -> {}", name, oldGold, gold);
  }

  // GameCharacter.java
  public void postBattleRegeneration() {
    // 전투 종료 후 자연 회복
    int hpRegenAmount = (int) Math.max(GameConstants.NUMBER_ONE, Math.round(maxHp * this.restoreHp / 100.0));
    heal(hpRegenAmount);

    int mpRegenAmount = (int) Math.max(GameConstants.NUMBER_ONE, Math.round(maxMp * this.restoreMana / 100.0));
    restoreMp(mpRegenAmount);

    logger.debug("{} 전투 후 체력 회복량 : {}, 마나 회복량 : {}", name, hpRegenAmount, mpRegenAmount);
  }

  /**
   * 스킬 매니저의 중복 스킬을 정리합니다. (데이터 로드 후 호출)
   */
  public void cleanupDuplicateSkills() {
    if (skillManager != null) {
      skillManager.removeDuplicateSkills();
      logger.info("{} 스킬 중복 정리 완료", name);
    }
  }

  /**
   * 스킬 시스템 상태를 검증합니다.
   */
  public void validateLoadedData() {
    if (skillManager == null) {
      logger.error("{} 스킬 매니저가 null", name);
      skillManager = new SkillService(); // 새로 생성
      return;
    }

    // 중복 스킬 정리
    cleanupDuplicateSkills();

    // 인벤토리 착용 장비 검증
    if (inventory != null) {
      inventory.validateEquippedItems();
    }
    if (questManager != null) {
      questManager.validateQuestData();
      logger.info("퀘스트 데이터 검증 완료: 활성 {}개, 완료 {}개", questManager.getActiveQuests().size(), questManager.getCompletedQuests().size());
    }

    // 디버그 모드에서 스킬 정보 출력
    if (logger.isDebugEnabled()) {
      skillManager.debugPrintSkills();
    }
  }

  /**
   * 캐릭터 로드 후 호출되는 초기화 메서드
   */
  public void postLoadInitialization() {
    logger.info("캐릭터 로드 후 초기화: {}", name);

    // 스킬 시스템 검증 및 정리
    validateLoadedData();

    // 기타 필요한 초기화 작업들...

    logger.debug("{} 로드 후 초기화 완료", name);
  }

  @Override
  public String toString() {
    return "Player [name=" + name + ", level=" + level + ", hp=" + hp + ", maxHp=" + maxHp + ", mp=" + mp + ", maxMp=" + maxMp + ", exp=" + exp
        + ", baseAttack=" + baseAttack + ", baseDefense=" + baseDefense + ", gold=" + gold + ", restoreHp=" + restoreHp + ", restoreMana="
        + restoreMana + ", inventory=" + inventory + ", skillManager=" + skillManager + ", playerStatusCondition=" + playerStatusCondition
        + ", questManager=" + questManager + "]";
  }
  
  
}
