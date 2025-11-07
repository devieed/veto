package org.veto.shared;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 延迟任务，给定任务的延迟时间，在任务到期后触发事件
 *
 * @author ray.zhou
 */
public class DelayUtil implements Runnable {
    private static final int LOOP_TIMEOUT_MS = 500;

    private final Map<String, Task<?>> taskMap = new ConcurrentSkipListMap<>();
    private final DelayQueue<Task<?>> delayQueue = new DelayQueue<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private AtomicBoolean running = new AtomicBoolean(false);

    public DelayUtil() {
        new Thread(this).start();
        running.set(true);
    }

    public int getCount() {
        return delayQueue.size();
    }

    public void shutdown() {
        running.set(false);
        executorService.shutdownNow();  // 停止线程池
        delayQueue.clear();  // 清理队列中的任务
    }

    public void add(Task<?> task) {
        if (running.get()) {
            if (taskMap.containsKey(task.id)){
                taskMap.remove(task.id);
                delayQueue.remove(task);
            }
            delayQueue.offer(task);
            taskMap.put(task.id, task);
        } else {
            throw new RuntimeException("Queue is stopped...");
        }
    }

    public boolean contains(String id) {
        return taskMap.containsKey(id);
    }

    public <T> T getMeta(String id){
        return (T) taskMap.get(id).meta;
    }

    public void remove(String id) {
        Task<?> task = taskMap.get(id);
        if (task != null) {
            delayQueue.remove(task);
        }
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Task<?> task = delayQueue.poll(LOOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (task != null) {
                    taskMap.remove(task.id);
                    executorService.submit(() -> {
                        try {
                            task.f.call(task.meta);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public interface Func<T> {
        void call(T t);
    }

    public static class Task<T extends Serializable> implements Delayed {
        private final String id;
        private final T meta;
        private final long expireAt;
        private final Func f;

        public Task(T meta, Func<T> f, long expireAt, String id) {
            this.meta = meta;
            this.expireAt = expireAt;
            this.f = f;
            this.id = id;
        }

        public Task(T meta, Func<T> f, Date expireAt, String id) {
            this.meta = meta;
            this.expireAt = expireAt.getTime();
            this.f = f;
            this.id = id;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.expireAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Task) {
                Task<?> task = (Task<?>) obj;
                return task.id.equalsIgnoreCase(this.id);
            }
            return false;
        }

        @Override
        public int compareTo(Delayed o) {
            long delay1 = this.getDelay(TimeUnit.MILLISECONDS);
            long delay2 = o.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(delay1, delay2);
        }
    }
}
