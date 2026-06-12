package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.UserProfile;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.logging.Level;

@Slf4j
@Service
public class PdfExportService {

    private static final Pattern PROFILE_TEMPLATE_STYLESHEET = Pattern.compile(
            "(?is)<link\\s+[^>]*profile-templates/[^>]*>"
    );
    private static final Pattern INLINE_STYLE_BLOCK = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final String CLASSPATH_STATIC_BASE_URI = "classpath:/static/";
    private static final String PDF_STYLESHEET_LOCATION = "classpath:/static/profile-templates/pdf.css";
    private static final String FALLBACK_PDF_STYLESHEET = """
            @page {
              size: A4;
              margin: 15mm;
            }

            html,
            body {
              margin: 0;
              padding: 0;
            }

            body {
              background: #ffffff;
              color: #222222;
              font-family: "ResumeCjk", "Noto Sans", Arial, sans-serif;
              font-size: 10.5pt;
              line-height: 1.45;
            }
            """;
    private static final List<String> CLASSPATH_CJK_FONT_LOCATIONS = Arrays.asList(
            "classpath:/fonts/NotoSansSC-VF.ttf",
            "classpath:/fonts/NotoSansSC-Regular.ttf",
            "classpath:/fonts/NotoSansCJKsc-Regular.ttf",
            "classpath:/fonts/NotoSansCJK-Regular.ttc",
            "classpath:/fonts/SourceHanSansSC-Regular.ttf",
            "classpath:/fonts/SourceHanSansCN-Regular.ttf",
            "classpath:/fonts/simhei.ttf",
            "classpath:/fonts/simsun.ttc",
            "classpath:/fonts/msyh.ttc"
    );

    static {
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
    }

    private final SpringTemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;
    private final MeterRegistry meterRegistry;

    @Autowired
    public PdfExportService(SpringTemplateEngine templateEngine, ResourceLoader resourceLoader, MeterRegistry meterRegistry) {
        this.templateEngine = templateEngine;
        this.resourceLoader = resourceLoader;
        this.meterRegistry = meterRegistry;
    }

    public byte[] generatePdf(UserProfile profile) throws IOException {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = "success";

        try {
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("currentUsersProfile", false);
            context.setVariable("userId", profile.getUserName());
            context.setVariable("userProfile", profile);

            String html = templateEngine.process(templateName(profile), context);
            return renderHtmlToPdf(prepareHtmlForPdf(html));
        } catch (IOException | RuntimeException exception) {
            status = "failure";
            throw exception;
        } finally {
            sample.stop(Timer.builder("resume.pdf.export.duration")
                    .description("Time spent rendering resume PDFs")
                    .tag("theme", String.valueOf(normalizedTheme(profile)))
                    .tag("status", status)
                    .register(meterRegistry));
        }
    }

    private byte[] renderHtmlToPdf(String html) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useProtocolsStreamImplementation(new ClasspathResourceStreamFactory(resourceLoader), "classpath");
        builder.withHtmlContent(html, CLASSPATH_STATIC_BASE_URI);
        builder.toStream(output);
        registerCjkFontIfAvailable(builder);

        try {
            builder.run();
        } catch (Exception exception) {
            throw new IOException("Failed to render resume HTML as PDF", exception);
        }

        return output.toByteArray();
    }

    private String templateName(UserProfile profile) {
        return "profile-templates/" + normalizedTheme(profile) + "/index";
    }

    private int normalizedTheme(UserProfile profile) {
        if (profile == null) {
            return 1;
        }
        int theme = profile.getTheme();
        if (theme < 1 || theme > 3) {
            theme = 1;
        }
        return theme;
    }

    private String prepareHtmlForPdf(String html) throws IOException {
        String normalized = html
                .replace("href=\"/profile-templates/", "href=\"profile-templates/")
                .replace("src=\"/profile-templates/", "src=\"profile-templates/")
                .replace("href=\"../profile-templates/", "href=\"profile-templates/")
                .replace("src=\"../profile-templates/", "src=\"profile-templates/")
                .replace("url(/profile-templates/", "url(profile-templates/")
                .replace("url(../profile-templates/", "url(profile-templates/");

        normalized = PROFILE_TEMPLATE_STYLESHEET.matcher(normalized).replaceAll("");
        normalized = INLINE_STYLE_BLOCK.matcher(normalized).replaceAll("");

        String pdfStyle = "<style>\n" + loadPdfStylesheet() + "\n</style>";
        if (normalized.toLowerCase(Locale.ROOT).contains("</head>")) {
            return normalized.replace("</head>", pdfStyle + "</head>");
        }
        return pdfStyle + normalized;
    }

    private void registerCjkFontIfAvailable(PdfRendererBuilder builder) {
        if (registerClasspathCjkFontIfAvailable(builder)) {
            return;
        }

        candidateSystemCjkFonts().stream()
                .filter(Files::exists)
                .findFirst()
                .map(Path::toFile)
                .ifPresent(font -> builder.useFont(font, "ResumeCjk"));
    }

    private String loadPdfStylesheet() throws IOException {
        Resource stylesheet = resourceLoader.getResource(PDF_STYLESHEET_LOCATION);
        if (!stylesheet.exists()) {
            log.warn("PDF stylesheet not found: {}. Falling back to built-in minimal PDF styles.",
                    PDF_STYLESHEET_LOCATION);
            return FALLBACK_PDF_STYLESHEET;
        }
        try (InputStream input = stylesheet.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.warn("Failed to read PDF stylesheet: {}. Falling back to built-in minimal PDF styles.",
                    PDF_STYLESHEET_LOCATION, exception);
            return FALLBACK_PDF_STYLESHEET;
        }
    }

    private boolean registerClasspathCjkFontIfAvailable(PdfRendererBuilder builder) {
        for (String location : CLASSPATH_CJK_FONT_LOCATIONS) {
            Resource font = resourceLoader.getResource(location);
            if (font.exists()) {
                builder.useFont(() -> openResourceStream(font), "ResumeCjk");
                return true;
            }
        }
        return false;
    }

    private InputStream openResourceStream(Resource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open classpath resource: " + resource, exception);
        }
    }

    private List<Path> candidateSystemCjkFonts() {
        String windowsDir = System.getenv().getOrDefault("WINDIR", "C:\\Windows");
        return Arrays.asList(
                Paths.get("/usr/share/opentype/noto/NotoSansCJK-Regular.ttc"),
                Paths.get("/usr/share/opentype/noto/NotoSansCJKsc-Regular.otf"),
                Paths.get("/usr/share/opentype/noto/NotoSansSC-Regular.otf"),
                Paths.get("/usr/share/opentype/source-han-sans/SourceHanSansSC-Regular.otf"),
                Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf"),
                Paths.get("/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf"),
                Paths.get("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),
                Paths.get("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"),
                Paths.get("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"),
                Paths.get("/usr/share/fonts/opentype/source-han-sans/SourceHanSansSC-Regular.otf"),
                Paths.get("/System/Library/Fonts/PingFang.ttc"),
                Paths.get("/System/Library/Fonts/Hiragino Sans GB.ttc"),
                Paths.get("/System/Library/Fonts/STHeiti Light.ttc"),
                Paths.get("/System/Library/Fonts/STHeiti Medium.ttc"),
                Paths.get("/System/Library/Fonts/Supplemental/Songti.ttc"),
                Paths.get("/Library/Fonts/NotoSansCJKsc-Regular.otf"),
                Paths.get(windowsDir, "Fonts", "simhei.ttf"),
                Paths.get(windowsDir, "Fonts", "simsun.ttc"),
                Paths.get(windowsDir, "Fonts", "msyh.ttc")
        );
    }

    private static class ClasspathResourceStreamFactory implements FSStreamFactory {
        private final ResourceLoader resourceLoader;

        private ClasspathResourceStreamFactory(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public FSStream getUrl(String url) {
            Resource resource = resourceLoader.getResource(toClasspathLocation(url));
            if (!resource.exists()) {
                return null;
            }
            return new ResourceFsStream(resource);
        }

        private String toClasspathLocation(String url) {
            if (url == null || url.isBlank()) {
                return "classpath:/";
            }

            String normalized = url.split("[?#]", 2)[0].replace("\\", "/");
            if (normalized.startsWith("classpath://")) {
                return "classpath:/" + normalized.substring("classpath://".length());
            }
            if (normalized.startsWith("classpath:")) {
                return normalized;
            }
            if (normalized.startsWith("/")) {
                return "classpath:" + normalized;
            }
            return "classpath:/" + normalized;
        }
    }

    private static class ResourceFsStream implements FSStream {
        private final Resource resource;

        private ResourceFsStream(Resource resource) {
            this.resource = resource;
        }

        @Override
        public InputStream getStream() {
            try {
                return resource.getInputStream();
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to open PDF resource: " + resource, exception);
            }
        }

        @Override
        public Reader getReader() {
            return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
        }
    }
}
