package com.ticket.zhigong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String provider = "anthropic";
    private String apiKey;
    private String model = "claude-sonnet-4-20250514";
    private String baseUrl;
    private ProviderEndpoints providers = new ProviderEndpoints();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ProviderEndpoints getProviders() {
        return providers;
    }

    public void setProviders(ProviderEndpoints providers) {
        this.providers = providers;
    }

    public static class ProviderEndpoints {
        private NamedEndpoint anthropic = new NamedEndpoint();
        private NamedEndpoint openai = new NamedEndpoint();
        private NamedEndpoint deepseek = new NamedEndpoint();

        public NamedEndpoint getAnthropic() {
            return anthropic;
        }

        public void setAnthropic(NamedEndpoint anthropic) {
            this.anthropic = anthropic;
        }

        public NamedEndpoint getOpenai() {
            return openai;
        }

        public void setOpenai(NamedEndpoint openai) {
            this.openai = openai;
        }

        public NamedEndpoint getDeepseek() {
            return deepseek;
        }

        public void setDeepseek(NamedEndpoint deepseek) {
            this.deepseek = deepseek;
        }
    }

    public static class NamedEndpoint {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
