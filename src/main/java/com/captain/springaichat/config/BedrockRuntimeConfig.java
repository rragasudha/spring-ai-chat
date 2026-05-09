package com.captain.springaichat.config;

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

    /**
     * Provides a custom BedrockRuntimeClient with an interceptor that strips null
     * entries from additionalModelRequestFields before the request reaches Bedrock.
     *
     * Spring AI 1.0.0 serialises all ChatOptions fields (including frequencyPenalty
     * and presencePenalty) into additionalModelRequestFields without filtering nulls.
     * Nova Micro rejects these two unknown null fields with "2 schema violations".
     */
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
