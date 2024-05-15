package eberry.poc.service.push;

import java.util.List;
import java.util.Map;

public class ReadResult {

    private final long latestTime;

    private final List<Map.Entry<String, String>> messages;

    public ReadResult(final long latestTime, final List<Map.Entry<String, String>> messages) {

        this.latestTime = latestTime;
        this.messages = messages;
    }

    public long getLatestTime() {

        return latestTime;
    }

    public List<Map.Entry<String, String>> getMessages() {

        return messages;
    }
}
