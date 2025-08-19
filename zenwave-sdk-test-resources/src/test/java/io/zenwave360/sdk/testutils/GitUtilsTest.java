package io.zenwave360.sdk.testutils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitUtilsTest {

    @TempDir
    Path tempDir;

    private Git git;
    private File moduleDir;
    private File testFile;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        // Initialize a git repository
        git = Git.init().setDirectory(tempDir.toFile()).call();

        // Create a module directory
        moduleDir = new File(tempDir.toFile(), "test-module");
        assertTrue(moduleDir.mkdir());

        // Create a test file in the module
        testFile = new File(moduleDir, "test-file.txt");
        Files.writeString(testFile.toPath(), "initial content");

        // Add and commit the file
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").setSign(false).call();

        // Create a tag
        git.tag().setName("v1.0.0").call();
    }

    @AfterEach
    void tearDown() {
        if (git != null) {
            git.close();
        }
    }

    @Test
    void hasModuleChangedSinceLastTag_noChanges() throws IOException, GitAPIException {
        // Test with no changes since tag
        File gitDir = new File(tempDir.toFile(), ".git");
        boolean hasChanged = GitUtils.hasModuleChangedSinceLastTag("test-module", gitDir);
        assertFalse(hasChanged, "Module should not have changes since last tag");
    }

    @Test
    void hasModuleChangedSinceLastTag_withChanges() throws IOException, GitAPIException {
        // Modify the file
        Files.writeString(testFile.toPath(), "modified content");

        // Add and commit the changes
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Modified test file").setSign(false).call();

        // Test with changes since tag
        File gitDir = new File(tempDir.toFile(), ".git");
        boolean hasChanged = GitUtils.hasModuleChangedSinceLastTag("test-module", gitDir);
        assertTrue(hasChanged, "Module should have changes since last tag");
    }

    @Test
    void hasModuleChangedSinceLastTag_newFile() throws IOException, GitAPIException {
        // Create a new file in the module
        File newFile = new File(moduleDir, "new-file.txt");
        Files.writeString(newFile.toPath(), "new file content");

        // Add and commit the new file
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Added new file").setSign(false).call();

        // Test with changes since tag
        File gitDir = new File(tempDir.toFile(), ".git");
        boolean hasChanged = GitUtils.hasModuleChangedSinceLastTag("test-module", gitDir);
        assertTrue(hasChanged, "Module should have changes since last tag when new file is added");
    }

    @Test
    void hasModuleChangedSinceLastTag_differentModule() throws IOException, GitAPIException {
        // Create a different module
        File otherModuleDir = new File(tempDir.toFile(), "other-module");
        assertTrue(otherModuleDir.mkdir());

        // Create a file in the other module
        File otherFile = new File(otherModuleDir, "other-file.txt");
        Files.writeString(otherFile.toPath(), "other content");

        // Add and commit the other module
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Added other module").setSign(false).call();

        // Test original module - should not have changes
        File gitDir = new File(tempDir.toFile(), ".git");
        boolean hasChanged = GitUtils.hasModuleChangedSinceLastTag("test-module", gitDir);
        assertFalse(hasChanged, "Original module should not have changes");

        // Test other module - should have changes
        boolean otherHasChanged = GitUtils.hasModuleChangedSinceLastTag("other-module", gitDir);
        assertTrue(otherHasChanged, "Other module should have changes since last tag");
    }
}
