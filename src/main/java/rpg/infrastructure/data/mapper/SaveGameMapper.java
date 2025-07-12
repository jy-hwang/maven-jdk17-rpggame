package rpg.infrastructure.data.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.factory.GameItemFactory;
import rpg.core.engine.GameState;
import rpg.domain.player.Player;
import rpg.infrastructure.persistence.GameDataRepository;
import rpg.shared.constant.SystemConstants;
import rpg.shared.dto.save.SaveGameDto;

public class SaveGameMapper {
  private static final Logger logger = LoggerFactory.getLogger(SaveGameMapper.class);

  private static final GameItemFactory itemFactory = GameItemFactory.getInstance();

  /**
   * 도메인 모델을 DTO로 변환 (저장용)
   */
  public static SaveGameDto toDto(Player character, GameState gameState, int slotNumber) {
    SaveGameDto dto = new SaveGameDto();

    // setter 사용
    dto.setCharacter(PlayerMapper.toDto(character));
    dto.setGameState(GameStateMapper.toDto(gameState));
    dto.setSaveTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    dto.setVersion(String.valueOf(SystemConstants.GAME_VERSION));
    dto.setSlotNumber(slotNumber);

    logger.debug("SaveGameDto 변환 완료: 캐릭터 {}", character.getName());
    return dto;
  }

  /**
   * DTO를 도메인 모델로 변환 (로드용)
   */
  public static GameDataRepository.SaveData fromDto(SaveGameDto dto) {
    Player character = PlayerMapper.fromDto(dto.getCharacter());
    GameState gameState = GameStateMapper.fromDto(dto.getGameState());

    // GameDataService.SaveData 생성
    GameDataRepository.SaveData saveData = new GameDataRepository.SaveData(character, gameState, dto.getSlotNumber());
    saveData.setSaveTime(dto.getSaveTime());
    saveData.setVersion(dto.getVersion());

    logger.debug("SaveData 변환 완료: 캐릭터 {}", character.getName());
    return saveData;
  }
}
