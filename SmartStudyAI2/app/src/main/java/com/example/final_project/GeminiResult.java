package com.example.final_project;


public abstract class GeminiResult {
    private GeminiResult() {}

    public static final class Success extends GeminiResult {
        private final String answer;

        public Success(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }
    }

    public static final class Error extends GeminiResult {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}