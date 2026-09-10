package com.nnp.kubernetes_integration.websockets;

import com.nnp.kubernetes_integration.configs.K8sIntgClientFactory;
import com.nnp.kubernetes_integration.dtos.NnpEnvironment;
import com.nnp.kubernetes_integration.services.NnpK8sSvc;
import com.nnp.kubernetes_integration.services.NnpService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private final NnpK8sSvc nnpK8sSvc;
    private final K8sIntgClientFactory k8sIntgClientFactory;
    private final Map<String, ExecWatch> execWatches = new ConcurrentHashMap<>();

    @Autowired
    public TerminalWebSocketHandler(NnpK8sSvc nnpK8sSvc, K8sIntgClientFactory k8sIntgClientFactory) {
        this.nnpK8sSvc = nnpK8sSvc;
        this.k8sIntgClientFactory = k8sIntgClientFactory;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // log.info("WebSocket connection established: {}", session.getId());

        String envName = getQueryParam(session, "envName");
        String podName = getQueryParam(session, "pod");
        String containerName = getQueryParam(session, "container");

        if (!StringUtils.hasText(envName) || !StringUtils.hasText(podName)) {
            session.sendMessage(new TextMessage("Error: envName and pod parameters are required"));
            session.close();
            return;
        }

        try {
            NnpEnvironment nnpEnvironment = nnpK8sSvc.getNnpEnvironment(envName);
            KubernetesClient k8sClient = k8sIntgClientFactory.getK8sClient(nnpEnvironment.adminK8sNsToken());
            // Execute shell in the pod
            ExecWatch execWatch = k8sClient.pods()
                    .inNamespace(nnpEnvironment.envNamespace())
                    .withName(podName)
                    .inContainer(containerName)
                    .redirectingInput()
                    .writingOutput(new WebSocketOutputStream(session))
                    .writingError(new WebSocketOutputStream(session))
                    .withTTY()
                    .usingListener(new TerminalListener(session))
                    .exec("/bin/sh", "-c", "TERM=xterm-256color; export TERM; [ -x /bin/bash ] && exec /bin/bash || exec /bin/sh");

            execWatches.put(session.getId(), execWatch);

        } catch (Exception e) {
            log.error("Error establishing exec connection", e);
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        OutputStream stdin = execWatches.get(session.getId()).getInput();
        if (Objects.isNull(stdin))
            return;
        stdin.write(message.getPayload().getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // log.info("WebSocket connection closed: {} - Status: {}", session.getId(), status);
        ExecWatch execWatch = execWatches.remove(session.getId());
        if (Objects.nonNull(execWatch)) {
            execWatch.close();
        }
    }
    private String getQueryParam(WebSocketSession session, String param) {
        if (session == null || param == null) {
            return null;
        }

        java.net.URI uri = session.getUri();
        if (uri == null) {
            return null;
        }

        String query = uri.getRawQuery();
        if (query == null) {
            return null;
        }

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);

            if (kv.length == 2 && param.equals(kv[0])) {
                return java.net.URLDecoder.decode(
                        kv[1],
                        StandardCharsets.UTF_8
                );
            }
        }

        return null;
    }
    private record TerminalListener(WebSocketSession session) implements ExecListener {
        private static String sanitizeForLog(Object value) {
            if (value == null) {
                return null;
            }

            return value.toString().replace("\r", "").replace("\n", "");
        }
        @Override
            public void onOpen() {
                // log.info("Exec connection opened for session: {}", session.getId());
            }

        @Override
        public void onFailure(Throwable t, Response response) {

            try {
                session.sendMessage(new TextMessage("Error: " + t.getMessage()));
                session.close();
            } catch (Exception e) {
                log.error("Error sending failure message {}", sanitizeForLog(e) );
            }
        }

            @Override
            public void onClose(int code, String reason) {
                // log.info("Exec connection closed for session: {} - Code: {}, Reason: {}",
                        // session.getId(), code, reason);
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) {
                    log.error("Error closing session", e);
                }
            }

        }

    private static class WebSocketOutputStream extends OutputStream {
        private final WebSocketSession session;

        public WebSocketOutputStream(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            if (session.isOpen()) {
                try {
                    String data = new String(b, off, len,StandardCharsets.UTF_8);
                    session.sendMessage(new TextMessage(data));
                } catch (Exception e) {
                    throw new java.io.IOException("Error sending message", e);
                }
            }
        }
    }

}