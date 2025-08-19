package io.zenwave360.sdk.testutils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitUtils {
    public static boolean hasModuleChangedSinceLastTag(String modulePath) throws IOException, GitAPIException {
        return hasModuleChangedSinceLastTag(modulePath, new File(".git"));
    }
    public static boolean hasModuleChangedSinceLastTag(String modulePath, File gitDir) throws IOException, GitAPIException {
        // Open the Git repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build();
             Git git = new Git(repository)) {

            // Find the latest tag
            List<Ref> tags = git.tagList().call();
            if (tags.isEmpty()) {
                // No tags found, assume changes exist
                return true;
            }

            // Get the latest tag by commit date
            try (RevWalk revWalk = new RevWalk(repository)) {
                String latestTagCommitId = null;
                int latestCommitTime = 0;

                for (Ref tag : tags) {
                    try {
                        RevCommit tagCommit = revWalk.parseCommit(tag.getObjectId());
                        if (tagCommit.getCommitTime() > latestCommitTime) {
                            latestCommitTime = tagCommit.getCommitTime();
                            latestTagCommitId = tagCommit.getId().getName();
                        }
                    } catch (IOException e) {
                        // Skip invalid tags
                        continue;
                    }
                }

                if (latestTagCommitId == null) {
                    return true; // No valid tags found
                }

                // Get commits between latest tag and HEAD for the module path
                RevCommit latestTagCommit = revWalk.parseCommit(repository.resolve(latestTagCommitId));
                RevCommit headCommit = revWalk.parseCommit(repository.resolve("HEAD"));

                // Compare trees for the module path
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilterGroup.createFromStrings(modulePath));

                    // Add trees in order: tag first, then HEAD
                    treeWalk.addTree(latestTagCommit.getTree());
                    treeWalk.addTree(headCommit.getTree());

                    // Use DiffFormatter to detect actual content changes
                    org.eclipse.jgit.diff.DiffFormatter diffFormatter = new org.eclipse.jgit.diff.DiffFormatter(
                            org.eclipse.jgit.util.io.NullOutputStream.INSTANCE);
                    diffFormatter.setRepository(repository);
                    diffFormatter.setPathFilter(org.eclipse.jgit.treewalk.filter.PathFilterGroup.createFromStrings(modulePath));

                    // Get the diffs between the two trees
                    List<org.eclipse.jgit.diff.DiffEntry> diffs = diffFormatter.scan(latestTagCommit.getTree(), headCommit.getTree());

                    // If there are any diffs, the module has changed
                    return !diffs.isEmpty();
                }
            }
        }
    }
}
