package rpg.infrastructure.data.mapper;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.quest.Quest;
import rpg.domain.quest.QuestReward;
import rpg.shared.dto.quest.QuestDto;

public class QuestMapper {
  private static final Logger logger = LoggerFactory.getLogger(QuestMapper.class);

  /**
   * Quest를 QuestDto로 변환
   */
  public static QuestDto toDto(Quest quest) {
    if (quest == null) {
      return null;
    }

    try {
      QuestDto dto = new QuestDto();
      dto.setId(quest.getId());
      dto.setTitle(quest.getTitle());
      dto.setDescription(quest.getDescription());
      dto.setType(quest.getType().name());
      dto.setRequiredLevel(quest.getRequiredLevel());
      dto.setObjectives(new HashMap<>(quest.getObjectives()));
      dto.setCurrentProgress(new HashMap<>(quest.getCurrentProgress()));
      dto.setReward(QuestRewardMapper.toDto(quest.getReward()));
      dto.setStatus(quest.getStatus().name());

      return dto;

    } catch (Exception e) {
      logger.error("Quest -> DTO 변환 실패: {}", quest.getTitle(), e);
      return null;
    }
  }

  /**
   * QuestDto를 Quest로 변환
   */
  public static Quest fromDto(QuestDto dto) {
    if (dto == null) {
      return null;
    }

    try {
      // Quest 생성
      QuestReward reward = QuestRewardMapper.fromDto(dto.getReward());
      Quest quest = new Quest(dto.getId(), dto.getTitle(), dto.getDescription(), Quest.QuestType.valueOf(dto.getType()), dto.getRequiredLevel(),
          dto.getObjectives(), reward);

      // 진행도와 상태 복원
      if (dto.getCurrentProgress() != null) {
        for (Map.Entry<String, Integer> entry : dto.getCurrentProgress().entrySet()) {
          quest.setProgress(entry.getKey(), entry.getValue());
        }
      }

      if (dto.getStatus() != null) {
        quest.setStatus(Quest.QuestStatus.valueOf(dto.getStatus()));
      }

      return quest;

    } catch (Exception e) {
      logger.error("DTO -> Quest 변환 실패: {}", dto.getTitle(), e);
      return null;
    }
  }
}
