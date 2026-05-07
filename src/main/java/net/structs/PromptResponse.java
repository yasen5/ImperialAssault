package net;

import java.io.Serializable;

public record PromptResponse(long promptId, String value) implements Serializable {
}
