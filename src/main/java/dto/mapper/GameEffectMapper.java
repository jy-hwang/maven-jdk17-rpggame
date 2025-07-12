package dto.mapper;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.GameEffectDto;
import model.effect.GameEffect;
import model.factory.GameEffectFactory;

public class GameEffectMapper {
  private static final Logger logger = LoggerFactory.getLogger(GameEffectMapper.class);

  public static List<GameEffectDto> toDtoList(List<GameEffect> effects) {
    List<GameEffectDto> dtoList = new ArrayList<>();
    for (GameEffect effect : effects) {
      dtoList.add(toDto(effect));
    }
    return dtoList;
  }

  public static GameEffectDto toDto(GameEffect effect) {
    GameEffectDto dto = new GameEffectDto();
    dto.setType(effect.getType().name());
    dto.setValue(effect.getValue());
    dto.setDescription(effect.getDescription());
    return dto;
  }

  public static List<GameEffect> fromDtoList(List<GameEffectDto> dtoList) {
    if (dtoList == null)
      return new ArrayList<>();

    List<GameEffect> effects = new ArrayList<>();
    for (GameEffectDto dto : dtoList) {
      GameEffect effect = fromDto(dto);
      if (effect != null) {
        effects.add(effect);
      }
    }
    return effects;
  }

  public static GameEffect fromDto(GameEffectDto dto) {
    try {
      // GameEffectFactory를 사용하여 효과 생성
      return GameEffectFactory.createEffect(dto.getType(), dto.getValue());
    } catch (Exception e) {
      logger.error("GameEffect 변환 실패: {}", dto.getType(), e);
      return null;
    }
  }
}
