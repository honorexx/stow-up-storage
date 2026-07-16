package br.com.stowup.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {
  private final Path uploads;

  public WebResourceConfig(@Value("${stowup.uploads.directory}") String directory) {
    this.uploads = Path.of(directory).toAbsolutePath().normalize();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/uploads/profiles/**")
        .addResourceLocations(uploads.resolve("profiles").toUri().toString());
    registry
        .addResourceHandler("/uploads/spaces/**")
        .addResourceLocations(uploads.resolve("spaces").toUri().toString());
  }
}
