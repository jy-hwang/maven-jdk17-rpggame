package rpg.infrastructure.data.mapper;

import rpg.domain.skill.Skill;
import rpg.domain.skill.SkillType;
import rpg.shared.dto.player.SkillDto;

public class SkillMapper {
  public static SkillDto toDto(Skill skill) {
    SkillDto dto = new SkillDto();

    // setter 사용으로 변경
    dto.setName(skill.getName());
    dto.setDescription(skill.getDescription());
    dto.setType(skill.getType().name());
    dto.setRequiredLevel(skill.getRequiredLevel());
    dto.setManaCost(skill.getManaCost());
    dto.setCooldown(skill.getCooldown());
    dto.setDamageMultiplier(skill.getDamageMultiplier());
    dto.setHealAmount(skill.getHealAmount());
    dto.setBuffDuration(skill.getBuffDuration());

    return dto;
  }

  public static Skill fromDto(SkillDto dto) {
    return new Skill(dto.getName(), dto.getDescription(), SkillType.valueOf(dto.getType()), dto.getRequiredLevel(), dto.getManaCost(), dto.getCooldown(), dto.getDamageMultiplier(),
        dto.getHealAmount(), dto.getBuffDuration());
  }
}
