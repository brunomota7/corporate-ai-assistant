package br.com.api_core.modules.admin;

import br.com.api_core.domain.Document;
import br.com.api_core.domain.repository.DocumentRepository;
import br.com.api_core.modules.admin.dto.IngestRequestDTO;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AdminService {

    private final DocumentRepository documentRepository;

    public AdminService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void createIngest(IngestRequestDTO dto, float[] embedding) {

        Document document = new Document();
        document.setTitle(dto.title());
        document.setContent(dto.content());
        document.setSourceType(dto.sourceType());
        document.setSourceId(dto.sourceId());
        document.setEmbedding(embedding);
        document.setMetadata(dto.metadata() != null ? dto.metadata() : new HashMap<>());

        documentRepository.save(document);
    }
}
