# File Renamer

Command-line utility for renaming downloaded files using the filename stored
in their corresponding JSON metadata.

## Requirements

- Java 17
- Maven 3.9 or later

## Build

```text
mvn clean package
```

The executable artifact is created under `target/`.

## Run

```text
java -jar file-renamer-1.0.0.jar <target-folder>
```

The target folder must contain direct child folders. Each child folder must
contain exactly one JSON file and one downloaded file. The utility reads
`files[0].fileName` from the JSON metadata and renames the downloaded file.

Logs are written to a `log` folder beside the packaged JAR.
