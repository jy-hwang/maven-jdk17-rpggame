package rpg.infrastructure.data.mapper;

import rpg.domain.inventory.ItemStack;
import rpg.domain.item.GameItem;
import rpg.shared.dto.item.ItemStackDto;

public class ItemStackMapper {
  public static ItemStackDto toDto(ItemStack stack) {
    ItemStackDto dto = new ItemStackDto();
    dto.setItem(GameItemMapper.toDto(stack.getItem()));
    dto.setQuantity(stack.getQuantity());
    return dto;
  }

  public static ItemStack fromDto(ItemStackDto dto) {
    GameItem item = GameItemMapper.fromDto(dto.getItem());
    return item != null ? new ItemStack(item, dto.getQuantity()) : null;
  }
}
