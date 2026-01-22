package com.recallstudio.exception;

public class GitRepoInvalidException extends AppException {
    public GitRepoInvalidException(String message) {
        super("GIT_REPO_INVALID", message, 400);
    }
}
