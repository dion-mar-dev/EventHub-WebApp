package au.edu.rmit.sept.webapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebConfig
 * Configures static resource handlers for serving uploaded photos.
 *
 * PURPOSE:
 * Maps /uploads/** URL pattern to the local filesystem uploads directory,
 * allowing photos to be served directly via <img src="/uploads/events/123/photo.jpg">
 *
 * ACTIVE PROFILES:
 * - dev, devprod: Serves from local filesystem
 * - prod: This config is still active, but photos may be served from GCS URLs instead
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${photo.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded photos from local filesystem
        // Maps /uploads/** to file://{absolute-path}/uploads/
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}
