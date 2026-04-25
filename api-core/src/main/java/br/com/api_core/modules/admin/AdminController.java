package br.com.api_core.modules.admin;

import br.com.api_core.client.AiServiceClient;
import br.com.api_core.domain.repository.DocumentRepository;
import br.com.api_core.modules.admin.dto.IngestRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AiServiceClient aiServiceClient;
    private final AdminService adminService;

    public AdminController(AiServiceClient aiServiceClient,
                           AdminService adminService) {
        this.aiServiceClient = aiServiceClient;
        this.adminService = adminService;
    }

    /**
     * Indexa um documento no pgvector para uso no RAG
     * Gera o embedding via ai-service
     */
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(
            @Valid @RequestBody IngestRequestDTO dto) {

        float[] embedding = aiServiceClient.embed(dto.content());
        adminService.createIngest(dto, embedding);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

}
