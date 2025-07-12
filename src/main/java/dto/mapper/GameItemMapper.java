package dto.mapper;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.GameItemDto;
import model.effect.GameEffect;
import model.item.GameConsumable;
import model.item.GameEquipment;
import model.item.GameItem;
import model.item.ItemRarity;

public class GameItemMapper {
  private static final Logger logger = LoggerFactory.getLogger(GameItemMapper.class);

  public static GameItemDto toDto(GameItem item) {
    if (item == null)
      return null;

    GameItemDto dto = new GameItemDto();

    // setter 사용으로 변경
    dto.setName(item.getName());
    dto.setDescription(item.getDescription());
    dto.setValue(item.getValue());
    dto.setRarity(item.getRarity().name());

    // 타입별 처리
    if (item instanceof GameConsumable consumable) {
      dto.setItemType("CONSUMABLE");
      dto.setEffects(GameEffectMapper.toDtoList(consumable.getEffects()));
      dto.setCooldown(consumable.getCooldown());
    } else if (item instanceof GameEquipment equipment) {
      dto.setItemType("EQUIPMENT");
      dto.setEquipmentType(equipment.getEquipmentType().name());
      dto.setAttackBonus(equipment.getAttackBonus());
      dto.setDefenseBonus(equipment.getDefenseBonus());
      dto.setHpBonus(equipment.getHpBonus());
    }

    return dto;
  }

  public static GameItem fromDto(GameItemDto dto) {
    if (dto == null)
      return null;

    try {
      ItemRarity rarity = ItemRarity.valueOf(dto.getRarity());

      if ("CONSUMABLE".equals(dto.getItemType())) {
        List<GameEffect> effects = GameEffectMapper.fromDtoList(dto.getEffects());
        return new GameConsumable(dto.getName(), dto.getDescription(), dto.getValue(), rarity, effects, dto.getCooldown());
      } else if ("EQUIPMENT".equals(dto.getItemType())) {
        GameEquipment.EquipmentType equipType = GameEquipment.EquipmentType.valueOf(dto.getEquipmentType());
        return new GameEquipment(dto.getName(), dto.getDescription(), dto.getValue(), rarity, equipType, dto.getAttackBonus(), dto.getDefenseBonus(),
            dto.getHpBonus());
      }
    } catch (Exception e) {
      logger.error("GameItem 변환 실패: {}", dto.getName(), e);
    }

    return null;
  }
}
