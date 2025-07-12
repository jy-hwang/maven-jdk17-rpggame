package model;

import config.BaseConstant;
import model.item.GameItem;

public class ShopItem {

  private final GameItem item;
  private final int price;
  private int stock;
  private final int maxStock;
  private final ShopItemCategory category;

  public ShopItem(GameItem item, int price, int stock, ShopItemCategory category) {
    this.item = item;
    this.price = price;
    this.stock = stock;
    this.maxStock = stock;
    this.category = category;
  }

  public GameItem getItem() {
    return item;
  }

  public int getPrice() {
    return price;
  }

  public int getStock() {
    return stock;
  }

  public int getMaxStock() {
    return maxStock;
  }

  public ShopItemCategory getCategory() {
    return category;
  }

  public void reduceStock(int amount) {
    this.stock = Math.max(BaseConstant.NUMBER_ZERO, this.stock - amount);
  }

  public void restockTo(int amount) {
    this.stock = amount;
  }

}
