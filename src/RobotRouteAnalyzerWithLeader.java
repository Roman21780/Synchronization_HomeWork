import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // Потокобезопасная мапа
import java.util.concurrent.ExecutorService;   // Управление потоками
import java.util.concurrent.Executors;          // Фабрика потоков
import java.util.concurrent.TimeUnit;           // Единицы времени

public class RobotRouteAnalyzerWithLeader {
    public static final Map<Integer, Integer> sizeToFreq = new ConcurrentHashMap<>();
    private static final Object monitor = new Object();
    private static volatile boolean running = true; // Флаг работы потока-лидера

    public static void main(String[] args) throws InterruptedException {
        // Поток для вывода текущего лидера
        Thread leaderThread = new Thread(() -> {
            printCurrentLeader();

            while (running) {
                synchronized (monitor) {
                    try {
                        // Ждем сигнала от рабочих потоков
                        monitor.wait(50); // добавляем таймаут для периодической проверки
                        printCurrentLeader();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Восстановление флага
                        break;
                    }
                }
            }
            System.out.println("Поток вывода лидера завершен");
        });

        leaderThread.start(); // Запуск потока-лидера

        int threadsCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> { // Добавление задачи в пул
                String route = generateRoute("RLRFR", 100);
                int rCount = countChar(route, 'R');

                // Атомарное обновление статистики
                sizeToFreq.merge(rCount, 1, Integer::sum);

                // Уведомляем поток вывода лидера
                synchronized (monitor) {
                    monitor.notify(); // Уведомление потока-лидера
                }

                System.out.println("Количество R в маршруте: " + rCount);
            });
        }

        executor.shutdown(); // Остановка приема новых задач
        executor.awaitTermination(1, TimeUnit.MINUTES); // Ожидание завершения

        running = false; // Сигнал остановки
        leaderThread.interrupt(); // Прерывание потока-лидера
        leaderThread.join(); // Ожидание завершения

        printStatistics();
    }

    // Находим текущего лидера
    private static void printCurrentLeader() {
        Optional<Map.Entry<Integer, Integer>> maxEntry = sizeToFreq.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue());

        if (maxEntry.isPresent()) {
            System.out.printf("[ЛИДЕР] %d (встретилось %d раз)%n",
                    maxEntry.get().getKey(), maxEntry.get().getValue());
        } else {
            System.out.println("[ЛИДЕР] Данные еще не доступны");
        }
    }

    public static String generateRoute(String letters, int length) {
        Random random = new Random();
        StringBuilder route = new StringBuilder();
        for (int i = 0; i < length; i++) {
            route.append(letters.charAt(random.nextInt(letters.length()))); // Случайный символ
        }
        return route.toString();
    }

    private static int countChar(String route, char r) {
        int count = 0;
        for (int i = 0; i < route.length(); i++) {
            if (route.charAt(i) == r) {
                count++;
            }
        }
        return count;
    }

    private static void printStatistics() {
        if (sizeToFreq.isEmpty()) {
            System.out.println("Нет данных для статистики");
            return;
        }

        // Находим наиболее часто встречающуюся частоту
        Map.Entry<Integer, Integer> maxEntry = Collections.max(
                sizeToFreq.entrySet(),
                Map.Entry.comparingByValue()
        );

        System.out.println("\nСамое частое количество повторений " +
                maxEntry.getKey() + " (встретилось " + maxEntry.getValue() + " раз)");

        // Выводим остальные значения, исключая максимальное
        System.out.println("Другие размеры:");
        sizeToFreq.entrySet().stream()
                .filter(entry -> !entry.equals(maxEntry)) // Исключаем лидера
                .sorted(Map.Entry.comparingByKey())         // Сортировка по ключу
                .forEach(entry ->                           // Вывод каждого элемента
                        System.out.println("- " + entry.getKey() + " (" + entry.getValue() + " раз)"));
    }
}
