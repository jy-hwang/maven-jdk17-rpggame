package dto.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.SkillCooldownDto;
import dto.SkillDto;
import dto.SkillManagerDto;
import model.Skill;
import model.SkillManager;

public class SkillManagerMapper {
  private static final Logger logger = LoggerFactory.getLogger(SkillManagerMapper.class);

  /**
   * SkillManager를 DTO로 변환합니다.
   */
  public static SkillManagerDto toDto(SkillManager skillManager) {
    if (skillManager == null) {
      logger.warn("SkillManager가 null - 빈 DTO 반환");
      return new SkillManagerDto();
    }

    SkillManagerDto dto = new SkillManagerDto();

    // 학습한 스킬들 변환
    List<SkillDto> skillDtos = new ArrayList<>();
    for (Skill skill : skillManager.getLearnedSkills()) {
      if (skill != null) {
        skillDtos.add(SkillMapper.toDto(skill));
      }
    }
    dto.setLearnedSkills(skillDtos);

    // 쿨다운 정보 변환 (Map을 List로)
    List<SkillCooldownDto> cooldownDtos = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : skillManager.getSkillCooldowns().entrySet()) {
      if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
        SkillCooldownDto cooldownDto = new SkillCooldownDto();
        cooldownDto.setSkillName(entry.getKey());
        cooldownDto.setRemainingTurns(entry.getValue());
        cooldownDtos.add(cooldownDto);
      }
    }
    dto.setSkillCooldowns(cooldownDtos);

    logger.debug("SkillManager -> DTO 변환 완료: {}개 스킬, {}개 쿨다운", skillDtos.size(), cooldownDtos.size());

    return dto;
  }

  /**
   * DTO에서 SkillManager를 생성합니다. (중복 방지)
   */
  public static SkillManager fromDto(SkillManagerDto dto) {
    if (dto == null) {
      logger.warn("SkillManagerDto가 null - 새 SkillManager 생성");
      return new SkillManager();
    }

    // 스킬 목록 변환 (중복 제거)
    List<Skill> skills = new ArrayList<>();
    Map<String, Skill> uniqueSkills = new HashMap<>(); // 중복 방지용

    if (dto.getLearnedSkills() != null) {
      for (SkillDto skillDto : dto.getLearnedSkills()) {
        if (skillDto != null) {
          Skill skill = SkillMapper.fromDto(skillDto);
          if (skill != null && skill.getName() != null) {
            // 중복 체크
            if (!uniqueSkills.containsKey(skill.getName())) {
              uniqueSkills.put(skill.getName(), skill);
              skills.add(skill);
            } else {
              logger.debug("중복 스킬 건너뜀: {}", skill.getName());
            }
          }
        }
      }
    }

    // 쿨다운 정보 변환
    Map<String, Integer> cooldowns = new HashMap<>();
    if (dto.getSkillCooldowns() != null) {
      for (SkillCooldownDto cooldownDto : dto.getSkillCooldowns()) {
        if (cooldownDto != null && cooldownDto.getSkillName() != null && cooldownDto.getRemainingTurns() > 0) {
          cooldowns.put(cooldownDto.getSkillName(), cooldownDto.getRemainingTurns());
        }
      }
    }

    // 저장된 데이터로 SkillManager 생성 (기본 스킬 초기화 방지)
    SkillManager skillManager = new SkillManager(skills, cooldowns);

    logger.debug("DTO -> SkillManager 변환 완료: {}개 스킬 (중복 제거됨), {}개 쿨다운", skills.size(), cooldowns.size());

    return skillManager;
  }

  /**
   * 스킬 매니저 데이터 검증
   */
  public static boolean validateSkillManagerDto(SkillManagerDto dto) {
    if (dto == null) {
      return false;
    }

    // 스킬 목록 검증
    if (dto.getLearnedSkills() != null) {
      for (SkillDto skillDto : dto.getLearnedSkills()) {
        if (skillDto == null || skillDto.getName() == null || skillDto.getName().trim().isEmpty()) {
          logger.warn("유효하지 않은 스킬 데이터 발견");
          return false;
        }
      }
    }

    // 쿨다운 정보 검증
    if (dto.getSkillCooldowns() != null) {
      for (SkillCooldownDto cooldownDto : dto.getSkillCooldowns()) {
        if (cooldownDto == null || cooldownDto.getSkillName() == null || cooldownDto.getSkillName().trim().isEmpty()
            || cooldownDto.getRemainingTurns() < 0) {
          logger.warn("유효하지 않은 쿨다운 데이터 발견");
          return false;
        }
      }
    }

    return true;
  }

  /**
   * 스킬 매니저 통계 출력 (디버그용)
   */
  public static void printSkillManagerStats(SkillManager skillManager) {
    if (skillManager == null) {
      logger.debug("SkillManager가 null");
      return;
    }

    List<Skill> skills = skillManager.getLearnedSkills();
    Map<String, Integer> cooldowns = skillManager.getSkillCooldowns();

    logger.debug("=== SkillManager 통계 ===");
    logger.debug("총 스킬 수: {}", skills.size());
    logger.debug("활성 쿨다운: {}", cooldowns.size());

    // 스킬 타입별 통계
    Map<Skill.SkillType, Long> typeStats =
        skills.stream().collect(java.util.stream.Collectors.groupingBy(Skill::getType, java.util.stream.Collectors.counting()));

    typeStats.forEach((type, count) -> logger.debug("  {}: {}개", type.getDisplayName(), count));

    logger.debug("========================");
  }
}
