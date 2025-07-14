package rpg.domain.quest;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestTemplateData {
  private String id;
  private String title;
  private String description;
  private String type;
  private int requiredLevel;
  private String category;
  private Map<String, Integer> objectives;
  private QuestRewardData reward;
  private List<String> prerequisites;
  private List<String> unlocks;
  private boolean isRepeatable;
  private int timeLimit;
  private List<String> tags;
  private List<String> variableTargets;
  private VariableQuantity variableQuantity;

  // 기본 생성자
  public QuestTemplateData() {}

//JsonCreator 생성자 추가
 @JsonCreator
 public QuestTemplateData(
//@formatter:off
 @JsonProperty("id") String id
 , @JsonProperty("title") String title
 , @JsonProperty("description") String description
 , @JsonProperty("type") String type
 , @JsonProperty("requiredLevel") Integer requiredLevel
 , @JsonProperty("category") String category
 , @JsonProperty("objectives") Map<String, Integer> objectives
 , @JsonProperty("reward") QuestRewardData reward
 , @JsonProperty("prerequisites") List<String> prerequisites
 , @JsonProperty("unlocks") List<String> unlocks
 , @JsonProperty("isRepeatable") Boolean isRepeatable
 , @JsonProperty("timeLimit") Integer timeLimit
 , @JsonProperty("tags") List<String> tags
 , @JsonProperty("variableTargets") List<String> variableTargets
 , @JsonProperty("variableQuantity") VariableQuantity variableQuantity
//@formatter:on
 ) {
   this.id = id;
   this.title = title;
   this.description = description;
   this.type = type;
   this.requiredLevel = requiredLevel != null ? requiredLevel : 1;
   this.category = category;
   this.objectives = objectives;
   this.reward = reward;
   this.prerequisites = prerequisites;
   this.unlocks = unlocks;
   this.isRepeatable = isRepeatable != null ? isRepeatable : false;
   this.timeLimit = timeLimit != null ? timeLimit : 0;
   this.tags = tags;
   this.variableTargets = variableTargets;
   this.variableQuantity = variableQuantity;
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Map<String, Integer> getObjectives() {
    return objectives;
  }

  public void setObjectives(Map<String, Integer> objectives) {
    this.objectives = objectives;
  }

  public QuestRewardData getReward() {
    return reward;
  }

  public void setReward(QuestRewardData reward) {
    this.reward = reward;
  }

  public List<String> getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(List<String> prerequisites) {
    this.prerequisites = prerequisites;
  }

  public List<String> getUnlocks() {
    return unlocks;
  }

  public void setUnlocks(List<String> unlocks) {
    this.unlocks = unlocks;
  }

  public boolean isRepeatable() {
    return isRepeatable;
  }

  public void setRepeatable(boolean repeatable) {
    isRepeatable = repeatable;
  }

  public int getTimeLimit() {
    return timeLimit;
  }

  public void setTimeLimit(int timeLimit) {
    this.timeLimit = timeLimit;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> getVariableTargets() {
    return variableTargets;
  }

  public void setVariableTargets(List<String> variableTargets) {
    this.variableTargets = variableTargets;
  }

  public VariableQuantity getVariableQuantity() {
    return variableQuantity;
  }

  public void setVariableQuantity(VariableQuantity variableQuantity) {
    this.variableQuantity = variableQuantity;
  }

}
