package dto.mapper;

import dto.GameStateDto;
import service.GameDataService;

public class GameStateMapper {
  public static GameStateDto toDto(GameDataService.GameState gameState) {
    GameStateDto dto = new GameStateDto();

    // setter 사용으로 변경
    dto.setTotalPlayTime(gameState.getTotalPlayTime());
    dto.setMonstersKilled(gameState.getMonstersKilled());
    dto.setQuestsCompleted(gameState.getQuestsCompleted());
    dto.setCurrentLocation(gameState.getCurrentLocation());

    return dto;
  }

  public static GameDataService.GameState fromDto(GameStateDto dto) {
    GameDataService.GameState gameState = new GameDataService.GameState();
    gameState.setTotalPlayTime(dto.getTotalPlayTime());
    gameState.setMonstersKilled(dto.getMonstersKilled());
    gameState.setQuestsCompleted(dto.getQuestsCompleted());
    gameState.setCurrentLocation(dto.getCurrentLocation());
    return gameState;
  }
}
