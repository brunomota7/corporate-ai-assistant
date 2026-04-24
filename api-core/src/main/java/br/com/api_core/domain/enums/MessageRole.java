package br.com.api_core.domain.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MessageRole {

    @JsonProperty("user")
    USER,

    @JsonProperty("assistant")
    ASSISTANT
}
