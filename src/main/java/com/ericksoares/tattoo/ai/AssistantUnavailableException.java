package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * The model server (Ollama) or the embedding call behind the vector store is unreachable.
 * Expected wherever Ollama is not deployed (e.g. production on Render), so it is a 503,
 * not a 500.
 */
class AssistantUnavailableException extends BaseException {

    public AssistantUnavailableException() {
        super("AI assistant is unavailable: the model server cannot be reached", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
