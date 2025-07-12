package rpg.infrastructure.data.mapper;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpg.domain.inventory.ItemStack;
import rpg.domain.inventory.PlayerInventory;
import rpg.domain.item.GameEquipment;
import rpg.shared.dto.item.ItemStackDto;
import rpg.shared.dto.player.PlayerInventoryDto;

public class PlayerInventoryMapper {
  private static final Logger logger = LoggerFactory.getLogger(PlayerInventoryMapper.class);

  public static PlayerInventoryDto toDto(PlayerInventory inventory) {
    PlayerInventoryDto dto = new PlayerInventoryDto();

    // setter 사용으로 변경
    dto.setMaxSlots(inventory.getMaxSize());

    // 아이템 스택들 변환
    List<ItemStackDto> itemDtos = new ArrayList<>();
    for (ItemStack stack : inventory.getItems()) {
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

  public static PlayerInventory fromDto(PlayerInventoryDto dto) {
    PlayerInventory inventory = new PlayerInventory(dto.getMaxSlots());

    // 아이템들 복원
    for (ItemStackDto stackDto : dto.getItems()) {
      ItemStack stack = ItemStackMapper.fromDto(stackDto);
      if (stack != null) {
        inventory.addItem(stack.getItem(), stack.getQuantity());
      }
    }

    // 🔥 착용 장비 복원 (수정된 부분)
    if (dto.getEquippedWeapon() != null) {
      GameEquipment weapon = (GameEquipment) GameItemMapper.fromDto(dto.getEquippedWeapon());
      if (weapon != null) {
        inventory.setEquippedWeapon(weapon);
        logger.debug("무기 복원: {}", weapon.getName());
      }
    }

    if (dto.getEquippedArmor() != null) {
      GameEquipment armor = (GameEquipment) GameItemMapper.fromDto(dto.getEquippedArmor());
      if (armor != null) {
        inventory.setEquippedArmor(armor);
        logger.debug("방어구 복원: {}", armor.getName());
      }
    }

    if (dto.getEquippedAccessory() != null) {
      GameEquipment accessory = (GameEquipment) GameItemMapper.fromDto(dto.getEquippedAccessory());
      if (accessory != null) {
        inventory.setEquippedAccessory(accessory);
        logger.debug("장신구 복원: {}", accessory.getName());
      }
    }

    logger.debug("GameInventory 변환 완료: {}개 아이템, 착용장비 복원", dto.getItems().size());
    return inventory;
  }
}
