package com.graduation.autograding.config;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupAccessHelper {

    private static final Logger log = LoggerFactory.getLogger(StartupAccessHelper.class);

    private final Environment environment;

    @Value("${app.startup.open-browser:true}")
    private boolean openBrowser;

    @Value("${app.startup.path:/}")
    private String startupPath;

    public StartupAccessHelper(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String baseUrl = buildBaseUrl();
        String localUrl = baseUrl + normalizePath(startupPath);
        String lanUrl = buildLanUrl();

        log.info("System entry: {}", localUrl);
        if (lanUrl != null) {
            log.info("LAN access: {}", lanUrl + normalizePath(startupPath));
        }

        if (openBrowser) {
            openInBrowser(localUrl);
        }
    }

    private String buildBaseUrl() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String contextPath = normalizePath(environment.getProperty("server.servlet.context-path", ""));
        if ("/".equals(contextPath)) {
            contextPath = "";
        }
        return "http://localhost:" + port + contextPath;
    }

    private String buildLanUrl() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String contextPath = normalizePath(environment.getProperty("server.servlet.context-path", ""));
        if ("/".equals(contextPath)) {
            contextPath = "";
        }

        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return "http://" + hostAddress + ":" + port + contextPath;
        } catch (UnknownHostException ex) {
            log.debug("Failed to resolve LAN address", ex);
            return null;
        }
    }

    private void openInBrowser(String url) {
        if (GraphicsEnvironment.isHeadless()) {
            log.info("Headless environment detected. Browser was not opened automatically.");
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                log.info("Browser opened automatically.");
                return;
            }

            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (osName.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                log.info("Browser opened automatically.");
                return;
            }

            log.info("Desktop browsing is not supported. Open this URL manually: {}", url);
        } catch (IOException | URISyntaxException ex) {
            log.warn("Failed to open browser automatically. Open this URL manually: {}", url, ex);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
