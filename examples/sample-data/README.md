# Sample Input

This sanitized fixture demonstrates the supported folder layout:

- `rename-me` should be renamed successfully.
- `already-correct` should be left unchanged.
- `invalid-count` should be skipped because it contains more than two files.
- `missing-name` should be skipped because the filename value is missing.

The metadata uses the `files[0].fileName` string field to specify the intended
filename.

Run the utility against the parent folder, not an individual child folder.
