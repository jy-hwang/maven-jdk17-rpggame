package rpg.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.domain.monster.Monster;
import rpg.domain.player.Player;
import rpg.domain.skill.Skill;
import rpg.domain.skill.SkillType;
import rpg.shared.constant.GameConstants;
import rpg.shared.constant.ItemConstants;

/**
 * 캐릭터의 스킬 관리 클래스 (중복 학습 문제 수정)
 */
public class SkillService {
  private static final Logger logger = LoggerFactory.getLogger(SkillService.class);

  private List<Skill> learnedSkills;
  private Map<String, Integer> skillCooldowns; // 스킬명 -> 남은 쿨다운
  private boolean defaultSkillsInitialized; // 기본 스킬 초기화 여부

  /**
   * 새로운 SkillManager 생성자 (기본 스킬 자동 초기화)
   */
  public SkillService() {
    this.learnedSkills = new ArrayList<>();
    this.skillCooldowns = new HashMap<>();
    this.defaultSkillsInitialized = false;
    initializeDefaultSkills();
  }

  /**
   * 저장된 데이터로 SkillManager 생성자 (기본 스킬 초기화 안함)
   */
  @JsonCreator
  public SkillService(@JsonProperty("learnedSkills") List<Skill> learnedSkills, @JsonProperty("skillCooldowns") Map<String, Integer> skillCooldowns) {
    this.learnedSkills = learnedSkills != null ? new ArrayList<>(learnedSkills) : new ArrayList<>();
    this.skillCooldowns = skillCooldowns != null ? new HashMap<>(skillCooldowns) : new HashMap<>();
    this.defaultSkillsInitialized = true; // 저장된 데이터에서 로드할 때는 기본 스킬 초기화 안함

    logger.debug("SkillManager 로드 완료: {}개 스킬", this.learnedSkills.size());
  }

  /**
   * 기본 스킬들을 초기화합니다. (중복 방지)
   */
  private void initializeDefaultSkills() {
    if (defaultSkillsInitialized) {
      logger.debug("기본 스킬이 이미 초기화됨 - 건너뜀");
      return;
    }

    // 레벨 1 기본 스킬들
    Skill powerStrike = new Skill("강타", "강력한 공격을 가합니다", SkillType.ATTACK, 1, 5, 2, 1.5, 0, 0);
    Skill heal = new Skill("치유", "체력을 회복합니다", SkillType.HEAL, 1, 8, 3, 0, 30, 0);

    // 중복 체크하면서 추가
    learnSkillIfNotExists(powerStrike);
    learnSkillIfNotExists(heal);

    defaultSkillsInitialized = true;
    logger.debug("기본 스킬 초기화 완료: {}개 스킬", learnedSkills.size());
  }

  /**
   * 스킬이 존재하지 않을 때만 학습합니다. (중복 방지)
   */
  private void learnSkillIfNotExists(Skill skill) {
    if (skill == null) {
      logger.warn("null 스킬 학습 시도");
      return;
    }

    if (hasSkill(skill.getName())) {
      logger.debug("스킬 이미 존재 - 건너뜀: {}", skill.getName());
      return;
    }

    learnedSkills.add(skill);
    logger.debug("스킬 학습: {}", skill.getName());
  }

  /**
   * 단일 스킬을 학습합니다. (중복 체크)
   */
  public void learnSkill(Skill skill) {
    learnSkillIfNotExists(skill);
  }

  /**
   * 여러 스킬을 학습합니다. (중복 체크)
   */
  public void learnSkills(List<Skill> skills) {
    if (skills == null || skills.isEmpty()) {
      return;
    }

    for (Skill skill : skills) {
      learnSkillIfNotExists(skill);
    }
  }

  /**
   * 레벨에 따라 새로운 스킬을 학습할 수 있는지 확인하고 학습합니다.
   */
  public List<Skill> checkAndLearnNewSkills(int currentLevel) {
    List<Skill> newSkills = new ArrayList<>();

    // 레벨별 스킬 해금 (중복 체크)
    if (currentLevel >= ItemConstants.BEGINNER_LEVEL && !hasSkill("화염구")) {
      Skill fireball = new Skill("화염구", "불타는 화염구를 던집니다", SkillType.ATTACK, 3, 12, 4, 2.0, 0, 0);
      learnedSkills.add(fireball);
      newSkills.add(fireball);
      logger.debug("레벨업 스킬 학습: {}", fireball.getName());
    }

    if (currentLevel >= GameConstants.NUMBER_FIVE && !hasSkill("방어 강화")) {
      Skill defenseBoost = new Skill("방어 강화", "일시적으로 방어력을 증가시킵니다", SkillType.BUFF, 5, 10, 5, 0, 0, 3);
      learnedSkills.add(defenseBoost);
      newSkills.add(defenseBoost);
      logger.debug("레벨업 스킬 학습: {}", defenseBoost.getName());
    }

    if (currentLevel >= ItemConstants.INTERMEDIATE_LEVEL && !hasSkill("대치유")) {
      Skill greatHeal = new Skill("대치유", "강력한 치유 마법입니다", SkillType.HEAL, 7, 20, 5, 0, 80, 0);
      learnedSkills.add(greatHeal);
      newSkills.add(greatHeal);
      logger.debug("레벨업 스킬 학습: {}", greatHeal.getName());
    }

    if (currentLevel >= GameConstants.NUMBER_TEN && !hasSkill("연쇄 번개")) {
      Skill chainLightning = new Skill("연쇄 번개", "강력한 번개 공격입니다", SkillType.ATTACK, 10, 25, 6, 3.0, 0, 0);
      learnedSkills.add(chainLightning);
      newSkills.add(chainLightning);
      logger.debug("레벨업 스킬 학습: {}", chainLightning.getName());
    }

    if (!newSkills.isEmpty()) {
      logger.info("레벨 {} 달성으로 새로운 스킬 {}개 학습", currentLevel, newSkills.size());
    }

    return newSkills;
  }

  /**
   * 스킬을 사용할 수 있는지 확인합니다.
   */
  public boolean canUseSkill(Skill skill, Player character) {
    if (skill == null || character == null) {
      return false;
    }

    // 쿨다운 확인
    if (skillCooldowns.getOrDefault(skill.getName(), GameConstants.NUMBER_ZERO) > GameConstants.NUMBER_ZERO) {
      return false;
    }

    // 마나 확인
    if (character.getMana() < skill.getManaCost()) {
      return false;
    }

    return true;
  }

  /**
   * 스킬을 사용합니다.
   */
  public Skill.SkillResult useSkill(String skillName, Player caster, Monster target) {
    Skill skill = getSkillByName(skillName);
    if (skill == null) {
      return new Skill.SkillResult(false, "해당 스킬을 찾을 수 없습니다.", GameConstants.NUMBER_ZERO);
    }

    if (!canUseSkill(skill, caster)) {
      if (skillCooldowns.getOrDefault(skillName, GameConstants.NUMBER_ZERO) > GameConstants.NUMBER_ZERO) {
        return new Skill.SkillResult(false, "스킬이 아직 쿨다운 중입니다.", GameConstants.NUMBER_ZERO);
      } else {
        return new Skill.SkillResult(false, "마나가 부족합니다.", GameConstants.NUMBER_ZERO);
      }
    }

    Skill.SkillResult result = skill.useSkill(caster, target);
    if (result.isSuccess()) {
      // 쿨다운 설정
      skillCooldowns.put(skillName, skill.getCooldown());
      logger.debug("스킬 사용: {} (쿨다운: {}턴)", skillName, skill.getCooldown());
    }

    return result;
  }

  /**
   * 턴이 끝날 때 쿨다운을 감소시킵니다.
   */
  public void reduceCooldowns() {
    skillCooldowns.replaceAll((name, cooldown) -> Math.max(GameConstants.NUMBER_ZERO, cooldown - GameConstants.NUMBER_ONE));
    skillCooldowns.entrySet().removeIf(entry -> entry.getValue() <= GameConstants.NUMBER_ZERO);
  }

  /**
   * 사용 가능한 스킬 목록을 반환합니다.
   */
  public List<Skill> getAvailableSkills(Player character) {
    return learnedSkills.stream().filter(skill -> skill.getRequiredLevel() <= character.getLevel()).filter(skill -> canUseSkill(skill, character))
        .toList();
  }

  /**
   * 학습한 모든 스킬을 표시합니다.
   */
  public void displaySkills(Player character) {
    System.out.println("\n=== 스킬 목록 ===");
    if (learnedSkills.isEmpty()) {
      System.out.println("학습한 스킬이 없습니다.");
      return;
    }

    for (int i = GameConstants.NUMBER_ZERO; i < learnedSkills.size(); i++) {
      Skill skill = learnedSkills.get(i);
      String status = "";

      if (skill.getRequiredLevel() > character.getLevel()) {
        status = " (레벨 부족)";
      } else if (skillCooldowns.getOrDefault(skill.getName(), GameConstants.NUMBER_ZERO) > GameConstants.NUMBER_ZERO) {
        status = " (쿨다운: " + skillCooldowns.get(skill.getName()) + "턴)";
      } else if (character.getMana() < skill.getManaCost()) {
        status = " (마나 부족)";
      } else {
        status = " (사용 가능)";
      }

      System.out.printf("%d. %s%s%n", i + GameConstants.NUMBER_ONE, skill.getName(), status);
    }
    System.out.println("================");
  }

  /**
   * 특정 스킬을 가지고 있는지 확인합니다.
   */
  private boolean hasSkill(String skillName) {
    if (skillName == null || skillName.trim().isEmpty()) {
      return false;
    }

    return learnedSkills.stream().anyMatch(skill -> skillName.equals(skill.getName()));
  }

  /**
   * 스킬 이름으로 스킬을 찾습니다.
   */
  private Skill getSkillByName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }

    return learnedSkills.stream().filter(skill -> name.equals(skill.getName())).findFirst().orElse(null);
  }

  /**
   * 인덱스로 스킬을 찾습니다.
   */
  public Skill getSkillByIndex(int index) {
    if (index >= GameConstants.NUMBER_ZERO && index < learnedSkills.size()) {
      return learnedSkills.get(index);
    }
    return null;
  }

  /**
   * 스킬 쿨다운을 설정합니다.
   */
  public void setSkillCooldown(String skillName, int remainingTurns) {
    if (skillName != null && remainingTurns > 0) {
      skillCooldowns.put(skillName, remainingTurns);
    }
  }

  /**
   * 디버그용 - 스킬 목록 출력
   */
  public void debugPrintSkills() {
    if (logger.isDebugEnabled()) {
      logger.debug("=== 현재 스킬 목록 ===");
      for (int i = 0; i < learnedSkills.size(); i++) {
        Skill skill = learnedSkills.get(i);
        logger.debug("{}. {} (레벨: {}, 마나: {})", i + 1, skill.getName(), skill.getRequiredLevel(), skill.getManaCost());
      }
      logger.debug("총 {}개 스킬", learnedSkills.size());
    }
  }

  /**
   * 중복 스킬을 제거합니다. (데이터 정리용)
   */
  public void removeDuplicateSkills() {
    Map<String, Skill> uniqueSkills = new HashMap<>();

    for (Skill skill : learnedSkills) {
      if (!uniqueSkills.containsKey(skill.getName())) {
        uniqueSkills.put(skill.getName(), skill);
      } else {
        logger.warn("중복 스킬 제거: {}", skill.getName());
      }
    }

    learnedSkills.clear();
    learnedSkills.addAll(uniqueSkills.values());

    logger.info("중복 스킬 제거 완료: {}개 스킬 남음", learnedSkills.size());
  }

  // Getters
  public List<Skill> getLearnedSkills() {
    return new ArrayList<>(learnedSkills);
  }

  public Map<String, Integer> getSkillCooldowns() {
    return new HashMap<>(skillCooldowns);
  }
}
