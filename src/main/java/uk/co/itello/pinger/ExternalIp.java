package uk.co.itello.pinger;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static java.time.Duration.ofSeconds;

@Component
public class ExternalIp {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalIp.class);

    private final String ip;

    public ExternalIp(RestTemplate restTemplate) {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .handle(RestClientException.class)
                .withDelay(ofSeconds(1))
                .withMaxRetries(100);

        String ip = Failsafe.with(retryPolicy).get(() -> {
            LOG.info("Attempting to determine external IP address...");
            String ipFound = restTemplate.getForObject(new URI("http://myexternalip.com/raw"), String.class);
            LOG.info("Found IP address: {}", ipFound);
            return ipFound;
        });

        if (ip == null) {
            throw new IllegalStateException("Unable to determine external ip address");
        }

        this.ip = ip.replace("\n", "");
    }

    public String getIp() {
        return ip;
    }
}
