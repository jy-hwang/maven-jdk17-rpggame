package rpg.domain.location;

public enum DangerLevel {
  //@formatter:off
    EASY("쉬움", "🟢"),
    NORMAL("보통", "🟡"),
    HARD("어려움", "🟠"),
    VERY_HARD("매우 어려움", "🔴"),
    EXTREME("극한", "🟣"),
    NIGHTMARE("악몽", "⚫"),
    DIVINE("신성", "⚪"),
    IMPOSSIBLE("불가능", "💀");
  //@formatter:on
  private final String displayName;
  private final String emoji;

  DangerLevel(String displayName, String emoji) {
    this.displayName = displayName;
    this.emoji = emoji;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmoji() {
    return emoji;
  }
}
