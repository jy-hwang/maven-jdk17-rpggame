package dto.mapper;

import dto.ItemStackDto;
import model.GameInventory;
import model.item.GameItem;

public class ItemStackMapper {
  public static ItemStackDto toDto(GameInventory.ItemStack stack) {
    ItemStackDto dto = new ItemStackDto();
    dto.setItem(GameItemMapper.toDto(stack.getItem()));
    dto.setQuantity(stack.getQuantity());
    return dto;
  }

  public static GameInventory.ItemStack fromDto(ItemStackDto dto) {
    GameItem item = GameItemMapper.fromDto(dto.getItem());
    return item != null ? new GameInventory.ItemStack(item, dto.getQuantity()) : null;
  }
}
