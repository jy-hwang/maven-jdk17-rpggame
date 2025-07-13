package rpg.domain.skill;

/**
 * 스킬 사용 결과 클래스
 */
public class SkillResult {
  private final boolean success;
  private final String message;
  private final int value; // 데미지 또는 힐량

  public SkillResult(boolean success, String message, int value) {
    this.success = success;
    this.message = message;
    this.value = value;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public int getValue() {
    return value;
  }
}
