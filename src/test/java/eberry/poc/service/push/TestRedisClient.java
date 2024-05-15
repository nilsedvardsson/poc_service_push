package eberry.poc.service.push;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestRedisClient {

    private Jedis jedis = mock(Jedis.class);

    @Test
    public void should_fetch() {

        final String streamName = "MY_STREAM";
        final String deviceId = "DEVICE_ID";
        final String message = "MSG";

        final long time = 1691574780123l;

        System.out.println(Instant.now().getEpochSecond());


        StreamEntryID id = new StreamEntryID(time);
        StreamEntry streamEntry = new StreamEntry(id, Map.of(deviceId, message));
        Map.Entry<String, List<StreamEntry>> entry = new AbstractMap.SimpleEntry<>(streamName, List.of(streamEntry));
        List<Map.Entry<String, List<StreamEntry>>> result = List.of(entry) ;

        given(jedis.xread(any(XReadParams.class), any(Map.class))).willReturn(result);

        RedisClient redisClient = new RedisClient(jedis, streamName);

        ReadResult rr = redisClient.read(jedis,0l);

        assertThat(rr.getLatestTime()).isEqualTo(time);
        assertThat(rr.getMessages().size()).isEqualTo(1);
        assertThat(rr.getMessages().get(0).getKey()).isEqualTo(deviceId);
        assertThat(rr.getMessages().get(0).getValue()).isEqualTo(message);

    }
}
