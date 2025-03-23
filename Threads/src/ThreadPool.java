import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class ThreadPool implements CustomerExecutor {

    private static final Logger logger = Logger.getLogger(ThreadPool.class.getName());

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    private final BlockingQueue<Runnable> taskQueue;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private volatile boolean isShutdown = false;
    
    private final Set<Thread> workers = Collections.synchronizedSet(new HashSet<>());

    public ThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;

        this.taskQueue = new LinkedBlockingQueue<>(queueSize);
        this.threadFactory = new CustomThreadFactory();
        this.rejectedExecutionHandler = new CustomRejectedExecutionHandler();

        for (int i = 0; i < corePoolSize; i++) {
            startWorker();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor is shutdown");
        }

        if (activeThreads.get() < maxPoolSize && taskQueue.size() >= minSpareThreads) {
            startWorker();
        }

        if (!taskQueue.offer(command)) {
            rejectedExecutionHandler.rejectedExecution(command, null);
        } else {
            logger.info("[Pool] Task accepted into queue: " + command.toString());
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("[Pool] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = drainQueue();
        synchronized (workers) {
            for (Thread worker : workers) {
                worker.interrupt();
            }
        }
        logger.info("[Pool] Shutdown now initiated. Remaining tasks: " + remainingTasks.size());
    }

    private List<Runnable> drainQueue() {
        List<Runnable> taskList = new ArrayList<>();
        taskQueue.drainTo(taskList);
        return taskList;
    }

    private void startWorker() {
        if (activeThreads.get() < maxPoolSize) {
            Thread worker = threadFactory.newThread(new CustomWorker());
            workers.add(worker); // Добавляем поток в коллекцию
            worker.start();
            activeThreads.incrementAndGet();
        }
    }

    private class CustomWorker implements Runnable {
        @Override
        public void run() {
            try {
                while (!isShutdown) {
                    Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                    if (task != null) {
                        logger.info("[Worker] " + Thread.currentThread().getName() + " executes " + task.toString());
                        task.run();
                    } else if (activeThreads.get() > corePoolSize) {
                        logger.info("[Worker] " + Thread.currentThread().getName() + " idle timeout, stopping.");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // Поток был прерван, завершаем работу
                logger.info("[Worker] " + Thread.currentThread().getName() + " was interrupted.");
            } finally {
                workers.remove(Thread.currentThread()); // Удаляем поток из коллекции
                activeThreads.decrementAndGet();
                logger.info("[Worker] " + Thread.currentThread().getName() + " terminated.");
            }
        }
    }

    private class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MyPool-worker-" + threadNumber.getAndIncrement());
            logger.info("[ThreadFactory] Creating new thread: " + thread.getName());
            return thread;
        }
    }

    private class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.warning("[Rejected] Task " + r.toString() + " was rejected due to overload!");
        }
    }
}

