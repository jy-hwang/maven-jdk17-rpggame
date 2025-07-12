package rpg.infrastructure.data.mapper;

import rpg.core.engine.GameState;
import rpg.shared.dto.save.GameStateDto;

public class GameStateMapper {
  public static GameStateDto toDto(GameState gameState) {
    GameStateDto dto = new GameStateDto();

    // setter 사용으로 변경
    dto.setTotalPlayTime(gameState.getTotalPlayTime());
    dto.setMonstersKilled(gameState.getMonstersKilled());
    dto.setQuestsCompleted(gameState.getQuestsCompleted());
    dto.setCurrentLocation(gameState.getCurrentLocation());

    return dto;
  }

  public static GameState fromDto(GameStateDto dto) {
    GameState gameState = new GameState();
    gameState.setTotalPlayTime(dto.getTotalPlayTime());
    gameState.setMonstersKilled(dto.getMonstersKilled());
    gameState.setQuestsCompleted(dto.getQuestsCompleted());
    gameState.setCurrentLocation(dto.getCurrentLocation());
    return gameState;
  }
}
