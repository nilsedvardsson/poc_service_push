package eberry.poc.service.push;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisClient {

    private final String streamName;

    public RedisClient(final Jedis jedis, final String streamName) {

        this.streamName = streamName;
    }

    public ReadResult read(final Jedis jedis, final long from) {

        XReadParams params = XReadParams.xReadParams();
        params.block(0);
        params.count(10);

        List<Map.Entry<String, List<StreamEntry>>> entries = jedis.xread(params, Map.of(streamName, new StreamEntryID(from)));

        return asResult(entries, from);
    }

    public ReadResult readNoBlock(final Jedis jedis, final long from) {

        XReadParams params = XReadParams.xReadParams();
        params.count(10);

        List<Map.Entry<String, List<StreamEntry>>> entries = jedis.xread(params, Map.of(streamName, new StreamEntryID(from)));

        return asResult(entries, from);

    }

    private ReadResult asResult(final List<Map.Entry<String, List<StreamEntry>>> entries, final long from) {

        if (entries == null) {
            return new ReadResult(from, List.of());
        }

        List<StreamEntry> streamEntries = entries.stream()
                .filter(entry -> entry.getKey().equals(streamName))
                .map(entry -> entry.getValue())
                .flatMap(List::stream)
                .collect(Collectors.toList());

        long maxTime = streamEntries.stream().mapToLong(streamEntry -> streamEntry.getID().getTime()).max().orElse(from);

        var x = streamEntries.stream()
                .map(streamEntry -> streamEntry.getFields())
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toList());

        ReadResult rr = new ReadResult(maxTime, x);

        return rr;
    }
}
