package com.cmx.filerename;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRenamerApplicationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void processesTargetFolderAndReturnsSuccess() throws IOException {
        Path target = Files.createDirectory(tempDirectory.resolve("downloads"));
        Path item = Files.createDirectory(target.resolve("item-001"));
        Files.createFile(item.resolve("download.bin"));
        Files.writeString(item.resolve("metadata.json"),
                "{\"files\":[{\"fileName\":\"final.pdf\"}]}" );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int exitCode = new FileRenamerApplication(new ObjectMapper(), tempDirectory)
                .run(new String[]{target.toString()}, new PrintStream(output),
                        new PrintStream(new ByteArrayOutputStream()));

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(Files.exists(item.resolve("final.pdf")));
        assertTrue(output.toString().contains("Renamed: 1"));
        assertTrue(Files.isDirectory(tempDirectory.resolve("log")));
    }

    @Test
    void rejectsMissingTargetArgument() throws IOException {
        int exitCode = new FileRenamerApplication(new ObjectMapper(), tempDirectory)
                .run(new String[0], new PrintStream(new ByteArrayOutputStream()),
                        new PrintStream(new ByteArrayOutputStream()));

        assertEquals(ExitCodes.INVALID_INPUT, exitCode);
    }
}
