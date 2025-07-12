package rpg.infrastructure.persistence;

/**
 * 저장 슬롯 정보 클래스
 */
public class SaveSlotInfo {
  private final int slotNumber;
  private final boolean occupied;
  private final String characterName;
  private final int characterLevel;
  private final String saveTime;
  private final int playTime;

  public SaveSlotInfo(int slotNumber, boolean occupied, String characterName, int characterLevel, String saveTime, int playTime) {
    this.slotNumber = slotNumber;
    this.occupied = occupied;
    this.characterName = characterName;
    this.characterLevel = characterLevel;
    this.saveTime = saveTime;
    this.playTime = playTime;
  }

  // Getters
  public int getSlotNumber() {
    return slotNumber;
  }

  public boolean isOccupied() {
    return occupied;
  }

  public String getCharacterName() {
    return characterName;
  }

  public int getCharacterLevel() {
    return characterLevel;
  }

  public String getSaveTime() {
    return saveTime;
  }

  public int getPlayTime() {
    return playTime;
  }
}
