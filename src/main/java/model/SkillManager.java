package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import config.BaseConstant;

/**
 * 캐릭터의 스킬 관리 클래스
 */
public class SkillManager {
  private static final Logger logger = LoggerFactory.getLogger(SkillManager.class);

  private List<Skill> learnedSkills;
  private Map<String, Integer> skillCooldowns; // 스킬명 -> 남은 쿨다운

  @JsonCreator
  public SkillManager() {
    this.learnedSkills = new ArrayList<>();
    this.skillCooldowns = new HashMap<>();
    initializeDefaultSkills();
  }

  /**
   * 기본 스킬들을 초기화합니다.
   */
  private void initializeDefaultSkills() {
    // 레벨 1 기본 스킬
    learnSkills(Arrays.asList(new Skill("강타", "강력한 공격을 가합니다", Skill.SkillType.ATTACK, 1, 5, 2, 1.5, 0, 0),
        new Skill("치유", "체력을 회복합니다", Skill.SkillType.HEAL, 1, 8, 3, 0, 30, 0)));
  }

  /**
   * 스킬을 학습합니다.
   */
  public void learnSkills(List<Skill> skills) {
    for (Skill skill : skills) {
      learnedSkills.add(skill);
      logger.debug("스킬 학습: {}", skill.getName());
    }
  }

  /**
   * 레벨에 따라 새로운 스킬을 학습할 수 있는지 확인하고 학습합니다.
   */
  public List<Skill> checkAndLearnNewSkills(int currentLevel) {
    List<Skill> newSkills = new ArrayList<>();

    // 레벨별 스킬 해금
    if (currentLevel >= BaseConstant.BEGINNER_LEVEL && !hasSkill("화염구")) {
      Skill fireball = new Skill("화염구", "불타는 화염구를 던집니다", Skill.SkillType.ATTACK, 3, 12, 4, 2.0, 0, 0);
      learnedSkills.add(fireball);
      newSkills.add(fireball);
    }

    if (currentLevel >= BaseConstant.NUMBER_FIVE && !hasSkill("방어 강화")) {
      Skill defenseBoost = new Skill("방어 강화", "일시적으로 방어력을 증가시킵니다", Skill.SkillType.BUFF, 5, 10, 5, 0, 0, 3);
      learnedSkills.add(defenseBoost);
      newSkills.add(defenseBoost);
    }

    if (currentLevel >= BaseConstant.INTERMEDIATE_LEVEL && !hasSkill("대치유")) {
      Skill greatHeal = new Skill("대치유", "강력한 치유 마법입니다", Skill.SkillType.HEAL, 7, 20, 5, 0, 80, 0);
      learnedSkills.add(greatHeal);
      newSkills.add(greatHeal);
    }

    if (currentLevel >= BaseConstant.NUMBER_TEN && !hasSkill("연쇄 번개")) {
      Skill chainLightning = new Skill("연쇄 번개", "강력한 번개 공격입니다", Skill.SkillType.ATTACK, 10, 25, 6, 3.0, 0, 0);
      learnedSkills.add(chainLightning);
      newSkills.add(chainLightning);
    }

    return newSkills;
  }

  /**
   * 스킬을 사용할 수 있는지 확인합니다.
   */
  public boolean canUseSkill(Skill skill, GameCharacter character) {
    // 쿨다운 확인
    if (skillCooldowns.getOrDefault(skill.getName(), BaseConstant.NUMBER_ZERO) > BaseConstant.NUMBER_ZERO) {
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
  public Skill.SkillResult useSkill(String skillName, GameCharacter caster, Monster target) {
    Skill skill = getSkillByName(skillName);
    if (skill == null) {
      return new Skill.SkillResult(false, "해당 스킬을 찾을 수 없습니다.", BaseConstant.NUMBER_ZERO);
    }

    if (!canUseSkill(skill, caster)) {
      if (skillCooldowns.getOrDefault(skillName, BaseConstant.NUMBER_ZERO) > BaseConstant.NUMBER_ZERO) {
        return new Skill.SkillResult(false, "스킬이 아직 쿨다운 중입니다.", BaseConstant.NUMBER_ZERO);
      } else {
        return new Skill.SkillResult(false, "마나가 부족합니다.", BaseConstant.NUMBER_ZERO);
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
    skillCooldowns.replaceAll((name, cooldown) -> Math.max(BaseConstant.NUMBER_ZERO, cooldown - BaseConstant.NUMBER_ONE));
    skillCooldowns.entrySet().removeIf(entry -> entry.getValue() <= BaseConstant.NUMBER_ZERO);
  }

  /**
   * 사용 가능한 스킬 목록을 반환합니다.
   */
  public List<Skill> getAvailableSkills(GameCharacter character) {
    return learnedSkills.stream().filter(skill -> skill.getRequiredLevel() <= character.getLevel()).filter(skill -> canUseSkill(skill, character))
        .toList();
  }

  /**
   * 학습한 모든 스킬을 표시합니다.
   */
  public void displaySkills(GameCharacter character) {
    System.out.println("\n=== 스킬 목록 ===");
    if (learnedSkills.isEmpty()) {
      System.out.println("학습한 스킬이 없습니다.");
      return;
    }

    for (int i = BaseConstant.NUMBER_ZERO; i < learnedSkills.size(); i++) {
      Skill skill = learnedSkills.get(i);
      String status = "";

      if (skill.getRequiredLevel() > character.getLevel()) {
        status = " (레벨 부족)";
      } else if (skillCooldowns.getOrDefault(skill.getName(), BaseConstant.NUMBER_ZERO) > BaseConstant.NUMBER_ZERO) {
        status = " (쿨다운: " + skillCooldowns.get(skill.getName()) + "턴)";
      } else if (character.getMana() < skill.getManaCost()) {
        status = " (마나 부족)";
      } else {
        status = " (사용 가능)";
      }

      System.out.printf("%d. %s%s%n", i + BaseConstant.NUMBER_ONE, skill.getName(), status);
    }
    System.out.println("================");
  }

  private boolean hasSkill(String skillName) {
    return learnedSkills.stream().anyMatch(skill -> skill.getName().equals(skillName));
  }

  private Skill getSkillByName(String name) {
    return learnedSkills.stream().filter(skill -> skill.getName().equals(name)).findFirst().orElse(null);
  }

  public Skill getSkillByIndex(int index) {
    if (index >= BaseConstant.NUMBER_ZERO && index < learnedSkills.size()) {
      return learnedSkills.get(index);
    }
    return null;
  }

  // Getters
  public List<Skill> getLearnedSkills() {
    return new ArrayList<>(learnedSkills);
  }

  public Map<String, Integer> getSkillCooldowns() {
    return new HashMap<>(skillCooldowns);
  }
  
  /**
   * 스킬을 학습합니다.
   */
  public void learnSkill(Skill skill) {
      if (skill != null && !learnedSkills.contains(skill)) {
          learnedSkills.add(skill);
          logger.debug("스킬 학습: {}", skill.getName());
      }
  }

  /**
   * 스킬 쿨다운을 설정합니다.
   */
  public void setSkillCooldown(String skillName, int remainingTurns) {
      if (skillName != null && remainingTurns > 0) {
          skillCooldowns.put(skillName, remainingTurns);
      }
  }

}
