package com.ai.github.agent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 本地Git操作封装，基于JGit（纯Java实现），无需本机安装git可执行文件。
 * push使用GitHub Token作为密码进行HTTPS认证，这是GitHub官方推荐的程序化推送方式。
 */
public class GitOperations {

    public Git initRepo(File dir) throws GitAPIException {
        return Git.init().setDirectory(dir).call();
    }

    public void ensureMainBranch(Git git) throws GitAPIException, IOException {
        String current = git.getRepository().getBranch();
        if (current != null && !current.equals("main")) {
            git.branchRename().setNewName("main").call();
        }
    }

    public void addAll(Git git) throws GitAPIException {
        git.add().addFilepattern(".").call();
    }

    public void commit(Git git, String message, String authorName, String authorEmail) throws GitAPIException {
        git.commit()
                .setMessage(message)
                .setAuthor(authorName, authorEmail)
                .setCommitter(authorName, authorEmail)
                .call();
    }

    public void addRemote(Git git, String name, String url) throws GitAPIException, URISyntaxException {
        git.remoteAdd()
                .setName(name)
                .setUri(new URIish(url))
                .call();
    }

    public void push(Git git, String username, String token) throws GitAPIException {
        git.push()
                .setRemote("origin")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                .add("main")
                .call();
    }

    public Git cloneRepo(String url, File dir, String username, String token) throws GitAPIException {
        return Git.cloneRepository()
                .setURI(url)
                .setDirectory(dir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                .call();
    }
}
