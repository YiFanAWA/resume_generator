package com.daemonsets.resumeportal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.pdf-export")
public class PdfExportProperties {
    private DataSize maxOutputSize = DataSize.ofMegabytes(10);

    private long maxProfileCharacters = 50_000;
}
