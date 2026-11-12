package com.cmx.filerename;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FolderProcessor {
    private final ObjectMapper objectMapper;

    public FolderProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result process(Path folder, LogWriter log) {
        List<Path> files;
        try {
            files = listRegularFiles(folder);
        } catch (IOException exception) {
            return error(log, folder, "Unable to inspect folder: " + exception.getMessage());
        }

        if (files.size() != 2) {
            return error(log, folder, "Expected exactly two regular files but found " + files.size());
        }

        List<Path> jsonFiles = files.stream()
                .filter(this::isJsonFile)
                .toList();
        if (jsonFiles.size() != 1) {
            return error(log, folder, "Expected exactly one JSON file but found " + jsonFiles.size());
        }

        Path jsonFile = jsonFiles.get(0);
        Path downloadedFile = files.stream()
                .filter(file -> !file.equals(jsonFile))
                .findFirst()
                .orElseThrow();

        String intendedFilename;
        try {
            intendedFilename = readFilename(jsonFile);
        } catch (IOException | IllegalArgumentException exception) {
            return error(log, folder, "Unable to read intended filename: " + exception.getMessage());
        }

        Path normalizedFolder = folder.toAbsolutePath().normalize();
        Path target = normalizedFolder.resolve(intendedFilename).normalize();
        if (!normalizedFolder.equals(target.getParent()) || containsSeparator(intendedFilename)) {
            return error(log, folder, "Intended filename is not a file name: " + intendedFilename);
        }

        Path normalizedDownloaded = downloadedFile.toAbsolutePath().normalize();
        try {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSameFile(normalizedDownloaded, target)) {
                    log.info(folder + ": already correct - " + intendedFilename);
                    return Result.ALREADY_CORRECT;
                }
                return error(log, folder, "Target filename already exists: " + intendedFilename);
            }

            Files.move(normalizedDownloaded, target);
            log.info(folder + ": renamed " + downloadedFile.getFileName() + " -> " + intendedFilename);
            return Result.RENAMED;
        } catch (IOException exception) {
            log.error(folder + ": Rename failed: " + exception.getMessage());
            return Result.FAILED;
        }
    }

    private List<Path> listRegularFiles(Path folder) throws IOException {
        try (var stream = Files.list(folder)) {
            return stream
                    .filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private boolean isJsonFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json");
    }

    private String readFilename(Path jsonFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonFile.toFile());
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("JSON root must be an object");
        }
        JsonNode filename = root.path("files").path(0).path("fileName");
        if (!filename.isTextual() || filename.asText().isBlank()) {
            throw new IllegalArgumentException("files[0].fileName is missing or empty");
        }
        return filename.asText();
    }

    private boolean containsSeparator(String filename) {
        return filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0;
    }

    private Result error(LogWriter log, Path folder, String message) {
        log.error(folder + ": " + message);
        return Result.SKIPPED;
    }

    public enum Result {
        RENAMED,
        ALREADY_CORRECT,
        SKIPPED,
        FAILED
    }
}
