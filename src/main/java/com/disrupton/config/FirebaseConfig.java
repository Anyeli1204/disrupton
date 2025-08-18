package com.disrupton.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.project.id:disrupton2025}")
    private String projectId;

    @Value("${firebase.project.storage.bucket:disrupton2025.appspot.com}")
    private String storageBucket;

    @Value("${firebase.service.account.file:firebase-service-account.json}")
    private String serviceAccountFile;

    @PostConstruct
    public void initializeFirebase() throws IOException {
        log.info("🚀 Inicializando Firebase Admin SDK...");
        log.info("📁 Proyecto ID: {}", projectId);
        log.info("🪣 Storage Bucket: {}", storageBucket);
        
        try {
            // Intentar cargar desde el archivo de recursos
            InputStream serviceAccount = getClass().getResourceAsStream("/" + serviceAccountFile);
            
            if (serviceAccount == null) {
                // Si no está en recursos, intentar desde el sistema de archivos
                log.info("🔍 Buscando archivo de credenciales en: {}", serviceAccountFile);
                serviceAccount = new FileInputStream("src/main/resources/" + serviceAccountFile);
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket(storageBucket)
                    .setProjectId(projectId)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK inicializado correctamente");
            } else {
                log.info("ℹ️ Firebase Admin SDK ya estaba inicializado");
            }
            
        } catch (IOException e) {
            log.error("❌ Error al inicializar Firebase Admin SDK: {}", e.getMessage());
            log.error("💡 Asegúrate de que el archivo {} existe en src/main/resources/", serviceAccountFile);
            throw e;
        }
    }

    @Bean
    public Firestore firestore() {
        log.info("📦 Exponiendo Firestore como bean");
        return FirestoreClient.getFirestore();
    }
} 