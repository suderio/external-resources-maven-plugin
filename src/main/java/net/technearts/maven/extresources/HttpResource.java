package net.technearts.maven.extresources;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 * Downloads a site
 *
 */
@Mojo(name = "http")
public class HttpResource extends AbstractMojo {
    private HashSet<String> links = new HashSet<>();
    /**
     * The source web site.
     */
    @Parameter(property = "source", required = true)
    private URL source;
    /**
     * The target directory. Source files will be copied here.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "target", required = true)
    private File target;
    /**
     * The depth. Zero means no depth limit.
     * Negative values will allow crawling outside the source host.
     * No depth limit outside the source host is not allowed.
     */
    @Parameter(defaultValue = "1", property = "depth", required = false)
    private Integer depth;
    /**
     * The server.id in settings.xml with credentials to the site.
     */
    @Parameter(property = "server", required = false)
    private String server;
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;
    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        crawl(source.toString(), 0);
    }

    private final void crawl(final String url, final int currentDepth) {
        if ((!links.contains(url) && allowedUrl(url, currentDepth))) {
            try {
                if (links.add(url)) {
                    getLog().info(currentDepth + ": " + url);
                }
                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");
                for (Element page : linksOnPage) {
                    crawl(page.attr("abs:href"), currentDepth + 1);
                }
            } catch (IOException e) {
                getLog().error("For '" + url + "': " + e.getMessage());
            }
        }
    }

    private final boolean allowedUrl(final String url, final int currentDepth) {
        try {
            return currentDepth < Math.abs(depth) 
                    && (depth < 0 || source.getHost().equals(new URL(url).getHost()));
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
