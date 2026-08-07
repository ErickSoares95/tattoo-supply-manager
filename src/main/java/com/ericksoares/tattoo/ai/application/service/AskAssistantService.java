package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.ai.application.dto.AskResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The RAG endpoint: embeds the question implicitly (VectorStore does it),
 * retrieves the most similar products from pgvector, grounds the LLM prompt
 * on that retrieved context, and asks the LlmClient to answer.
 */
@Service
public class AskAssistantService {

    private static final int TOP_K = 4;

    private final VectorStore vectorStore;
    private final LlmClient llmClient;

    public AskAssistantService(VectorStore vectorStore, LlmClient llmClient) {
        this.vectorStore = vectorStore;
        this.llmClient = llmClient;
    }

    public AskResponse execute(String question) {

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .build()
        );

        if (results.isEmpty()) {
            return new AskResponse(
                    "No indexed products yet — ask an admin to run /assistant/reindex-catalog first.",
                    List.of()
            );
        }

        String context = results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a helpful assistant for a tattoo supply store. \
                Answer the customer's question using ONLY the product context below. \
                If the context doesn't contain the answer, say you don't know — don't make it up.

                Context:
                %s

                Question: %s
                """.formatted(context, question);

        String answer = llmClient.complete(prompt);

        List<String> sources = results.stream()
                .map(doc -> String.valueOf(doc.getMetadata().get("productName")))
                .toList();

        return new AskResponse(answer, sources);
    }
}
