package rpg.core.exploration;

/**
 * 탐험 결과 데이터를 담는 래퍼 클래스
 * ExploreResult enum과 추가 메시지를 포함
 */
public class ExploreResultData {
  private final ExploreResult result;
  private final String customMessage;

  public ExploreResultData(ExploreResult result, String customMessage) {
    this.result = result;
    this.customMessage = customMessage;
  }

  public ExploreResultData(ExploreResult result) {
    this.result = result;
    this.customMessage = result.getDefaultMessage();
  }

  public ExploreResult getResult() {
    return result;
  }

  public String getMessage() {
    return customMessage != null ? customMessage : result.getDefaultMessage();
  }

  public boolean isPositive() {
    return result.isPositive();
  }

  public boolean isBattleResult() {
    return result.isBattleResult();
  }

  public boolean isEventResult() {
    return result.isEventResult();
  }

  public boolean hasReward() {
    return result.hasReward();
  }

  @Override
  public String toString() {
    return String.format("ExploreResultData{result=%s, message='%s'}", result, getMessage());
  }
}

