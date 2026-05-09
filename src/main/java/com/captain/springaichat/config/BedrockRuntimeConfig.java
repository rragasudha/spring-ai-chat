package com.captain.springaichat.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.model.bedrock.autoconfigure.BedrockAwsConnectionProperties;
import org.springframework.ai.model.bedrock.titan.autoconfigure.BedrockTitanEmbeddingProperties;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class BedrockRuntimeConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(
            AwsCredentialsProvider credentialsProvider,
            AwsRegionProvider regionProvider) {
        return BedrockRuntimeClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(regionProvider.getRegion())
                .overrideConfiguration(c -> c.addExecutionInterceptor(new StripNullAdditionalFieldsInterceptor()))
                .build();
    }

    /**
     * Bean override: gives TitanEmbeddingBedrockApi a NON_NULL ObjectMapper so that
     * the null inputImage field is excluded from text embedding requests.
     * Acts as @ConditionalOnMissingBean override for BedrockTitanEmbeddingAutoConfiguration.
     */
    @Bean
    public TitanEmbeddingBedrockApi titanEmbeddingBedrockApi(
            AwsCredentialsProvider credentialsProvider,
            AwsRegionProvider regionProvider,
            BedrockTitanEmbeddingProperties embeddingProperties,
            BedrockAwsConnectionProperties connectionProperties,
            ObjectMapper objectMapper) {
        ObjectMapper nonNullMapper = objectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new TitanEmbeddingBedrockApi(
                embeddingProperties.getModel(),
                credentialsProvider,
                regionProvider.getRegion(),
                nonNullMapper,
                connectionProperties.getTimeout());
    }

    /**
     * Safety net: adds a Jackson MixIn to the Spring Boot ObjectMapper so that
     * TitanEmbeddingRequest.inputImage is never serialised as null, regardless of
     * which TitanEmbeddingBedrockApi bean instance is ultimately used.
     * Spring AI 1.0.0 serialises TitanEmbeddingRequest (record with inputText +
     * inputImage) without filtering nulls; Titan Embed Text V2 rejects inputImage:null
     * with "2 schema violations".
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer titanEmbeddingNonNullMixIn() {
        return builder -> builder.mixIn(
                TitanEmbeddingBedrockApi.TitanEmbeddingRequest.class,
                TitanEmbeddingRequestNonNullMixin.class);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class TitanEmbeddingRequestNonNullMixin {}

    static class StripNullAdditionalFieldsInterceptor implements ExecutionInterceptor {

        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
            SdkRequest request = context.request();
            if (!(request instanceof ConverseRequest cr)) return request;

            Document additionalFields = cr.additionalModelRequestFields();
            if (additionalFields == null) return request;

            Map<String, Document> cleaned = additionalFields.asMap().entrySet().stream()
                    .filter(e -> !e.getValue().isNull())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Document cleanedDoc = cleaned.isEmpty() ? null : Document.fromMap(cleaned);
            return cr.toBuilder().additionalModelRequestFields(cleanedDoc).build();
        }
    }
}
