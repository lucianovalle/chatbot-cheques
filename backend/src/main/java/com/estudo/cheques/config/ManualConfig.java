package com.estudo.cheques.config;

import com.estudo.cheques.conversa.MemoriaPorSessao;
import com.estudo.cheques.service.AssistenteProduto;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import java.time.Duration;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta o caminho 3 na subida da API. A pergunta ainda não chegou.
 * 1. FileSystemDocumentLoader lê os .md da pasta docs.
 * 2. O texto é partido em pedaços.
 * 3. O nomic-embed-text transforma cada pedaço em números.
 * 4. Esses números ficam no InMemoryEmbeddingStore. Somem quando a API desliga.
 * 5. assistenteProduto junta o llama3.2, esses pedaços e a memória _manual.
 * Quem chama o assistente pronto é o ChatService.reply. O modelo escreve a frase e não calcula valor.
 */
@Configuration
public class ManualConfig {

    private static final Logger log = LoggerFactory.getLogger(ManualConfig.class);

    /** Modelo só de embedding. O llama3.2 não faz essa etapa. */
    @Bean
    EmbeddingModel embeddingModel(OllamaProperties ollama) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollama.baseUrl())
                .modelName(ollama.embeddingModel())
                .timeout(Duration.ofSeconds(ollama.timeoutSeconds()))
                .build();
    }

    /** Lê docs/ uma vez e guarda os pedaços. Some quando a API desliga. */
    @Bean
    EmbeddingStore<TextSegment> manuais(EmbeddingModel embeddingModel, DocsProperties docs) throws IOException {
        Path pasta = resolverPasta(docs.path());
        List<Document> documentos = FileSystemDocumentLoader.loadDocuments(pasta, new TextDocumentParser());
        if (documentos.isEmpty()) {
            throw new IllegalStateException("Nenhum manual encontrado em " + pasta);
        }
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.builder()
                // 800 caracteres por pedaço, com 100 de repetição para não cortar uma frase ao meio.
                .documentSplitter(DocumentSplitters.recursive(800, 100))
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build()
                .ingest(documentos);
        log.info("Manuais de produto carregados de {}: {} arquivo(s)", pasta, documentos.size());
        return store;
    }

    /** Busca os 3 pedaços mais parecidos com a pergunta e entrega ao modelo. */
    @Bean
    ContentRetriever manuaisRetriever(EmbeddingStore<TextSegment> manuais, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(manuais)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.0)
                .build();
    }

    /** A memória do manual usa o mesmo provedor. A chave da sessão termina em "_manual". */
    @Bean
    AssistenteProduto assistenteProduto(
            ChatModel chatModel,
            ContentRetriever manuaisRetriever,
            MemoriaPorSessao memorias) {
        return AiServices.builder(AssistenteProduto.class)
                .chatModel(chatModel)
                .contentRetriever(manuaisRetriever)
                .chatMemoryProvider(memorias)
                .build();
    }

    private static Path resolverPasta(String configurado) {
        Path informado = Path.of(configurado == null || configurado.isBlank() ? "../docs" : configurado);
        Path direto = informado.toAbsolutePath().normalize();
        if (Files.isDirectory(direto)) {
            return direto;
        }
        Path aPartirDoModulo = Path.of("").toAbsolutePath().resolve(informado).normalize();
        if (Files.isDirectory(aPartirDoModulo)) {
            return aPartirDoModulo;
        }
        throw new IllegalStateException("Pasta de manuais não encontrada: " + direto);
    }
}
