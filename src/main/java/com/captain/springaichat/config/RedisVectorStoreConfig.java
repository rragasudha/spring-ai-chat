package com.captain.springaichat.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {

    @Bean
    public RedisVectorStore vectorStore(JedisConnectionFactory jedisConnectionFactory,
                                        EmbeddingModel embeddingModel,
                                        RedisVectorStoreProperties props) {
        JedisPooled jedis = buildJedisPooled(jedisConnectionFactory);

        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(props.getIndexName())
                .prefix(props.getPrefix())
                .initializeSchema(props.isInitializeSchema())
                .metadataFields(RedisVectorStore.MetadataField.text("cache_answer"))
                .build();
    }

    private JedisPooled buildJedisPooled(JedisConnectionFactory factory) {
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(factory.isUseSsl())
                .clientName(factory.getClientName())
                .timeoutMillis(factory.getTimeout())
                .password(factory.getPassword())
                .build();

        return new JedisPooled(
                new HostAndPort(factory.getHostName(), factory.getPort()),
                clientConfig
        );
    }
}
