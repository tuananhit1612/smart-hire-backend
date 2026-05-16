package com.smarthire.backend.core.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from .env file located in the project root
 */
@SuppressWarnings({"deprecation", "removal"})
public class EnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> envMap = new HashMap<>();
        
        try {
            // Look for .env file in the working directory
            File envFile = new File(".env");
            if (!envFile.exists()) {
                // Also check the classpath root
                envFile = new File("src/main/resources/.env");
            }
            
            if (envFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        
                        // Skip empty lines and comments
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        
                        // Parse key=value pairs
                        int equalsIndex = line.indexOf('=');
                        if (equalsIndex > 0) {
                            String key = line.substring(0, equalsIndex).trim();
                            String value = line.substring(equalsIndex + 1).trim();
                            
                            // Remove surrounding quotes if present
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            envMap.put(key, value);
                        }
                    }
                }
                
                // Add all env vars to the environment
                if (!envMap.isEmpty()) {
                    MapPropertySource source = new MapPropertySource("env-file", envMap);
                    environment.getPropertySources().addFirst(source);
                }
            }
        } catch (IOException e) {
            // .env file not found, use environment variables instead
            System.out.println("Warning: .env file not found. Using system environment variables.");
        }
    }
}
