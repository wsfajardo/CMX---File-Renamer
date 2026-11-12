package com.cmx.filerename;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new FileRenamerApplication().run(args, System.out, System.err);
        System.exit(exitCode);
    }
}
