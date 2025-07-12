package dto.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.BaseConstant;
import dto.SaveGameDto;
import model.GameCharacter;
import model.factory.GameItemFactory;
import service.GameDataService;

public class SaveGameMapper {
  private static final Logger logger = LoggerFactory.getLogger(SaveGameMapper.class);

  private static final GameItemFactory itemFactory = GameItemFactory.getInstance();

  /**
   * 도메인 모델을 DTO로 변환 (저장용)
   */
  public static SaveGameDto toDto(GameCharacter character, GameDataService.GameState gameState, int slotNumber) {
    SaveGameDto dto = new SaveGameDto();

    // setter 사용
    dto.setCharacter(GameCharacterMapper.toDto(character));
    dto.setGameState(GameStateMapper.toDto(gameState));
    dto.setSaveTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    dto.setVersion(String.valueOf(BaseConstant.GAME_VERSION));
    dto.setSlotNumber(slotNumber);

    logger.debug("SaveGameDto 변환 완료: 캐릭터 {}", character.getName());
    return dto;
  }

  /**
   * DTO를 도메인 모델로 변환 (로드용)
   */
  public static GameDataService.SaveData fromDto(SaveGameDto dto) {
    GameCharacter character = GameCharacterMapper.fromDto(dto.getCharacter());
    GameDataService.GameState gameState = GameStateMapper.fromDto(dto.getGameState());

    // GameDataService.SaveData 생성
    GameDataService.SaveData saveData = new GameDataService.SaveData(character, gameState, dto.getSlotNumber());
    saveData.setSaveTime(dto.getSaveTime());
    saveData.setVersion(dto.getVersion());

    logger.debug("SaveData 변환 완료: 캐릭터 {}", character.getName());
    return saveData;
  }
}
