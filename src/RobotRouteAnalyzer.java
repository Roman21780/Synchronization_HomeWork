import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RobotRouteAnalyzer {
    // Создается HashMap для хранения статистики (количество повторений каждого значения R)
    public static final Map<Integer, Integer> sizeToFreq = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        int threadsCount = 1000;
        // Создается пул из 1000 потоков (ExecutorService)
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                String route = generateRoute("RLRFR", 100);
                int rCount = countChar(route, 'R');

                // Каждый поток обновляет общую мапу sizeToFreq в синхронизированном блоке
                synchronized (sizeToFreq) {
                    sizeToFreq.put(rCount, sizeToFreq.getOrDefault(rCount, 0) + 1);
                }

                System.out.println("Количество R в маршруте: " + rCount);
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        printStatistics();
    }

    // Каждый поток генерирует случайный маршрут длиной 100 символов
    // из букв "R", "L", "F", "R", "R" (метод generateRoute())
    public static String generateRoute(String letters, int length) {
        Random random = new Random();
        StringBuilder route = new StringBuilder();
        for (int i = 0; i < length; i++) {
            route.append(letters.charAt(random.nextInt(letters.length())));
        }
        return route.toString();
    }

    // Подсчитывается количество букв 'R' в маршруте (метод countChar())
    private static int countChar(String route, char r) {
        int count = 0;
        for (int i = 0; i < route.length(); i++) {
            if (route.charAt(i) == r) {
                count++;
            }
        }
        return count;
    }

    // Вывод статистики
    private static void printStatistics() {
        // Доступ к sizeToFreq только в synchronized блоке
        synchronized (sizeToFreq) {
            if (sizeToFreq.isEmpty()) {
                System.out.println("Нет данных для статистики");
                return;
            }

            // Находим запись с максимальным значением
            Map.Entry<Integer, Integer> maxEntry = Collections.max(
                    sizeToFreq.entrySet(),
                    Map.Entry.comparingByValue()
            );

            System.out.println("Самое частое количество повторений " +
                    maxEntry.getKey() + " (встретилось " + maxEntry.getValue() + " раз)");

            // Выводим остальные значения, исключая максимальное
            System.out.println("Другие размеры:");
            sizeToFreq.entrySet().stream()
                    .filter(entry -> !entry.equals(maxEntry)) // Исключаем максимальную запись
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                            System.out.println("- " + entry.getKey() + " (" + entry.getValue() + " раз)"));
        }
    }
}
