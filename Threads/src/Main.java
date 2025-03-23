import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(2, 4, 5, TimeUnit.SECONDS, 5, 1);

        for (int i = 0; i < 10; i++) {
            int taskId = i;
            pool.execute(() -> {
                try {
                    System.out.println("Task " + taskId + " started.");
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " finished.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        pool.shutdown();
        // pool.shutdownNow(); // Для тестирования немедленного завершения
    }
}