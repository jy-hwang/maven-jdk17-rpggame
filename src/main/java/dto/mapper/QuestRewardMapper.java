package dto.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.GameItemDto;
import dto.ItemRewardDto;
import dto.QuestRewardDto;
import model.QuestReward;
import model.item.GameItem;

public class QuestRewardMapper {
  private static final Logger logger = LoggerFactory.getLogger(QuestRewardMapper.class);

  /**
   * QuestReward를 QuestRewardDto로 변환 (수정됨)
   */
  public static QuestRewardDto toDto(QuestReward reward) {
    if (reward == null) {
      return null;
    }

    try {
      // 🔥 Map을 List로 변환
      List<ItemRewardDto> itemRewardDtos = new ArrayList<>();
      if (reward.getItemRewards() != null) {
        for (Map.Entry<GameItem, Integer> entry : reward.getItemRewards().entrySet()) {
          GameItemDto itemDto = GameItemMapper.toDto(entry.getKey());
          if (itemDto != null) {
            itemRewardDtos.add(new ItemRewardDto(itemDto, entry.getValue()));
          }
        }
      }

      return new QuestRewardDto(reward.getExpReward(), reward.getGoldReward(), itemRewardDtos);

    } catch (Exception e) {
      logger.error("QuestReward -> DTO 변환 실패", e);
      return new QuestRewardDto(0, 0, new ArrayList<>());
    }
  }

  /**
   * QuestRewardDto를 QuestReward로 변환 (수정됨)
   */
  public static QuestReward fromDto(QuestRewardDto dto) {
    if (dto == null) {
      return new QuestReward();
    }

    try {
      // 🔥 List를 Map으로 변환
      Map<GameItem, Integer> itemRewards = new HashMap<>();
      if (dto.getItemRewards() != null) {
        for (ItemRewardDto itemRewardDto : dto.getItemRewards()) {
          GameItem item = GameItemMapper.fromDto(itemRewardDto.getItem());
          if (item != null) {
            itemRewards.put(item, itemRewardDto.getQuantity());
          }
        }
      }

      return new QuestReward(dto.getExpReward(), dto.getGoldReward(), itemRewards);

    } catch (Exception e) {
      logger.error("DTO -> QuestReward 변환 실패", e);
      return new QuestReward();
    }
  }
}
