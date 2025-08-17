package io.nitro.antlers.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    // Можно задавать либо путь в properties, либо через GOOGLE_APPLICATION_CREDENTIALS
    @Value("${firebase.credentials:}")
    private Resource serviceAccount;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        FirebaseOptions.Builder builder = FirebaseOptions.builder().setProjectId(projectId);

        if (serviceAccount != null && serviceAccount.exists()) {
            try (InputStream in = serviceAccount.getInputStream()) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
        } else {
            // Fallback на ADC (переменная окружения GOOGLE_APPLICATION_CREDENTIALS или gcloud auth)
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        FirebaseOptions options = builder.build();
        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}
