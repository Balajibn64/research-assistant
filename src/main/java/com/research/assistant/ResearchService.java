package com.research.assistant;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResearchService {
	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
		this.webClient = webClientBuilder.build();
		this.objectMapper = objectMapper;
	}

	public String processContent(ResearchRequest request) {
		String prompt = buildPrompt(request);

		Map<String, Object> requestBody = Map.of("contents",
				new Object[] { Map.of("parts", new Object[] { Map.of("text", prompt) }) });

		String response = webClient.post().uri(geminiApiUrl + geminiApiKey).bodyValue(requestBody).retrieve()
				.bodyToMono(String.class).block();

		return extractTextFromResponse(response);
	}

	private String extractTextFromResponse(String response) {
		try {
			GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
			if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
				GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
				if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null
						&& !firstCandidate.getContent().getParts().isEmpty()) {
					return firstCandidate.getContent().getParts().get(0).getText();
				}
			}
			return "No content Found in response";
		} catch (Exception e) {
			return "Error parsing: " + e.getMessage();
		}
	}

	private String buildPrompt(ResearchRequest request) {
		StringBuilder prompt = new StringBuilder();
		switch (request.getOperation()) {
		case "summarize":
			prompt.append(
					"Summarize the following text into well-structured, clear, and concise points. Each point should appear on a new line with proper sentence structure. Highlight the key ideas, important features, and essential information in plain text without using any symbols or bullet points.\n\n");
			break;

		case "suggest":
			prompt.append(
					"Based on the following text, provide practical suggestions and improvements. Focus on enhancing clarity, structure, and overall quality. Give detailed explanations for each suggestion while preserving the original meaning. Present each suggestion on a new line in plain text format.\n\n");
			break;

		case "paraphrase":
			prompt.append(
					"Rewrite the following text in a more concise, clear, and natural way while maintaining the original meaning. Use simple language, proper sentence structure, and plain text without any special formatting symbols.\n\n");
			break;

		case "grammarCheck":
			prompt.append(
					"Proofread the following text for grammatical, spelling, and punctuation errors. Provide the corrected version with explanations for each correction where necessary. Use plain text without any symbols.\n\n");
			break;

		case "expand":
			prompt.append(
					"Expand the following brief text into a more detailed explanation while preserving clarity and the original meaning. Include necessary context, examples, and descriptions in plain text without using any formatting symbols.\n\n");
			break;
		default:
			throw new IllegalArgumentException("Unknown Operation :" + request.getOperation());
		}
		prompt.append(request.getContent());
		return prompt.toString();
	}
}
