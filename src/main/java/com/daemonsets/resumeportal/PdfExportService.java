package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.UserProfile;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.logging.Level;

@Service
public class PdfExportService {

    private static final Pattern PROFILE_TEMPLATE_STYLESHEET = Pattern.compile(
            "(?is)<link\\s+[^>]*profile-templates/[^>]*>"
    );
    private static final Pattern INLINE_STYLE_BLOCK = Pattern.compile("(?is)<style[^>]*>.*?</style>");

    static {
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
    }

    private final SpringTemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;

    @Autowired
    public PdfExportService(SpringTemplateEngine templateEngine, ResourceLoader resourceLoader) {
        this.templateEngine = templateEngine;
        this.resourceLoader = resourceLoader;
    }

    public byte[] generatePdf(UserProfile profile) throws IOException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("currentUsersProfile", false);
        context.setVariable("userId", profile.getUserName());
        context.setVariable("userProfile", profile);

        String html = templateEngine.process(templateName(profile), context);
        return renderHtmlToPdf(prepareHtmlForPdf(html));
    }

    private byte[] renderHtmlToPdf(String html) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, staticBaseUri());
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
        int theme = profile.getTheme();
        if (theme < 1 || theme > 3) {
            theme = 1;
        }
        return "profile-templates/" + theme + "/index";
    }

    private String prepareHtmlForPdf(String html) {
        String normalized = html
                .replace("href=\"/profile-templates/", "href=\"profile-templates/")
                .replace("src=\"/profile-templates/", "src=\"profile-templates/")
                .replace("href=\"../profile-templates/", "href=\"profile-templates/")
                .replace("src=\"../profile-templates/", "src=\"profile-templates/")
                .replace("url(/profile-templates/", "url(profile-templates/")
                .replace("url(../profile-templates/", "url(profile-templates/");

        normalized = PROFILE_TEMPLATE_STYLESHEET.matcher(normalized).replaceAll("");
        normalized = INLINE_STYLE_BLOCK.matcher(normalized).replaceAll("");

        String pdfStyle = "<link rel=\"stylesheet\" href=\"profile-templates/pdf.css\"/>";
        return normalized.replace("</head>", pdfStyle + "</head>");
    }

    private String staticBaseUri() throws IOException {
        Resource staticRoot = resourceLoader.getResource("classpath:/static/");
        return staticRoot.getURL().toExternalForm();
    }

    private void registerCjkFontIfAvailable(PdfRendererBuilder builder) {
        String windowsDir = System.getenv().getOrDefault("WINDIR", "C:\\Windows");
        Arrays.asList(
                        Paths.get(windowsDir, "Fonts", "simhei.ttf"),
                        Paths.get(windowsDir, "Fonts", "simsun.ttc"),
                        Paths.get(windowsDir, "Fonts", "msyh.ttc")
                )
                .stream()
                .filter(Files::exists)
                .findFirst()
                .map(Path::toFile)
                .ifPresent(font -> builder.useFont(font, "ResumeCjk"));
    }
}
