package rpg.shared.dto.quest;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestManagerDto {
  @JsonProperty("availableQuests")
  private List<QuestDto> availableQuests;

  @JsonProperty("activeQuests")
  private List<QuestDto> activeQuests;

  @JsonProperty("completedQuests")
  private List<QuestDto> completedQuests;

  // 기본 생성자
  public QuestManagerDto() {}

  // 생성자
  public QuestManagerDto(List<QuestDto> availableQuests, List<QuestDto> activeQuests, List<QuestDto> completedQuests) {
    this.availableQuests = availableQuests;
    this.activeQuests = activeQuests;
    this.completedQuests = completedQuests;
  }

  // Getters and Setters
  public List<QuestDto> getAvailableQuests() {
    return availableQuests;
  }

  public void setAvailableQuests(List<QuestDto> availableQuests) {
    this.availableQuests = availableQuests;
  }

  public List<QuestDto> getActiveQuests() {
    return activeQuests;
  }

  public void setActiveQuests(List<QuestDto> activeQuests) {
    this.activeQuests = activeQuests;
  }

  public List<QuestDto> getCompletedQuests() {
    return completedQuests;
  }

  public void setCompletedQuests(List<QuestDto> completedQuests) {
    this.completedQuests = completedQuests;
  }
}
