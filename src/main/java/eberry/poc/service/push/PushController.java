package eberry.poc.service.push;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PushController {

    private final static String REDIS_SERVER = "host.docker.internal";
    //private final static String REDIS_SERVER = "localhost";
    private final static String REDIS_STREAM_NAME = "PUSH_STREAM_3";
    private final static String SERVER_NAME = System.getenv("NAME") == null ? "UNKNOWN" : System.getenv("NAME");

    private final Jedis jedis = new Jedis(REDIS_SERVER, 6379, false);
    private final RedisClient redisClient = new RedisClient(jedis, REDIS_STREAM_NAME);

    private Map<String, Sinks.Many> sinks = new HashMap<>();

    public PushController() {

        new Thread(() -> {

            long latestTime = 0l;
            for (;;) {

                ReadResult rr = redisClient.read(jedis, latestTime);

                for (Map.Entry<String, String> entry : rr.getMessages()) {

                    Sinks.Many sink = sinks.get(entry.getKey());

                    if (sink != null) {

                        sink.tryEmitNext(entry.getValue());
                    }
                }

                latestTime = rr.getLatestTime();

            }
        }).start();
    }

    @GetMapping(path = "/subscribe/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> subscribe(@PathVariable final String id, @RequestParam(required = false) final Integer lookbackSeconds) {

        final Sinks.Many sink = Sinks.many().unicast().onBackpressureBuffer();

        long from = (Instant.now().getEpochSecond() - (lookbackSeconds == null ? 0 : lookbackSeconds)) * 1_000;

        Jedis jedis = new Jedis(REDIS_SERVER, 6379, false);


        sink.tryEmitNext("Welcome from " + SERVER_NAME);

        ReadResult rr = redisClient.readNoBlock(jedis, from);

        rr.getMessages().stream()
                .filter(e -> e.getKey().equals(id))
                .forEach(e -> sink.tryEmitNext(e.getValue()));


        sinks.put(id, sink);

        return sink.asFlux();
    }

    @GetMapping(path = "/push/{id}/{msg}")
    public void pushRedis(@PathVariable final String id, @PathVariable final String msg) {

        Jedis jedis = new Jedis(REDIS_SERVER, 6379, false);
        jedis.xadd(REDIS_STREAM_NAME, StreamEntryID.NEW_ENTRY, Map.of(id, msg));
    }

    @GetMapping(path = "/ping")
    public String ping() {
        return "pong from " + SERVER_NAME;
    }
}
