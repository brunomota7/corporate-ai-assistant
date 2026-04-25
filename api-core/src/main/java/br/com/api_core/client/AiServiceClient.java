package br.com.api_core.client;

import br.com.api_core.modules.chat.dto.MessageDTO;
import br.com.api_core.infra.exception.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.List;

@Component
public class AiServiceClient {

    private final WebClient aiServiceWebClient;

    public AiServiceClient(@Qualifier("aiServiceWebClient") WebClient aiServiceWebClient) {
        this.aiServiceWebClient = aiServiceWebClient;
    }

    /**
     * Busca chunks relevantes no pgvector.
     * Recebe a query do usuário, retorna lista de trechos similares.
     */
    public List<String> search(String query) {
        try {
            SearchResponse response = aiServiceWebClient.post()
                    .uri("/search")
                    .bodyValue(new SearchRequest(query))
                    .retrieve()
                    .bodyToMono(SearchResponse.class)
                    .block();

            return response != null ? response.chunks() : List.of();
         } catch (WebClientException e) {
            throw new AiServiceUnavailableException("ai-service unavailable during search" + e);
        }
    }

    /**
     * Envia histórico + contexto ao LLM.
     * Retorna a resposta do modelo e quantos tokens foram usados.
     */
    public ChatCompletion chat(List<MessageDTO> messages, List<String> context) {
        try {
            return aiServiceWebClient.post()
                    .uri("/chat")
                    .bodyValue(new ChatRequest(messages, context))
                    .retrieve()
                    .bodyToMono(ChatCompletion.class)
                    .block();
        } catch (WebClientException e) {
            throw new AiServiceUnavailableException("ai-service unavailable during chat" + e);
        }
    }

    // Records internos — apenas para comunicação com o ai-service
    // Não são expostos fora dessa classe
    record SearchRequest(String query) {}
    record SearchResponse(List<String> chunks) {}
    record ChatRequest(List<MessageDTO> messages, List<String> context) {}
    public record ChatCompletion(String answer, Integer tokensUsed) {}
}