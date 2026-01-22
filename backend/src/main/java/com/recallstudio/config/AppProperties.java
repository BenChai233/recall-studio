package com.recallstudio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String configDir = ".recall-studio";
    private String dataDir = "";
}
