package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ApplicationConfig {
    public boolean headless = true;
    public List<GameConfig> games;

    public static ApplicationConfig fromFile(Path path) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(path.toFile(), ApplicationConfig.class);
    }
}
