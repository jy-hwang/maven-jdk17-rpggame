package rpg.infrastructure.data.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.application.service.QuestManager;
import rpg.domain.quest.Quest;
import rpg.shared.dto.quest.QuestDto;
import rpg.shared.dto.quest.QuestManagerDto;

public class QuestManagerMapper {
  private static final Logger logger = LoggerFactory.getLogger(QuestManagerMapper.class);

  /**
   * QuestManager를 QuestManagerDto로 변환
   */
  public static QuestManagerDto toDto(QuestManager questManager) {
    if (questManager == null) {
      logger.warn("QuestManager가 null입니다. 기본 DTO 반환");
      return new QuestManagerDto(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    try {
      // 각 퀘스트 리스트를 DTO로 변환
      List<QuestDto> availableQuestDtos = questManager.getAvailableQuests().stream().map(QuestMapper::toDto).collect(Collectors.toList());

      List<QuestDto> activeQuestDtos = questManager.getActiveQuests().stream().map(QuestMapper::toDto).collect(Collectors.toList());

      List<QuestDto> completedQuestDtos = questManager.getCompletedQuests().stream().map(QuestMapper::toDto).collect(Collectors.toList());

      QuestManagerDto dto = new QuestManagerDto(availableQuestDtos, activeQuestDtos, completedQuestDtos);

      logger.debug("QuestManagerDto 변환 완료: 사용가능 {}개, 활성 {}개, 완료 {}개", availableQuestDtos.size(), activeQuestDtos.size(), completedQuestDtos.size());

      return dto;

    } catch (Exception e) {
      logger.error("QuestManager -> DTO 변환 실패", e);
      return new QuestManagerDto(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
  }

  /**
   * QuestManagerDto를 QuestManager로 변환 (수정된 버전)
   */
  public static QuestManager fromDto(QuestManagerDto dto) {
    if (dto == null) {
      logger.warn("QuestManagerDto가 null입니다. 새 QuestManager 생성");
      return new QuestManager();
    }

    try {
      // 로드 전용 QuestManager 생성 (기본 퀘스트 없음)
      QuestManager questManager = QuestManager.createForLoading();

      // DTO에서 퀘스트 리스트 변환
      List<Quest> availableQuests = new ArrayList<>();
      List<Quest> activeQuests = new ArrayList<>();
      List<Quest> completedQuests = new ArrayList<>();

      // 사용 가능한 퀘스트 복원
      if (dto.getAvailableQuests() != null) {
        for (QuestDto questDto : dto.getAvailableQuests()) {
          Quest quest = QuestMapper.fromDto(questDto);
          if (quest != null) {
            availableQuests.add(quest);
          }
        }
      }

      // 활성 퀘스트 복원
      if (dto.getActiveQuests() != null) {
        for (QuestDto questDto : dto.getActiveQuests()) {
          Quest quest = QuestMapper.fromDto(questDto);
          if (quest != null) {
            activeQuests.add(quest);
          }
        }
      }

      // 완료된 퀘스트 복원
      if (dto.getCompletedQuests() != null) {
        for (QuestDto questDto : dto.getCompletedQuests()) {
          Quest quest = QuestMapper.fromDto(questDto);
          if (quest != null) {
            completedQuests.add(quest);
          }
        }
      }

      // 한 번에 모든 데이터 교체
      questManager.replaceAllQuestsForLoad(availableQuests, activeQuests, completedQuests);

      // 데이터 검증
      questManager.validateQuestData();

      logger.debug("QuestManager 변환 완료: 사용가능 {}개, 활성 {}개, 완료 {}개", availableQuests.size(), activeQuests.size(), completedQuests.size());

      return questManager;

    } catch (Exception e) {
      logger.error("DTO -> QuestManager 변환 실패", e);
      QuestManager fallbackManager = new QuestManager();
      fallbackManager.validateQuestData();
      return fallbackManager;
    }
  }
}
