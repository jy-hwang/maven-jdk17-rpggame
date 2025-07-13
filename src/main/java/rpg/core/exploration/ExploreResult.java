package rpg.core.exploration;

public class ExploreResult {
  public enum ResultType {
    BATTLE_VICTORY, BATTLE_DEFEAT, BATTLE_ESCAPED, TREASURE, KNOWLEDGE, MERCHANT, REST, ERROR
  }

  private final ResultType type;
  private final String message;

  public ExploreResult(ResultType type, String message) {
    this.type = type;
    this.message = message;
  }

  public ResultType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }
}
