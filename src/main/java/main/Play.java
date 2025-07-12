package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import controller.Game;

public class Play {

  private static final Logger logger = LoggerFactory.getLogger(Play.class);

  /**
   * 프로그램 진입점
   * 
   * @param args 명령행 인수
   */
  public static void main(String[] args) {
    try {
      logger.info("RPG 게임 애플리케이션 시작");

      // JVM 종료 시 정리 작업을 위한 셧다운 훅 등록
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        logger.info("애플리케이션 종료 - 정리 작업 수행");
        System.out.println("\n게임을 종료합니다...");
      }));

      // 게임 인스턴스 생성 및 시작
      Game game = new Game();
      game.start();

      logger.info("RPG 게임 애플리케이션 정상 종료");

    } catch (Exception e) {
      logger.error("애플리케이션 실행 중 치명적 오류 발생", e);
      System.err.println("게임 실행 중 치명적인 오류가 발생했습니다.");
      System.err.println("오류 내용: " + e.getMessage());
      System.err.println("로그 파일을 확인해주세요.");

      // 비정상 종료
      System.exit(1);
    }
  }

}
