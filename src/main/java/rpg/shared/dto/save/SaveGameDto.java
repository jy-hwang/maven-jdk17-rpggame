package rpg.shared.dto.save;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rpg.shared.dto.player.PlayerDto;

public class SaveGameDto {
  private PlayerDto character;
  private GameStateDto gameState;
  private String saveTime;
  private String version;
  private int slotNumber;

  // 기본 생성자
  public SaveGameDto() {}

  // Jackson 생성자
  @JsonCreator
  public SaveGameDto(
//@formatter:off
  @JsonProperty("character") PlayerDto character
, @JsonProperty("gameState") GameStateDto gameState
, @JsonProperty("saveTime") String saveTime
, @JsonProperty("version") String version
, @JsonProperty("slotNumber") int slotNumber
//@formatter:on
  ) {
    this.character = character;
    this.gameState = gameState;
    this.saveTime = saveTime;
    this.version = version;
    this.slotNumber = slotNumber;
  }

  // Getters and Setters
  public PlayerDto getCharacter() {
    return character;
  }

  public void setCharacter(PlayerDto character) {
    this.character = character;
  }

  public GameStateDto getGameState() {
    return gameState;
  }

  public void setGameState(GameStateDto gameState) {
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
