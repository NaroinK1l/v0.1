package io.nitro.antlers.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials:}")
    private String credentialsLocation;

    @Value("${firebase.project-id:}")
    private String explicitProjectId;

    @Bean
    public Firestore firestore() throws Exception {
        synchronized (FirebaseConfig.class) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder builder = FirebaseOptions.builder();

                final String emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
                final boolean usingEmulator = emulatorHost != null && !emulatorHost.isBlank();

                // projectId: берём из проперти, потом из ENV, иначе дефолт
                String projectId = explicitProjectId;
                if (projectId == null || projectId.isBlank()) {
                    String envProject = System.getenv("GOOGLE_CLOUD_PROJECT");
                    projectId = (envProject != null && !envProject.isBlank()) ? envProject : "demo-project";
                }
                builder.setProjectId(projectId);

                if (usingEmulator) {
                    // ЭМУЛЯТОР: ключи не нужны — даём «фейковый» GoogleCredentials
                    GoogleCredentials fakeCreds = GoogleCredentials.create(
                            new AccessToken("owner", new Date(System.currentTimeMillis() + 3_600_000L))
                    );
                    builder.setCredentials(fakeCreds);
                } else {
                    // ПРОД: нужен сервис-аккаунт
                    if (credentialsLocation == null || credentialsLocation.isBlank()) {
                        throw new IllegalStateException(
                                "firebase.credentials не задан. Укажи ключ сервис-аккаунта " +
                                "(напр., firebase.credentials=classpath:firebase-key.json) " +
                                "или запускай в режиме эмулятора (FIRESTORE_EMULATOR_HOST)."
                        );
                    }
                    try (InputStream in = resolveInput(credentialsLocation)) {
                        builder.setCredentials(GoogleCredentials.fromStream(in));
                    }
                }

                FirebaseApp.initializeApp(builder.build());
            }
        }
        return FirestoreClient.getFirestore();
    }

    private InputStream resolveInput(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (in == null) throw new IllegalStateException("Не найден в classpath: " + path);
            return in;
        }
        if (location.startsWith("file:")) {
            return Files.newInputStream(Paths.get(location.substring("file:".length())));
        }
        return Files.newInputStream(Paths.get(location));
    }
}
