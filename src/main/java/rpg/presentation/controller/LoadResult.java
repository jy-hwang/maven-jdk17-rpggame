package rpg.presentation.controller;

import rpg.core.engine.GameState;
import rpg.domain.player.Player;

/**
 * 로드 결과를 담는 클래스
 */
public class LoadResult {
  private final boolean success;
  private final Player character;
  private final GameState gameState;
  private final int slotNumber;

  public LoadResult(boolean success, Player character, GameState gameState, int slotNumber) {
    this.success = success;
    this.character = character;
    this.gameState = gameState;
    this.slotNumber = slotNumber;
  }

  public boolean isSuccess() {
    return success;
  }

  public Player getCharacter() {
    return character;
  }

  public GameState getGameState() {
    return gameState;
  }

  public int getSlotNumber() {
    return slotNumber;
  }
}
