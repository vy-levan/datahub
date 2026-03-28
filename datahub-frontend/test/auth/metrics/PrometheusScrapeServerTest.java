package auth.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PrometheusScrapeServerTest {

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Test
  void getActuatorPrometheus_returnsPrometheusText() throws Exception {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    registry.counter("test.frontend.scrape", "env", "unit").increment();

    int port = freePort();
    HttpServer server = PrometheusScrapeServer.createAndStartForTests(registry, port);
    try {
      URL url = new URL("http://127.0.0.1:" + port + "/actuator/prometheus");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      assertEquals(200, conn.getResponseCode());
      assertTrue(
          conn.getContentType() != null && conn.getContentType().startsWith("text/plain"),
          "content-type: " + conn.getContentType());
      String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(
          body.contains("test_frontend_scrape"),
          "Expected Micrometer counter in scrape body: " + body);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void postActuatorPrometheus_returns405() throws Exception {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    int port = freePort();
    HttpServer server = PrometheusScrapeServer.createAndStartForTests(registry, port);
    try {
      URL url = new URL("http://127.0.0.1:" + port + "/actuator/prometheus");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      assertEquals(405, conn.getResponseCode());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void getUnknownPath_returns404() throws Exception {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    int port = freePort();
    HttpServer server = PrometheusScrapeServer.createAndStartForTests(registry, port);
    try {
      URL url = new URL("http://127.0.0.1:" + port + "/actuator/health");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      assertEquals(404, conn.getResponseCode());
    } finally {
      server.stop(0);
    }
  }
}
