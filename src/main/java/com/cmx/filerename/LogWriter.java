package com.cmx.filerename;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LogWriter implements Closeable {
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final DateTimeFormatter LINE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final BufferedWriter writer;

    private LogWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public static LogWriter create(Path baseDirectory) throws IOException {
        Path logDirectory = baseDirectory.resolve("log");
        Files.createDirectories(logDirectory);
        String baseName = FILE_FORMAT.format(LocalDateTime.now());
        Path logFile = logDirectory.resolve(baseName + ".log");
        int suffix = 1;
        while (Files.exists(logFile)) {
            logFile = logDirectory.resolve(baseName + "_" + suffix++ + ".log");
        }
        BufferedWriter writer = Files.newBufferedWriter(
                logFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        return new LogWriter(writer);
    }

    public synchronized void info(String message) {
        write("INFO", message);
    }

    public synchronized void error(String message) {
        write("ERROR", message);
    }

    private void write(String level, String message) {
        try {
            writer.write(LINE_FORMAT.format(LocalDateTime.now()));
            writer.write(" [");
            writer.write(level);
            writer.write("] ");
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            throw new LogWriteException(exception);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public static final class LogWriteException extends RuntimeException {
        public LogWriteException(IOException cause) {
            super(cause);
        }
    }
}
