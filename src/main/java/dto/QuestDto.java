package dto;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestDto {
  @JsonProperty("id")
  private String id;

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  @JsonProperty("type")
  private String type;

  @JsonProperty("requiredLevel")
  private int requiredLevel;

  @JsonProperty("objectives")
  private Map<String, Integer> objectives;

  @JsonProperty("currentProgress")
  private Map<String, Integer> currentProgress;

  @JsonProperty("reward")
  private QuestRewardDto reward;

  @JsonProperty("status")
  private String status;

  // 기본 생성자
  public QuestDto() {}

  // 생성자
  public QuestDto(String id, String title, String description, String type, int requiredLevel, Map<String, Integer> objectives,
      Map<String, Integer> currentProgress, QuestRewardDto reward, String status) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.type = type;
    this.requiredLevel = requiredLevel;
    this.objectives = objectives;
    this.currentProgress = currentProgress;
    this.reward = reward;
    this.status = status;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public void setRequiredLevel(int requiredLevel) {
    this.requiredLevel = requiredLevel;
  }

  public Map<String, Integer> getObjectives() {
    return objectives;
  }

  public void setObjectives(Map<String, Integer> objectives) {
    this.objectives = objectives;
  }

  public Map<String, Integer> getCurrentProgress() {
    return currentProgress;
  }

  public void setCurrentProgress(Map<String, Integer> currentProgress) {
    this.currentProgress = currentProgress;
  }

  public QuestRewardDto getReward() {
    return reward;
  }

  public void setReward(QuestRewardDto reward) {
    this.reward = reward;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
