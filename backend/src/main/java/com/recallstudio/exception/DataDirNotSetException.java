package com.recallstudio.exception;

public class DataDirNotSetException extends AppException {
    public DataDirNotSetException() {
        super("DATA_DIR_NOT_SET", "dataDir is not configured", 400);
    }
}
