package net.technearts.maven.extresources;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
/**
 * Clones or updates a git repository 
 *
 */
@Mojo(name = "git")
public class GitResource extends AbstractMojo {
    /**
     * The source git repository.
     */
    @Parameter(property = "source", required = true)
    private URL source;
    /**
     * The target directory. Source files will be copied here.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "target", required = true)
    private File target;
    /**
     * The git branch to be copied.
     */
    @Parameter(defaultValue = "master", property = "branch", required = false)
    private String branch;
    /**
     * The server.id in settings.xml with credentials to git server.
     */
    @Parameter(property = "server", required = false)
    private String server;
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;
    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public void execute() throws MojoExecutionException {
        Git git = null;
        CredentialsProvider cp = getCredentialsProvider();
        try {
            if (!target.exists()) {
                getLog().info("Cloning " + source + " branch " + branch + " into " + target);
                target.mkdirs();
                CloneCommand clone = Git.cloneRepository()
                        .setURI(source.toURI().toString())
                        .setDirectory(target)
                        .setBranch(branch);
                if (cp != null) {
                    clone = clone.setCredentialsProvider(cp);
                }
                git = clone.call();
            } else {
                getLog().info("Pulling " + source + " branch " + branch + " into " + target);
                git = Git.init()
                        .setDirectory(target)
                        .call();
                git.remoteAdd().setUri(new URIish(source));
                git.checkout()
                        .setName(branch)
                        .call();
                PullCommand pull = git.pull();
                if (cp != null) {
                    pull = pull.setCredentialsProvider(cp);
                }
                pull.call();
            }
        } catch (TransportException e) {
            throw new MojoExecutionException(
                    "Error connecting to source. Did you set username and password in your <server> in settings.xml?",
                    e);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Error: <source> is not a valid URL", e);
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Error creating source " + source, e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    private CredentialsProvider getCredentialsProvider() {
        Optional<Server> optionalServer = settings.getServers().stream()
                .filter(s -> s.getId().equals(server)).findFirst();
        if (optionalServer.isPresent()) {
            return new UsernamePasswordCredentialsProvider(optionalServer.get().getUsername(),
                    settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(optionalServer.get())).getServer()
                            .getPassword());
        } else {
            return null;
        }
    }
}
