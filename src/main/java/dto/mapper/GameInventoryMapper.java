package dto.mapper;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dto.GameInventoryDto;
import dto.ItemStackDto;
import model.GameInventory;

public class GameInventoryMapper {
  private static final Logger logger = LoggerFactory.getLogger(GameInventoryMapper.class);
  
  public static GameInventoryDto toDto(GameInventory inventory) {
      GameInventoryDto dto = new GameInventoryDto();
      
      // setter 사용으로 변경
      dto.setMaxSlots(inventory.getMaxSize());
      
      // 아이템 스택들 변환
      List<ItemStackDto> itemDtos = new ArrayList<>();
      for (GameInventory.ItemStack stack : inventory.getItems()) {
          itemDtos.add(ItemStackMapper.toDto(stack));
      }
      dto.setItems(itemDtos);
      
      // 착용 장비 변환
      dto.setEquippedWeapon(GameItemMapper.toDto(inventory.getEquippedWeapon()));
      dto.setEquippedArmor(GameItemMapper.toDto(inventory.getEquippedArmor()));
      dto.setEquippedAccessory(GameItemMapper.toDto(inventory.getEquippedAccessory()));
      
      logger.debug("GameInventoryDto 변환 완료: {}개 아이템", dto.getItems().size());
      return dto;
  }
  
  public static GameInventory fromDto(GameInventoryDto dto) {
      GameInventory inventory = new GameInventory(dto.getMaxSlots());
      
      // 아이템들 복원
      for (ItemStackDto stackDto : dto.getItems()) {
          GameInventory.ItemStack stack = ItemStackMapper.fromDto(stackDto);
          if (stack != null) {
              inventory.addItem(stack.getItem(), stack.getQuantity());
          }
      }
      
      // 착용 장비 복원 (추후 GameInventory에 setter 메서드 필요)
      // TODO: 착용 장비 설정 메서드 구현 필요
      
      logger.debug("GameInventory 변환 완료: {}개 아이템", dto.getItems().size());
      return inventory;
  }
}
