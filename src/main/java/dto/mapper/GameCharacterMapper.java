package dto.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.GameCharacterDto;
import model.GameCharacter;
import model.effect.GameStatusCondition;

public class GameCharacterMapper {
  private static final Logger logger = LoggerFactory.getLogger(GameCharacterMapper.class);

  public static GameCharacterDto toDto(GameCharacter character) {
    GameCharacterDto dto = new GameCharacterDto();

    // setter 사용으로 변경
    dto.setName(character.getName());
    dto.setLevel(character.getLevel());
    dto.setHp(character.getHp());
    dto.setMaxHp(character.getMaxHp());
    dto.setMana(character.getMana());
    dto.setMaxMana(character.getMaxMana());
    dto.setRestoreHp(character.getRestoreHp());
    dto.setRestoreMana(character.getRestoreMana());
    dto.setExp(character.getExp());
    dto.setBaseAttack(character.getBaseAttack());
    dto.setBaseDefense(character.getBaseDefense());
    dto.setGold(character.getGold());
    dto.setInventory(GameInventoryMapper.toDto(character.getInventory()));
    dto.setSkillManager(SkillManagerMapper.toDto(character.getSkillManager()));
    dto.setPlayerStatusCondition(character.getPlayerStatusCondition().name());
    dto.setQuestManager(QuestManagerMapper.toDto(character.getQuestManager()));

    logger.debug("GameCharacterDto 변환 완료: {}", character.getName());
    return dto;
  }

  public static GameCharacter fromDto(GameCharacterDto dto) {
    // GameCharacter의 @JsonCreator 생성자 사용 (getter 사용)
    GameCharacter character = new GameCharacter(dto.getName(), dto.getLevel(), dto.getHp(), dto.getMaxHp(), dto.getMana(), dto.getMaxMana(),
        dto.getRestoreHp(), dto.getRestoreMana(), dto.getExp(), dto.getBaseAttack(), dto.getBaseDefense(), dto.getGold(),
        GameInventoryMapper.fromDto(dto.getInventory()), SkillManagerMapper.fromDto(dto.getSkillManager()),
        GameStatusCondition.valueOf(dto.getPlayerStatusCondition()), QuestManagerMapper.fromDto(dto.getQuestManager()));

    logger.debug("GameCharacter 변환 완료: {}", dto.getName());
    return character;
  }
}
