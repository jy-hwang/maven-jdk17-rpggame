package rpg.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestProgress {
  private final String questId;
  private final Map<String, Integer> progress;
  private final String status;

  @JsonCreator
  public QuestProgress(
//@formatter:off
  @JsonProperty("questId") String questId
, @JsonProperty("progress") Map<String, Integer> progress
, @JsonProperty("status") String status
 //@formatter:on               
  ) {
    this.questId = questId;
    this.progress = progress != null ? progress : new HashMap<>();
    this.status = status != null ? status : "ACTIVE";
  }

  public String getQuestId() {
    return questId;
  }

  public Map<String, Integer> getProgress() {
    return progress;
  }

  public String getStatus() {
    return status;
  }
}
