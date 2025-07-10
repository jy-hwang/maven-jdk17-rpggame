package service;

import model.Character;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GameData {
  private static final String SAVE_FILE = "data/rpg_save.csv";

  public static void saveGame(Character character) {
    try (FileWriter writer = new FileWriter(SAVE_FILE)) {
      writer.write("name,level,hp,maxHp,exp,attack,defense,gold\n");
      writer.write(character.toCsv() + "\n");
      System.out.println("게임이 저장되었습니다!");
    } catch (IOException e) {
      System.out.println("저장 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  public static Character loadGame() {
    try (BufferedReader reader = new BufferedReader(new FileReader(SAVE_FILE))) {
      String header = reader.readLine();
      String data = reader.readLine();
      if (data != null && !data.isEmpty()) {
        return Character.fromCsv(data);
      }
    } catch (IOException e) {
      System.out.println("저장 파일을 찾을 수 없습니다.");
    }
    return null;
  }
}
