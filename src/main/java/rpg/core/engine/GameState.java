package rpg.core.engine;

/**
 * 게임 상태 클래스
 */
public class GameState {
  private int totalPlayTime;
  private int monstersKilled;
  private int questsCompleted;
  private String currentLocation;

  public GameState() {
    this.currentLocation = "시작 지점";
  }

  // Getters and Setters
  public int getTotalPlayTime() {
    return totalPlayTime;
  }

  public void setTotalPlayTime(int totalPlayTime) {
    this.totalPlayTime = totalPlayTime;
  }

  public int getMonstersKilled() {
    return monstersKilled;
  }

  public void setMonstersKilled(int monstersKilled) {
    this.monstersKilled = monstersKilled;
  }

  public int getQuestsCompleted() {
    return questsCompleted;
  }

  public void setQuestsCompleted(int questsCompleted) {
    this.questsCompleted = questsCompleted;
  }

  public String getCurrentLocation() {
    return currentLocation;
  }

  public void setCurrentLocation(String currentLocation) {
    this.currentLocation = currentLocation;
  }

  // 편의 메서드들
  public void incrementMonstersKilled() {
    this.monstersKilled++;
  }

  public void incrementQuestsCompleted() {
    this.questsCompleted++;
  }

  public void addPlayTime(int minutes) {
    this.totalPlayTime += minutes;
  }

  /**
   * 게임 통계를 표시합니다. (기존 메서드와 동일할 수도 있음)
   */

  // 또는 더 자세한 통계가 필요하다면:
  public void displayGameStats() {
    System.out.println("\n=== 📊 게임 통계 ===");
    System.out.println("📍 마지막 탐험 위치: " + currentLocation);
    System.out.println("⚔️ 처치한 몬스터: " + monstersKilled + "마리");
    System.out.println("📋 완료한 퀘스트: " + questsCompleted + "개");
    System.out.println("⏰ 총 플레이 시간: " + totalPlayTime + "분");

    // 분당 통계 정보로 변경
    if (totalPlayTime > 0) {
      double monstersPerMinute = (double) monstersKilled / totalPlayTime;
      double questsPerMinute = (double) questsCompleted / totalPlayTime;
      System.out.printf("📈 분당 몬스터 처치: %.2f마리%n", monstersPerMinute);
      System.out.printf("📈 분당 퀘스트 완료: %.2f개%n", questsPerMinute);
    }


    System.out.println("==================");
  }
}
