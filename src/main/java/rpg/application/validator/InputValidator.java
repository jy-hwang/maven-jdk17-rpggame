package rpg.application.validator;

import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputValidator {
  private static final Logger logger = LoggerFactory.getLogger(InputValidator.class);
  private static final Scanner scanner = new Scanner(System.in);

  public static int getIntInput(String prompt, int min, int max) {
    while (true) {
      try {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
          System.out.println("입력이 비어있습니다. 다시 입력해주세요.");
          logger.debug("빈 입력 감지: {}", prompt);
          continue;
        }

        int value = Integer.parseInt(input);

        if (value < min || value > max) {
          System.out.printf("입력값이 범위를 벗어났습니다. %d~%d 사이의 값을 입력해주세요.%n", min, max);
          logger.debug("범위 벗어난 입력: {} (범위: {}-{})", value, min, max);
          continue;
        }

        logger.debug("유효한 정수 입력: {}", value);
        return value;

      } catch (NumberFormatException e) {
        System.out.println("잘못된 형식입니다. 숫자를 입력해주세요.");
        logger.debug("숫자 형식 오류: {}", e.getMessage());
      }
    }
  }

  public static String getStringInput(String prompt, int minLength, int maxLength) {
    while (true) {
      try {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        if (input.length() < minLength) {
          System.out.printf("입력이 너무 짧습니다. 최소 %d자 이상 입력해주세요.%n", minLength);
          logger.debug("입력 길이 부족: {} (최소: {})", input.length(), minLength);
          continue;
        }

        if (input.length() > maxLength) {
          System.out.printf("입력이 너무 깁니다. 최대 %d자까지 입력 가능합니다.%n", maxLength);
          logger.debug("입력 길이 초과: {} (최대: {})", input.length(), maxLength);
          continue;
        }

        // 특수문자 제한 (선택사항)
        if (!input.matches("^[가-힣a-zA-Z0-9\\s]+$")) {
          System.out.println("한글, 영문, 숫자, 공백만 사용 가능합니다.");
          logger.debug("유효하지 않은 문자 포함: {}", input);
          continue;
        }

        logger.debug("유효한 문자열 입력: {}", input);
        return input;

      } catch (Exception e) {
        System.out.println("입력 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        logger.error("문자열 입력 처리 오류", e);
      }
    }
  }

  public static boolean getConfirmation(String prompt) {
    while (true) {
      try {
        System.out.print(prompt + " (y/n): ");
        String input = scanner.nextLine().trim().toLowerCase();

        if (input.equals("y") || input.equals("yes") || input.equals("예")) {
          logger.debug("사용자 확인: true");
          return true;
        } else if (input.equals("n") || input.equals("no") || input.equals("아니오")) {
          logger.debug("사용자 확인: false");
          return false;
        } else {
          System.out.println("y(예) 또는 n(아니오)를 입력해주세요.");
          logger.debug("유효하지 않은 확인 입력: {}", input);
        }

      } catch (Exception e) {
        System.out.println("입력 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        logger.error("확인 입력 처리 오류", e);
      }
    }
  }


  public static void waitForAnyKey(String message) {
    try {
      System.out.print(message);
      scanner.nextLine();
      logger.debug("사용자 키 입력 대기 완료");
    } catch (Exception e) {
      logger.error("키 입력 대기 중 오류", e);
    }
  }
}
