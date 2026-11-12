package com.cmx.filerename;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;

public final class FileRenamerApplication {
    private final ObjectMapper objectMapper;
    private final Path logBaseOverride;

    public FileRenamerApplication() {
        this(new ObjectMapper(), null);
    }

    FileRenamerApplication(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    FileRenamerApplication(ObjectMapper objectMapper, Path logBaseOverride) {
        this.objectMapper = objectMapper;
        this.logBaseOverride = logBaseOverride;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        Path logBase = logBaseOverride != null ? logBaseOverride : resolveLogBase();
        try (LogWriter log = LogWriter.create(logBase)) {
            if (args == null || args.length != 1) {
                String message = "Usage: java -jar file-renamer.jar <target-folder>";
                log.error(message);
                err.println(message);
                return ExitCodes.INVALID_INPUT;
            }

            Path targetFolder = Path.of(args[0]).toAbsolutePath().normalize();
            if (!Files.isDirectory(targetFolder, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isReadable(targetFolder)) {
                String message = "Target folder does not exist or is not accessible: " + targetFolder;
                log.error(message);
                err.println(message);
                return ExitCodes.INVALID_INPUT;
            }

            ProcessingSummary summary = new ProcessingSummary();
            FolderProcessor processor = new FolderProcessor(objectMapper);
            log.info("Processing started: " + targetFolder);
            out.println("Target folder: " + targetFolder);

            try (var stream = Files.list(targetFolder)) {
                for (Path folder : stream
                        .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    summary.scanned();
                    applyResult(summary, processor.process(folder, log));
                }
            }

            printSummary(summary, out, log);
            return summary.hasErrors() ? ExitCodes.PROCESSING_ERRORS : ExitCodes.SUCCESS;
        } catch (LogWriter.LogWriteException | IOException exception) {
            err.println("Fatal logging failure: " + exception.getMessage());
            return ExitCodes.FATAL_FAILURE;
        } catch (RuntimeException exception) {
            err.println("Fatal startup failure: " + exception.getMessage());
            return ExitCodes.FATAL_FAILURE;
        }
    }

    private void applyResult(ProcessingSummary summary, FolderProcessor.Result result) {
        switch (result) {
            case RENAMED -> summary.renamed();
            case ALREADY_CORRECT -> summary.alreadyCorrect();
            case SKIPPED -> summary.skipped();
            case FAILED -> summary.failed();
        }
    }

    private void printSummary(ProcessingSummary summary, PrintStream out, LogWriter log) {
        String text = System.lineSeparator()
                + "Summary" + System.lineSeparator()
                + "Scanned: " + summary.scannedCount() + System.lineSeparator()
                + "Renamed: " + summary.renamedCount() + System.lineSeparator()
                + "Already correct: " + summary.alreadyCorrectCount() + System.lineSeparator()
                + "Skipped: " + summary.skippedCount() + System.lineSeparator()
                + "Failed: " + summary.failedCount();
        out.println(text);
        log.info(text.replace(System.lineSeparator(), " | "));
    }

    private Path resolveLogBase() {
        try {
            var location = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codeLocation = Path.of(location);
            if (Files.isRegularFile(codeLocation)) {
                return codeLocation.getParent();
            }
        } catch (Exception ignored) {
            // Fall back to the working directory for development launches.
        }
        return Path.of(System.getProperty("user.dir"));
    }
}
