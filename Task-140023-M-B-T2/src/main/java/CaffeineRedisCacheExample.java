import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;

public class CaffeineRedisCacheExample {

    private static final Cache<String, String> caffeineCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1000)
            .build(key -> loadDataFromDatabase(key));

    private static final RedisClient redisClient = RedisClient.create("redis://your_redis_endpoint:6379");

    public static void main(String[] args) {
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> redisCommands = connection.sync();

        redisCommands.publish("cache-invalidation", "user1");

        String cachedValue = caffeineCache.getIfPresent("user1");
        if (cachedValue != null) {
            System.out.println("Cache hit: " + cachedValue);
        } else {
            System.out.println("Cache miss. Data loaded from database.");
        }

        connection.close();
        redisClient.shutdown();
    }

    private static String loadDataFromDatabase(String key) {
        return "Data for " + key + " from the database";
    }

    static {
        redisClient.connect().async().subscribe(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                caffeineCache.invalidate(message);
                System.out.println("Cache entry for '" + message + "' invalidated.");
            }
        }, "cache-invalidation");
    }
}

