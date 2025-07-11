package dto.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dto.SkillCooldownDto;
import dto.SkillDto;
import dto.SkillManagerDto;
import model.Skill;
import model.SkillManager;

public class SkillManagerMapper {
  public static SkillManagerDto toDto(SkillManager skillManager) {
    SkillManagerDto dto = new SkillManagerDto();

    // setter 사용으로 변경
    List<SkillDto> skillDtos = new ArrayList<>();
    for (Skill skill : skillManager.getLearnedSkills()) {
      skillDtos.add(SkillMapper.toDto(skill));
    }
    dto.setLearnedSkills(skillDtos);

    // 쿨다운 정보 (Map을 List로 변환)
    List<SkillCooldownDto> cooldownDtos = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : skillManager.getSkillCooldowns().entrySet()) {
      SkillCooldownDto cooldownDto = new SkillCooldownDto();
      cooldownDto.setSkillName(entry.getKey());
      cooldownDto.setRemainingTurns(entry.getValue());
      cooldownDtos.add(cooldownDto);
    }
    dto.setSkillCooldowns(cooldownDtos);

    return dto;
  }

  public static SkillManager fromDto(SkillManagerDto dto) {
    SkillManager skillManager = new SkillManager();

    // 학습한 스킬들 복원
    for (SkillDto skillDto : dto.getLearnedSkills()) {
      Skill skill = SkillMapper.fromDto(skillDto);
      if (skill != null) {
        skillManager.learnSkill(skill); // TODO: SkillManager에 이 메서드 필요
      }
    }

    // 쿨다운 정보 복원
    for (SkillCooldownDto cooldownDto : dto.getSkillCooldowns()) {
      skillManager.setSkillCooldown(cooldownDto.getSkillName(), cooldownDto.getRemainingTurns()); // TODO: SkillManager에 이 메서드 필요
    }

    return skillManager;
  }
}
