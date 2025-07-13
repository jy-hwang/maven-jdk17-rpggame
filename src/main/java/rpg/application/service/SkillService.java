package rpg.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.application.factory.SkillFactory;
import rpg.domain.monster.Monster;
import rpg.domain.player.Player;
import rpg.domain.skill.Skill;
import rpg.domain.skill.SkillResult;

/**
 * Factory 패턴을 활용한 스킬 관리 서비스
 * - ID 기반 스킬 관리
 * - JSON 템플릿에서 스킬 로드
 * - 중복 방지 및 성능 최적화
 */
public class SkillService {
  private static final Logger logger = LoggerFactory.getLogger(SkillService.class);

  // 학습한 스킬 ID 목록 (Factory에서 인스턴스 생성)
  private List<String> learnedSkillIds;
  // 스킬 쿨다운 (스킬 ID -> 남은 쿨다운)
  private Map<String, Integer> skillCooldowns;
  // 기본 스킬 초기화 여부
  private boolean defaultSkillsInitialized;

  /**
   * 새로운 SkillService 생성자 (기본 스킬 자동 초기화)
   */
  public SkillService() {
    this.learnedSkillIds = new ArrayList<>();
    this.skillCooldowns = new HashMap<>();
    this.defaultSkillsInitialized = false;
    initializeDefaultSkills();
  }

  /**
   * 저장된 데이터로 SkillService 생성자 (기본 스킬 초기화 안함)
   */
  @JsonCreator
  public SkillService(
    @JsonProperty("learnedSkillIds") List<String> learnedSkillIds,
    @JsonProperty("skillCooldowns") Map<String, Integer> skillCooldowns
  ) {
    this.learnedSkillIds = learnedSkillIds != null ? new ArrayList<>(learnedSkillIds) : new ArrayList<>();
    this.skillCooldowns = skillCooldowns != null ? new HashMap<>(skillCooldowns) : new HashMap<>();
    this.defaultSkillsInitialized = true; // 저장된 데이터에서 로드할 때는 기본 스킬 초기화 안함

    logger.debug("SkillService 로드 완료: {}개 스킬 ID", this.learnedSkillIds.size());
  }

  /**
   * 기본 스킬들을 초기화합니다. (중복 방지)
   */
  private void initializeDefaultSkills() {
    if (defaultSkillsInitialized) {
      logger.debug("기본 스킬이 이미 초기화됨 - 건너뜀");
      return;
    }

    // Factory에서 기본 스킬 ID 목록 가져오기
    List<String> defaultSkillIds = SkillFactory.getDefaultSkillIds();
    
    for (String skillId : defaultSkillIds) {
      learnSkillIfNotExists(skillId);
    }

    defaultSkillsInitialized = true;
    logger.debug("기본 스킬 초기화 완료: {}개 스킬", learnedSkillIds.size());
  }

  /**
   * 스킬 ID가 존재하지 않을 때만 학습합니다. (중복 방지)
   */
  private void learnSkillIfNotExists(String skillId) {
    if (skillId == null || skillId.trim().isEmpty()) {
      logger.warn("null 또는 빈 스킬 ID 학습 시도");
      return;
    }

    if (hasSkillId(skillId)) {
      logger.debug("스킬 ID 이미 존재 - 건너뜀: {}", skillId);
      return;
    }

    // Factory에서 스킬 존재 여부 확인
    if (!SkillFactory.hasSkill(skillId)) {
      logger.warn("존재하지 않는 스킬 ID: {}", skillId);
      return;
    }

    learnedSkillIds.add(skillId);
    logger.debug("스킬 학습: {}", skillId);
  }

  /**
   * 단일 스킬 ID로 학습합니다. (중복 체크)
   */
  public void learnSkill(String skillId) {
    learnSkillIfNotExists(skillId);
  }

  /**
   * 여러 스킬 ID를 학습합니다. (중복 체크)
   */
  public void learnSkills(List<String> skillIds) {
    if (skillIds == null || skillIds.isEmpty()) {
      return;
    }

    for (String skillId : skillIds) {
      learnSkillIfNotExists(skillId);
    }
  }

  /**
   * 레벨에 따라 새로운 스킬을 학습할 수 있는지 확인하고 학습합니다.
   */
  public List<String> checkAndLearnNewSkills(int currentLevel) {
    List<String> newSkillIds = new ArrayList<>();
    List<String> availableSkillIds = SkillFactory.getAvailableSkillIds(currentLevel);

    for (String skillId : availableSkillIds) {
      if (!hasSkillId(skillId)) {
        learnedSkillIds.add(skillId);
        newSkillIds.add(skillId);
        
        // 스킬 정보 로그
        Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);
        if (skillInfo != null) {
          logger.debug("레벨업 스킬 학습: {} ({})", skillId, skillInfo.get("name"));
        }
      }
    }

    if (!newSkillIds.isEmpty()) {
      logger.info("레벨 {} 달성으로 새로운 스킬 {}개 학습", currentLevel, newSkillIds.size());
    }

    return newSkillIds;
  }

  /**
   * 스킬을 사용할 수 있는지 확인합니다.
   */
  public boolean canUseSkill(String skillId, Player character) {
    if (skillId == null || character == null) {
      return false;
    }

    // 학습한 스킬인지 확인
    if (!hasSkillId(skillId)) {
      return false;
    }

    // Factory에서 스킬 정보 가져오기
    Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);
    if (skillInfo == null) {
      return false;
    }

    // 레벨 확인
    int requiredLevel = (Integer) skillInfo.get("requiredLevel");
    if (character.getLevel() < requiredLevel) {
      return false;
    }

    // 쿨다운 확인
    if (skillCooldowns.getOrDefault(skillId, 0) > 0) {
      return false;
    }

    // 마나 확인
    int manaCost = (Integer) skillInfo.get("manaCost");
    if (character.getMana() < manaCost) {
      return false;
    }

    return true;
  }

  /**
   * 스킬을 사용합니다.
   */
  public SkillResult useSkill(String skillId, Player caster, Monster target) {
    if (!hasSkillId(skillId)) {
      return new SkillResult(false, "해당 스킬을 학습하지 않았습니다.", 0);
    }

    if (!canUseSkill(skillId, caster)) {
      if (skillCooldowns.getOrDefault(skillId, 0) > 0) {
        return new SkillResult(false, "스킬이 아직 쿨다운 중입니다.", 0);
      } else {
        return new SkillResult(false, "마나가 부족하거나 레벨이 부족합니다.", 0);
      }
    }

    // Factory에서 스킬 인스턴스 생성
    Skill skill = SkillFactory.createSkill(skillId);
    if (skill == null) {
      return new SkillResult(false, "스킬을 생성할 수 없습니다.", 0);
    }

    // 스킬 사용
    SkillResult result = skill.useSkill(caster, target);
    if (result.isSuccess()) {
      // 쿨다운 설정
      skillCooldowns.put(skillId, skill.getCooldown());
      logger.debug("스킬 사용: {} (쿨다운: {}턴)", skillId, skill.getCooldown());
    }

    return result;
  }

  /**
   * 스킬 이름으로 사용 (하위 호환성)
   */
  public SkillResult useSkillByName(String skillName, Player caster, Monster target) {
    String skillId = SkillFactory.getSkillIdByName(skillName);
    if (skillId == null) {
      return new SkillResult(false, "해당 이름의 스킬을 찾을 수 없습니다.", 0);
    }
    
    return useSkill(skillId, caster, target);
  }

  /**
   * 턴이 끝날 때 쿨다운을 감소시킵니다.
   */
  public void reduceCooldowns() {
    skillCooldowns.replaceAll((skillId, cooldown) -> Math.max(0, cooldown - 1));
    skillCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
  }

  /**
   * 사용 가능한 스킬 목록을 반환합니다.
   */
  public List<Skill> getAvailableSkills(Player character) {
    return learnedSkillIds.stream()
      .filter(skillId -> canUseSkill(skillId, character))
      .map(SkillFactory::createSkill)
      .filter(skill -> skill != null)
      .collect(Collectors.toList());
  }

  /**
   * 학습한 모든 스킬을 표시합니다.
   */
  public void displaySkills(Player character) {
    System.out.println("\n=== 스킬 목록 ===");
    if (learnedSkillIds.isEmpty()) {
      System.out.println("학습한 스킬이 없습니다.");
      return;
    }

    for (int i = 0; i < learnedSkillIds.size(); i++) {
      String skillId = learnedSkillIds.get(i);
      Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);
      
      if (skillInfo == null) continue;
      
      String name = (String) skillInfo.get("name");
      int requiredLevel = (Integer) skillInfo.get("requiredLevel");
      int manaCost = (Integer) skillInfo.get("manaCost");
      
      String status = "";
      if (requiredLevel > character.getLevel()) {
        status = " (레벨 부족)";
      } else if (skillCooldowns.getOrDefault(skillId, 0) > 0) {
        status = " (쿨다운: " + skillCooldowns.get(skillId) + "턴)";
      } else if (character.getMana() < manaCost) {
        status = " (마나 부족)";
      } else {
        status = " (사용 가능)";
      }

      System.out.printf("%d. %s%s%n", i + 1, name, status);
    }
    System.out.println("================");
  }

  /**
   * 특정 스킬 ID를 가지고 있는지 확인합니다.
   */
  public boolean hasSkillId(String skillId) {
    if (skillId == null || skillId.trim().isEmpty()) {
      return false;
    }
    return learnedSkillIds.contains(skillId);
  }

  /**
   * 스킬 이름으로 보유 여부 확인 (하위 호환성)
   */
  public boolean hasSkillByName(String skillName) {
    String skillId = SkillFactory.getSkillIdByName(skillName);
    return skillId != null && hasSkillId(skillId);
  }

  /**
   * 인덱스로 스킬을 찾습니다.
   */
  public Skill getSkillByIndex(int index) {
    if (index >= 0 && index < learnedSkillIds.size()) {
      String skillId = learnedSkillIds.get(index);
      return SkillFactory.createSkill(skillId);
    }
    return null;
  }

  /**
   * 스킬 쿨다운을 설정합니다.
   */
  public void setSkillCooldown(String skillId, int remainingTurns) {
    if (skillId != null && remainingTurns > 0) {
      skillCooldowns.put(skillId, remainingTurns);
    }
  }

  /**
   * 중복 스킬 ID를 제거합니다. (데이터 정리용)
   */
  public void removeDuplicateSkills() {
    List<String> uniqueSkillIds = learnedSkillIds.stream()
      .distinct()
      .collect(Collectors.toList());

    int removedCount = learnedSkillIds.size() - uniqueSkillIds.size();
    learnedSkillIds = uniqueSkillIds;

    if (removedCount > 0) {
      logger.info("중복 스킬 제거 완료: {}개 제거, {}개 스킬 남음", removedCount, learnedSkillIds.size());
    }
  }

  /**
   * 학습한 스킬 인스턴스 목록 반환 (하위 호환성)
   */
  public List<Skill> getLearnedSkills() {
    return learnedSkillIds.stream()
      .map(SkillFactory::createSkill)
      .filter(skill -> skill != null)
      .collect(Collectors.toList());
  }

  /**
   * 학습한 스킬 ID 목록 반환
   */
  public List<String> getLearnedSkillIds() {
    return new ArrayList<>(learnedSkillIds);
  }

  /**
   * 스킬 쿨다운 정보 반환
   */
  public Map<String, Integer> getSkillCooldowns() {
    return new HashMap<>(skillCooldowns);
  }

  /**
   * 디버그용 - 스킬 목록 출력
   */
  public void debugPrintSkills() {
    if (logger.isDebugEnabled()) {
      logger.debug("=== 현재 스킬 목록 ===");
      for (int i = 0; i < learnedSkillIds.size(); i++) {
        String skillId = learnedSkillIds.get(i);
        Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);
        if (skillInfo != null) {
          logger.debug("{}. {} ({}) - 레벨: {}, 마나: {}", 
            i + 1, skillInfo.get("name"), skillId, 
            skillInfo.get("requiredLevel"), skillInfo.get("manaCost"));
        }
      }
      logger.debug("총 {}개 스킬", learnedSkillIds.size());
    }
  }

  /**
   * 통계 정보 반환
   */
  public Map<String, Integer> getStatistics() {
    Map<String, Integer> stats = new HashMap<>();
    stats.put("totalLearnedSkills", learnedSkillIds.size());
    stats.put("activeCooldowns", skillCooldowns.size());
    
    // 타입별 통계 (Factory에서 정보 가져오기)
    Map<String, Integer> typeStats = new HashMap<>();
    for (String skillId : learnedSkillIds) {
      Map<String, Object> skillInfo = SkillFactory.getSkillInfo(skillId);
      if (skillInfo != null) {
        String type = (String) skillInfo.get("type");
        typeStats.merge(type.toLowerCase() + "Skills", 1, Integer::sum);
      }
    }
    stats.putAll(typeStats);
    
    return stats;
  }
}