package com.captain.springaichat.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.model.bedrock.autoconfigure.BedrockAwsConnectionProperties;
import org.springframework.ai.model.bedrock.titan.autoconfigure.BedrockTitanEmbeddingProperties;
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
     * Spring AI 1.0.0 uses ModelOptionsUtils.OBJECT_MAPPER (no NON_NULL inclusion), so
     * TitanEmbeddingRequest serialises both inputText and inputImage even when inputImage
     * is null. Titan Embed Text V2 rejects the unknown inputImage field with schema
     * violations. Providing this bean overrides the @ConditionalOnMissingBean in
     * BedrockTitanEmbeddingAutoConfiguration, using a NON_NULL ObjectMapper copy instead.
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
