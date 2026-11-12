package com.cmx.filerename;

public final class ProcessingSummary {
    private int scanned;
    private int renamed;
    private int alreadyCorrect;
    private int skipped;
    private int failed;

    public void scanned() {
        scanned++;
    }

    public void renamed() {
        renamed++;
    }

    public void alreadyCorrect() {
        alreadyCorrect++;
    }

    public void skipped() {
        skipped++;
    }

    public void failed() {
        failed++;
    }

    public int scannedCount() {
        return scanned;
    }

    public int renamedCount() {
        return renamed;
    }

    public int alreadyCorrectCount() {
        return alreadyCorrect;
    }

    public int skippedCount() {
        return skipped;
    }

    public int failedCount() {
        return failed;
    }

    public boolean hasErrors() {
        return skipped > 0 || failed > 0;
    }
}
