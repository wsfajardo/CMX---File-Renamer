package com.cmx.filerename;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderProcessorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void renamesDownloadedFileUsingFirstFilenameValue() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-001"));
        Path downloaded = Files.createFile(folder.resolve("download.bin"));
        Files.writeString(folder.resolve("metadata.json"),
                "{\"files\":[{\"fileName\":[\"final.pdf\"]}]}" );

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.RENAMED,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }

        assertTrue(Files.exists(folder.resolve("final.pdf")));
        assertTrue(Files.notExists(downloaded));
    }

    @Test
    void skipsFolderWithMoreThanTwoFiles() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-002"));
        Files.createFile(folder.resolve("download.bin"));
        Files.createFile(folder.resolve("metadata.json"));
        Files.createFile(folder.resolve("extra.txt"));

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.SKIPPED,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }
    }

    @Test
    void treatsExistingTargetNameAsAlreadyCorrect() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-003"));
        Path downloaded = Files.createFile(folder.resolve("final.pdf"));
        Files.writeString(folder.resolve("metadata.json"),
                "{\"files\":[{\"fileName\":[\"final.pdf\"]}]}" );

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.ALREADY_CORRECT,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }

        assertTrue(Files.exists(downloaded));
    }

    @Test
    void skipsFolderWhenFilenameIsMissing() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-004"));
        Files.createFile(folder.resolve("download.bin"));
        Files.writeString(folder.resolve("metadata.json"), "{\"files\":[{}]}");

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.SKIPPED,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }
    }

    @Test
    void skipsFolderWhenTargetWouldEscapeFolder() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-005"));
        Files.createFile(folder.resolve("download.bin"));
        Files.writeString(folder.resolve("metadata.json"),
                "{\"files\":[{\"fileName\":[\"..\\\\outside.pdf\"]}]}" );

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.SKIPPED,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }
    }

    @Test
    void skipsFolderWhenTargetAlreadyBelongsToAnotherFile() throws IOException {
        Path folder = Files.createDirectory(tempDirectory.resolve("item-006"));
        Files.createFile(folder.resolve("download.bin"));
        Files.createDirectory(folder.resolve("final.pdf"));
        Files.writeString(folder.resolve("metadata.json"),
                "{\"files\":[{\"fileName\":[\"final.pdf\"]}]}" );

        try (LogWriter log = LogWriter.create(tempDirectory)) {
            assertEquals(FolderProcessor.Result.SKIPPED,
                    new FolderProcessor(new ObjectMapper()).process(folder, log));
        }
    }
}
