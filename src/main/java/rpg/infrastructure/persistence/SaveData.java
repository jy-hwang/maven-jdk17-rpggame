package rpg.infrastructure.persistence;

import rpg.core.engine.GameState;
import rpg.domain.player.Player;

/**
 * 저장 데이터 래핑 클래스
 */
public class SaveData {
  private Player character;
  private GameState gameState;
  private String saveTime;
  private String version;
  private int slotNumber;

  public SaveData() {}

  public SaveData(Player character, GameState gameState, int slotNumber) {
    this.character = character;
    this.gameState = gameState;
    this.slotNumber = slotNumber;
  }

  // Getters and Setters
  public Player getCharacter() {
    return character;
  }

  public void setCharacter(Player character) {
    this.character = character;
  }

  public GameState getGameState() {
    return gameState;
  }

  public void setGameState(GameState gameState) {
    this.gameState = gameState;
  }

  public String getSaveTime() {
    return saveTime;
  }

  public void setSaveTime(String saveTime) {
    this.saveTime = saveTime;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public int getSlotNumber() {
    return slotNumber;
  }

  public void setSlotNumber(int slotNumber) {
    this.slotNumber = slotNumber;
  }
}
